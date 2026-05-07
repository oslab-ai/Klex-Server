"""
Management command: dispatch waiting report jobs.

Only active when scheduler engine is set to "airflow" in scheduler_config.yaml.

Usage:
    python manage.py dispatch_jobs
    python manage.py dispatch_jobs --batch-size 100
    python manage.py dispatch_jobs --dry-run
"""
from django.core.management.base import BaseCommand

from reports.scheduler import get_active_engine
from reports.enums import JobStatus
from reports.models import ReportExecutionJob


class Command(BaseCommand):
    help = "Dispatch waiting report execution jobs (Airflow engine only)."

    def add_arguments(self, parser):
        parser.add_argument(
            "--batch-size",
            type=int,
            default=None,
            help="Max number of jobs to dispatch in this cycle (default: from config).",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Show what would be dispatched without actually triggering.",
        )

    def handle(self, *args, **options):
        engine = get_active_engine()
        if engine != "airflow":
            self.stdout.write(
                self.style.WARNING(
                    f"Active scheduler engine is '{engine}'. "
                    f"dispatch_jobs is only available with the Airflow engine. Skipping."
                )
            )
            return

        from reports.dispatch_service import ReportDispatchService

        service = ReportDispatchService()
        batch_size = options["batch_size"]
        dry_run = options["dry_run"]

        waiting = ReportExecutionJob.objects.filter(
            status__in=list(JobStatus.pending_statuses())
        ).count()
        self.stdout.write(f"Pending jobs: {waiting}")

        if dry_run:
            eligible = ReportExecutionJob.objects.filter(
                status__in=list(JobStatus.pending_statuses())
            ).order_by("priority", "requested_at")
            limit = batch_size or 50
            for job in eligible[:limit]:
                self.stdout.write(
                    f"  [DRY] {job.id} dag={job.dag_id} "
                    f"pri={job.priority} wl={job.workload_class}"
                )
            self.stdout.write(self.style.WARNING("Dry run — no jobs dispatched."))
            return

        dispatched = service.dispatch_due_jobs(batch_size=batch_size)
        self.stdout.write(
            self.style.SUCCESS(f"Dispatched {len(dispatched)} jobs.")
        )
