"""
Chatbot action tools for the Klex AI agent.

Each tool is a LangChain @tool function that performs a specific action
on the Klex platform. Tools are conditionally loaded based on user
permissions in the ChatbotAPIView.
"""
import json
import logging
from typing import Optional

from langchain_core.tools import tool

logger = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────────
# Navigation tools (available to all authenticated users)
# ──────────────────────────────────────────────────────────────────────

@tool
def navigate_to_page(page_url: str) -> str:
    """Navigates the user to a specific page in the application.

    Valid page_url values are:
    - "/reports" (to view all reports)
    - "/adapters" or "/data-adapters" (to view or create data adapters/sources)
    - "/users" (to manage users)
    - "/permissions" (to manage roles and permissions)
    - "/schedules" (to view scheduled report deliveries)
    - "/embeddings" (API Reference)
    """
    return json.dumps({
        "__action__": "navigate",
        "navigation_target": page_url,
        "message": f"Navigating to {page_url}",
    })


@tool
def open_report(report_id: int, report_name: str) -> str:
    """Opens a specific report by its ID.

    Use this when the user asks to open, view, or see a specific report.
    Pick the report_id from the report catalog provided in the system context.
    The report_name parameter is for confirmation messaging only.
    """
    return json.dumps({
        "__action__": "navigate",
        "navigation_target": f"/reports/{report_id}",
        "message": f"Opening report '{report_name}' (id={report_id})",
    })


# ──────────────────────────────────────────────────────────────────────
# Report action tools
# ──────────────────────────────────────────────────────────────────────

