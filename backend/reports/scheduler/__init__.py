"""
Unified Scheduler Abstraction Layer.

Provides a common interface that views and services use regardless
of whether the active scheduling engine is Quartz (KlexReportingService)
or Apache Airflow.

Configuration is read from ``backend/scheduler_config.yaml`` at import
time, with environment-variable substitution.  The active engine is
determined by ``scheduler.active_engine`` in that file.

Usage::

    from reports.scheduler import get_scheduler_client

    client = get_scheduler_client()
    result = client.create_schedule(schedule_data)
    schedules = client.list_schedules()
"""
from __future__ import annotations

import abc
import logging
import os
import re
from pathlib import Path
from typing import Any

import yaml
from django.conf import settings

logger = logging.getLogger(__name__)


# ═══════════════════════════════════════════════════════════════
#  Abstract interface
# ═══════════════════════════════════════════════════════════════


class SchedulerClient(abc.ABC):
    """
    Scheduler backend contract.

    Every concrete implementation (Quartz, Airflow) must implement
    these methods.  Views call them without knowing which engine
    is underneath.
    """

    @abc.abstractmethod
    def create_schedule(self, schedule_data: dict) -> dict:
        """Create a new scheduled job. Returns engine-specific response."""

    def create_schedule_from_payload(self, payload: dict) -> dict:
        """
        Create a schedule from a stored engine-agnostic payload.

        Used during engine migration — the ``schedule_payload`` from
        ``ScheduledJob`` is passed here.  Default implementation
        delegates to ``create_schedule()``; subclasses may override
        for engine-specific translation.
        """
        return self.create_schedule(payload)

    @abc.abstractmethod
    def list_schedules(self) -> dict:
        """List all scheduled jobs. Returns ``{count, schedules/jobs}``."""

    @abc.abstractmethod
    def get_schedule(self, job_id: str) -> dict:
        """Get details for a single schedule by its ID."""

    @abc.abstractmethod
    def delete_schedule(self, job_id: str) -> dict:
        """Delete a scheduled job by its ID."""

    @abc.abstractmethod
    def perform_action(self, job_id: str, action: str, **kwargs) -> dict:
        """
        Perform a lifecycle action on a job (hold, release, cancel, …).
        Implementations may ignore actions that are unsupported.
        """

    @abc.abstractmethod
    def get_history(self, job_id: str, **params) -> dict:
        """Return execution history for a job."""

    @abc.abstractmethod
    def get_logs(self, job_id: str, **params) -> dict:
        """Return execution logs for a job."""

    @abc.abstractmethod
    def health_check(self) -> bool:
        """Return True if the scheduling engine is reachable."""

    @property
    @abc.abstractmethod
    def engine_name(self) -> str:
        """Human-readable name of the engine (e.g. 'quartz', 'airflow')."""


# ═══════════════════════════════════════════════════════════════
#  Configuration loading
# ═══════════════════════════════════════════════════════════════

def _resolve_env(value: str) -> str:
    """Replace ``${VAR:-default}`` / ``${VAR}`` with environment values."""
    def _replacer(match):
        var_expr = match.group(1)
        if ":-" in var_expr:
            var_name, default = var_expr.split(":-", 1)
        else:
            var_name, default = var_expr, ""
        return os.environ.get(var_name, default)
    return re.sub(r"\$\{([^}]+)\}", _replacer, value)


def _resolve_dict(d: dict) -> dict:
    """Recursively resolve env-var placeholders in a dict."""
    out = {}
    for k, v in d.items():
        if isinstance(v, str):
            out[k] = _resolve_env(v)
        elif isinstance(v, dict):
            out[k] = _resolve_dict(v)
        else:
            out[k] = v
    return out


def load_scheduler_config() -> dict:
    """
    Load ``scheduler_config.yaml`` from the backend directory.
    Falls back to ``quartz`` engine if file is missing.
    """
    config_path = Path(settings.BASE_DIR) / "scheduler_config.yaml"
    if not config_path.exists():
        logger.warning(
            "scheduler_config.yaml not found at %s — defaulting to quartz",
            config_path,
        )
        return {
            "active_engine": "quartz",
            "quartz": {
                "url": getattr(settings, "SCHEDULER_SERVICE_URL", "http://localhost:8081"),
            },
            "airflow": {},
        }

    with open(config_path) as f:
        raw = yaml.safe_load(f)

    cfg = raw.get("scheduler", raw)
    return _resolve_dict(cfg)


def get_active_engine() -> str:
    """Return the currently configured engine name (``'quartz'`` or ``'airflow'``)."""
    cfg = load_scheduler_config()
    return cfg.get("active_engine", "quartz").lower()


def write_active_engine(engine: str) -> None:
    """
    Update the ``active_engine`` value in ``scheduler_config.yaml``.

    Used by the migration service to flip engines atomically after
    all schedules have been migrated.
    """
    if engine not in ("quartz", "airflow"):
        raise ValueError(f"Invalid engine: {engine!r}. Must be 'quartz' or 'airflow'.")

    config_path = Path(settings.BASE_DIR) / "scheduler_config.yaml"
    with open(config_path) as f:
        raw = yaml.safe_load(f) or {}

    raw.setdefault("scheduler", {})["active_engine"] = engine

    with open(config_path, "w") as f:
        yaml.dump(raw, f, default_flow_style=False, sort_keys=False)

    logger.info("scheduler_config.yaml updated: active_engine → %s", engine)


# ═══════════════════════════════════════════════════════════════
#  Factory
# ═══════════════════════════════════════════════════════════════

_cached_client: SchedulerClient | None = None


def get_scheduler_client(*, force_reload: bool = False) -> SchedulerClient:
    """
    Return the active :class:`SchedulerClient` singleton.

    The first call reads ``scheduler_config.yaml``, instantiates the
    correct backend, and caches it.  Pass ``force_reload=True`` to
    re-read the YAML (useful in tests).
    """
    global _cached_client
    if _cached_client is not None and not force_reload:
        return _cached_client

    cfg = load_scheduler_config()
    engine = cfg.get("active_engine", "quartz").lower()

    if engine == "airflow":
        from .airflow_adapter import AirflowSchedulerAdapter
        _cached_client = AirflowSchedulerAdapter(cfg.get("airflow", {}))
    else:
        from .quartz_client import QuartzSchedulerClient
        _cached_client = QuartzSchedulerClient(cfg.get("quartz", {}))

    logger.info("Scheduler engine initialised: %s", engine)
    return _cached_client


def reload_engine() -> SchedulerClient:
    """
    Force-reload the scheduler client after a config change.

    Clears the cached singleton and re-reads ``scheduler_config.yaml``.
    Call this after ``write_active_engine()`` to ensure subsequent
    requests use the new engine.
    """
    global _cached_client
    _cached_client = None
    return get_scheduler_client(force_reload=True)
