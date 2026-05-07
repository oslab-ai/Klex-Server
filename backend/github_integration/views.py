"""
Views for GitHub OAuth integration.
"""
import requests
from urllib.parse import urlencode

from django.conf import settings
from django.shortcuts import redirect
from rest_framework import status
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import GitHubToken
from .serializers import GitHubRepoSerializer


class GitHubAuthView(APIView):
    """
    Start GitHub OAuth flow.
    Returns the URL to redirect user to GitHub.
    """
    permission_classes = [IsAuthenticated]
    
    def get(self, request):
        if not settings.GITHUB_CLIENT_ID:
            return Response(
                {'error': 'GitHub OAuth not configured'},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        
        # Build GitHub OAuth URL
        params = {
            'client_id': settings.GITHUB_CLIENT_ID,
            'redirect_uri': f"{settings.BACKEND_BASE_URL}/api/github/callback/",
            'scope': 'repo read:user',
            'state': str(request.user.id),  # Pass user ID in state
        }
        
        auth_url = f"https://github.com/login/oauth/authorize?{urlencode(params)}"
        
        return Response({
            'auth_url': auth_url,
            'message': 'Redirect user to this URL to authorize GitHub access'
        })


class GitHubCallbackView(APIView):
    """
    Handle GitHub OAuth callback.
    Exchange code for access token and redirect to frontend.
    """
    permission_classes = [AllowAny]  # Can't require auth on OAuth callback
    
    def get(self, request):
        code = request.query_params.get('code')
        state = request.query_params.get('state')  # User ID
        error = request.query_params.get('error')
        
        if error:
            # Redirect to frontend with error
            return redirect(f"{settings.FRONTEND_BASE_URL}/?github_error={error}")
        
        if not code:
            return redirect(f"{settings.FRONTEND_BASE_URL}/?github_error=no_code")
        
        # Exchange code for access token
        token_url = 'https://github.com/login/oauth/access_token'
        payload = {
            'client_id': settings.GITHUB_CLIENT_ID,
            'client_secret': settings.GITHUB_CLIENT_SECRET,
            'code': code,
            'redirect_uri': f"{settings.BACKEND_BASE_URL}/api/github/callback/",
        }
        headers = {'Accept': 'application/json'}
        
        try:
            response = requests.post(token_url, data=payload, headers=headers, timeout=30)
            response.raise_for_status()
            token_data = response.json()
        except requests.RequestException as e:
            return redirect(f"{settings.FRONTEND_BASE_URL}/?github_error=token_exchange_failed")
        
        if 'error' in token_data:
            return redirect(f"{settings.FRONTEND_BASE_URL}/?github_error={token_data['error']}")
        
        access_token = token_data.get('access_token')
        if not access_token:
            return redirect(f"{settings.FRONTEND_BASE_URL}/?github_error=no_access_token")
        
        # Store token for user
        from accounts.models import User
        try:
            user = User.objects.get(id=state)
        except User.DoesNotExist:
            return redirect(f"{settings.FRONTEND_BASE_URL}/?github_error=invalid_user")
        
        GitHubToken.objects.update_or_create(
            user=user,
            defaults={
                'access_token': access_token,
                'token_type': token_data.get('token_type', 'bearer'),
                'scope': token_data.get('scope', ''),
            }
        )
        
        # Redirect to frontend homepage with success
        return redirect(f"{settings.FRONTEND_BASE_URL}/?github_connected=true")


class GitHubReposView(APIView):
    """
    List GitHub repositories for the authenticated user.
    """
    permission_classes = [IsAuthenticated]
    
    def get(self, request):
        try:
            github_token = GitHubToken.objects.get(user=request.user)
        except GitHubToken.DoesNotExist:
            return Response(
                {'error': 'GitHub not connected. Please connect GitHub first.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Fetch repos from GitHub
        headers = {
            'Authorization': f'token {github_token.access_token}',
            'Accept': 'application/vnd.github.v3+json',
        }
        
        try:
            # Get user repos (including private if scope allows)
            response = requests.get(
                'https://api.github.com/user/repos',
                headers=headers,
                params={'per_page': 100, 'sort': 'updated'},
                timeout=30
            )
            response.raise_for_status()
            repos = response.json()
        except requests.RequestException as e:
            return Response(
                {'error': f'Failed to fetch repositories: {str(e)}'},
                status=status.HTTP_502_BAD_GATEWAY
            )
        
        # Serialize and return
        serializer = GitHubRepoSerializer(repos, many=True)
        return Response(serializer.data)


class GitHubStatusView(APIView):
    """
    Check if GitHub is connected for the current user.
    """
    permission_classes = [IsAuthenticated]
    
    def get(self, request):
        try:
            github_token = GitHubToken.objects.get(user=request.user)
            return Response({
                'connected': True,
                'scope': github_token.scope,
            })
        except GitHubToken.DoesNotExist:
            return Response({
                'connected': False,
            })