def _make_compile_report_tool(user):
    """Factory that creates a compile_report tool bound to a specific user."""

    @tool
    def compile_report(
        report_id: int,
        output_format: str = "PDF",
        parameters: Optional[str] = None,
    ) -> str:
        """Compile/run a report and generate output in the specified format.

        Use this when the user wants to run, compile, generate, or export a report.

        IMPORTANT: Match the report from the catalog by name. The user will say
        things like "run the customer report" or "compile sales report as Excel".
        Find the best matching report_id from the report catalog yourself.

        Translate natural language formats:
        - "as Excel" / "in spreadsheet" → output_format="XLSX"
        - "as PDF" / "in PDF" → output_format="PDF"
        - "as Word" / "in Word" / "as document" → output_format="DOCX"
        - "as CSV" → output_format="CSV"
        - "as PowerPoint" / "as slides" → output_format="PPTX"
        - "as HTML" / "as web page" → output_format="HTML"

        Args:
            report_id: The ID of the report from the report catalog.
            output_format: Output format — one of PDF, XLSX, CSV, HTML, DOCX, PPTX, ODT.
                          Defaults to PDF.
            parameters: Optional JSON string of report parameters, e.g. '{"startDate": "2024-01-01"}'.
        """
        from reports.models import Report, Execution
        from reports.views import (
            _get_accessible_report_ids, ensure_local_jrxml,
            _resolve_yaml_datasource,
        )
        from django.conf import settings
        from django.utils import timezone
        from pathlib import Path
        import requests as http_requests

        try:
            report = Report.objects.get(pk=report_id)
        except Report.DoesNotExist:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "compile_report",
                                      "details": {"error": f"Report with ID {report_id} not found."}},
                "message": f"Report with ID {report_id} not found.",
            })

        # Permission check
        if not user.is_admin:
            permitted_ids = _get_accessible_report_ids(user)
            if not (report.is_public or report.id in permitted_ids):
                return json.dumps({
                    "__action_result__": {"type": "error", "action": "compile_report",
                                          "details": {"error": "Permission denied"}},
                    "message": "You don't have permission to run this report.",
                })

        # Normalize format
        fmt = output_format.upper().strip()
        FORMAT_MAP = {"EXCEL": "XLSX", "XLS": "XLSX", "WORD": "DOCX", "POWERPOINT": "PPTX"}
        fmt = FORMAT_MAP.get(fmt, fmt)
        VALID_FORMATS = {"PDF", "XLSX", "CSV", "HTML", "DOCX", "PPTX", "ODT"}
        if fmt not in VALID_FORMATS:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "compile_report",
                                      "details": {"error": f"Unsupported format: {fmt}"}},
                "message": f"Unsupported format '{fmt}'. Supported: {', '.join(sorted(VALID_FORMATS))}",
            })

        FORMAT_EXTENSIONS = {
            "PDF": "pdf", "XLSX": "xlsx", "CSV": "csv", "HTML": "html",
            "DOCX": "docx", "PPTX": "pptx", "ODT": "odt",
        }
        file_ext = FORMAT_EXTENSIONS[fmt]

        # Create execution record
        execution = Execution.objects.create(
            report=report, invoked_by=user, status="running", output_format=file_ext,
        )
        execution.started_at = timezone.now()
        execution.save()

        try:
            local_jrxml_path = ensure_local_jrxml(report, user)
            output_dir = Path(settings.MEDIA_ROOT) / "reports" / str(report.id)
            output_dir.mkdir(parents=True, exist_ok=True)
            output_filename = f"{execution.id}.{file_ext}"
            java_output_path = str(output_dir / output_filename)

            compiler_payload = {
                "sourceType": "LOCAL",
                "path": str(local_jrxml_path),
                "format": fmt,
                "outputPath": java_output_path,
            }

            # Parse user parameters
            if parameters:
                try:
                    compiler_payload["parameters"] = json.loads(parameters)
                except json.JSONDecodeError:
                    pass

            # Inject data source
            adapter_payload = _resolve_yaml_datasource(report, user)
            compiler_payload.update(adapter_payload)

            compiler_url = f"{settings.COMPILER_SERVICE_URL}/api/reports/generate"
            response = http_requests.post(compiler_url, json=compiler_payload, timeout=120)

            if response.status_code != 200:
                raise Exception(f"Compiler service returned {response.status_code}: {response.text}")

            content_type = response.headers.get("Content-Type", "")
            if "application/" in content_type and "json" not in content_type:
                with open(java_output_path, "wb") as f:
                    f.write(response.content)
            else:
                result_text = response.text.strip()
                if result_text.startswith("Error"):
                    raise Exception(result_text)
                if not Path(java_output_path).exists():
                    raise Exception("Output file not generated")

            execution.status = "success"
            execution.finished_at = timezone.now()
            execution.output_location = f"/media/reports/{report.id}/{output_filename}"
            execution.save()

            display = report.display_name or report.report_name
            download_url = f"/api/reports/{report.id}/download/{execution.id}/"

            return json.dumps({
                "__action_result__": {
                    "type": "success",
                    "action": "compile_report",
                    "details": {
                        "report_name": display,
                        "format": file_ext,
                        "download_url": download_url,
                        "execution_id": execution.id,
                    },
                },
                "message": f"Successfully compiled '{display}' as {fmt}. Download: {download_url}",
            })

        except Exception as e:
            execution.status = "failure"
            execution.finished_at = timezone.now()
            execution.save()
            logger.error(f"Chatbot compile_report failed: {e}")
            return json.dumps({
                "__action_result__": {"type": "error", "action": "compile_report",
                                      "details": {"error": str(e)}},
                "message": f"Report compilation failed: {str(e)}",
            })

    return compile_report


# ──────────────────────────────────────────────────────────────────────
# Scheduling tools (admin-only)
# ──────────────────────────────────────────────────────────────────────

