"""
Scheduler Migration Service — seamless Quartz ↔ Airflow engine switching.

Orchestrates the migration of active scheduled jobs from one scheduling
engine to another, enabling zero-downtime engine switches.

Usage::

    from reports.migration_service import SchedulerMigrationService

    service = SchedulerMigrationService()

    # Pre-flight check
    ok, issues = service.preflight_check("airflow")

    # Dry run
    report = service.migrate_all_jobs("airflow", dry_run=True)

    # Execute
    report = service.migrate_all_jobs("airflow")
"""
from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Any

from django.utils import timezone

from .models import ScheduledJob
from .scheduler import (
    SchedulerClient,
    get_active_engine,
    get_scheduler_client,
    load_scheduler_config,
    reload_engine,
    write_active_engine,
)

logger = logging.getLogger(__name__)


# ═══════════════════════════════════════════════════════════════
#  Data classes for migration reporting
# ═══════════════════════════════════════════════════════════════


@dataclass
class JobMigrationResult:
    """Result of migrating a single job."""

    job_id: int
    job_name: str
    status: str  # "migrated", "skipped", "failed"
    reason: str = ""
    old_dag_id: str = ""
    new_dag_id: str = ""


@dataclass
class MigrationReport:
    """Full migration report."""

    previous_engine: str
    target_engine: str
    total_jobs: int = 0
    migrated: int = 0
    skipped: int = 0
    failed: int = 0
    details: list[JobMigrationResult] = field(default_factory=list)
    started_at: str = ""
    completed_at: str = ""

    def to_dict(self) -> dict[str, Any]:
        return {
            "previous_engine": self.previous_engine,
            "target_engine": self.target_engine,
            "total_jobs": self.total_jobs,
            "migrated": self.migrated,
            "skipped": self.skipped,
            "failed": self.failed,
            "started_at": self.started_at,
            "completed_at": self.completed_at,
            "details": [
                {
                    "job_id": d.job_id,
                    "name": d.job_name,
                    "status": d.status,
                    "reason": d.reason,
                    "old_dag_id": d.old_dag_id,
                    "new_dag_id": d.new_dag_id,
                }
                for d in self.details
            ],
        }


# ═══════════════════════════════════════════════════════════════
#  Migration Service
# ═══════════════════════════════════════════════════════════════


