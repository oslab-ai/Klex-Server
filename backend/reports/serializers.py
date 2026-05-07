"""
Serializers for reports app.
"""
from rest_framework import serializers
from .models import Report, ReportPermission, OrganizationReportPermission, Execution, ScheduledJob, JobComment


class ReportSerializer(serializers.ModelSerializer):
    """Serializer for Report model."""
    repo_name = serializers.CharField(source='repo.full_name', read_only=True)
    groups = serializers.SerializerMethodField()

    class Meta:
        model = Report
        fields = [
            'id', 'repo', 'repo_name', 'path', 'report_name',
            'display_name', 'latest_commit', 'is_public', 'created_at',
            'groups',
        ]
        read_only_fields = ['id', 'created_at']

    def get_groups(self, obj):
        return list(
            obj.group_memberships.values_list('group_id', flat=True)
        )


class ReportPermissionSerializer(serializers.ModelSerializer):
    """Serializer for ReportPermission model."""
    user_name = serializers.CharField(source='user.username', read_only=True)
    report_name = serializers.CharField(source='report.report_name', read_only=True)

    class Meta:
        model = ReportPermission
        fields = [
            'id', 'user', 'user_name', 'report', 'report_name',
            'can_access', 'can_schedule', 'created_at'
        ]


class OrganizationReportPermissionSerializer(serializers.ModelSerializer):
    """Serializer for OrganizationReportPermission model."""
    organization_name = serializers.CharField(source='organization.name', read_only=True)
    report_name = serializers.CharField(source='report.report_name', read_only=True)

    class Meta:
        model = OrganizationReportPermission
        fields = [
            'id', 'organization', 'organization_name', 'report', 'report_name',
            'can_access', 'can_schedule', 'created_at'
        ]


class ExecutionSerializer(serializers.ModelSerializer):
    """Serializer for Execution model."""
    report_name = serializers.CharField(source='report.report_name', read_only=True)
    invoked_by_name = serializers.CharField(source='invoked_by.username', read_only=True)

    class Meta:
        model = Execution
        fields = [
            'id', 'report', 'report_name', 'invoked_by', 'invoked_by_name',
            'status', 'started_at', 'finished_at', 'output_location', 'created_at'
        ]
        read_only_fields = ['id', 'created_at', 'started_at', 'finished_at', 'output_location']


class ScheduledJobSerializer(serializers.ModelSerializer):
    """Serializer for ScheduledJob model."""
    report_name = serializers.CharField(source='report.report_name', read_only=True, default=None)
    created_by_name = serializers.CharField(source='created_by.username', read_only=True, default=None)
    comment_count = serializers.IntegerField(source='comments.count', read_only=True)

    class Meta:
        model = ScheduledJob
        fields = [
            'id', 'dag_id', 'schedule_name', 'report', 'report_name',
            'created_by', 'created_by_name', 'status', 'priority',
            'department', 'cron_expression', 'frequency_label',
            'machine_name', 'estimated_runtime_min', 'max_runtime_min',
            'next_run', 'last_run', 'is_active',
            'termination_description', 'exit_code',
            'comment_count', 'created_at', 'updated_at',
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']


class JobCommentSerializer(serializers.ModelSerializer):
    """Serializer for JobComment model."""
    user_name = serializers.CharField(source='user.username', read_only=True, default=None)

    class Meta:
        model = JobComment
        fields = ['id', 'job', 'user', 'user_name', 'text', 'created_at']
        read_only_fields = ['id', 'created_at']
