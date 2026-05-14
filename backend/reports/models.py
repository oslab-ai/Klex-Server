"""
Report, ReportPermission, Execution, and ReportExecutionJob models.
"""
import uuid

from django.db import models
from django.conf import settings

from .enums import JobStatus, OverlapPolicy, WorkloadClass, PriorityTier


class Report(models.Model):
    """
    A report corresponds to a folder in a GitHub repository.
    Each folder under path_prefix becomes one report entry.
    """
    repo = models.ForeignKey(
        'repos.Repo',
        on_delete=models.CASCADE,
        related_name='reports'
    )
    path = models.CharField(max_length=500)  # Relative path inside repo
    report_name = models.CharField(max_length=255)  # Must match folder name
    latest_commit = models.CharField(max_length=40, null=True, blank=True)
    display_name = models.CharField(max_length=255, null=True, blank=True)
    is_public = models.BooleanField(default=True)  # Visible to all authenticated users
    metadata_cache = models.JSONField(
        default=dict, blank=True,
        help_text="Compact metadata summary for chatbot/search. "
                  "Populated from Java MetadataService.",
    )
    metadata_cached_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        db_table = 'reports'
        unique_together = ['repo', 'path']
    
    def __str__(self):
        return self.display_name or self.report_name


class ReportPermission(models.Model):
    """
    Maps user permissions to reports.
    Admin users implicitly have all permissions.
    """
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='report_permissions'
    )
    report = models.ForeignKey(
        Report,
        on_delete=models.CASCADE,
        related_name='permissions'
    )
    can_access = models.BooleanField(default=True)
    can_schedule = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        db_table = 'report_permissions'
        unique_together = ['user', 'report']
        indexes = [
            models.Index(fields=['user'], name='idx_report_permissions_user'),
            models.Index(fields=['report'], name='idx_report_permissions_report'),
        ]
    
    def __str__(self):
        return f"{self.user.username} -> {self.report.report_name}"


class OrganizationReportPermission(models.Model):
    """
    Maps organization-level permissions to reports.
    All members of the organization inherit these permissions.
    """
    organization = models.ForeignKey(
        'accounts.Organization',
        on_delete=models.CASCADE,
        related_name='report_permissions'
    )
    report = models.ForeignKey(
        Report,
        on_delete=models.CASCADE,
        related_name='org_permissions'
    )
    can_access = models.BooleanField(default=True)
    can_schedule = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'organization_report_permissions'
        unique_together = ['organization', 'report']
        indexes = [
            models.Index(fields=['organization'], name='idx_org_report_perm_org'),
            models.Index(fields=['report'], name='idx_org_report_perm_report'),
        ]

    def __str__(self):
        return f"{self.organization.name} -> {self.report.report_name}"


class Execution(models.Model):
    """
    Tracks report execution history.
    """
    STATUS_CHOICES = [
        ('queued', 'Queued'),
        ('running', 'Running'),
        ('success', 'Success'),
        ('failure', 'Failure'),
    ]
    
    report = models.ForeignKey(
        Report,
        on_delete=models.CASCADE,
        related_name='executions'
    )
    invoked_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        related_name='executions'
    )
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='queued')
    output_format = models.CharField(max_length=10, default='pdf')  # pdf, xlsx, csv, html, docx, pptx, odt
    started_at = models.DateTimeField(null=True, blank=True)
    finished_at = models.DateTimeField(null=True, blank=True)
    output_location = models.CharField(max_length=500, null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        db_table = 'executions'
        ordering = ['-created_at']
    
    def __str__(self):
        return f"{self.report.report_name} - {self.status}"


class ScheduledJob(models.Model):
    """
    Unified local shadow for scheduled report jobs.

    Serves as the single source of truth regardless of whether the
    active scheduling engine is Quartz or Airflow.  The
    ``schedule_payload`` stores the full engine-agnostic configuration
    needed to recreate this schedule on *any* engine during migration.
    """
    STATUS_CHOICES = [
        ('running', 'Running'),
        ('finished', 'Finished'),
        ('failed', 'Failed'),
        ('on_hold', 'On Hold'),
    ]

    ENGINE_CHOICES = [
        ('quartz', 'Quartz'),
        ('airflow', 'Airflow'),
    ]

    dag_id = models.CharField(max_length=255, unique=True)
    schedule_name = models.CharField(max_length=255)
    report = models.ForeignKey(
        Report,
        on_delete=models.SET_NULL,
        null=True, blank=True,
        related_name='scheduled_jobs',
    )
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        related_name='scheduled_jobs',
    )
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='running')
    priority = models.IntegerField(default=0)
    department = models.CharField(max_length=100, default='General')
    cron_expression = models.CharField(max_length=255, blank=True, default='')
    frequency_label = models.CharField(max_length=100, blank=True, default='')
    machine_name = models.CharField(max_length=255, blank=True, default='')
    estimated_runtime_min = models.FloatField(null=True, blank=True)
    max_runtime_min = models.FloatField(null=True, blank=True, default=30)
    next_run = models.DateTimeField(null=True, blank=True)
    last_run = models.DateTimeField(null=True, blank=True)
    is_active = models.BooleanField(default=True)  # synced with Airflow is_paused
    termination_description = models.CharField(max_length=500, blank=True, default='')
    exit_code = models.CharField(max_length=100, blank=True, default='')

    # ── migration support ───────────────────────────────────────
    schedule_payload = models.JSONField(
        default=dict,
        blank=True,
        help_text="Full schedule configuration — enough to recreate this job on any engine.",
    )
    created_on_engine = models.CharField(
        max_length=20,
        choices=ENGINE_CHOICES,
        default='quartz',
        help_text="Scheduling engine that originally created this job.",
    )

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'scheduled_jobs'
        ordering = ['-priority', '-created_at']

    def __str__(self):
        return f"{self.schedule_name} ({self.dag_id})"


