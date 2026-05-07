"""
Management command: migrate scheduled jobs between scheduling engines.

Usage:
    python manage.py migrate_scheduler --to airflow
    python manage.py migrate_scheduler --to airflow --dry-run
    python manage.py migrate_scheduler --to quartz --force
"""
from django.core.management.base import BaseCommand

from reports.scheduler import get_active_engine
from reports.migration_service import SchedulerMigrationService


class Command(BaseCommand):
    help = "Migrate all active scheduled jobs from the current engine to a target engine."

    def add_arguments(self, parser):
        parser.add_argument(
            "--to",
            type=str,
            required=True,
            choices=["quartz", "airflow"],
            help="Target scheduling engine.",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Preview the migration without making any changes.",
        )
        parser.add_argument(
            "--force",
            action="store_true",
            help="Skip pre-flight health checks.",
        )

    def handle(self, *args, **options):
        target = options["to"]
        dry_run = options["dry_run"]
        force = options["force"]
        current = get_active_engine()

        self.stdout.write(f"\n{'='*60}")
        self.stdout.write(f"  Scheduler Migration: {current} → {target}")
        if dry_run:
            self.stdout.write(self.style.WARNING("  MODE: DRY RUN"))
        self.stdout.write(f"{'='*60}\n")

        if current == target:
            self.stdout.write(
                self.style.WARNING(f"Already running on '{target}'. Nothing to do.")
            )
            return

        service = SchedulerMigrationService()

        # ── preflight ───────────────────────────────────────────
        if not force:
            self.stdout.write("Running pre-flight checks...")
            ok, issues = service.preflight_check(target)
            if not ok:
                self.stdout.write(self.style.ERROR("\nPre-flight checks FAILED:"))
                for issue in issues:
                    self.stdout.write(f"  ✗ {issue}")
                self.stdout.write(
                    self.style.WARNING("\nUse --force to skip pre-flight checks.")
                )
                return
            self.stdout.write(self.style.SUCCESS("  ✓ All checks passed\n"))

        # ── execute ─────────────────────────────────────────────
        report = service.migrate_all_jobs(
            target, dry_run=dry_run, force=force,
        )

        # ── print results ───────────────────────────────────────
        self.stdout.write(f"\n{'─'*60}")
        self.stdout.write(f"  Total jobs:  {report.total_jobs}")
        self.stdout.write(
            self.style.SUCCESS(f"  Migrated:    {report.migrated}")
        )
        if report.skipped:
            self.stdout.write(
                self.style.WARNING(f"  Skipped:     {report.skipped}")
            )
        if report.failed:
            self.stdout.write(
                self.style.ERROR(f"  Failed:      {report.failed}")
            )
        self.stdout.write(f"{'─'*60}\n")

        # ── per-job details ─────────────────────────────────────
        for detail in report.details:
            icon = {"migrated": "✓", "skipped": "○", "failed": "✗"}.get(
                detail.status, "?"
            )
            style = {
                "migrated": self.style.SUCCESS,
                "skipped": self.style.WARNING,
                "failed": self.style.ERROR,
            }.get(detail.status, str)

            line = f"  {icon} [{detail.status:>8}] {detail.job_name}"
            if detail.reason:
                line += f" — {detail.reason}"
            if detail.old_dag_id and detail.new_dag_id:
                line += f"  ({detail.old_dag_id} → {detail.new_dag_id})"
            self.stdout.write(style(line))

        if not dry_run and (report.migrated > 0 or report.total_jobs == 0):
            self.stdout.write(
                self.style.SUCCESS(
                    f"\n✅ Engine switched to '{target}'. "
                    f"All new schedules will use {target}."
                )
            )
        elif dry_run:
            self.stdout.write(
                self.style.WARNING(
                    "\n⚠️  Dry run complete — no changes made."
                )
            )