def _make_schedule_report_tool(user):
    """Factory that creates a schedule_report tool bound to a specific user."""

    @tool
    def schedule_report(
        report_id: int,
        schedule_name: str,
        department: str = "General",
        priority: int = 0,
        trigger_type: str = "calendar",
        cron_expression: Optional[str] = None,
        interval: Optional[int] = None,
        interval_unit: Optional[str] = None,
        max_runs: Optional[int] = None,
        notification_email: Optional[str] = None,
        output_formats: Optional[str] = None,
    ) -> str:
        """Schedule a report for automated recurring execution.

        Use this when the user wants to schedule, automate, or set up
        recurring execution of a report.

        IMPORTANT: For normal schedules, use trigger_type="calendar" and translate natural language timing to a cron expression.
        Examples:
        - "every day at 9 AM" → cron_expression="0 9 * * *"
        - "every Monday at 8 AM" → cron_expression="0 8 * * 1"
        
        HOWEVER, if the user specifies a maximum number of runs (e.g., "only 3 times", "run 5 times"), 
        you MUST use trigger_type="simple" and provide interval and interval_unit (MINUTE, HOUR, DAY, WEEK) instead of cron_expression.
        Examples:
        - "every minute, 3 times" → trigger_type="simple", interval=1, interval_unit="MINUTE", max_runs=3
        - "every 2 hours, 5 times" → trigger_type="simple", interval=2, interval_unit="HOUR", max_runs=5

        Parse the schedule name from user input like "name - test", "call it X", "name: X".
        If no name given, auto-generate one like "Daily Customer Report".

        If the user did not specify an email address to send the report to, or the output formats (like PDF, Excel), you should ask them for it before scheduling!

        Args:
            report_id: The ID of the report from the report catalog.
            schedule_name: A descriptive name for the schedule.
            department: Department this schedule belongs to. Defaults to "General".
            priority: Priority level (0 = normal, higher = more important). Defaults to 0.
            trigger_type: "calendar" or "simple". Use "simple" ONLY if max_runs is specified.
            cron_expression: Cron expression for the schedule timing (required if trigger_type="calendar").
            interval: Numeric interval (required if trigger_type="simple").
            interval_unit: "MINUTE", "HOUR", "DAY", "WEEK" (required if trigger_type="simple").
            max_runs: Optional maximum number of times the schedule should run.
            notification_email: Optional email address to send the report to.
            output_formats: Optional comma-separated formats (e.g. "PDF,XLSX").
        """
        from reports.models import Report, ScheduledJob
        from reports.views import ensure_local_jrxml, _resolve_yaml_datasource
        from reports.scheduler import get_scheduler_client, get_active_engine

        try:
            report = Report.objects.get(pk=report_id)
        except Report.DoesNotExist:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "schedule_report",
                                      "details": {"error": f"Report {report_id} not found"}},
                "message": f"Report with ID {report_id} not found.",
            })

        formats_list = []
        if output_formats:
            for fmt in output_formats.split(","):
                fmt = fmt.strip().upper()
                fmt = {"EXCEL": "XLSX", "WORD": "DOCX", "POWERPOINT": "PPTX"}.get(fmt, fmt)
                formats_list.append(fmt)
        else:
            formats_list = ["PDF"]

        trigger_data = {}
        if trigger_type == "simple":
            trigger_data = {
                "simpleTrigger": {
                    "timezone": "UTC",
                    "recurrenceInterval": interval or 1,
                    "recurrenceIntervalUnit": interval_unit or "MINUTE",
                    "occurrenceCount": max_runs if max_runs is not None else -1
                }
            }
        else:
            trigger_data = {
                "calendarTrigger": {
                    "timezone": "UTC",
                    "cronExpression": cron_expression or "* * * * *"
                }
            }

        schedule_data = {
            "report_id": report_id,
            "reportUnitUri": getattr(report, "path", "dummy_uri"),
            "scheduleName": schedule_name,
            "outputFormats": { "outputFormat": formats_list },
            "outputTimeZone": "UTC",
            "trigger": trigger_data,
            # These keep backwards compatibility with any existing adapters expecting flat values
            "cronExpression": cron_expression or "",
            "department": department,
            "priority": priority,
        }

        if notification_email:
            schedule_data["deliveryMethod"] = "EMAIL"
            schedule_data["mailNotification"] = {
                "toAddresses": {
                    "address": [email.strip() for email in notification_email.split(",")]
                },
                "subject": f"Scheduled Report: {report.display_name or report.report_name}",
                "messageText": "Please find attached the scheduled report.",
                "resultSendType": "SEND_ATTACHMENT"
            }

        if max_runs is not None:
            schedule_data["maxRuns"] = max_runs
            schedule_data["max_runs"] = max_runs

        # Resolve report artefacts
        try:
            local_jrxml_path = ensure_local_jrxml(report, user)
            schedule_data["reportUnitUri"] = local_jrxml_path
            schedule_data["report_unit_uri"] = local_jrxml_path
        except Exception as e:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "schedule_report",
                                      "details": {"error": str(e)}},
                "message": f"Failed to prepare report files: {str(e)}",
            })

        try:
            adapter_payload = _resolve_yaml_datasource(report, user)
            schedule_data["dataAdapter"] = adapter_payload
            schedule_data["data_adapter"] = adapter_payload
        except Exception as e:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "schedule_report",
                                      "details": {"error": str(e)}},
                "message": f"Failed to resolve datasource: {str(e)}",
            })

        schedule_data["_user"] = user
        schedule_data["_organization"] = getattr(user, "organization", None)

        try:
            client = get_scheduler_client()
            engine_result = client.create_schedule(schedule_data)
            dag_id = (
                engine_result.get("jobId")
                or engine_result.get("dag_id")
                or schedule_data.get("dag_id", "")
            )

            stored_payload = {
                k: v for k, v in schedule_data.items()
                if k not in ("_user", "_organization") and not callable(v)
            }

            job = ScheduledJob.objects.create(
                dag_id=dag_id or f"{get_active_engine()}_{ScheduledJob.objects.count() + 1}",
                schedule_name=schedule_name,
                report=report,
                created_by=user,
                status="running",
                priority=priority,
                department=department,
                cron_expression=cron_expression or "",
                is_active=True,
                schedule_payload=stored_payload,
                created_on_engine=get_active_engine(),
            )

            display = report.display_name or report.report_name
            details = {
                "job_id": job.id,
                "schedule_name": schedule_name,
                "report_name": display,
                "cron": cron_expression,
                "department": department,
                "priority": priority,
            }
            if max_runs is not None:
                details["max_runs"] = max_runs

            # Build human-readable summary
            summary_parts = [f"Schedule '{schedule_name}' created for '{display}'."]
            summary_parts.append(f"Cron: {cron_expression}")
            if max_runs is not None:
                summary_parts.append(f"Max runs: {max_runs}")
            summary_parts.append(f"Department: {department}")
            summary_parts.append(f"Priority: {priority}")

            return json.dumps({
                "__action_result__": {
                    "type": "success",
                    "action": "schedule_report",
                    "details": details,
                },
                "message": " | ".join(summary_parts),
            })

        except Exception as e:
            logger.error(f"Chatbot schedule_report failed: {e}")
            return json.dumps({
                "__action_result__": {"type": "error", "action": "schedule_report",
                                      "details": {"error": str(e)}},
                "message": f"Failed to create schedule: {str(e)}",
            })

    return schedule_report


