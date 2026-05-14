"""
Views for reports app.
"""
import os
import re
import logging
from pathlib import Path
from django.conf import settings
from django.http import FileResponse, Http404
from django.utils import timezone
from rest_framework import status, generics
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
import requests as http_requests

from .models import (
    Report, ReportPermission, OrganizationReportPermission, Execution,
    ReportGroupMember, GroupUserPermission, GroupOrganizationPermission,
)
from .serializers import ReportSerializer, ReportPermissionSerializer, ExecutionSerializer

logger = logging.getLogger(__name__)


def _get_accessible_report_ids(user):
    """
    Return the set of report IDs the user can access across all
    permission layers: direct user grants, org grants, and group grants.
    Does NOT include public reports — callers combine this with is_public.
    """
    ids = set()

    # 1. Direct user permissions
    ids.update(
        ReportPermission.objects.filter(
            user=user, can_access=True
        ).values_list('report_id', flat=True)
    )

    # 2. Organization permissions
    if user.organization_id:
        ids.update(
            OrganizationReportPermission.objects.filter(
                organization=user.organization, can_access=True
            ).values_list('report_id', flat=True)
        )

    # 3. Group permissions (user-level + org-level)
    group_ids = set(
        GroupUserPermission.objects.filter(
            user=user, can_access=True
        ).values_list('group_id', flat=True)
    )
    if user.organization_id:
        group_ids.update(
            GroupOrganizationPermission.objects.filter(
                organization=user.organization, can_access=True
            ).values_list('group_id', flat=True)
        )
    if group_ids:
        ids.update(
            ReportGroupMember.objects.filter(
                group_id__in=group_ids
            ).values_list('report_id', flat=True)
        )

    return ids


class OpenAPISpecView(APIView):
    """
    GET /api/reports/openapi.json — serve the OpenAPI 3.0 spec.
    Public endpoint (no auth required) so the Swagger UI can load it.
    """
    permission_classes = []
    authentication_classes = []

    def get(self, request):
        from .openapi_spec import OPENAPI_SPEC
        return Response(OPENAPI_SPEC)


class ReportListView(generics.ListAPIView):
    """
    List reports visible to the current user.
    Admin sees all; others see public reports and reports they have permission for.
    """
    serializer_class = ReportSerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        if user.is_admin:
            return Report.objects.all().order_by('-created_at')

        permitted_report_ids = _get_accessible_report_ids(user)

        from django.db.models import Q
        return Report.objects.filter(
            Q(id__in=permitted_report_ids) | Q(is_public=True)
        ).order_by('-created_at')


class ReportDetailView(generics.RetrieveAPIView):
    """Get a specific report."""
    serializer_class = ReportSerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        if user.is_admin:
            return Report.objects.all()

        permitted_report_ids = _get_accessible_report_ids(user)

        from django.db.models import Q
        return Report.objects.filter(
            Q(id__in=permitted_report_ids) | Q(is_public=True)
        )


def _resolve_env_vars(value: str) -> str:
    """
    Replace ${ENV_VAR} placeholders in a string with values from os.environ.
    Raises KeyError if an env var is not set.
    """
    def _replacer(match):
        var_name = match.group(1)
        env_val = os.environ.get(var_name)
        if env_val is None:
            raise KeyError(
                f"Environment variable '{var_name}' referenced in "
                f"datasource YAML is not set."
            )
        return env_val
    return re.sub(r'\$\{([^}]+)\}', _replacer, value)


def ensure_local_datasource_yaml(report, user):
    """
    Download the datasource .yaml/.yml file from the GitHub repository
    folder that contains the report.  Resolve ${ENV_VAR} placeholders and
    return the parsed YAML dict.

    Raises Exception if no YAML file is found.
    """
    import yaml

    repo = report.repo

    from github_integration.models import GitHubToken
    github_token = None
    for u in [user, repo.created_by]:
        if u:
            try:
                github_token = GitHubToken.objects.get(user=u)
                break
            except GitHubToken.DoesNotExist:
                continue

    if not github_token:
        raise Exception("No GitHub token available. Please connect GitHub first.")

    gh_headers = {
        'Authorization': f'token {github_token.access_token}',
        'Accept': 'application/vnd.github.v3+json',
    }

    folder_path = report.path.strip('/')
    contents_url = (
        f'https://api.github.com/repos/{repo.owner}/{repo.name}'
        f'/contents/{folder_path}'
    )
    gh_response = http_requests.get(
        contents_url,
        headers=gh_headers,
        params={'ref': repo.branch},
        timeout=30,
    )

    if gh_response.status_code != 200:
        raise Exception(
            f"Failed to fetch report folder from GitHub: {gh_response.status_code}"
        )

    folder_contents = gh_response.json()

    yaml_file = None
    for item in folder_contents:
        if (
            item.get('type') == 'file'
            and item.get('name', '').lower().endswith(('.yaml', '.yml'))
        ):
            yaml_file = item
            break

    if not yaml_file:
        raise Exception(
            f"No datasource YAML file found in report folder: {folder_path}. "
            f"Each report must have a .yaml/.yml datasource configuration file."
        )

    download_url = yaml_file.get('download_url')
    if not download_url:
        raise Exception("Could not get download URL for datasource YAML file")

    yaml_response = http_requests.get(download_url, headers=gh_headers, timeout=30)
    if yaml_response.status_code != 200:
        raise Exception(
            f"Failed to download datasource YAML: {yaml_response.status_code}"
        )

    # Save locally (useful for debugging / caching)
    local_dir = Path(settings.MEDIA_ROOT) / 'reports' / str(report.id) / 'source'
    local_dir.mkdir(parents=True, exist_ok=True)
    local_yaml_path = local_dir / yaml_file['name']
    local_yaml_path.write_bytes(yaml_response.content)

    # Resolve env-var placeholders in the raw text, then parse YAML
    raw_text = yaml_response.content.decode('utf-8')
    resolved_text = _resolve_env_vars(raw_text)
    datasource_config = yaml.safe_load(resolved_text)

    return datasource_config


def _resolve_yaml_datasource(report, user):
    """
    Download and parse the datasource YAML for a report and return a dict
    of data-source fields the Java ReportService understands.
    """
    ds = ensure_local_datasource_yaml(report, user)
    ds_type = (ds.get('type') or '').lower()
    config = ds.get('config') or {}
    
    # Delegate non-JDBC connections to the UI-configured Data Adapters
    if ds_type != 'jdbc':
        adapter_name = ds.get('name')
        if not adapter_name:
            raise Exception("Non-JDBC data source YAML must provide a 'name' to match a configured Data Adapter.")
            
        from dataadapter.models import DataAdapter
        # Use filter().first() to avoid MultipleObjectsReturned when
        # duplicate adapter names exist (e.g. comma-separated names in YAML).
        adapter = (
            DataAdapter.objects.filter(name=adapter_name, is_active=True).first()
            or DataAdapter.objects.filter(name=adapter_name).first()
        )
        if not adapter:
            raise Exception(f"Configured Data Adapter '{adapter_name}' not found. Please create it in the Data Adapters page.")
        ds_type = adapter.adapter_type.lower()
        config = adapter.connection_details

    payload = {}

    if ds_type == 'jdbc':
        payload['dataSourceType'] = 'jdbc'
        payload['jdbcUrl'] = config.get('url', '')
        payload['jdbcUser'] = config.get('username', '')
        payload['jdbcPassword'] = config.get('password', '')
    elif ds_type == 'csv':
        payload['dataSourceType'] = 'csv'
        payload['csvFilePath'] = config.get('file_path', '')
    elif ds_type == 'json':
        payload['dataSourceType'] = 'json'
        payload['jsonFilePath'] = config.get('file_path', '')
    elif ds_type == 'xml':
        payload['dataSourceType'] = 'xml'
        payload['xmlFilePath'] = config.get('file_path', '')
        if config.get('record_path'):
            payload['xmlRecordPath'] = config['record_path']
    elif ds_type in ('inmemory', 'mock'):
        payload['dataSourceType'] = 'inmemory'
    else:
        raise Exception(
            f"Unknown datasource type '{ds_type}' in YAML. "
            f"Supported: jdbc, csv, json, xml, inmemory, mock."
        )

    return payload


