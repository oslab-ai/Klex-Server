"""
Report Dispatch Service — admission control, queueing, and orchestration.

This is the main orchestration layer between the application and Airflow.
All report execution requests pass through this service, which handles:

- dedupe key computation
- overlap policy enforcement
- admission control (capacity / concurrency / tenant fairness)
- Airflow trigger dispatch
- status reconciliation
- retry and expiry management
- observability / queue summary
"""
from __future__ import annotations

import hashlib
import logging
from datetime import timedelta
from typing import Any

from django.db import transaction
from django.db.models import Count, Q, Avg, F
from django.utils import timezone

from .airflow_client import AirflowClient, airflow_client
from .dispatch_config import DispatchConfig, dispatch_config
from .enums import JobStatus, OverlapPolicy, PriorityTier, WorkloadClass
from .exceptions import (
    AirflowClientError,
    AirflowConnectionError,
    AirflowNotFoundError,
)
from .models import ReportExecutionJob

logger = logging.getLogger(__name__)


class ReportDispatchService:
    """
    Capacity-aware report dispatch layer.

    Typical usage::

        service = ReportDispatchService()

        # 1. Submit a job (from a view or scheduler)
        job = service.submit_report_job(
            dag_id="klex_sales_daily",
            report_id=42,
            created_by=user,
            payload={"output_format": "pdf"},
        )

        # 2. Periodic dispatch (management command / cron)
        dispatched = service.dispatch_due_jobs()

        # 3. Periodic reconciliation
        service.reconcile_airflow_runs()
    """

    def __init__(
        self,
        client: AirflowClient | None = None,
        config: DispatchConfig | None = None,
    ) -> None:
        self.client = client or airflow_client
        self.config = config or dispatch_config

    # ═══════════════════════════════════════════════════════════
    #  PUBLIC API
    # ═══════════════════════════════════════════════════════════

    def submit_report_job(
        self,
        *,
        dag_id: str,
        report_id: int | None = None,
        report=None,
        created_by=None,
        organization=None,
        payload: dict | None = None,
        schedule_time=None,
        priority: str = PriorityTier.NORMAL,
        workload_class: str = WorkloadClass.MEDIUM,
        overlap_policy: str = OverlapPolicy.QUEUE_ALL,
        expected_runtime_seconds: int | None = None,
        max_retries: int | None = None,
        expiry_time=None,
    ) -> ReportExecutionJob:
        """
        Accept a new report execution request.

        Steps:
          1. Compute dedupe key
          2. Check for existing duplicate
          3. Apply overlap policy
          4. Persist job as RECEIVED or WAITING
          5. Attempt immediate dispatch if capacity allows

        Returns the created (or existing) ``ReportExecutionJob``.
        """
        dedupe_key = self._compute_dedupe_key(
            dag_id=dag_id,
            report_id=report_id,
            schedule_time=schedule_time,
        )

        # ── step 1: check for exact duplicate ───────────────────
        # For QUEUE_ALL, a job with the same dedupe key that is already
        # pending/active is a true duplicate — return it.
        # For overlap-aware policies (SKIP_IF_RUNNING, LATEST_ONLY, etc.),
        # only dedupe against pending jobs; active-run conflicts are handled
        # by the overlap policy in step 2.
        if overlap_policy == OverlapPolicy.QUEUE_ALL:
            dedupe_statuses = list(
                JobStatus.pending_statuses() | JobStatus.active_statuses()
            )
        else:
            dedupe_statuses = list(JobStatus.pending_statuses())

        existing = ReportExecutionJob.objects.filter(
            dedupe_key=dedupe_key,
            status__in=dedupe_statuses,
        ).first()

        if existing:
            logger.info(
                "duplicate_job_detected",
                extra={"dedupe_key": dedupe_key, "existing_id": str(existing.id)},
            )
            return existing

        # ── step 2: apply overlap policy ────────────────────────
        skip = self._apply_overlap_policy(
            dag_id=dag_id,
            report_id=report_id,
            overlap_policy=overlap_policy,
            dedupe_key=dedupe_key,
        )
        if skip:
            # Create a SKIPPED record for auditing
            job = ReportExecutionJob.objects.create(
                dag_id=dag_id,
                report=report,
                report_id=report_id if not report else None,
                created_by=created_by,
                organization=organization or getattr(created_by, "organization", None),
                dedupe_key=dedupe_key,
                schedule_time=schedule_time,
                priority=priority,
                workload_class=workload_class,
                overlap_policy=overlap_policy,
                status=JobStatus.SKIPPED,
                payload=payload or {},
                expected_runtime_seconds=expected_runtime_seconds,
                max_retries=max_retries or self.config.max_job_retries,
                expiry_time=expiry_time,
                last_error="Skipped by overlap policy",
            )
            logger.info(
                "job_skipped_overlap",
                extra={"job_id": str(job.id), "policy": overlap_policy},
            )
            return job

        # ── step 3: create the job ──────────────────────────────
        job = ReportExecutionJob.objects.create(
            dag_id=dag_id,
            report=report,
            created_by=created_by,
            organization=organization or getattr(created_by, "organization", None),
            dedupe_key=dedupe_key,
            schedule_time=schedule_time,
            priority=priority,
            workload_class=workload_class,
            overlap_policy=overlap_policy,
            status=JobStatus.WAITING,
            payload=payload or {},
            expected_runtime_seconds=expected_runtime_seconds,
            max_retries=max_retries or self.config.max_job_retries,
            expiry_time=expiry_time,
        )

        logger.info(
            "job_submitted",
            extra={
                "job_id": str(job.id),
                "dag_id": dag_id,
                "priority": priority,
                "workload_class": workload_class,
            },
        )

        # ── step 4: try immediate dispatch ──────────────────────
        self._try_dispatch_single(job)

        return job

    def dispatch_due_jobs(self, batch_size: int | None = None) -> list[ReportExecutionJob]:
        """
        Dispatch up to ``batch_size`` WAITING jobs, respecting admission
        control constraints.

        Returns list of jobs that were successfully dispatched.
        """
        batch_size = batch_size or self.config.dispatch_batch_size

        # ── expire stale jobs first ─────────────────────────────
        self._expire_stale_jobs()

        # ── check Airflow health once per cycle ─────────────────
        if not self.client.health_check():
            logger.warning("dispatch_skipped_airflow_unhealthy")
            return []

        # ── query eligible jobs, ordered by priority then age ───
        eligible = (
            ReportExecutionJob.objects.filter(
                status__in=[JobStatus.WAITING, JobStatus.RECEIVED]
            )
            .order_by("priority", "requested_at")
        )

        dispatched: list[ReportExecutionJob] = []
        for job in eligible[:batch_size * 2]:  # fetch extra to account for skips
            if len(dispatched) >= batch_size:
                break
            if self._admit_and_dispatch(job):
                dispatched.append(job)

        logger.info(
            "dispatch_cycle_complete",
            extra={
                "dispatched_count": len(dispatched),
                "eligible_count": eligible.count(),
            },
        )
        return dispatched

    def reconcile_airflow_runs(self) -> dict[str, int]:
        """
        Scan jobs in TRIGGERED / RUNNING states and sync their status
        with Airflow.

        Returns counters: ``{updated, failed, expired, errors}``.
        """
        counters = {"updated": 0, "failed": 0, "expired": 0, "errors": 0}
        now = timezone.now()

        jobs = ReportExecutionJob.objects.filter(
            status__in=[JobStatus.TRIGGERED, JobStatus.RUNNING]
        )

        # Group by dag_id for efficient bulk lookup
        dag_jobs: dict[str, list[ReportExecutionJob]] = {}
        for job in jobs:
            dag_jobs.setdefault(job.dag_id, []).append(job)

        for dag_id, job_list in dag_jobs.items():
            run_ids = [j.airflow_run_id for j in job_list if j.airflow_run_id]
            if not run_ids:
                continue

            try:
                run_map = self.client.get_dag_runs_bulk(dag_id, run_ids)
            except AirflowClientError as exc:
                logger.error(
                    "reconcile_fetch_error",
                    extra={"dag_id": dag_id, "error": str(exc)},
                )
                counters["errors"] += len(run_ids)
                continue

            for job in job_list:
                if not job.airflow_run_id:
                    continue

                run_data = run_map.get(job.airflow_run_id)
                if not run_data:
                    # Run might not exist yet or was cleaned up
                    if job.expiry_time and now > job.expiry_time:
                        job.status = JobStatus.EXPIRED
                        job.completed_at = now
                        job.save(update_fields=["status", "completed_at", "updated_at"])
                        counters["expired"] += 1
                    continue

                airflow_state = run_data.get("state", "")
                new_status = self._map_airflow_state(airflow_state)

                if new_status and new_status != job.status:
                    job.status = new_status
                    if new_status == JobStatus.RUNNING and not job.started_at:
                        job.started_at = now
                    if new_status in JobStatus.terminal_statuses():
                        job.completed_at = now
                    if new_status == JobStatus.FAILED_RETRYABLE:
                        counters["failed"] += 1
                    job.save(update_fields=[
                        "status", "started_at", "completed_at", "updated_at",
                    ])
                    counters["updated"] += 1

        # ── also handle LATEST_ONLY cleanup ─────────────────────
        self._cleanup_latest_only_jobs()

        logger.info("reconcile_complete", extra=counters)
        return counters

    def retry_failed_jobs(self) -> list[ReportExecutionJob]:
        """Re-enqueue FAILED_RETRYABLE jobs that haven't exceeded max retries."""
        now = timezone.now()
        min_interval = timedelta(seconds=self.config.min_retry_interval_seconds)

        retryable = ReportExecutionJob.objects.filter(
            status=JobStatus.FAILED_RETRYABLE,
            retry_count__lt=F("max_retries"),
        )

        retried: list[ReportExecutionJob] = []
        for job in retryable:
            # Prevent retry storms
            if job.last_retry_at and (now - job.last_retry_at) < min_interval:
                continue

            job.status = JobStatus.WAITING
            job.retry_count += 1
            job.last_retry_at = now
            job.save(update_fields=[
                "status", "retry_count", "last_retry_at", "updated_at",
            ])
            retried.append(job)
            logger.info(
                "job_retried",
                extra={
                    "job_id": str(job.id),
                    "retry_count": job.retry_count,
                },
            )

        # Mark jobs that exceeded retries as FAILED_FINAL
        ReportExecutionJob.objects.filter(
            status=JobStatus.FAILED_RETRYABLE,
            retry_count__gte=F("max_retries"),
        ).update(status=JobStatus.FAILED_FINAL, completed_at=now)

        return retried

    def cancel_job(self, job_id) -> ReportExecutionJob:
        """Cancel a job and attempt to kill the Airflow run if active."""
        job = ReportExecutionJob.objects.get(id=job_id)

        if job.is_terminal:
            logger.info("cancel_skip_terminal", extra={"job_id": str(job.id)})
            return job

        # Attempt Airflow cancellation
        if job.airflow_run_id and job.status in JobStatus.active_statuses():
            try:
                self.client.set_dag_run_state(
                    job.dag_id, job.airflow_run_id, "failed"
                )
            except AirflowClientError as exc:
                logger.warning(
                    "cancel_airflow_error",
                    extra={"job_id": str(job.id), "error": str(exc)},
                )

        job.status = JobStatus.CANCELLED
        job.completed_at = timezone.now()
        job.save(update_fields=["status", "completed_at", "updated_at"])
        logger.info("job_cancelled", extra={"job_id": str(job.id)})
        return job

    def get_queue_summary(self) -> dict[str, Any]:
        """Return operational summary of the execution queue."""
        now = timezone.now()

        # Counts by status
        status_counts = dict(
            ReportExecutionJob.objects.values_list("status")
            .annotate(count=Count("id"))
            .values_list("status", "count")
        )

        # Counts by workload class (active only)
        workload_counts = dict(
            ReportExecutionJob.objects.filter(
                status__in=list(JobStatus.active_statuses())
            )
            .values_list("workload_class")
            .annotate(count=Count("id"))
            .values_list("workload_class", "count")
        )

        # Average wait time (WAITING → TRIGGERED)
        avg_wait = (
            ReportExecutionJob.objects.filter(
                status__in=[JobStatus.TRIGGERED, JobStatus.RUNNING, JobStatus.SUCCESS],
                admitted_at__isnull=False,
            )
            .aggregate(
                avg_wait=Avg(F("admitted_at") - F("requested_at"))
            )
        )

        # Long-running jobs (running > expected_runtime_seconds)
        long_running = ReportExecutionJob.objects.filter(
            status=JobStatus.RUNNING,
            started_at__isnull=False,
            expected_runtime_seconds__isnull=False,
        ).extra(
            where=[
                "NOW() - started_at > make_interval(secs => expected_runtime_seconds)",
            ]
        ).count()

        return {
            "status_counts": status_counts,
            "workload_counts": workload_counts,
            "total_waiting": status_counts.get(JobStatus.WAITING, 0)
                + status_counts.get(JobStatus.RECEIVED, 0),
            "total_active": sum(
                status_counts.get(s, 0) for s in JobStatus.active_statuses()
            ),
            "avg_wait_seconds": (
                avg_wait["avg_wait"].total_seconds()
                if avg_wait.get("avg_wait")
                else None
            ),
            "long_running_count": long_running,
            "timestamp": now.isoformat(),
        }

    # ═══════════════════════════════════════════════════════════
    #  INTERNAL HELPERS
    # ═══════════════════════════════════════════════════════════

    @staticmethod
    def _compute_dedupe_key(
        *,
        dag_id: str,
        report_id: int | None,
        schedule_time=None,
    ) -> str:
        """
        Build a deterministic dedupe key.

        Format: ``dag_id:report_id:schedule_window_hash``
        """
        parts = [dag_id, str(report_id or "none")]
        if schedule_time:
            # Truncate to minute for schedule-window dedup
            ts = schedule_time.strftime("%Y%m%d%H%M")
            parts.append(ts)
        else:
            # For ad-hoc runs, use a hash that won't collide with
            # scheduled runs but does collide with same-second submissions
            parts.append(timezone.now().strftime("%Y%m%d%H%M"))
        raw = ":".join(parts)
        return raw

    @staticmethod
    def _compute_airflow_run_id(job: ReportExecutionJob) -> str:
        """
        Deterministic Airflow run ID tied to the job UUID.

        Using the job UUID ensures retries reuse the same run ID, preventing
        duplicate DAG runs in Airflow.
        """
        return f"klex__{job.id!s}"

    def _apply_overlap_policy(
        self,
        *,
        dag_id: str,
        report_id: int | None,
        overlap_policy: str,
        dedupe_key: str,
    ) -> bool:
        """
        Apply overlap policy.  Returns True if the new job should be
        **skipped** (i.e. not created).
        """
        if overlap_policy == OverlapPolicy.QUEUE_ALL:
            return False

        active_qs = ReportExecutionJob.objects.filter(
            dag_id=dag_id,
            status__in=list(JobStatus.active_statuses()),
        )
        if report_id:
            active_qs = active_qs.filter(report_id=report_id)

        if overlap_policy == OverlapPolicy.SKIP_IF_RUNNING:
            return active_qs.exists()

        if overlap_policy == OverlapPolicy.LATEST_ONLY:
            # Mark older pending jobs as SKIPPED
            older_pending = ReportExecutionJob.objects.filter(
                dag_id=dag_id,
                status__in=list(JobStatus.pending_statuses()),
            )
            if report_id:
                older_pending = older_pending.filter(report_id=report_id)
            count = older_pending.update(
                status=JobStatus.SKIPPED,
                completed_at=timezone.now(),
                last_error="Superseded by newer job (LATEST_ONLY)",
            )
            if count:
                logger.info(
                    "latest_only_skipped_older",
                    extra={"dag_id": dag_id, "skipped_count": count},
                )
            return False  # the *new* job should proceed

        if overlap_policy == OverlapPolicy.COALESCE:
            # If there's a pending job, reuse it instead of creating new
            pending = ReportExecutionJob.objects.filter(
                dag_id=dag_id,
                status__in=list(JobStatus.pending_statuses()),
            )
            if report_id:
                pending = pending.filter(report_id=report_id)
            return pending.exists()

        return False

    def _try_dispatch_single(self, job: ReportExecutionJob) -> bool:
        """Attempt to dispatch a single job if admission control passes."""
        if not self._check_admission(job):
            return False
        return self._dispatch_to_airflow(job)

    def _admit_and_dispatch(self, job: ReportExecutionJob) -> bool:
        """Check admission + dispatch. Used by batch dispatch loop."""
        # Re-check status in case another process changed it
        job.refresh_from_db()
        if job.status not in JobStatus.pending_statuses():
            return False

        if not self._check_admission(job):
            return False
        return self._dispatch_to_airflow(job)

    def _check_admission(self, job: ReportExecutionJob) -> bool:
        """
        Return True if the job should be dispatched now.

        Checks:
          1. Per-DAG active count
          2. Per-workload-class active count
          3. Per-tenant active count
          4. Global active count
        """
        # 1. Per-DAG
        dag_active = ReportExecutionJob.objects.filter(
            dag_id=job.dag_id,
            status__in=list(JobStatus.active_statuses()),
        ).count()
        if dag_active >= self.config.per_dag_max_active:
            return False

        # 2. Per-workload-class
        wl_config = self.config.get_workload_config(job.workload_class)
        wl_active = ReportExecutionJob.objects.filter(
            workload_class=job.workload_class,
            status__in=list(JobStatus.active_statuses()),
        ).count()
        if wl_active >= wl_config.max_concurrency:
            return False

        # 3. Per-tenant (user's organization)
        if job.organization_id:
            tenant_active = ReportExecutionJob.objects.filter(
                organization_id=job.organization_id,
                status__in=list(JobStatus.active_statuses()),
            ).count()
            if tenant_active >= self.config.per_tenant_max_active:
                return False

        # 4. Global
        global_active = ReportExecutionJob.objects.filter(
            status__in=list(JobStatus.active_statuses()),
        ).count()
        if global_active >= self.config.global_max_active_jobs:
            return False

        return True

    def _dispatch_to_airflow(self, job: ReportExecutionJob) -> bool:
        """
        Trigger the Airflow DAG run for *job*.

        Uses a deterministic ``dag_run_id`` so retried submissions are
        idempotent.
        """
        now = timezone.now()
        run_id = self._compute_airflow_run_id(job)

        try:
            result = self.client.trigger_dag(
                dag_id=job.dag_id,
                conf=job.payload,
                dag_run_id=run_id,
            )
            job.airflow_run_id = result.get("dag_run_id", run_id)
            job.status = JobStatus.TRIGGERED
            job.admitted_at = now
            job.last_error = ""
            job.last_error_type = ""
            job.save(update_fields=[
                "airflow_run_id", "status", "admitted_at",
                "last_error", "last_error_type", "updated_at",
            ])
            logger.info(
                "job_dispatched",
                extra={
                    "job_id": str(job.id),
                    "dag_id": job.dag_id,
                    "airflow_run_id": job.airflow_run_id,
                },
            )
            return True

        except AirflowNotFoundError:
            # DAG doesn't exist — mark as FAILED_FINAL (not retryable)
            job.status = JobStatus.FAILED_FINAL
            job.last_error = f"DAG '{job.dag_id}' not found in Airflow"
            job.last_error_type = "AirflowNotFoundError"
            job.completed_at = now
            job.save(update_fields=[
                "status", "last_error", "last_error_type",
                "completed_at", "updated_at",
            ])
            logger.error(
                "dispatch_dag_not_found",
                extra={"job_id": str(job.id), "dag_id": job.dag_id},
            )
            return False

        except AirflowClientError as exc:
            # Transient failure — keep in WAITING for later retry
            job.status = JobStatus.FAILED_RETRYABLE
            job.last_error = str(exc)[:500]
            job.last_error_type = type(exc).__name__
            job.airflow_run_id = run_id  # store for dedup on retry
            job.save(update_fields=[
                "status", "last_error", "last_error_type",
                "airflow_run_id", "updated_at",
            ])
            logger.warning(
                "dispatch_airflow_error",
                extra={
                    "job_id": str(job.id),
                    "dag_id": job.dag_id,
                    "error": str(exc),
                },
            )
            return False

    def _expire_stale_jobs(self) -> int:
        """Mark expired jobs that have waited too long."""
        now = timezone.now()
        cutoff = now - timedelta(seconds=self.config.stale_job_timeout_seconds)

        # Explicit expiry_time takes precedence
        expired_explicit = ReportExecutionJob.objects.filter(
            status__in=list(JobStatus.pending_statuses()),
            expiry_time__isnull=False,
            expiry_time__lte=now,
        ).update(
            status=JobStatus.EXPIRED,
            completed_at=now,
            last_error="Job expired (explicit expiry_time reached)",
        )

        # Fall back to global stale timeout
        expired_stale = ReportExecutionJob.objects.filter(
            status__in=list(JobStatus.pending_statuses()),
            expiry_time__isnull=True,
            requested_at__lte=cutoff,
        ).update(
            status=JobStatus.EXPIRED,
            completed_at=now,
            last_error="Job expired (stale timeout exceeded)",
        )

        total = expired_explicit + expired_stale
        if total:
            logger.info("stale_jobs_expired", extra={"count": total})
        return total

    def _cleanup_latest_only_jobs(self) -> int:
        """
        For LATEST_ONLY reports, mark older pending jobs as SKIPPED whenever
        a newer pending or active job exists for the same dag/report.
        """
        now = timezone.now()
        skipped = 0

        # Find all pending LATEST_ONLY jobs
        pending_lo = ReportExecutionJob.objects.filter(
            overlap_policy=OverlapPolicy.LATEST_ONLY,
            status__in=list(JobStatus.pending_statuses()),
        ).order_by("dag_id", "report_id", "-requested_at")

        seen: set[tuple[str, int | None]] = set()
        for job in pending_lo:
            key = (job.dag_id, job.report_id)
            if key in seen:
                # This is an older job — skip it
                job.status = JobStatus.SKIPPED
                job.completed_at = now
                job.last_error = "Superseded by newer job (LATEST_ONLY)"
                job.save(update_fields=[
                    "status", "completed_at", "last_error", "updated_at",
                ])
                skipped += 1
            else:
                seen.add(key)

        return skipped

    @staticmethod
    def _map_airflow_state(airflow_state: str) -> str | None:
        """Map an Airflow DAG run state to a ``JobStatus``."""
        mapping = {
            "queued": JobStatus.TRIGGERED,
            "running": JobStatus.RUNNING,
            "success": JobStatus.SUCCESS,
            "failed": JobStatus.FAILED_RETRYABLE,
        }
        return mapping.get(airflow_state)
