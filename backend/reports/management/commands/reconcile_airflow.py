"""
Management command: reconcile Airflow run states with the app-side queue.

Only active when scheduler engine is set to "airflow" in scheduler_config.yaml.

Usage:
    python manage.py reconcile_airflow
    python manage.py reconcile_airflow --retry
    python manage.py reconcile_airflow --dry-run
"""
from django.core.management.base import BaseCommand

from reports.scheduler import get_active_engine
from reports.enums import JobStatus
from reports.models import ReportExecutionJob


class Command(BaseCommand):
    help = "Reconcile Airflow DAG run states with app-side job queue (Airflow engine only)."

    def add_arguments(self, parser):
        parser.add_argument(
            "--retry",
            action="store_true",
            help="Also retry failed-retryable jobs after reconciliation.",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Show jobs that would be reconciled without making changes.",
        )

    def handle(self, *args, **options):
        engine = get_active_engine()
        if engine != "airflow":
            self.stdout.write(
                self.style.WARNING(
                    f"Active scheduler engine is '{engine}'. "
                    f"reconcile_airflow is only available with the Airflow engine. Skipping."
                )
            )
            return

        from reports.dispatch_service import ReportDispatchService

        service = ReportDispatchService()
        dry_run = options["dry_run"]

        in_flight = ReportExecutionJob.objects.filter(
            status__in=[JobStatus.TRIGGERED, JobStatus.RUNNING]
        ).count()
        self.stdout.write(f"In-flight jobs to reconcile: {in_flight}")

        if dry_run:
            jobs = ReportExecutionJob.objects.filter(
                status__in=[JobStatus.TRIGGERED, JobStatus.RUNNING]
            )
            for job in jobs[:100]:
                self.stdout.write(
                    f"  [DRY] {job.id} dag={job.dag_id} "
                    f"status={job.status} run_id={job.airflow_run_id}"
                )
            self.stdout.write(self.style.WARNING("Dry run — no changes made."))
            return

        counters = service.reconcile_airflow_runs()
        self.stdout.write(
            self.style.SUCCESS(
                f"Reconciliation complete: "
                f"updated={counters['updated']} "
                f"failed={counters['failed']} "
                f"expired={counters['expired']} "
                f"errors={counters['errors']}"
            )
        )

        if options["retry"]:
            retried = service.retry_failed_jobs()
            self.stdout.write(
                self.style.SUCCESS(f"Retried {len(retried)} failed jobs.")
            )