def ensure_local_jrxml(report, user):
    """
    Download the .jrxml file from the GitHub repository associated with the report.
    Returns the local absolute path to the downloaded file.
    """
    repo = report.repo
    
    from github_integration.models import GitHubToken
    github_token = None
    for u in [user, repo.created_by]:
        if u:
            try:
                github_token = GitHubToken.objects.get(user=u)
                break
            except GitHubToken.DoesNotExist:
                continue
    
    if not github_token:
        raise Exception("No GitHub token available. Please connect GitHub first.")
    
    gh_headers = {
        'Authorization': f'token {github_token.access_token}',
        'Accept': 'application/vnd.github.v3+json',
    }
    
    folder_path = report.path.strip('/')
    contents_url = f'https://api.github.com/repos/{repo.owner}/{repo.name}/contents/{folder_path}'
    gh_response = http_requests.get(
        contents_url,
        headers=gh_headers,
        params={'ref': repo.branch},
        timeout=30
    )
    
    if gh_response.status_code != 200:
        raise Exception(f"Failed to fetch report folder from GitHub: {gh_response.status_code}")
    
    folder_contents = gh_response.json()
    
    # Collect ALL .jrxml files (main report + subreports)
    jrxml_files = [
        item for item in folder_contents
        if item.get('type') == 'file' and item.get('name', '').endswith('.jrxml')
    ]
    
    if not jrxml_files:
        raise Exception(f"No .jrxml file found in report folder: {folder_path}")
    
    local_dir = Path(settings.MEDIA_ROOT) / 'reports' / str(report.id) / 'source'
    local_dir.mkdir(parents=True, exist_ok=True)
    
    main_jrxml_path = None
    for jrxml_item in jrxml_files:
        download_url = jrxml_item.get('download_url')
        if not download_url:
            continue
        
        jrxml_response = http_requests.get(download_url, headers=gh_headers, timeout=30)
        if jrxml_response.status_code != 200:
            logger.warning(f"Failed to download {jrxml_item['name']}: {jrxml_response.status_code}")
            continue
        
        local_path = local_dir / jrxml_item['name']
        local_path.write_bytes(jrxml_response.content)
        
        # Heuristic: the main report file is the one whose name doesn't
        # contain 'sub' (case-insensitive), or simply the first file.
        name_lower = jrxml_item['name'].lower()
        if main_jrxml_path is None or 'sub' not in name_lower:
            main_jrxml_path = local_path
    
    if main_jrxml_path is None:
        raise Exception("Could not download any .jrxml files from report folder")
    
    return str(main_jrxml_path)


def _populate_report_metadata_cache(report, user):
    """
    Fetch metadata from the Java MetadataService and store a compact
    summary in report.metadata_cache.  Returns True on success.

    This is intentionally fail-safe: if the Java service is down or the
    JRXML cannot be parsed, we log a warning and return False — the
    chatbot will simply not have metadata for that report.
    """
    try:
        local_jrxml_path = ensure_local_jrxml(report, user)

        metadata_url = f"{settings.COMPILER_SERVICE_URL}/api/metadata/extract"
        payload = {'sourceType': 'LOCAL', 'path': local_jrxml_path}

        response = http_requests.post(metadata_url, json=payload, timeout=60)
        if response.status_code != 200:
            logger.warning(
                f"Metadata extraction returned {response.status_code} "
                f"for report {report.id}: {response.text[:200]}"
            )
            return False

        data = response.json()

        # Build compact summary
        param_names = [
            p['name'] for p in (data.get('parameters') or [])
        ]
        field_names = [
            f['name'] for f in (data.get('fields') or [])
        ]
        chart_types = data.get('charts') or []
        sub_reports = data.get('subReports') or []

        # Build a natural-language summary for the LLM
        parts = []
        display = report.display_name or report.report_name
        parts.append(f"{display} report")
        if data.get('queryLanguage'):
            parts.append(f"uses {data['queryLanguage'].upper()} queries")
        if data.get('dataAdapterName'):
            parts.append(f"data source: {data['dataAdapterName']}")
        if chart_types:
            parts.append(f"contains {', '.join(chart_types)}")
        if param_names:
            parts.append(
                f"filterable by {', '.join(param_names[:5])}"
                + (" and more" if len(param_names) > 5 else "")
            )
        if sub_reports:
            parts.append(f"has {len(sub_reports)} sub-report(s)")

        summary = ". ".join(parts) + "."

        report.metadata_cache = {
            'report_name': data.get('reportName', report.report_name),
            'query_language': data.get('queryLanguage'),
            'data_adapter': data.get('dataAdapterName'),
            'parameter_names': param_names,
            'field_names': field_names[:20],  # cap for size
            'chart_types': chart_types,
            'has_subreports': bool(sub_reports),
            'band_count': len(data.get('bands') or []),
            'summary': summary,
        }
        report.metadata_cached_at = timezone.now()
        report.save(update_fields=['metadata_cache', 'metadata_cached_at'])
        logger.info(f"Metadata cache populated for report {report.id}")
        return True

    except http_requests.exceptions.ConnectionError:
        logger.warning(
            f"Java service unavailable — skipped metadata cache for report {report.id}"
        )
        return False
    except Exception as e:
        logger.warning(
            f"Metadata cache population failed for report {report.id}: {e}"
        )
        return False

