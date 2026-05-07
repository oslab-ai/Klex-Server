"""
URL patterns for github_integration app.
"""
from django.urls import path
from .views import GitHubAuthView, GitHubCallbackView, GitHubReposView, GitHubStatusView

urlpatterns = [
    path('auth/', GitHubAuthView.as_view(), name='github-auth'),
    path('callback/', GitHubCallbackView.as_view(), name='github-callback'),
    path('repos/', GitHubReposView.as_view(), name='github-repos'),
    path('status/', GitHubStatusView.as_view(), name='github-status'),
]