@tool
def list_jobs(status_filter: Optional[str] = None, search: Optional[str] = None) -> str:
    """List scheduled jobs in the system.

    Use this when the user asks to see, list, or check scheduled jobs,
    running jobs, or scheduled reports.

    Args:
        status_filter: Optional filter — one of "running", "finished", "failed", "on_hold".
        search: Optional text to search in job name, DAG ID, or department.
    """
    from reports.models import ScheduledJob
    from django.db import models as db_models

    jobs = ScheduledJob.objects.all()

    if status_filter:
        jobs = jobs.filter(status=status_filter.lower())
    if search:
        jobs = jobs.filter(
            db_models.Q(schedule_name__icontains=search)
            | db_models.Q(dag_id__icontains=search)
            | db_models.Q(department__icontains=search)
        )

    job_list = []
    for j in jobs[:20]:  # Cap at 20 to avoid overwhelming the LLM
        report_name = ""
        if j.report:
            report_name = j.report.display_name or j.report.report_name
        job_list.append({
            "id": j.id,
            "name": j.schedule_name,
            "report": report_name,
            "status": j.status,
            "cron": j.cron_expression,
            "department": j.department,
            "priority": j.priority,
            "is_active": j.is_active,
        })

    return json.dumps({
        "message": f"Found {len(job_list)} scheduled job(s).",
        "jobs": job_list,
        "total_count": jobs.count(),
    })