class ReportCompileView(APIView):
    """
    Compile and run a report by proxying to the Java compiler service.
    Sends the .jrxml path and JDBC credentials to the Java service,
    receives PDF bytes back, and stores them for download.
    """
    permission_classes = [IsAuthenticated]
    
    def post(self, request, pk):
        user = request.user
        
        # Check permission
        try:
            report = Report.objects.get(pk=pk)
        except Report.DoesNotExist:
            return Response(
                {'error': 'Report not found'},
                status=status.HTTP_404_NOT_FOUND
            )
        
        # Check if user has access (admin always can)
        if not user.is_admin:
            permitted_ids = _get_accessible_report_ids(user)
            has_permission = report.is_public or report.id in permitted_ids
            if not has_permission:
                return Response(
                    {'error': 'You do not have permission to run this report'},
                    status=status.HTTP_403_FORBIDDEN
                )
        
        # Create execution record
        export_format = request.data.get('format', 'PDF').upper()
        VALID_FORMATS = {'PDF', 'XLSX', 'CSV', 'HTML', 'DOCX', 'PPTX', 'ODT'}
        if export_format not in VALID_FORMATS:
            return Response(
                {'error': f'Unsupported format: {export_format}. Supported: {sorted(VALID_FORMATS)}'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        FORMAT_EXTENSIONS = {
            'PDF': 'pdf', 'XLSX': 'xlsx', 'CSV': 'csv', 'HTML': 'html',
            'DOCX': 'docx', 'PPTX': 'pptx', 'ODT': 'odt',
        }
        file_ext = FORMAT_EXTENSIONS[export_format]
        
        execution = Execution.objects.create(
            report=report,
            invoked_by=user,
            status='running',
            output_format=file_ext,
        )
        execution.started_at = timezone.now()
        execution.save()
        
        try:
            # ── Step 1: Download .jrxml from GitHub ──────────────────────
            local_jrxml_path = ensure_local_jrxml(report, user)
            logger.info(f"Downloaded .jrxml to {local_jrxml_path}")
            
            # ── Step 2: Build compiler payload ───────────────────────────
            # Prepare output path — Java service writes PDF here
            output_dir = Path(settings.MEDIA_ROOT) / 'reports' / str(report.id)
            output_dir.mkdir(parents=True, exist_ok=True)
            output_filename = f"{execution.id}.{file_ext}"
            java_output_path = str(output_dir / output_filename)

            compiler_payload = {
                'sourceType': 'LOCAL',
                'path': str(local_jrxml_path),
                'format': export_format,
                'outputPath': java_output_path,
            }

            # Include user-supplied parameters if provided
            user_params = request.data.get('parameters', {})
            if user_params:
                compiler_payload['parameters'] = user_params

            # Inject data source credentials from report's YAML file
            adapter_payload = _resolve_yaml_datasource(report, user)
            compiler_payload.update(adapter_payload)
            
            # Call Java compiler service
            compiler_url = f"{settings.COMPILER_SERVICE_URL}/api/reports/generate"
            logger.info(f"Calling compiler service at {compiler_url}")
            
            response = http_requests.post(
                compiler_url,
                json=compiler_payload,
                timeout=120
            )
            
            if response.status_code != 200:
                raise Exception(f"Compiler service returned {response.status_code}: {response.text}")
            
            # Check if response is binary content or a text message
            content_type = response.headers.get('Content-Type', '')
            
            if 'application/' in content_type and 'json' not in content_type:
                # Java returned the file inline
                file_bytes = response.content
                with open(java_output_path, 'wb') as f:
                    f.write(file_bytes)
            else:
                # Java wrote the file to outputPath and returned a text message
                result_text = response.text.strip()
                if result_text.startswith('Error'):
                    raise Exception(result_text)
                
                # Verify the output was written to disk
                if not Path(java_output_path).exists():
                    raise Exception(f"Java service reported success but output not found at {java_output_path}")
            
            # Update execution record
            execution.status = 'success'
            execution.finished_at = timezone.now()
            execution.output_location = f"/media/reports/{report.id}/{output_filename}"
            execution.save()

            # Populate metadata cache as a side-effect on first successful compile
            if not report.metadata_cache:
                _populate_report_metadata_cache(report, user)
            
            return Response({
                'execution_id': execution.id,
                'status': execution.status,
                'output_url': f"/api/reports/{report.id}/download/{execution.id}/",
                'message': f'{export_format} report compiled successfully',
                'format': file_ext,
            })
            
        except http_requests.exceptions.ConnectionError:
            execution.status = 'failure'
            execution.finished_at = timezone.now()
            execution.save()
            # Send failure notification email (background thread)
            from .notifications import send_failure_notification
            send_failure_notification(
                report, execution,
                'Compiler service is not available. Please ensure the Java service is running.',
                user,
            )
            return Response(
                {'error': 'Compiler service is not available. Please ensure the Java service is running.'},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        except Exception as e:
            logger.error(f"Report compilation failed: {e}")
            execution.status = 'failure'
            execution.finished_at = timezone.now()
            execution.save()
            # Send failure notification email (background thread)
            from .notifications import send_failure_notification
            send_failure_notification(report, execution, str(e), user)
            return Response(
                {'error': f'Report compilation failed: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class ReportDownloadView(APIView):
    """
    Download the compiled report PDF.
    Serves the generated PDF from the execution record.
    """
    permission_classes = [IsAuthenticated]
    
    def get(self, request, pk, execution_id=None):
        try:
            report = Report.objects.get(pk=pk)
        except Report.DoesNotExist:
            return Response(
                {'error': 'Report not found'},
                status=status.HTTP_404_NOT_FOUND
            )
        
        # Find the execution
        if execution_id:
            try:
                execution = Execution.objects.get(id=execution_id, report=report)
            except Execution.DoesNotExist:
                return Response(
                    {'error': 'Execution not found'},
                    status=status.HTTP_404_NOT_FOUND
                )
        else:
            # Get latest successful execution
            execution = Execution.objects.filter(
                report=report,
                status='success'
            ).order_by('-created_at').first()
            
            if not execution:
                return Response(
                    {'error': 'No compiled report available'},
                    status=status.HTTP_404_NOT_FOUND
                )
        
        # Serve the output file
        MIME_TYPES = {
            'pdf': 'application/pdf',
            'xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'csv': 'text/csv',
            'html': 'text/html',
            'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'pptx': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            'odt': 'application/vnd.oasis.opendocument.text',
        }
        
        fmt = getattr(execution, 'output_format', 'pdf') or 'pdf'
        content_type = MIME_TYPES.get(fmt, 'application/octet-stream')
        
        if execution.output_location:
            # output_location is like /media/reports/{id}/{exec_id}.{ext}
            # Convert to filesystem path
            relative_path = execution.output_location.replace('/media/', '', 1)
            file_path = Path(settings.MEDIA_ROOT) / relative_path
            
            if file_path.exists():
                return FileResponse(
                    open(file_path, 'rb'),
                    content_type=content_type,
                    as_attachment=(fmt != 'pdf' and fmt != 'html'),
                    filename=f'{report.report_name}.{fmt}'
                )
        
        return Response(
            {'error': 'Report output file not found'},
            status=status.HTTP_404_NOT_FOUND
        )


class ReportPermissionListView(generics.ListCreateAPIView):
    """List and create report permissions (admin only)."""
    serializer_class = ReportPermissionSerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        return ReportPermission.objects.all().order_by('report__report_name', 'user__username')
    
    def list(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().list(request, *args, **kwargs)
    
    def create(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().create(request, *args, **kwargs)


class ReportPermissionDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Get, update, or delete a specific permission (admin only)."""
    serializer_class = ReportPermissionSerializer
    permission_classes = [IsAuthenticated]
    queryset = ReportPermission.objects.all()
    
    def retrieve(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().retrieve(request, *args, **kwargs)
    
    def update(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().update(request, *args, **kwargs)
    
    def destroy(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().destroy(request, *args, **kwargs)


class ExecutionListView(generics.ListAPIView):
    """List executions for the current user."""
    serializer_class = ExecutionSerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        if user.is_admin:
            return Execution.objects.all()
        return Execution.objects.filter(invoked_by=user)


class ExecutionDetailView(generics.RetrieveAPIView):
    """Get a specific execution."""
    serializer_class = ExecutionSerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        if user.is_admin:
            return Execution.objects.all()
        return Execution.objects.filter(invoked_by=user)


class ReportMetadataView(APIView):
    """
    Proxy to KlexReportingService metadata extraction.
    Downloads the JRXML from GitHub (same as compile) and
    forwards to Java POST /api/metadata/extract.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        try:
            report = Report.objects.get(pk=pk)
        except Report.DoesNotExist:
            return Response({'error': 'Report not found'}, status=status.HTTP_404_NOT_FOUND)

        try:
            local_jrxml_path = ensure_local_jrxml(report, request.user)

            metadata_url = f"{settings.COMPILER_SERVICE_URL}/api/metadata/extract"
            payload = {
                'sourceType': 'LOCAL',
                'path': local_jrxml_path,
            }

            response = http_requests.post(metadata_url, json=payload, timeout=60)

            if response.status_code != 200:
                raise Exception(f"Metadata service returned {response.status_code}: {response.text}")

            # Cache a compact summary as a side-effect
            metadata_json = response.json()
            try:
                self._cache_metadata(report, metadata_json)
            except Exception as cache_err:
                logger.warning(f"Metadata cache side-effect failed: {cache_err}")

            return Response(metadata_json)

        except http_requests.exceptions.ConnectionError:
            return Response(
                {'error': 'Metadata service is not available. Please ensure the Java service is running.'},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        except Exception as e:
            logger.error(f"Metadata extraction failed: {e}")
            return Response(
                {'error': f'Metadata extraction failed: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @staticmethod
    def _cache_metadata(report, data):
        """Distil full metadata response into a compact cache on the Report."""
        param_names = [p['name'] for p in (data.get('parameters') or [])]
        field_names = [f['name'] for f in (data.get('fields') or [])]
        chart_types = data.get('charts') or []
        sub_reports = data.get('subReports') or []

        parts = []
        display = report.display_name or report.report_name
        parts.append(f"{display} report")
        if data.get('queryLanguage'):
            parts.append(f"uses {data['queryLanguage'].upper()} queries")
        if data.get('dataAdapterName'):
            parts.append(f"data source: {data['dataAdapterName']}")
        if chart_types:
            parts.append(f"contains {', '.join(chart_types)}")
        if param_names:
            parts.append(
                f"filterable by {', '.join(param_names[:5])}"
                + (" and more" if len(param_names) > 5 else "")
            )
        if sub_reports:
            parts.append(f"has {len(sub_reports)} sub-report(s)")
        summary = ". ".join(parts) + "."

        report.metadata_cache = {
            'report_name': data.get('reportName', report.report_name),
            'query_language': data.get('queryLanguage'),
            'data_adapter': data.get('dataAdapterName'),
            'parameter_names': param_names,
            'field_names': field_names[:20],
            'chart_types': chart_types,
            'has_subreports': bool(sub_reports),
            'band_count': len(data.get('bands') or []),
            'summary': summary,
        }
        report.metadata_cached_at = timezone.now()
        report.save(update_fields=['metadata_cache', 'metadata_cached_at'])


class ReportParametersView(APIView):
    """
    Proxy to KlexReportingService parameter validation.
    Downloads the JRXML from GitHub and calls Java POST /api/validation/parameters
    with empty parameters to extract the parameter definitions.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        try:
            report = Report.objects.get(pk=pk)
        except Report.DoesNotExist:
            return Response({'error': 'Report not found'}, status=status.HTTP_404_NOT_FOUND)

        try:
            # Download the JRXML from GitHub
            local_jrxml_path = ensure_local_jrxml(report, request.user)

            # Forward user-supplied parameters for validation
            user_params = request.data.get('parameters', {})

            validation_url = f"{settings.COMPILER_SERVICE_URL}/api/validation/parameters"
            payload = {
                'sourceType': 'LOCAL',
                'path': local_jrxml_path,
                'parameters': user_params,
            }

            response = http_requests.post(validation_url, json=payload, timeout=60)

            if response.status_code != 200:
                raise Exception(f"Validation service returned {response.status_code}: {response.text}")

            return Response(response.json())

        except http_requests.exceptions.ConnectionError:
            return Response(
                {'error': 'Validation service is not available. Please ensure the Java service is running.'},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        except Exception as e:
            logger.error(f"Parameter extraction failed: {e}")
            return Response(
                {'error': f'Parameter extraction failed: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


def _extract_jrxml_param_field_map(jrxml_path: str) -> dict:
    """
    Parse a JRXML file and build a mapping of parameter names to the
    data-source field (column) they are associated with.

    Scans <filterExpression>, <variableExpression>, <textFieldExpression>,
    and <groupExpression> for patterns like:
        $F{Region}.equals($P{P_REGION})
        $P{P_REGION}.equals($F{Region})
    and returns e.g. {'P_REGION': 'Region'}.
    """
    import xml.etree.ElementTree as ET

    param_field_map: dict = {}

    try:
        tree = ET.parse(jrxml_path)
        root = tree.getroot()

        ns = ''
        if root.tag.startswith('{'):
            ns = root.tag.split('}')[0] + '}'

        # Collect all expression text from relevant elements
        expression_tags = [
            f'{ns}filterExpression',
            f'{ns}variableExpression',
            f'{ns}textFieldExpression',
            f'{ns}groupExpression',
        ]

        expressions: list[str] = []
        for tag in expression_tags:
            for elem in root.iter(tag):
                text = elem.text
                if text:
                    expressions.append(text)

        # Regex patterns to find $P{param} paired with $F{field}
        # Pattern 1:  $F{Field}.equals($P{Param})  or  $F{Field} == $P{Param}
        # Pattern 2:  $P{Param}.equals($F{Field})  or  $P{Param} == $F{Field}
        import re

        # Matches: $F{FieldName}...$P{ParamName}  in any comparison context
        pat_f_p = re.compile(
            r'\$F\{([^}]+)\}'
            r'[^$]*'
            r'\$P\{([^}]+)\}'
        )
        # Matches: $P{ParamName}...$F{FieldName}
        pat_p_f = re.compile(
            r'\$P\{([^}]+)\}'
            r'[^$]*'
            r'\$F\{([^}]+)\}'
        )

        for expr in expressions:
            for m in pat_f_p.finditer(expr):
                field_name, param_name = m.group(1), m.group(2)
                if param_name not in param_field_map:
                    param_field_map[param_name] = field_name

            for m in pat_p_f.finditer(expr):
                param_name, field_name = m.group(1), m.group(2)
                if param_name not in param_field_map:
                    param_field_map[param_name] = field_name

        # Fallback: if a parameter name matches a field name exactly (case-insensitive),
        # or matches after stripping common prefixes like P_, PARAM_, IN_
        all_fields: set[str] = set()
        for elem in root.iter(f'{ns}field'):
            fname = elem.get('name')
            if fname:
                all_fields.add(fname)

        all_params: set[str] = set()
        for elem in root.iter(f'{ns}parameter'):
            pname = elem.get('name')
            if pname:
                all_params.add(pname)

        # Build a lowercase→original lookup for fields
        field_lower_map = {f.lower(): f for f in all_fields}

        for pname in all_params:
            if pname in param_field_map:
                continue  # already mapped from expressions
            # Strip common prefixes and check for match
            stripped = pname
            for prefix in ('P_', 'p_', 'PARAM_', 'param_', 'IN_', 'in_'):
                if stripped.startswith(prefix):
                    stripped = stripped[len(prefix):]
                    break
            # Direct case-insensitive match
            if stripped.lower() in field_lower_map:
                param_field_map[pname] = field_lower_map[stripped.lower()]

    except Exception as e:
        logger.warning(f"Failed to parse JRXML for param-field mapping: {e}")

    return param_field_map


class ReportParameterMetadataView(APIView):
    """
    Return clean parameter metadata for UI rendering.
    Proxies to the Java validation API with empty parameters,
    then transforms each ParameterDetail into a UI-ready schema.
    GET /api/reports/<pk>/parameter-metadata/
    """
    permission_classes = [IsAuthenticated]

    # Java type → simplified UI type
    TYPE_MAP = {
        'java.lang.String': 'string',
        'java.lang.Integer': 'number',
        'int': 'number',
        'java.lang.Long': 'number',
        'long': 'number',
        'java.lang.Double': 'number',
        'double': 'number',
        'java.lang.Float': 'number',
        'float': 'number',
        'java.lang.Short': 'number',
        'short': 'number',
        'java.lang.Byte': 'number',
        'byte': 'number',
        'java.math.BigDecimal': 'number',
        'java.lang.Boolean': 'boolean',
        'boolean': 'boolean',
        'java.util.Date': 'date',
        'java.sql.Date': 'date',
        'java.sql.Timestamp': 'timestamp',
    }

    WIDGET_MAP = {
        'string': 'TextField',
        'number': 'NumericField',
        'boolean': 'Switch',
        'date': 'DatePicker',
        'timestamp': 'DateTimePicker',
    }

    DATE_FORMATS = {
        'date': ['yyyy-MM-dd', 'MM/dd/yyyy', 'dd/MM/yyyy'],
        'timestamp': ['yyyy-MM-dd\'T\'HH:mm:ss', 'yyyy-MM-dd HH:mm:ss'],
    }

    # Parameter name patterns that hint at "advanced" grouping
    ADVANCED_PATTERNS = (
        'debug', 'trace', 'log', 'internal', 'system', 'hidden',
        'sub_', 'subreport', 'config', 'xml_', 'json_',
    )

    def get(self, request, pk):
        try:
            report = Report.objects.get(pk=pk)
        except Report.DoesNotExist:
            return Response(
                {'error': 'Report not found'},
                status=status.HTTP_404_NOT_FOUND,
            )

        try:
            local_jrxml_path = ensure_local_jrxml(report, request.user)

            # Parse JRXML to map parameters to data-source columns
            param_field_map = _extract_jrxml_param_field_map(local_jrxml_path)
            logger.info(f"JRXML param→field mapping: {param_field_map}")
            
            # Extract potential filters from query parameters
            filters = {}
            for k, v in request.query_params.items():
                if k not in ('format', 'page') and v:
                    if '|' in v:
                        filters[k] = [x for x in v.split('|') if x]
                    elif ',' in v:
                        filters[k] = [x for x in v.split(',') if x]
                    else:
                        filters[k] = v
            
            # Resolve data source configuration to fetch dynamic parameter values
            ds_config = None
            try:
                ds_config = _resolve_yaml_datasource(report, request.user)
            except Exception as e:
                logger.warning(f"Could not resolve data source for dynamic parameters: {e}")

            validation_url = (
                f"{settings.COMPILER_SERVICE_URL}/api/validation/parameters"
            )
            payload = {
                'sourceType': 'LOCAL',
                'path': local_jrxml_path,
                'parameters': {},
            }

            resp = http_requests.post(
                validation_url, json=payload, timeout=60,
            )
            if resp.status_code != 200:
                raise Exception(
                    f"Validation service returned {resp.status_code}: "
                    f"{resp.text}"
                )

            raw = resp.json()
            details = raw.get('parameterDetails', [])

            parameters = []
            for idx, p in enumerate(details):
                name = p.get('name', '')
                expected_type = p.get('expectedType') or 'java.lang.String'
                ui_type = self.TYPE_MAP.get(expected_type, 'string')
                for_prompting = p.get('forPrompting', True)
                default_value = p.get('defaultValue')
                has_default = default_value is not None

                # Determine if required
                required = for_prompting and not has_default

                # Build label from name
                label = self._build_label(name)

                # Widget
                widget = self.WIDGET_MAP.get(ui_type, 'TextField')

                # Select widget for known enum-like patterns
                allowed_values = self._guess_allowed_values(name)
                
                # Dynamic values from JRXML param→field mapping
                if not allowed_values and ds_config:
                    mapped_field = param_field_map.get(name)
                    if mapped_field:
                        allowed_values = self._fetch_dynamic_allowed_values_by_column(
                            ds_config, mapped_field, filters
                        )
                
                # Legacy fallback: dot-convention (TableName.ColumnName)
                if not allowed_values and '.' in name and ds_config:
                    allowed_values = self._fetch_dynamic_allowed_values(ds_config, name, filters)
                    
                # Force MultiSelect for string/number types (no text fields)
                if ui_type in ('string', 'number'):
                    widget = 'MultiSelect'
                elif allowed_values:
                    widget = 'Select'

                # Supported date formats
                supported_formats = self.DATE_FORMATS.get(ui_type)

                # Placeholder
                placeholder = self._build_placeholder(
                    name, ui_type, supported_formats,
                )

                # Group
                name_lower = name.lower()
                is_advanced = any(
                    pat in name_lower for pat in self.ADVANCED_PATTERNS
                )
                group = (
                    'Advanced Filters'
                    if (is_advanced or not for_prompting)
                    else 'Basic Filters'
                )

                # Hidden / readOnly
                hidden = not for_prompting and not has_default
                read_only = False

                parameters.append({
                    'name': name,
                    'label': label,
                    'type': ui_type,
                    'required': required,
                    'defaultValue': default_value,
                    'forPrompting': for_prompting,
                    'widget': widget,
                    'supportedFormats': supported_formats,
                    'placeholder': placeholder,
                    'group': group,
                    'operator': None,
                    'allowedValues': allowed_values,
                    'hidden': hidden,
                    'readOnly': read_only,
                    'dependencies': [],
                    'order': idx,
                })

            return Response({
                'reportId': pk,
                'reportName': report.display_name or report.report_name,
                'parameters': parameters,
            })

        except http_requests.exceptions.ConnectionError:
            return Response(
                {
                    'error': (
                        'Metadata service is not available. '
                        'Please ensure the Java service is running.'
                    ),
                },
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )
        except Exception as e:
            logger.error(f"Parameter metadata extraction failed: {e}")
            return Response(
                {'error': f'Parameter metadata extraction failed: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

    # ── private helpers ──────────────────────────────────────

    @staticmethod
    def _build_label(name: str) -> str:
        """Convert PARAM_NAME or paramName into 'Param Name'."""
        import re
        # Insert space before uppercase letters (camelCase)
        s = re.sub(r'([a-z])([A-Z])', r'\1 \2', name)
        # Replace underscores / hyphens with spaces
        s = s.replace('_', ' ').replace('-', ' ')
        return s.strip().title()

    @staticmethod
    def _build_placeholder(name, ui_type, supported_formats):
        if ui_type == 'date' and supported_formats:
            return f"e.g. {supported_formats[0]}"
        if ui_type == 'timestamp' and supported_formats:
            return f"e.g. {supported_formats[0]}"
        if ui_type == 'number':
            return 'Enter a number'
        if ui_type == 'boolean':
            return ''
        return f"Enter {name}"

    @staticmethod
    def _guess_allowed_values(name: str):
        """Return allowed values for well-known enum parameter names."""
        lower = name.lower()
        if lower in ('format', 'output_format', 'exportformat'):
            return [
                {'value': 'PDF', 'label': 'PDF'},
                {'value': 'XLSX', 'label': 'Excel'},
                {'value': 'CSV', 'label': 'CSV'},
                {'value': 'HTML', 'label': 'HTML'},
            ]
        if lower in ('sort_order', 'sortorder', 'sort_dir', 'sortdirection'):
            return [
                {'value': 'ASC', 'label': 'Ascending'},
                {'value': 'DESC', 'label': 'Descending'},
            ]
        return None

    @staticmethod
    def _fetch_dynamic_allowed_values_by_column(ds_config, column_name: str, filters=None):
        """
        Fetch distinct values for a specific column from the configured data source.
        Used when JRXML parsing maps a parameter to a field/column name.
        """
        ds_type = ds_config.get('dataSourceType')
        try:
            if ds_type == 'jdbc':
                # For JDBC, we need a table — we can't know it from the JRXML alone.
                # Skip JDBC unless a table is somehow known.
                return None
            elif ds_type == 'csv':
                return ReportParameterMetadataView._fetch_csv_distinct(ds_config, column_name)
            elif ds_type == 'json':
                return ReportParameterMetadataView._fetch_json_distinct(ds_config, column_name)
        except Exception as e:
            logger.warning(f"Failed to fetch dynamic values for column '{column_name}': {e}")
        return None

    @staticmethod
    def _fetch_dynamic_allowed_values(ds_config, name: str, filters=None):
        """
        If name has format table.column, connect to data source and fetch distinct values.
        """
        parts = name.split('.')
        if len(parts) < 2:
            return None
        
        column_name = parts[-1]
        table_name = '.'.join(parts[:-1])
        
        ds_type = ds_config.get('dataSourceType')
        try:
            if ds_type == 'jdbc':
                return ReportParameterMetadataView._fetch_jdbc_distinct(ds_config, table_name, column_name, filters)
            elif ds_type == 'csv':
                return ReportParameterMetadataView._fetch_csv_distinct(ds_config, column_name)
            elif ds_type == 'json':
                return ReportParameterMetadataView._fetch_json_distinct(ds_config, column_name)
        except Exception as e:
            logger.warning(f"Failed to fetch dynamic parameter values for {name}: {e}")
        return None

    @staticmethod
    def _fetch_jdbc_distinct(ds_config, table, column, filters=None):
        url = ds_config.get('jdbcUrl', '')
        user = ds_config.get('jdbcUser', '')
        password = ds_config.get('jdbcPassword', '')
        
        import psycopg2
        import re
        match = re.match(r'jdbc:postgresql://([^:/]+)(?::(\d+))?/(\w+)', url)
        if not match:
            return None
            
        host = match.group(1)
        port = int(match.group(2)) if match.group(2) else 5432
        database = match.group(3)
        
        conn = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password, connect_timeout=5
        )
        try:
            with conn.cursor() as cur:
                where_clauses = [f'"{column}" IS NOT NULL']
                sql_params = []
                
                if filters:
                    for k, v in filters.items():
                        if not v:
                            continue
                        if isinstance(v, (list, tuple)):
                            placeholders = ', '.join(['%s'] * len(v))
                            where_clauses.append(f'"{k}" IN ({placeholders})')
                            sql_params.extend(v)
                        else:
                            where_clauses.append(f'"{k}" = %s')
                            sql_params.append(v)
                            
                where_sql = ' AND '.join(where_clauses)
                query = f'SELECT DISTINCT "{column}" FROM "{table}" WHERE {where_sql} ORDER BY "{column}" LIMIT 100'
                
                cur.execute(query, sql_params)
                rows = cur.fetchall()
                results = []
                for row in rows:
                    val = str(row[0])
                    results.append({'value': val, 'label': val})
                return results
        finally:
            conn.close()

    @staticmethod
    def _fetch_csv_distinct(ds_config, column):
        file_path = ds_config.get('csvFilePath')
        if not file_path:
            return None
        import csv
        unique_vals = set()
        with open(file_path, newline='', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            # Find the actual column name ignoring case
            actual_col = None
            if reader.fieldnames:
                for fn in reader.fieldnames:
                    if fn.lower() == column.lower():
                        actual_col = fn
                        break
            if not actual_col:
                return None
                
            for row in reader:
                val = row.get(actual_col)
                if val:
                    unique_vals.add(str(val))
        
        sorted_vals = sorted(list(unique_vals))[:100]
        return [{'value': v, 'label': v} for v in sorted_vals]

    @staticmethod
    def _fetch_json_distinct(ds_config, column):
        file_path = ds_config.get('jsonFilePath')
        if not file_path:
            return None
        import json
        unique_vals = set()
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            if isinstance(data, list):
                for item in data:
                    if isinstance(item, dict):
                        # Match case-insensitively
                        for k, v in item.items():
                            if k.lower() == column.lower() and v is not None:
                                unique_vals.add(str(v))
        
        sorted_vals = sorted(list(unique_vals))[:100]
        return [{'value': v, 'label': v} for v in sorted_vals]


class ReportAuditLogsView(APIView):
    """
    Proxy to KlexReportingService audit logs.
    Admin-only. Forwards GET /api/audit/logs with optional query params.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        try:
            audit_url = f"{settings.COMPILER_SERVICE_URL}/api/audit/logs"
            params = {}
            for key in ('status', 'format', 'dataSourceType'):
                val = request.query_params.get(key)
                if val:
                    params[key] = val

            response = http_requests.get(audit_url, params=params, timeout=30)

            if response.status_code != 200:
                raise Exception(f"Audit service returned {response.status_code}: {response.text}")

            return Response(response.json())

        except http_requests.exceptions.ConnectionError:
            return Response(
                {'error': 'Audit service is not available. Please ensure the Java service is running.'},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        except Exception as e:
            logger.error(f"Audit logs fetch failed: {e}")
            return Response(
                {'error': f'Failed to fetch audit logs: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class ReportAuditStatsView(APIView):
    """
    Proxy to KlexReportingService audit performance stats.
    Admin-only. Forwards GET /api/audit/stats.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        try:
            stats_url = f"{settings.COMPILER_SERVICE_URL}/api/audit/stats"
            response = http_requests.get(stats_url, timeout=30)

            if response.status_code != 200:
                raise Exception(f"Audit stats service returned {response.status_code}: {response.text}")

            return Response(response.json())

        except http_requests.exceptions.ConnectionError:
            return Response(
                {'error': 'Audit service is not available. Please ensure the Java service is running.'},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        except Exception as e:
            logger.error(f"Audit stats fetch failed: {e}")
            return Response(
                {'error': f'Failed to fetch audit stats: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


# ──────────────────────────────────────────────────────────────
# Unified Scheduling Views (Quartz / Airflow)
# ──────────────────────────────────────────────────────────────

from .models import ScheduledJob, JobComment
from .serializers import ScheduledJobSerializer, JobCommentSerializer
from .scheduler import get_scheduler_client, get_active_engine


class SchedulerEngineView(APIView):
    """
    GET /api/reports/scheduler-engine/ — return which scheduling engine
    is currently active ("quartz" or "airflow").
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        engine = get_active_engine()
        client = get_scheduler_client()
        return Response({
            'engine': engine,
            'healthy': client.health_check(),
        })


class ScheduledJobListView(APIView):
    """
    GET /api/reports/jobs/ — list all managed scheduled jobs.
    Works with both Quartz and Airflow.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        # ── Sync job statuses from engine before returning ──
        engine = get_active_engine()
        if engine == 'quartz':
            self._sync_quartz_statuses()

        jobs = ScheduledJob.objects.all()

        # Filter by status
        status_filter = request.query_params.get('status')
        if status_filter:
            jobs = jobs.filter(status=status_filter)

        # Search
        search = request.query_params.get('search')
        if search:
            jobs = jobs.filter(
                models.Q(schedule_name__icontains=search) |
                models.Q(dag_id__icontains=search) |
                models.Q(department__icontains=search)
            )

        serializer = ScheduledJobSerializer(jobs, many=True)
        return Response({
            'count': jobs.count(),
            'jobs': serializer.data,
            'engine': engine,
        })

    @staticmethod
    def _sync_quartz_statuses():
        """
        Query the Quartz engine for the actual trigger state of each
        'running' job.  If all occurrences have fired (trigger state
        is COMPLETE or NONE) update the local record to 'finished'.
        If the schedule no longer exists in Quartz, also mark finished.
        """
        running_jobs = ScheduledJob.objects.filter(status='running')
        if not running_jobs.exists():
            return

        try:
            client = get_scheduler_client()
        except Exception:
            return

        for job in running_jobs:
            try:
                info = client.get_schedule(job.dag_id)
                schedule_detail = info.get('schedule', {})
                trigger_state = (schedule_detail.get('triggerState') or '').upper()
                job_status = (schedule_detail.get('status') or '').upper()

                # Quartz trigger states: NORMAL, PAUSED, COMPLETE, ERROR,
                # BLOCKED, NONE.  COMPLETE / NONE = all occurrences done.
                if trigger_state in ('COMPLETE', 'NONE') or job_status == 'COMPLETED':
                    job.status = 'finished'
                    job.is_active = False
                    job.save(update_fields=['status', 'is_active', 'updated_at'])
                elif trigger_state == 'ERROR':
                    job.status = 'failed'
                    job.is_active = False
                    job.save(update_fields=['status', 'is_active', 'updated_at'])
                elif trigger_state == 'PAUSED':
                    job.status = 'on_hold'
                    job.is_active = False
                    job.save(update_fields=['status', 'is_active', 'updated_at'])
            except Exception:
                # If the schedule no longer exists in Quartz (404/error),
                # treat it as finished.
                try:
                    job.status = 'finished'
                    job.is_active = False
                    job.save(update_fields=['status', 'is_active', 'updated_at'])
                except Exception:
                    pass


class ScheduledJobDetailView(APIView):
    """
    GET /api/reports/jobs/<id>/ — get job detail.
    Enriches with live engine data when Airflow is active.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        serializer = ScheduledJobSerializer(job)
        data = serializer.data
        data['engine'] = get_active_engine()

        # Enrich with live engine data (if Airflow)
        if get_active_engine() == 'airflow':
            try:
                client = get_scheduler_client()
                schedule_info = client.get_schedule(job.dag_id)
                schedule_detail = schedule_info.get('schedule', {})
                data['airflow'] = {
                    'is_paused': schedule_detail.get('is_paused'),
                    'is_active': schedule_detail.get('is_active'),
                    'schedule_interval': schedule_detail.get('schedule_interval'),
                    'next_dagrun': schedule_detail.get('next_dagrun'),
                    'last_parsed_time': schedule_detail.get('last_parsed_time'),
                    'owners': schedule_detail.get('owners', []),
                    'tags': [t.get('name') for t in schedule_detail.get('tags', [])],
                    'description': schedule_detail.get('description'),
                }
            except Exception as e:
                data['airflow'] = {'error': str(e)}

        return Response(data)


class ScheduleReportView(APIView):
    """
    POST /api/reports/schedule/ — create a new scheduled job.

    Delegates to the active scheduler engine (Quartz or Airflow).
    Always creates a local ScheduledJob record as the unified
    source of truth.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        try:
            schedule_data = dict(request.data)
            report_id = schedule_data.get('report_id')
            schedule_name = schedule_data.get('scheduleName', '')
            cron_expression = schedule_data.get('cronExpression', '')
            department = schedule_data.get('department', 'General')
            priority = int(schedule_data.get('priority', 0))

            report = None
            if report_id:
                try:
                    report = Report.objects.get(id=report_id)
                except Report.DoesNotExist:
                    return Response(
                        {'error': f'Report with ID {report_id} not found'},
                        status=status.HTTP_404_NOT_FOUND,
                    )

            # Resolve report artefacts
            if report:
                try:
                    local_jrxml_path = ensure_local_jrxml(report, request.user)
                    schedule_data['reportUnitUri'] = local_jrxml_path
                    schedule_data['report_unit_uri'] = local_jrxml_path
                except Exception as e:
                    return Response(
                        {'error': f'Failed to download compilation file: {str(e)}'},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    )

                # Resolve data source for schedule payload
                try:
                    adapter_payload = _resolve_yaml_datasource(report, request.user)
                    schedule_data['dataAdapter'] = adapter_payload
                    schedule_data['data_adapter'] = adapter_payload
                except Exception as e:
                    return Response(
                        {'error': f'Failed to resolve datasource config: {str(e)}'},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    )

            # Inject context for the adapter
            schedule_data['_user'] = request.user
            schedule_data['_organization'] = getattr(request.user, 'organization', None)

            # ── Delegate to active engine ───────────────────
            client = get_scheduler_client()
            engine_result = client.create_schedule(schedule_data)

            # ── Create local ScheduledJob record ────────────
            dag_id = engine_result.get('jobId') or engine_result.get('dag_id') or schedule_data.get('dag_id', '')

            # Build a serializable copy of the payload for migration support
            stored_payload = {
                k: v for k, v in schedule_data.items()
                if k not in ('_user', '_organization') and not callable(v)
            }

            job = ScheduledJob.objects.create(
                dag_id=dag_id or f'{get_active_engine()}_{ScheduledJob.objects.count() + 1}',
                schedule_name=schedule_name,
                report=report,
                created_by=request.user,
                status='running',
                priority=priority,
                department=department,
                cron_expression=cron_expression,
                frequency_label=schedule_data.get('frequencyLabel', ''),
                machine_name=schedule_data.get('machineName', ''),
                estimated_runtime_min=schedule_data.get('estimatedRuntimeMin'),
                max_runtime_min=schedule_data.get('maxRuntimeMin', 30),
                is_active=True,
                schedule_payload=stored_payload,
                created_on_engine=get_active_engine(),
            )

            return Response({
                'id': job.id,
                'dag_id': dag_id,
                'engine': get_active_engine(),
                'engine_response': engine_result,
                'message': f'Schedule created: {schedule_name}',
                'status': 'success',
            })

        except http_requests.exceptions.ConnectionError:
            return Response(
                {'error': f'Scheduler service ({get_active_engine()}) is not available.'},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )
        except Exception as e:
            logger.error(f"Schedule creation failed: {e}")
            return Response(
                {'error': f'Failed to create schedule: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class ScheduleDeleteView(APIView):
    """
    DELETE /api/reports/schedule/<id>/ — delete a scheduled job.
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        # Delete from engine
        try:
            client = get_scheduler_client()
            client.delete_schedule(job.dag_id)
        except Exception as e:
            logger.warning(f"Engine schedule deletion failed: {e}")

        dag_id = job.dag_id
        job.delete()

        return Response({
            'message': f'Job {dag_id} deleted',
            'status': 'success',
        })


class ScheduledJobActionView(APIView):
    """
    POST /api/reports/jobs/<id>/action/ — perform an action on a job.
    Body: { "action": "hold|release|cancel|kill|force_start|restart|
                       force_restart|skip|mark_finished|mark_failed" }

    Delegates engine-specific work to the active scheduler client.
    Always updates the local ScheduledJob status.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        action = request.data.get('action', '').lower()
        VALID_ACTIONS = [
            'hold', 'release', 'cancel', 'kill', 'force_start',
            'restart', 'force_restart', 'skip', 'mark_finished', 'mark_failed',
        ]
        if action not in VALID_ACTIONS:
            return Response(
                {'error': f'Invalid action. Must be one of: {", ".join(VALID_ACTIONS)}'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            client = get_scheduler_client()
            engine_result = client.perform_action(
                job.dag_id, action, username=request.user.username,
            )

            # Update local ScheduledJob status
            STATUS_MAP = {
                'hold': 'on_hold',
                'release': 'running',
                'cancel': 'failed',
                'kill': 'failed',
                'force_start': 'running',
                'restart': 'running',
                'force_restart': 'running',
                'mark_finished': 'finished',
                'mark_failed': 'failed',
            }
            new_status = STATUS_MAP.get(action)
            if new_status:
                job.status = new_status
                if action in ('hold', 'kill'):
                    job.is_active = False
                elif action in ('release', 'force_start', 'restart', 'force_restart'):
                    job.is_active = True
                job.save()

            response = {
                'message': engine_result.get('message', f'{job.schedule_name} {action}'),
                'status': 'success',
                'job_status': job.status,
                'engine': get_active_engine(),
            }
            if engine_result.get('warnings'):
                response['warnings'] = engine_result['warnings']
            return Response(response)

        except Exception as e:
            logger.error(f"Job action '{action}' failed for {job.dag_id}: {e}")
            return Response(
                {'error': f'Action failed: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class ScheduledJobPriorityView(APIView):
    """
    PATCH /api/reports/jobs/<id>/priority/ — update job priority.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        priority = request.data.get('priority')
        if priority is None:
            return Response({'error': 'priority is required'}, status=status.HTTP_400_BAD_REQUEST)

        job.priority = int(priority)
        job.save()

        return Response({
            'id': job.id,
            'priority': job.priority,
            'message': f'Priority updated to {job.priority}',
        })


class ScheduledJobCommentView(APIView):
    """
    GET/POST /api/reports/jobs/<id>/comments/ — list & add comments.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        comments = job.comments.all()
        serializer = JobCommentSerializer(comments, many=True)
        return Response({'comments': serializer.data})

    def post(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        text = request.data.get('text', '').strip()
        if not text:
            return Response({'error': 'Comment text is required'}, status=status.HTTP_400_BAD_REQUEST)

        comment = JobComment.objects.create(
            job=job,
            user=request.user,
            text=text,
        )
        serializer = JobCommentSerializer(comment)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class ScheduledJobHistoryView(APIView):
    """
    GET /api/reports/jobs/<id>/history/ — fetch job history.
    Delegates to the active engine.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        try:
            client = get_scheduler_client()
            result = client.get_history(
                job.dag_id,
                limit=int(request.query_params.get('limit', 25)),
                offset=int(request.query_params.get('offset', 0)),
                start_date=request.query_params.get('start_date'),
                end_date=request.query_params.get('end_date'),
            )
            return Response(result)
        except Exception as e:
            logger.error(f"Failed to fetch history for {job.dag_id}: {e}")
            return Response(
                {'error': f'Failed to fetch job history: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class ScheduledJobLogsView(APIView):
    """
    GET /api/reports/jobs/<id>/logs/?run_id=xxx&task_id=yyy
    Delegates to the active engine.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            job = ScheduledJob.objects.get(pk=pk)
        except ScheduledJob.DoesNotExist:
            return Response({'error': 'Job not found'}, status=status.HTTP_404_NOT_FOUND)

        try:
            client = get_scheduler_client()
            result = client.get_logs(
                job.dag_id,
                run_id=request.query_params.get('run_id'),
                task_id=request.query_params.get('task_id'),
            )
            return Response(result)
        except Exception as e:
            logger.error(f"Failed to fetch logs for {job.dag_id}: {e}")
            return Response(
                {'error': f'Failed to fetch logs: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


# ──────────────────────────────────────────────────────────────
# Dispatch Service Views (Airflow-only — active only when engine=airflow)
# ──────────────────────────────────────────────────────────────


class SubmitReportJobView(APIView):
    """
    POST /api/reports/dispatch/submit/ — submit a report execution job
    through the dispatch service (Airflow-only, with admission control / dedupe).
    Returns 503 if Airflow is not the active engine.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        if get_active_engine() != 'airflow':
            return Response(
                {'error': 'Dispatch service is only available with Airflow engine.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            from .dispatch_service import ReportDispatchService
            from .enums import OverlapPolicy, WorkloadClass, PriorityTier

            data = request.data
            report_id = data.get('report_id')
            dag_id = data.get('dag_id')
            report = None

            if report_id:
                try:
                    report = Report.objects.get(id=report_id)
                except Report.DoesNotExist:
                    return Response(
                        {'error': f'Report {report_id} not found'},
                        status=status.HTTP_404_NOT_FOUND,
                    )

            if not dag_id and report:
                dag_id = "klex_report_dispatcher"
            elif not dag_id:
                return Response(
                    {'error': 'dag_id is required when report_id is not provided'},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            service = ReportDispatchService()
            job = service.submit_report_job(
                dag_id=dag_id,
                report_id=report_id,
                report=report,
                created_by=request.user,
                organization=getattr(request.user, 'organization', None),
                payload=data.get('payload', {}),
                priority=data.get('priority', PriorityTier.NORMAL),
                workload_class=data.get('workload_class', WorkloadClass.MEDIUM),
                overlap_policy=data.get('overlap_policy', OverlapPolicy.QUEUE_ALL),
                expected_runtime_seconds=data.get('expected_runtime_seconds'),
                max_retries=data.get('max_retries'),
            )

            return Response({
                'id': str(job.id),
                'dag_id': job.dag_id,
                'status': job.status,
                'dedupe_key': job.dedupe_key,
                'airflow_run_id': job.airflow_run_id,
                'message': f'Job {job.status.lower()}',
            }, status=status.HTTP_201_CREATED if job.status != 'SKIPPED' else status.HTTP_200_OK)

        except Exception as e:
            logger.error(f"Job submission failed: {e}")
            return Response(
                {'error': f'Job submission failed: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class DispatchQueueSummaryView(APIView):
    """
    GET /api/reports/dispatch/summary/ — queue summary (Airflow-only).
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN,
            )

        if get_active_engine() != 'airflow':
            return Response({
                'status_counts': {},
                'workload_counts': {},
                'total_waiting': 0,
                'total_active': 0,
                'avg_wait_seconds': None,
                'long_running_count': 0,
                'engine': 'quartz',
                'message': 'Dispatch queue is Airflow-only. Currently using Quartz.',
            })

        from .dispatch_service import ReportDispatchService
        service = ReportDispatchService()
        summary = service.get_queue_summary()
        summary['engine'] = 'airflow'
        return Response(summary)


# ──────────────────────────────────────────────────────────────
# Scheduler Engine Migration Views
# ──────────────────────────────────────────────────────────────


class SchedulerEngineSwitchView(APIView):
    """
    POST /api/reports/scheduler-engine/switch/
    Body: { "target_engine": "airflow" }

    Superadmin-only.  Triggers a full schedule migration from the
    current engine to the target engine.  Returns a migration report
    with per-job status.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        # ── auth: superadmin only ───────────────────────────────
        if not getattr(request.user, 'is_superuser', False):
            return Response(
                {'error': 'Superadmin access required to switch scheduling engines.'},
                status=status.HTTP_403_FORBIDDEN,
            )

        target_engine = (request.data.get('target_engine') or '').lower().strip()
        if target_engine not in ('quartz', 'airflow'):
            return Response(
                {'error': 'target_engine must be "quartz" or "airflow".'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        current_engine = get_active_engine()
        if current_engine == target_engine:
            return Response({
                'status': 'no_change',
                'message': f'Already running on {target_engine}.',
                'engine': current_engine,
            })

        dry_run = request.data.get('dry_run', False)
        force = request.data.get('force', False)

        try:
            from .migration_service import SchedulerMigrationService

            service = SchedulerMigrationService()

            # ── preflight (optional) ────────────────────────────
            if not force and not dry_run:
                ok, issues = service.preflight_check(target_engine)
                if not ok:
                    return Response({
                        'status': 'preflight_failed',
                        'issues': issues,
                        'message': 'Pre-flight checks failed. Use force=true to skip.',
                    }, status=status.HTTP_412_PRECONDITION_FAILED)

            # ── execute migration ───────────────────────────────
            report = service.migrate_all_jobs(
                target_engine,
                dry_run=dry_run,
                force=force,
            )

            response_status = 'success'
            if report.failed > 0 and report.migrated == 0:
                response_status = 'failed'
            elif report.failed > 0:
                response_status = 'partial'

            return Response({
                'status': response_status,
                'dry_run': dry_run,
                'previous_engine': report.previous_engine,
                'new_engine': report.target_engine if not dry_run else report.previous_engine,
                'migration_report': report.to_dict(),
            })

        except Exception as e:
            logger.error(f"Scheduler engine switch failed: {e}")
            return Response(
                {'error': f'Migration failed: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class AirflowUIRedirectView(APIView):
    """
    GET /api/reports/airflow-ui/

    Returns the Airflow web UI URL so the frontend can redirect
    power users to the native Airflow DAGs interface.

    Only available when the active engine is Airflow.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        engine = get_active_engine()

        if engine != 'airflow':
            return Response({
                'available': False,
                'engine': engine,
                'message': 'Airflow UI is only available when the active engine is Airflow.',
            })

        from .scheduler import load_scheduler_config
        cfg = load_scheduler_config()
        airflow_cfg = cfg.get('airflow', {})

        # Derive the UI URL from the API URL
        # API URL is like http://localhost:8080/api/v1 or http://localhost:8080/api/v2
        api_url = airflow_cfg.get('url', 'http://localhost:8080/api/v2')
        # Strip /api/v1 or /api/v2 to get the base UI URL
        if '/api/' in api_url:
            ui_url = api_url[:api_url.index('/api/')]
        else:
            ui_url = api_url.rstrip('/')

        return Response({
            'available': True,
            'engine': 'airflow',
            'ui_url': ui_url,
            'dags_url': f'{ui_url}/dags',
        })


class ReportMetadataBulkRefreshView(APIView):
    """
    POST /api/reports/metadata/refresh/
    Admin-only endpoint to bulk-refresh the metadata_cache for all reports
    (or a subset specified via report_ids in the request body).
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN,
            )

        # Optional: subset of report IDs
        report_ids = request.data.get('report_ids')
        qs = Report.objects.all()
        if report_ids:
            qs = qs.filter(id__in=report_ids)

        total = qs.count()
        refreshed = 0
        failed = 0

        for report in qs.select_related('repo'):
            ok = _populate_report_metadata_cache(report, request.user)
            if ok:
                refreshed += 1
            else:
                failed += 1

        return Response({
            'total': total,
            'refreshed': refreshed,
            'failed': failed,
            'message': f'Metadata refreshed for {refreshed}/{total} reports.',
        })
