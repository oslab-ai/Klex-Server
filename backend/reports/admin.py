from django.contrib import admin
from .models import Report, ReportPermission, Execution, ReportExecutionJob

@admin.register(Report)
class ReportAdmin(admin.ModelAdmin):
    list_display = ['report_name', 'display_name', 'repo', 'path', 'created_at']
    list_filter = ['repo']
    search_fields = ['report_name', 'display_name', 'path']

@admin.register(ReportPermission)
class ReportPermissionAdmin(admin.ModelAdmin):
    list_display = ['user', 'report', 'can_access', 'can_schedule', 'created_at']
    list_filter = ['can_access', 'can_schedule']

@admin.register(Execution)
class ExecutionAdmin(admin.ModelAdmin):
    list_display = ['report', 'invoked_by', 'status', 'started_at', 'finished_at']
    list_filter = ['status']


@admin.register(ReportExecutionJob)
class ReportExecutionJobAdmin(admin.ModelAdmin):
    list_display = [
        'id', 'dag_id', 'status', 'priority', 'workload_class',
        'overlap_policy', 'retry_count', 'requested_at', 'completed_at',
    ]
    list_filter = ['status', 'priority', 'workload_class', 'overlap_policy']
    search_fields = ['dag_id', 'dedupe_key', 'airflow_run_id']
    readonly_fields = ['id', 'requested_at', 'created_at', 'updated_at']