def _make_manage_job_tool(user):
    """Factory that creates a manage_job tool bound to a specific user."""

    @tool
    def manage_job(job_id: int, action: str) -> str:
        """Perform an action on a scheduled job.

        Use this when the user wants to hold, release, cancel, kill,
        restart, or otherwise manage a scheduled job.

        Args:
            job_id: The ID of the scheduled job (from list_jobs results).
            action: The action to perform. Must be one of:
                    hold, release, cancel, kill, force_start, restart,
                    force_restart, skip, mark_finished, mark_failed.
        """
        from reports.models import ScheduledJob
        from reports.scheduler import get_scheduler_client, get_active_engine

        VALID_ACTIONS = [
            "hold", "release", "cancel", "kill", "force_start",
            "restart", "force_restart", "skip", "mark_finished", "mark_failed",
        ]
        action = action.lower().strip()
        if action not in VALID_ACTIONS:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "manage_job",
                                      "details": {"error": f"Invalid action: {action}"}},
                "message": f"Invalid action '{action}'. Valid: {', '.join(VALID_ACTIONS)}",
            })

        try:
            job = ScheduledJob.objects.get(pk=job_id)
        except ScheduledJob.DoesNotExist:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "manage_job",
                                      "details": {"error": f"Job {job_id} not found"}},
                "message": f"Scheduled job with ID {job_id} not found.",
            })

        try:
            client = get_scheduler_client()
            engine_result = client.perform_action(
                job.dag_id, action, username=user.username,
            )

            STATUS_MAP = {
                "hold": "on_hold", "release": "running",
                "cancel": "failed", "kill": "failed",
                "force_start": "running", "restart": "running",
                "force_restart": "running", "mark_finished": "finished",
                "mark_failed": "failed",
            }
            new_status = STATUS_MAP.get(action)
            if new_status:
                job.status = new_status
                if action in ("hold", "kill"):
                    job.is_active = False
                elif action in ("release", "force_start", "restart", "force_restart"):
                    job.is_active = True
                job.save()

            return json.dumps({
                "__action_result__": {
                    "type": "success",
                    "action": "manage_job",
                    "details": {
                        "job_id": job.id,
                        "job_name": job.schedule_name,
                        "action": action,
                        "new_status": job.status,
                    },
                },
                "message": (
                    f"Action '{action}' performed on '{job.schedule_name}'. "
                    f"New status: {job.status}"
                ),
            })

        except Exception as e:
            logger.error(f"Chatbot manage_job failed: {e}")
            return json.dumps({
                "__action_result__": {"type": "error", "action": "manage_job",
                                      "details": {"error": str(e)}},
                "message": f"Action '{action}' failed: {str(e)}",
            })

    return manage_job


# ──────────────────────────────────────────────────────────────────────
# Data adapter tools (admin-only)
# ──────────────────────────────────────────────────────────────────────

def _make_create_data_adapter_tool(user):
    """Factory that creates a create_data_adapter tool bound to a specific user."""

    @tool
    def create_data_adapter(
        name: str,
        adapter_type: str,
        url: str = "",
        username: str = "",
        password: str = "",
        host: str = "",
        port: int = 5432,
        database: str = "",
    ) -> str:
        """Create a new data adapter (database connection).

        Use this when the user wants to create, add, or configure a new
        data source connection.

        IMPORTANT: Extract connection details from natural language. Examples:
        - "add a PostgreSQL connection to mydb on localhost" → adapter_type="jdbc", host="localhost", database="mydb"
        - "create a CSV data source called Sales Data" → adapter_type="csv", name="Sales Data"
        - "connect to postgres://user:pass@host:5432/db" → parse the URL into components

        Translate natural language adapter types:
        - "PostgreSQL" / "Postgres" / "PG" → adapter_type="jdbc"
        - "MySQL" / "SQL" / "database" → adapter_type="jdbc"
        - "CSV file" → adapter_type="csv"
        - "JSON file" → adapter_type="json"
        - "XML file" → adapter_type="xml"
        - "in-memory" / "test data" → adapter_type="inmemory"

        If you create a file-based adapter (csv, json, xml), you MUST explicitly ask the user 
        to upload the corresponding file using the web interface, as the chatbot cannot accept file uploads directly.

        Args:
            name: A descriptive name for the adapter (e.g., "Sales_DB").
            adapter_type: Type of adapter — one of: jdbc, csv, json, xml, inmemory, mock.
            url: JDBC connection URL (for jdbc type), e.g. "jdbc:postgresql://host:5432/dbname".
            username: Database username (for jdbc type).
            password: Database password (for jdbc type).
            host: Database host (alternative to url for jdbc type).
            port: Database port (alternative to url for jdbc type). Defaults to 5432.
            database: Database name (alternative to url for jdbc type).
        """
        from dataadapter.models import DataAdapter
        from audit.utils import log_action

        adapter_type = adapter_type.lower().strip()
        VALID_TYPES = {"jdbc", "csv", "json", "xml", "inmemory", "mock"}
        TYPE_MAP = {"postgresql": "jdbc", "postgres": "jdbc", "pg": "jdbc",
                    "mysql": "jdbc", "sql": "jdbc"}
        adapter_type = TYPE_MAP.get(adapter_type, adapter_type)

        if adapter_type not in VALID_TYPES:
            return json.dumps({
                "__action_result__": {"type": "error", "action": "create_data_adapter",
                                      "details": {"error": f"Unknown adapter type: {adapter_type}"}},
                "message": f"Unknown adapter type '{adapter_type}'. Valid: {', '.join(sorted(VALID_TYPES))}",
            })

        # Build connection details
        connection_details = {}
        if adapter_type == "jdbc":
            if url:
                connection_details["url"] = url
            elif host and database:
                connection_details["url"] = f"jdbc:postgresql://{host}:{port}/{database}"
            connection_details["username"] = username
            connection_details["password"] = password
            if host:
                connection_details["host"] = host
            connection_details["port"] = port
            if database:
                connection_details["database"] = database

        try:
            adapter = DataAdapter.objects.create(
                name=name,
                adapter_type=adapter_type,
                connection_details=connection_details,
                is_active=True,
                created_by=user,
            )

            log_action(user, "data_adapter_created", {
                "adapter_name": name,
                "adapter_type": adapter_type,
                "source": "chatbot",
            })

            msg = f"Data adapter '{name}' ({adapter_type}) created successfully."
            if adapter_type in ("csv", "json", "xml"):
                msg += " Please make sure to upload the required file for this adapter in the Data Adapters menu."

            return json.dumps({
                "__action_result__": {
                    "type": "success",
                    "action": "create_data_adapter",
                    "details": {
                        "adapter_id": adapter.id,
                        "name": name,
                        "type": adapter_type,
                    },
                },
                "message": msg,
            })

        except Exception as e:
            logger.error(f"Chatbot create_data_adapter failed: {e}")
            return json.dumps({
                "__action_result__": {"type": "error", "action": "create_data_adapter",
                                      "details": {"error": str(e)}},
                "message": f"Failed to create data adapter: {str(e)}",
            })

    return create_data_adapter


