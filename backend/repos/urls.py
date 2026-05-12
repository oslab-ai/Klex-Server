"""
URL patterns for repos app.
"""
from django.urls import path
from .views import RepoListView, RepoDetailView, RepoSyncView, RepoChangeView

urlpatterns = [
    path('', RepoListView.as_view(), name='repo-list'),
    path('<int:pk>/', RepoDetailView.as_view(), name='repo-detail'),
    path('<int:pk>/sync/', RepoSyncView.as_view(), name='repo-sync'),
    path('<int:pk>/change/', RepoChangeView.as_view(), name='repo-change'),
]

