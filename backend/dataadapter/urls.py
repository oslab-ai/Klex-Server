"""
URL patterns for dataadapter app.
"""
from django.urls import path
from .views import DataAdapterListView, DataAdapterDetailView, TestConnectionView

urlpatterns = [
    path('', DataAdapterListView.as_view(), name='adapter-list'),
    path('<int:pk>/', DataAdapterDetailView.as_view(), name='adapter-detail'),
    path('<int:pk>/test/', TestConnectionView.as_view(), name='adapter-test'),
    path('test/', TestConnectionView.as_view(), name='test-connection'),
]