@tool
def list_adapters() -> str:
    """List all configured data adapters/data sources.

    Use this when the user asks about existing data sources,
    database connections, or adapters.
    """
    from dataadapter.models import DataAdapter

    adapters = DataAdapter.objects.all().order_by("-created_at")
    adapter_list = []
    for a in adapters[:20]:
        adapter_list.append({
            "id": a.id,
            "name": a.name,
            "type": a.adapter_type,
            "is_active": a.is_active,
        })

    return json.dumps({
        "message": f"Found {len(adapter_list)} data adapter(s).",
        "adapters": adapter_list,
    })


@tool
def test_adapter_connection(adapter_id: int) -> str:
    """Test the connection of an existing data adapter.

    Use this when the user wants to verify or test a data source connection.

    Args:
        adapter_id: The ID of the adapter to test (from list_adapters results).
    """
    from dataadapter.models import DataAdapter
    import psycopg2
    import os

    try:
        adapter = DataAdapter.objects.get(pk=adapter_id)
    except DataAdapter.DoesNotExist:
        return json.dumps({
            "__action_result__": {"type": "error", "action": "test_adapter_connection",
                                  "details": {"error": f"Adapter {adapter_id} not found"}},
            "message": f"Data adapter with ID {adapter_id} not found.",
        })

    details = adapter.connection_details
    adapter_type = adapter.adapter_type

    try:
        if adapter_type == "jdbc":
            import re
            host = details.get("host", "localhost")
            port = details.get("port", 5432)
            database = details.get("database", "postgres")
            username = details.get("username", "postgres")
            password = details.get("password", "")

            jdbc_url = details.get("url", "")
            if jdbc_url:
                match = re.match(
                    r"jdbc:postgresql://([^:/]+)(?::(\d+))?/(\w+)", jdbc_url
                )
                if match:
                    host = match.group(1)
                    port = int(match.group(2)) if match.group(2) else 5432
                    database = match.group(3)

            conn = psycopg2.connect(
                host=host, port=port, database=database,
                user=username, password=password, connect_timeout=5,
            )
            conn.close()
            msg = "PostgreSQL connection successful"
            success = True

        elif adapter_type in ("csv", "json", "xml"):
            file_path = details.get("file_path", "")
            if file_path and os.path.exists(file_path):
                msg = f"{adapter_type.upper()} file is accessible"
                success = True
            else:
                msg = f"{adapter_type.upper()} file not found"
                success = False

        elif adapter_type in ("inmemory", "mock"):
            msg = f"{adapter_type.capitalize()} data source is ready"
            success = True
        else:
            msg = f"Unknown adapter type: {adapter_type}"
            success = False

        result_type = "success" if success else "error"
        return json.dumps({
            "__action_result__": {
                "type": result_type,
                "action": "test_adapter_connection",
                "details": {
                    "adapter_id": adapter.id,
                    "adapter_name": adapter.name,
                    "success": success,
                    "message": msg,
                },
            },
            "message": f"Connection test for '{adapter.name}': {msg}",
        })

    except Exception as e:
        return json.dumps({
            "__action_result__": {"type": "error", "action": "test_adapter_connection",
                                  "details": {"error": str(e)}},
            "message": f"Connection test failed: {str(e)}",
        })


