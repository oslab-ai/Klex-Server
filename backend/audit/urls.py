"""
URL patterns for audit app.
"""
from django.urls import path
from .views import AuditLogListView

urlpatterns = [
    path('', AuditLogListView.as_view(), name='audit-log-list'),
]
