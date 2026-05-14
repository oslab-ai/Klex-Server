"""
Views for repos app.
"""
from django.utils import timezone
from rest_framework import status, generics
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Repo
from .serializers import RepoSerializer, RepoCreateSerializer
from reports.models import Report
from github_integration.models import GitHubToken
from audit.utils import log_action

import requests


class RepoListView(generics.ListCreateAPIView):
    """List all repos or create a new repo."""
    queryset = Repo.objects.all().order_by('-created_at')
    permission_classes = [IsAuthenticated]
    
    def get_serializer_class(self):
        if self.request.method == 'POST':
            return RepoCreateSerializer
        return RepoSerializer
    
    def create(self, request, *args, **kwargs):
        # Only admin can create repos
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required to create repositories'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().create(request, *args, **kwargs)
    
    def perform_create(self, serializer):
        repo = serializer.save(created_by=self.request.user)
        log_action(self.request.user, 'repo_created', {
            'repo': f"{repo.owner}/{repo.name}"
        })


class RepoDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Get, update, or delete a specific repo."""
    queryset = Repo.objects.all()
    serializer_class = RepoSerializer
    permission_classes = [IsAuthenticated]
    
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


class RepoSyncView(APIView):
    """
    Sync a repo - fetch folders from GitHub and create report entries.
    Each folder under path_prefix becomes a report.
    """
    permission_classes = [IsAuthenticated]
    
    def post(self, request, pk):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        try:
            repo = Repo.objects.get(pk=pk)
        except Repo.DoesNotExist:
            return Response(
                {'error': 'Repo not found'},
                status=status.HTTP_404_NOT_FOUND
            )
        
        # Get GitHub token
        try:
            github_token = GitHubToken.objects.get(user=request.user)
        except GitHubToken.DoesNotExist:
            return Response(
                {'error': 'GitHub not connected. Please connect GitHub first.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Fetch folder listing from GitHub
        headers = {
            'Authorization': f'token {github_token.access_token}',
            'Accept': 'application/vnd.github.v3+json',
        }
        
        # Get contents of path_prefix directory (or root if empty)
        base_url = f'https://api.github.com/repos/{repo.owner}/{repo.name}/contents'
        if repo.path_prefix:
            url = f'{base_url}/{repo.path_prefix}'
        else:
            url = base_url
        params = {'ref': repo.branch}
        
        try:
            response = requests.get(url, headers=headers, params=params, timeout=30)
            response.raise_for_status()
        except requests.RequestException as e:
            return Response(
                {'error': f'Failed to fetch from GitHub: {str(e)}'},
                status=status.HTTP_502_BAD_GATEWAY
            )
        
        contents = response.json()
        
        # Filter to directories only, exclude dot-folders
        folders = [
            item for item in contents 
            if item.get('type') == 'dir' and not item.get('name', '').startswith('.')
        ]
        
        # Create or update reports for each folder
        created_count = 0
        updated_count = 0
        
        for folder in folders:
            folder_name = folder['name']
            folder_path = f"{repo.path_prefix}/{folder_name}"
            
            report, created = Report.objects.update_or_create(
                repo=repo,
                path=folder_path,
                defaults={
                    'report_name': folder_name,
                    'display_name': folder_name.replace('_', ' ').replace('-', ' ').title(),
                    'latest_commit': folder.get('sha'),
                }
            )
            
            if created:
                created_count += 1
            else:
                updated_count += 1
        
        # Update last_synced_at
        repo.last_synced_at = timezone.now()
        repo.save()
        
        log_action(request.user, 'repo_synced', {
            'repo': repo.full_name,
            'created': created_count,
            'updated': updated_count,
        })

        # Trigger metadata cache population in a background thread for
        # reports that don't have cached metadata yet (non-blocking).
        import threading
        from reports.views import _populate_report_metadata_cache

        uncached = Report.objects.filter(
            repo=repo, metadata_cache={},
        ).select_related('repo')
        if uncached.exists():
            def _bg_populate(reports_qs, user):
                for r in reports_qs:
                    _populate_report_metadata_cache(r, user)

            threading.Thread(
                target=_bg_populate,
                args=(list(uncached), request.user),
                daemon=True,
            ).start()

        return Response({
            'message': 'Sync completed',
            'created': created_count,
            'updated': updated_count,
            'total_reports': Report.objects.filter(repo=repo).count(),
        })
