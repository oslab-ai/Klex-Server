"""
URL patterns for reports app.
"""
from django.urls import path
from .views import (
    OpenAPISpecView,
    ReportListView,
    ReportDetailView,
    ReportCompileView,
    ReportDownloadView,
    ReportPermissionListView,
    ReportPermissionDetailView,
    ExecutionListView,
    ExecutionDetailView,
    ReportMetadataView,
    ReportParametersView,
    ReportParameterMetadataView,
    ReportAuditLogsView,
    ReportAuditStatsView,
    # Unified scheduling (Quartz / Airflow)
    SchedulerEngineView,
    ScheduledJobListView,
    ScheduledJobDetailView,
    ScheduleReportView,
    ScheduleDeleteView,
    ScheduledJobActionView,
    ScheduledJobPriorityView,
    ScheduledJobCommentView,
    ScheduledJobHistoryView,
    ScheduledJobLogsView,
    # Dispatch service (Airflow-only)
    SubmitReportJobView,
    DispatchQueueSummaryView,
    # Engine migration
    SchedulerEngineSwitchView,
    AirflowUIRedirectView,
    # Metadata refresh
    ReportMetadataBulkRefreshView,
)

urlpatterns = [
    path('openapi.json', OpenAPISpecView.as_view(), name='openapi-spec'),
    path('', ReportListView.as_view(), name='report-list'),
    path('<int:pk>/', ReportDetailView.as_view(), name='report-detail'),
    path('<int:pk>/compile/', ReportCompileView.as_view(), name='report-compile'),
    path('<int:pk>/download/', ReportDownloadView.as_view(), name='report-download'),
    path('<int:pk>/download/<int:execution_id>/', ReportDownloadView.as_view(), name='report-download-execution'),
    path('<int:pk>/metadata/', ReportMetadataView.as_view(), name='report-metadata'),
    path('<int:pk>/parameters/', ReportParametersView.as_view(), name='report-parameters'),
    path('<int:pk>/parameter-metadata/', ReportParameterMetadataView.as_view(), name='report-parameter-metadata'),
    path('permissions/', ReportPermissionListView.as_view(), name='permission-list'),
    path('permissions/<int:pk>/', ReportPermissionDetailView.as_view(), name='permission-detail'),
    path('executions/', ExecutionListView.as_view(), name='execution-list'),
    path('executions/<int:pk>/', ExecutionDetailView.as_view(), name='execution-detail'),
    path('audit/logs/', ReportAuditLogsView.as_view(), name='report-audit-logs'),
    path('audit/stats/', ReportAuditStatsView.as_view(), name='report-audit-stats'),
    # Unified scheduling (Quartz / Airflow)
    path('scheduler-engine/', SchedulerEngineView.as_view(), name='scheduler-engine'),
    path('jobs/', ScheduledJobListView.as_view(), name='job-list'),
    path('jobs/<int:pk>/', ScheduledJobDetailView.as_view(), name='job-detail'),
    path('jobs/<int:pk>/action/', ScheduledJobActionView.as_view(), name='job-action'),
    path('jobs/<int:pk>/priority/', ScheduledJobPriorityView.as_view(), name='job-priority'),
    path('jobs/<int:pk>/comments/', ScheduledJobCommentView.as_view(), name='job-comments'),
    path('jobs/<int:pk>/history/', ScheduledJobHistoryView.as_view(), name='job-history'),
    path('jobs/<int:pk>/logs/', ScheduledJobLogsView.as_view(), name='job-logs'),
    path('schedule/', ScheduleReportView.as_view(), name='schedule-report'),
    path('schedule/<int:pk>/', ScheduleDeleteView.as_view(), name='schedule-delete'),
    # Dispatch service (Airflow-only)
    path('dispatch/submit/', SubmitReportJobView.as_view(), name='dispatch-submit'),
    path('dispatch/summary/', DispatchQueueSummaryView.as_view(), name='dispatch-summary'),
    # Engine migration
    path('scheduler-engine/switch/', SchedulerEngineSwitchView.as_view(), name='scheduler-engine-switch'),
    path('airflow-ui/', AirflowUIRedirectView.as_view(), name='airflow-ui'),
    # Metadata cache management
    path('metadata/refresh/', ReportMetadataBulkRefreshView.as_view(), name='report-metadata-refresh'),
]

