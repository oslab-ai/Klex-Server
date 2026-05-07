"""
Enums / constants for the report dispatch layer.

Text-choice enums compatible with Django ``CharField(choices=…)``.
"""
from django.db import models


class JobStatus(models.TextChoices):
    """Full lifecycle status for ReportExecutionJob."""

    RECEIVED = "RECEIVED", "Received"
    WAITING = "WAITING", "Waiting"
    ADMITTED = "ADMITTED", "Admitted"
    TRIGGERED = "TRIGGERED", "Triggered"
    RUNNING = "RUNNING", "Running"
    SUCCESS = "SUCCESS", "Success"
    FAILED_RETRYABLE = "FAILED_RETRYABLE", "Failed (Retryable)"
    FAILED_FINAL = "FAILED_FINAL", "Failed (Final)"
    SKIPPED = "SKIPPED", "Skipped"
    EXPIRED = "EXPIRED", "Expired"
    CANCELLED = "CANCELLED", "Cancelled"

    # ── convenience sets ─────────────────────────────────────
    @classmethod
    def active_statuses(cls) -> set[str]:
        """Statuses that represent an in-flight job."""
        return {cls.ADMITTED, cls.TRIGGERED, cls.RUNNING}

    @classmethod
    def pending_statuses(cls) -> set[str]:
        """Statuses eligible for dispatch."""
        return {cls.RECEIVED, cls.WAITING}

    @classmethod
    def terminal_statuses(cls) -> set[str]:
        """Statuses that are final — the job is done."""
        return {cls.SUCCESS, cls.FAILED_FINAL, cls.SKIPPED, cls.EXPIRED, cls.CANCELLED}


class OverlapPolicy(models.TextChoices):
    """Policy when a new trigger overlaps with an existing active run."""

    QUEUE_ALL = "QUEUE_ALL", "Queue All"
    SKIP_IF_RUNNING = "SKIP_IF_RUNNING", "Skip If Running"
    LATEST_ONLY = "LATEST_ONLY", "Latest Only"
    COALESCE = "COALESCE", "Coalesce"


class WorkloadClass(models.TextChoices):
    """Classification of report workload weight."""

    INTERACTIVE = "INTERACTIVE", "Interactive"
    LIGHT = "LIGHT", "Light"
    MEDIUM = "MEDIUM", "Medium"
    HEAVY = "HEAVY", "Heavy"


class PriorityTier(models.TextChoices):
    """Dispatch priority tier (lower numeric = higher priority)."""

    CRITICAL = "CRITICAL", "Critical"
    HIGH = "HIGH", "High"
    NORMAL = "NORMAL", "Normal"
    LOW = "LOW", "Low"

    @classmethod
    def ordering_value(cls, tier: str) -> int:
        """Return a numeric sort key — lower = dispatched first."""
        order = {
            cls.CRITICAL: 0,
            cls.HIGH: 1,
            cls.NORMAL: 2,
            cls.LOW: 3,
        }
        return order.get(tier, 2)
