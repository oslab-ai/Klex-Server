"""
Management command: archive (delete) terminal ReportExecutionJob rows.

Keeps the hot `report_execution_jobs` table lean by removing completed
jobs that are older than a configurable retention period.

Usage:
    python manage.py archive_completed_jobs
    python manage.py archive_completed_jobs --days 14
    python manage.py archive_completed_jobs --dry-run
    python manage.py archive_completed_jobs --days 7 --batch-size 5000

Recommended: run via weekly cron or Airflow DAG to keep the table
under ~50 K active rows.
"""
from datetime import timedelta

from django.core.management.base import BaseCommand
from django.utils import timezone

from reports.enums import JobStatus
from reports.models import ReportExecutionJob


class Command(BaseCommand):
    help = (
        "Archive (delete) terminal ReportExecutionJob rows older than "
        "--days days.  Keeps the hot table small for fast admission control."
    )

    def add_arguments(self, parser):
        parser.add_argument(
            "--days",
            type=int,
            default=30,
            help="Retain completed jobs for this many days (default: 30).",
        )
        parser.add_argument(
            "--batch-size",
            type=int,
            default=10_000,
            help="Delete in batches of this size to avoid long locks (default: 10 000).",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Show how many jobs would be archived without deleting.",
        )

    def handle(self, *args, **options):
        days = options["days"]
        batch_size = options["batch_size"]
        dry_run = options["dry_run"]

        cutoff = timezone.now() - timedelta(days=days)
        terminal_statuses = list(JobStatus.terminal_statuses())

        base_qs = ReportExecutionJob.objects.filter(
            status__in=terminal_statuses,
            completed_at__lte=cutoff,
        )

        total_eligible = base_qs.count()
        self.stdout.write(
            f"Terminal jobs older than {days} days: {total_eligible}"
        )

        if dry_run:
            # Show breakdown by status
            for status in terminal_statuses:
                count = base_qs.filter(status=status).count()
                if count:
                    self.stdout.write(f"  {status}: {count}")
            self.stdout.write(
                self.style.WARNING("Dry run — no deletions performed.")
            )
            return

        if total_eligible == 0:
            self.stdout.write(self.style.SUCCESS("Nothing to archive."))
            return

        # Delete in batches to avoid locking the table for too long
        total_deleted = 0
        while True:
            batch_ids = list(
                base_qs.values_list("id", flat=True)[:batch_size]
            )
            if not batch_ids:
                break
            deleted, _ = ReportExecutionJob.objects.filter(
                id__in=batch_ids,
            ).delete()
            total_deleted += deleted
            self.stdout.write(f"  Deleted batch: {deleted} (total: {total_deleted})")

        self.stdout.write(
            self.style.SUCCESS(
                f"Archive complete: {total_deleted} jobs deleted "
                f"(retention: {days} days)."
            )
        )