class JobComment(models.Model):
    """Comment thread on a scheduled job."""
    job = models.ForeignKey(
        ScheduledJob,
        on_delete=models.CASCADE,
        related_name='comments',
    )
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        related_name='job_comments',
    )
    text = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'job_comments'
        ordering = ['-created_at']

    def __str__(self):
        return f"Comment on {self.job.schedule_name} by {self.user}"


class ReportExecutionJob(models.Model):
    """
    Application-side persistent queue for report executions.

    This model is the single source-of-truth for job lifecycle before and
    after Airflow is involved.  Airflow is only triggered when the admission
    controller decides it's safe/ready.

    Key design points:
    - ``dedupe_key`` prevents duplicate submissions for the same logical run.
    - ``overlap_policy`` controls behaviour when a new run arrives while a
      prior one is still active.
    - ``airflow_run_id`` stores the deterministic DAG run ID used when
      triggering Airflow, ensuring retries don't create duplicate runs.
    """

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── ownership / tenant context ──────────────────────────────
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="execution_jobs",
    )
    organization = models.ForeignKey(
        "accounts.Organization",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="execution_jobs",
    )

    # ── report / DAG reference ──────────────────────────────────
    report = models.ForeignKey(
        Report,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="execution_jobs",
    )
    dag_id = models.CharField(max_length=255)
    dedupe_key = models.CharField(
        max_length=512,
        db_index=True,
        help_text="Deterministic key computed from dag_id + report + schedule window.",
    )
    airflow_run_id = models.CharField(
        max_length=255,
        null=True,
        blank=True,
        help_text="The dag_run_id sent to (or received from) Airflow.",
    )

    # ── timestamps ──────────────────────────────────────────────
    schedule_time = models.DateTimeField(
        null=True,
        blank=True,
        help_text="The logical schedule time this run represents.",
    )
    requested_at = models.DateTimeField(auto_now_add=True)
    admitted_at = models.DateTimeField(null=True, blank=True)
    started_at = models.DateTimeField(null=True, blank=True)
    completed_at = models.DateTimeField(null=True, blank=True)

    # ── classification ──────────────────────────────────────────
    priority = models.CharField(
        max_length=20,
        choices=PriorityTier.choices,
        default=PriorityTier.NORMAL,
    )
    workload_class = models.CharField(
        max_length=20,
        choices=WorkloadClass.choices,
        default=WorkloadClass.MEDIUM,
    )

    # ── status / lifecycle ──────────────────────────────────────
    status = models.CharField(
        max_length=30,
        choices=JobStatus.choices,
        default=JobStatus.RECEIVED,
        db_index=True,
    )

    # ── retry ───────────────────────────────────────────────────
    retry_count = models.PositiveIntegerField(default=0)
    max_retries = models.PositiveIntegerField(default=3)
    last_retry_at = models.DateTimeField(null=True, blank=True)

    # ── payload ─────────────────────────────────────────────────
    payload = models.JSONField(
        default=dict,
        blank=True,
        help_text="Airflow DAG run conf / report parameters.",
    )

    # ── overlap / runtime ───────────────────────────────────────
    overlap_policy = models.CharField(
        max_length=30,
        choices=OverlapPolicy.choices,
        default=OverlapPolicy.QUEUE_ALL,
    )
    expected_runtime_seconds = models.PositiveIntegerField(null=True, blank=True)

    # ── error tracking ──────────────────────────────────────────
    last_error = models.TextField(blank=True, default="")
    last_error_type = models.CharField(max_length=255, blank=True, default="")

    # ── debug / observability ───────────────────────────────────
    attempt_metadata = models.JSONField(
        default=dict,
        blank=True,
        help_text="Arbitrary debug data per attempt (timing, request IDs, …).",
    )

    # ── expiry ──────────────────────────────────────────────────
    expiry_time = models.DateTimeField(
        null=True,
        blank=True,
        help_text="If set, job is considered stale after this time.",
    )

    # ── auto timestamps ─────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "report_execution_jobs"
        ordering = ["priority", "requested_at"]
        indexes = [
            models.Index(fields=["status", "priority"], name="idx_rej_status_pri"),
            models.Index(fields=["dag_id", "status"], name="idx_rej_dag_status"),
            models.Index(fields=["dedupe_key"], name="idx_rej_dedupe"),
            models.Index(fields=["created_by", "status"], name="idx_rej_user_status"),
            models.Index(fields=["organization", "status"], name="idx_rej_org_status"),
        ]

    def __str__(self) -> str:
        return f"Job {self.id!s:.8} [{self.status}] dag={self.dag_id}"

    @property
    def is_active(self) -> bool:
        return self.status in JobStatus.active_statuses()

    @property
    def is_terminal(self) -> bool:
        return self.status in JobStatus.terminal_statuses()


