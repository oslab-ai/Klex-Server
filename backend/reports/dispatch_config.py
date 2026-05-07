"""
Dispatch configuration for the report execution layer.

Loaded from ``settings.DISPATCH_CONFIG`` at import time.  Every value has a
sensible default so the system works out-of-the-box without explicit config.

Recommended Airflow-side setup
------------------------------
* Create separate pools for each workload class:
    - ``klex_interactive`` (slots ~5)   — short-lived, user-initiated
    - ``klex_light``       (slots ~10)  — sub-minute scheduled jobs
    - ``klex_medium``      (slots ~5)   — medium reports
    - ``klex_heavy``       (slots ~2)   — heavy/long reports
* Set ``max_active_runs`` on heavy DAGs to 1 or 2.
* If using CeleryExecutor, consider dedicated queues per workload class and
  route workers accordingly.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from django.conf import settings


@dataclass(frozen=True)
class WorkloadConfig:
    """Per-workload-class routing & concurrency."""

    max_concurrency: int = 10
    airflow_pool: str = "default_pool"
    priority_weight: int = 1
    queue_name: str = "default"


# ── defaults ────────────────────────────────────────────────────

_DEFAULT_WORKLOAD_MAP: dict[str, dict[str, Any]] = {
    "INTERACTIVE": {
        "max_concurrency": 5,
        "airflow_pool": "klex_interactive",
        "priority_weight": 10,
        "queue_name": "default",
    },
    "LIGHT": {
        "max_concurrency": 10,
        "airflow_pool": "klex_light",
        "priority_weight": 5,
        "queue_name": "default",
    },
    "MEDIUM": {
        "max_concurrency": 5,
        "airflow_pool": "klex_medium",
        "priority_weight": 3,
        "queue_name": "default",
    },
    "HEAVY": {
        "max_concurrency": 2,
        "airflow_pool": "klex_heavy",
        "priority_weight": 1,
        "queue_name": "default",
    },
}


@dataclass(frozen=True)
class DispatchConfig:
    """Top-level dispatch configuration."""

    # ── global limits ───────────────────────────────────────────
    global_max_active_jobs: int = 20
    dispatch_batch_size: int = 50
    per_dag_max_active: int = 3

    # ── tenant fairness ─────────────────────────────────────────
    per_tenant_max_active: int = 10

    # ── staleness / expiry ──────────────────────────────────────
    stale_job_timeout_seconds: int = 3600  # 1 hour
    max_job_retries: int = 3
    min_retry_interval_seconds: int = 60   # prevent retry storms

    # ── workload routing (populated from settings) ──────────────
    workload_map: dict[str, WorkloadConfig] = field(default_factory=dict)

    def get_workload_config(self, workload_class: str) -> WorkloadConfig:
        return self.workload_map.get(
            workload_class, WorkloadConfig()
        )


def _load_config() -> DispatchConfig:
    """Build a ``DispatchConfig`` from ``settings.DISPATCH_CONFIG``."""
    raw: dict[str, Any] = getattr(settings, "DISPATCH_CONFIG", {})

    wl_raw = raw.get("workload_map", _DEFAULT_WORKLOAD_MAP)
    workload_map = {
        k: WorkloadConfig(**v) for k, v in wl_raw.items()
    }

    return DispatchConfig(
        global_max_active_jobs=raw.get("global_max_active_jobs", 20),
        dispatch_batch_size=raw.get("dispatch_batch_size", 50),
        per_dag_max_active=raw.get("per_dag_max_active", 3),
        per_tenant_max_active=raw.get("per_tenant_max_active", 10),
        stale_job_timeout_seconds=raw.get("stale_job_timeout_seconds", 3600),
        max_job_retries=raw.get("max_job_retries", 3),
        min_retry_interval_seconds=raw.get("min_retry_interval_seconds", 60),
        workload_map=workload_map,
    )


dispatch_config = _load_config()
