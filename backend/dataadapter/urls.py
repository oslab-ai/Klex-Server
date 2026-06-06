"""
URL patterns for dataadapter app.
"""
from django.urls import path
from .views import (
    DataAdapterListView,
    DataAdapterDetailView,
    TestConnectionView,
    DataAdapterTablesView,
    DataAdapterColumnsView,
    DataAdapterQueryView,
    DataAdapterDistinctValuesView,
    DataAdapterMultiColumnsView,
)

urlpatterns = [
    path('', DataAdapterListView.as_view(), name='adapter-list'),
    path('<int:pk>/', DataAdapterDetailView.as_view(), name='adapter-detail'),
    path('<int:pk>/test/', TestConnectionView.as_view(), name='adapter-test'),
    path('test/', TestConnectionView.as_view(), name='test-connection'),
    # Data Exploration endpoints
    path('<int:pk>/tables/', DataAdapterTablesView.as_view(), name='adapter-tables'),
    path('<int:pk>/columns/', DataAdapterColumnsView.as_view(), name='adapter-columns'),
    path('<int:pk>/multi-columns/', DataAdapterMultiColumnsView.as_view(), name='adapter-multi-columns'),
    path('<int:pk>/query/', DataAdapterQueryView.as_view(), name='adapter-query'),
    path('<int:pk>/distinct-values/', DataAdapterDistinctValuesView.as_view(), name='adapter-distinct-values'),
]