def _make_get_report_parameters_tool(user):
    @tool
    def get_report_parameters(report_id: int) -> str:
        """Get the parameter schema for a specific report.
        
        Always use this tool FIRST when a user asks to fill a form, filter,
        or apply criteria to a report in natural language. You must know the 
        expected parameter names and types before you can fill the form.
        """
        from reports.models import Report
        from reports.views import ensure_local_jrxml
        import requests as http_requests
        from django.conf import settings
        
        try:
            report = Report.objects.filter(id=report_id).first()
            if not report:
                return f"Error: Report with ID {report_id} not found."
                
            local_jrxml_path = ensure_local_jrxml(report, user)
            
            validation_url = f"{settings.COMPILER_SERVICE_URL}/api/validation/parameters"
            payload = {
                'sourceType': 'LOCAL',
                'path': local_jrxml_path,
                'parameters': {},
            }
            
            resp = http_requests.post(validation_url, json=payload, timeout=60)
            if resp.status_code != 200:
                return f"Error: Failed to fetch parameters from compiler service."
                
            raw = resp.json()
            details = raw.get('parameterDetails', [])
            
            # Create a compact schema for the LLM
            schema = []
            for p in details:
                if not p.get('forPrompting', True):
                    continue
                schema.append({
                    "name": p.get('name'),
                    "type": p.get('expectedType', '').split('.')[-1],
                    "default": p.get('defaultValue')
                })
            
            return json.dumps({
                "message": f"Parameter schema for {report.report_name}. Use fill_report_form to apply values.",
                "schema": schema
            })
            
        except Exception as e:
            return f"Error fetching parameters: {str(e)}"
            
    return get_report_parameters

def _make_fill_report_form_tool(user):
    @tool
    def fill_report_form(report_id: int, parameters_json: str) -> str:
        """Fill a report's parameter form with structured data.
        
        Use this tool AFTER you have fetched the parameter schema using get_report_parameters.
        Extract the values the user requested in natural language and map them to the 
        correct parameter names in the schema.
        
        Args:
            report_id: The ID of the report.
            parameters_json: A JSON string mapping parameter names to extracted values.
                             E.g. '{"start_date": "2025-01-01", "department": "Sales"}'
        """
        try:
            params = json.loads(parameters_json)
            return json.dumps({
                "__action__": "fill_form",
                "report_id": report_id,
                "parameters": params,
                "message": "Form parameters extracted successfully.",
            })
        except json.JSONDecodeError:
            return "Error: parameters_json must be a valid JSON string."

    return fill_report_form


# ──────────────────────────────────────────────────────────────────────
# Tool builder — called by ChatbotAPIView to assemble the tools list
# ──────────────────────────────────────────────────────────────────────

def build_tools_for_user(user):
    """
    Return a list of LangChain tools available to the given user.

    Navigation + compile are available to all authenticated users.
    Scheduling, adapter, and job management tools require admin privileges.
    """
    tools = [
        navigate_to_page,
        open_report,
        _make_compile_report_tool(user),
        _make_get_report_parameters_tool(user),
        _make_fill_report_form_tool(user),
    ]

    if user.is_admin:
        tools += [
            _make_schedule_report_tool(user),
            list_jobs,
            _make_manage_job_tool(user),
            _make_create_data_adapter_tool(user),
            list_adapters,
            test_adapter_connection,
        ]

    return tools