class SchedulerMigrationService:
    """
    Capacity-aware scheduler migration layer.

    Migrates all active ``ScheduledJob`` records from the current
    scheduling engine to a target engine, re-creating each schedule
    on the new backend and cleaning up the old one.
    """

    def preflight_check(self, target_engine: str) -> tuple[bool, list[str]]:
        """
        Verify that both engines are reachable and the switch is valid.

        Returns ``(ok, issues)`` where *issues* is a list of human-readable
        problems.  An empty list means all checks passed.
        """
        issues: list[str] = []
        current_engine = get_active_engine()

        if target_engine not in ("quartz", "airflow"):
            issues.append(f"Invalid target engine: {target_engine!r}")
            return False, issues

        if current_engine == target_engine:
            issues.append(
                f"Already running on '{target_engine}'. No migration needed."
            )
            return False, issues

        # Check source engine health
        try:
            source_client = get_scheduler_client()
            if not source_client.health_check():
                issues.append(
                    f"Source engine '{current_engine}' health check failed. "
                    f"Cannot read existing schedules."
                )
        except Exception as exc:
            issues.append(f"Source engine '{current_engine}' unreachable: {exc}")

        # Check target engine health
        try:
            target_client = self._build_client(target_engine)
            if not target_client.health_check():
                issues.append(
                    f"Target engine '{target_engine}' health check failed. "
                    f"Cannot create schedules."
                )
        except Exception as exc:
            issues.append(f"Target engine '{target_engine}' unreachable: {exc}")

        return len(issues) == 0, issues

    def migrate_all_jobs(
        self,
        target_engine: str,
        *,
        dry_run: bool = False,
        force: bool = False,
    ) -> MigrationReport:
        """
        Migrate all active scheduled jobs to *target_engine*.

        Args:
            target_engine: ``"quartz"`` or ``"airflow"``
            dry_run: If True, compute what would happen but change nothing.
            force: If True, skip preflight checks.

        Returns:
            A :class:`MigrationReport` with per-job details.
        """
        current_engine = get_active_engine()
        now = timezone.now()

        report = MigrationReport(
            previous_engine=current_engine,
            target_engine=target_engine,
            started_at=now.isoformat(),
        )

        # ── preflight ───────────────────────────────────────────
        if not force:
            ok, issues = self.preflight_check(target_engine)
            if not ok:
                report.completed_at = timezone.now().isoformat()
                for issue in issues:
                    report.details.append(
                        JobMigrationResult(
                            job_id=0,
                            job_name="PREFLIGHT",
                            status="failed",
                            reason=issue,
                        )
                    )
                    report.failed += 1
                return report

        # ── load clients ────────────────────────────────────────
        source_client = get_scheduler_client()
        target_client = self._build_client(target_engine)

        # ── enumerate active jobs ───────────────────────────────
        active_jobs = ScheduledJob.objects.filter(
            is_active=True,
            status__in=["running", "on_hold"],
        )
        report.total_jobs = active_jobs.count()

        logger.info(
            "migration_start",
            extra={
                "from": current_engine,
                "to": target_engine,
                "job_count": report.total_jobs,
                "dry_run": dry_run,
            },
        )

        # ── migrate each job ────────────────────────────────────
        for job in active_jobs:
            result = self._migrate_single_job(
                job=job,
                source_client=source_client,
                target_client=target_client,
                target_engine=target_engine,
                dry_run=dry_run,
            )
            report.details.append(result)
            if result.status == "migrated":
                report.migrated += 1
            elif result.status == "skipped":
                report.skipped += 1
            else:
                report.failed += 1

        # ── flip the config ─────────────────────────────────────
        if not dry_run and report.failed == 0:
            write_active_engine(target_engine)
            reload_engine()
            logger.info(
                "migration_engine_switched",
                extra={"new_engine": target_engine},
            )
        elif not dry_run and report.migrated > 0:
            # Some succeeded, some failed — still flip but warn
            write_active_engine(target_engine)
            reload_engine()
            logger.warning(
                "migration_partial_switch",
                extra={
                    "new_engine": target_engine,
                    "migrated": report.migrated,
                    "failed": report.failed,
                },
            )

        report.completed_at = timezone.now().isoformat()

        logger.info(
            "migration_complete",
            extra={
                "migrated": report.migrated,
                "skipped": report.skipped,
                "failed": report.failed,
            },
        )

        return report

    # ═══════════════════════════════════════════════════════════
    #  INTERNAL HELPERS
    # ═══════════════════════════════════════════════════════════

    def _migrate_single_job(
        self,
        *,
        job: ScheduledJob,
        source_client: SchedulerClient,
        target_client: SchedulerClient,
        target_engine: str,
        dry_run: bool,
    ) -> JobMigrationResult:
        """
        Migrate a single job.  Errors are caught and returned as
        a failed result — they never abort the migration loop.
        """
        old_dag_id = job.dag_id

        # ── check payload ───────────────────────────────────────
        if not job.schedule_payload:
            return JobMigrationResult(
                job_id=job.id,
                job_name=job.schedule_name,
                status="skipped",
                reason="No schedule_payload stored — cannot recreate.",
                old_dag_id=old_dag_id,
            )

        if dry_run:
            return JobMigrationResult(
                job_id=job.id,
                job_name=job.schedule_name,
                status="migrated",
                reason="[DRY RUN] Would migrate.",
                old_dag_id=old_dag_id,
                new_dag_id="(pending)",
            )

        try:
            # ── step 1: translate payload ───────────────────────
            translated = self._translate_payload(
                payload=job.schedule_payload,
                source_engine=job.created_on_engine or get_active_engine(),
                target_engine=target_engine,
            )

            # ── step 2: create on target engine ─────────────────
            engine_result = target_client.create_schedule_from_payload(translated)
            new_dag_id = (
                engine_result.get("jobId")
                or engine_result.get("dag_id")
                or engine_result.get("dispatch_job_id")
                or old_dag_id
            )

            # ── step 3: clean up source engine ──────────────────
            self._cleanup_source(job, source_client)

            # ── step 4: update local record ─────────────────────
            job.dag_id = new_dag_id
            job.created_on_engine = target_engine
            job.save(update_fields=[
                "dag_id", "created_on_engine", "updated_at",
            ])

            logger.info(
                "job_migrated",
                extra={
                    "job_id": job.id,
                    "old_dag_id": old_dag_id,
                    "new_dag_id": new_dag_id,
                    "target": target_engine,
                },
            )

            return JobMigrationResult(
                job_id=job.id,
                job_name=job.schedule_name,
                status="migrated",
                old_dag_id=old_dag_id,
                new_dag_id=new_dag_id,
            )

        except Exception as exc:
            logger.error(
                "job_migration_failed",
                extra={
                    "job_id": job.id,
                    "error": str(exc),
                },
                exc_info=True,
            )
            return JobMigrationResult(
                job_id=job.id,
                job_name=job.schedule_name,
                status="failed",
                reason=str(exc)[:500],
                old_dag_id=old_dag_id,
            )

    def _translate_payload(
        self,
        *,
        payload: dict,
        source_engine: str,
        target_engine: str,
    ) -> dict:
        """
        Translate a schedule payload between engine formats.

        The stored ``schedule_payload`` is mostly engine-agnostic (report_id,
        output formats, mail config, cron expression, data adapter), so the
        translation is lightweight.  Engine-specific keys are mapped or
        stripped as needed.
        """
        translated = dict(payload)

        # Strip internal Django objects that can't be serialized
        for key in ("_user", "_organization"):
            translated.pop(key, None)

        if source_engine == "quartz" and target_engine == "airflow":
            return self._translate_quartz_to_airflow(translated)
        elif source_engine == "airflow" and target_engine == "quartz":
            return self._translate_airflow_to_quartz(translated)

        # Same engine (shouldn't happen) — return as-is
        return translated

    @staticmethod
    def _translate_quartz_to_airflow(payload: dict) -> dict:
        """
        Map Quartz-format payload to Airflow DAG conf.

        Quartz payloads use keys like ``reportUnitUri``, ``trigger``,
        ``mailNotification``, ``outputFormats``.  The Airflow DAG
        (``klex_report_dispatcher``) expects snake_case keys.
        """
        conf = {
            "report_id": payload.get("report_id"),
            "schedule_name": payload.get("scheduleName", ""),
            "output_formats": payload.get("outputFormats", {}),
            "output_timezone": (
                payload.get("outputTimeZone")
                or payload.get("trigger", {}).get("timezone", "UTC")
            ),
            "delivery_method": payload.get("deliveryMethod", "EMAIL"),
            "mail_notification": payload.get("mailNotification", {}),
            "parameters": payload.get("parameters", {}),
        }

        if "report_ids" in payload:
            conf["report_ids"] = payload["report_ids"]

        # Carry over report URI and data adapter
        for key in ("report_unit_uri", "reportUnitUri"):
            if payload.get(key):
                conf["report_unit_uri"] = payload[key]
                break

        for key in ("reportUnitUris", "report_unit_uris"):
            if payload.get(key):
                conf["reportUnitUris"] = payload[key]
                break

        for key in ("data_adapter", "dataAdapter"):
            if payload.get(key):
                conf["data_adapter"] = payload[key]
                break

        # Airflow uses the generic dispatcher DAG
        conf["dag_id"] = "klex_report_dispatcher"

        return conf

    @staticmethod
    def _translate_airflow_to_quartz(payload: dict) -> dict:
        """
        Map Airflow conf back to Quartz-format payload.

        Reconstruct the trigger, mail notification, and other fields
        in the format that the Java KlexReportingService expects.
        """
        quartz_payload: dict[str, Any] = {
            "report_id": payload.get("report_id"),
            "scheduleName": payload.get("schedule_name", ""),
            "outputFormats": payload.get("output_formats", {}),
            "outputTimeZone": payload.get("output_timezone", "UTC"),
            "deliveryMethod": payload.get("delivery_method", "EMAIL"),
            "mailNotification": payload.get("mail_notification", {}),
            "parameters": payload.get("parameters", {}),
        }

        if "report_ids" in payload:
            quartz_payload["report_ids"] = payload["report_ids"]

        # Report URI
        if payload.get("report_unit_uri"):
            quartz_payload["reportUnitUri"] = payload["report_unit_uri"]

        if "reportUnitUris" in payload:
            quartz_payload["reportUnitUris"] = payload["reportUnitUris"]
        elif "report_unit_uris" in payload:
            quartz_payload["reportUnitUris"] = payload["report_unit_uris"]

        # Data adapter
        if payload.get("data_adapter"):
            quartz_payload["dataAdapter"] = payload["data_adapter"]

        # Reconstruct trigger from cron if available
        cron = payload.get("cronExpression") or payload.get("cron_expression", "")
        if cron:
            quartz_payload["trigger"] = {
                "triggerType": "CALENDAR",
                "cronExpression": cron,
                "timezone": payload.get("output_timezone", "UTC"),
            }

        return quartz_payload

    @staticmethod
    def _cleanup_source(job: ScheduledJob, source_client: SchedulerClient) -> None:
        """
        Remove or pause the schedule from the source engine.

        Best-effort — failures here are logged but don't block migration.
        """
        try:
            source_client.delete_schedule(job.dag_id)
            logger.info(
                "source_schedule_deleted",
                extra={"dag_id": job.dag_id, "engine": source_client.engine_name},
            )
        except Exception as exc:
            logger.warning(
                "source_schedule_cleanup_failed",
                extra={
                    "dag_id": job.dag_id,
                    "engine": source_client.engine_name,
                    "error": str(exc),
                },
            )

    @staticmethod
    def _build_client(engine: str) -> SchedulerClient:
        """Instantiate a SchedulerClient for the given engine."""
        cfg = load_scheduler_config()

        if engine == "airflow":
            from .scheduler.airflow_adapter import AirflowSchedulerAdapter
            return AirflowSchedulerAdapter(cfg.get("airflow", {}))
        else:
            from .scheduler.quartz_client import QuartzSchedulerClient
            return QuartzSchedulerClient(cfg.get("quartz", {}))