class ReportGroup(models.Model):
    """
    Server-side report group (folder).

    Reports can belong to multiple groups.  Group-level permissions
    grant access to *all* reports within the group.
    """
    name = models.CharField(max_length=255)
    color = models.CharField(max_length=50, default='violet')
    description = models.TextField(blank=True, default='')
    reports = models.ManyToManyField(
        Report,
        through='ReportGroupMember',
        related_name='groups',
        blank=True,
    )
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='created_groups',
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'report_groups'
        ordering = ['name']

    def __str__(self):
        return self.name


class ReportGroupMember(models.Model):
    """Through table for ReportGroup ↔ Report (many-to-many)."""
    group = models.ForeignKey(
        ReportGroup,
        on_delete=models.CASCADE,
        related_name='memberships',
    )
    report = models.ForeignKey(
        Report,
        on_delete=models.CASCADE,
        related_name='group_memberships',
    )
    added_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'report_group_members'
        unique_together = ['group', 'report']
        indexes = [
            models.Index(fields=['group'], name='idx_rgm_group'),
            models.Index(fields=['report'], name='idx_rgm_report'),
        ]

    def __str__(self):
        return f"{self.report.report_name} ∈ {self.group.name}"


class GroupUserPermission(models.Model):
    """Grant a user access to all reports in a group."""
    group = models.ForeignKey(
        ReportGroup,
        on_delete=models.CASCADE,
        related_name='user_permissions',
    )
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='group_permissions',
    )
    can_access = models.BooleanField(default=True)
    can_schedule = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'group_user_permissions'
        unique_together = ['group', 'user']
        indexes = [
            models.Index(fields=['group'], name='idx_gup_group'),
            models.Index(fields=['user'], name='idx_gup_user'),
        ]

    def __str__(self):
        return f"{self.user.username} → {self.group.name}"


class GroupOrganizationPermission(models.Model):
    """Grant an organization access to all reports in a group."""
    group = models.ForeignKey(
        ReportGroup,
        on_delete=models.CASCADE,
        related_name='org_permissions',
    )
    organization = models.ForeignKey(
        'accounts.Organization',
        on_delete=models.CASCADE,
        related_name='group_permissions',
    )
    can_access = models.BooleanField(default=True)
    can_schedule = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'group_organization_permissions'
        unique_together = ['group', 'organization']
        indexes = [
            models.Index(fields=['group'], name='idx_gop_group'),
            models.Index(fields=['organization'], name='idx_gop_org'),
        ]

    def __str__(self):
        return f"{self.organization.name} → {self.group.name}"

