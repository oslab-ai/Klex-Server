"""
Quartz Scheduler Client — proxies to KlexReportingService.

This restores the original scheduling behaviour that existed before
the Airflow integration.  All scheduling calls are forwarded to the
Java-based Quartz scheduler running inside KlexReportingService.

Endpoints used:
  POST   /api/reports/scheduleReport   — create schedule
  GET    /api/reports/schedules        — list all schedules
  GET    /api/reports/schedule/{id}    — get one schedule
  DELETE /api/reports/schedule/{id}    — delete schedule
"""
from __future__ import annotations

import logging
from typing import Any

import requests as http_requests
from django.conf import settings

from . import SchedulerClient

logger = logging.getLogger(__name__)


class QuartzSchedulerClient(SchedulerClient):
    """Schedule reports via KlexReportingService (Quartz)."""

    def __init__(self, config: dict | None = None):
        config = config or {}
        self.base_url: str = config.get(
            "url",
            getattr(settings, "SCHEDULER_SERVICE_URL",
                    getattr(settings, "COMPILER_SERVICE_URL", "http://localhost:8081")),
        )
        self.timeout: int = int(config.get("timeout", 30))

    @property
    def engine_name(self) -> str:
        return "quartz"

    # ── helpers ──────────────────────────────────────────────────

    def _get(self, path: str, **kwargs) -> dict:
        url = f"{self.base_url}{path}"
        resp = http_requests.get(url, timeout=self.timeout, **kwargs)
        resp.raise_for_status()
        return resp.json()

    def _post(self, path: str, json: dict | None = None, **kwargs) -> dict:
        url = f"{self.base_url}{path}"
        resp = http_requests.post(url, json=json, timeout=self.timeout, **kwargs)
        resp.raise_for_status()
        return resp.json()

    def _delete(self, path: str, **kwargs) -> dict:
        url = f"{self.base_url}{path}"
        resp = http_requests.delete(url, timeout=self.timeout, **kwargs)
        resp.raise_for_status()
        return resp.json()

    # ── SchedulerClient contract ─────────────────────────────────

    def create_schedule(self, schedule_data: dict) -> dict:
        """
        POST /api/reports/scheduleReport

        ``schedule_data`` is forwarded verbatim to the Java service.
        The Java service returns ``{jobId, message, triggerType, status}``.
        """
        # Strip internal Django objects before sending to the engine
        payload = dict(schedule_data)
        payload.pop("_user", None)
        payload.pop("_organization", None)
        return self._post("/api/reports/scheduleReport", json=payload)

    def list_schedules(self) -> dict:
        """
        GET /api/reports/schedules

        Returns ``{count, schedules, status}``.
        """
        return self._get("/api/reports/schedules")

    def get_schedule(self, job_id: str) -> dict:
        """
        GET /api/reports/schedule/{jobId}

        Returns ``{jobId, status, schedule}``.
        """
        return self._get(f"/api/reports/schedule/{job_id}")

    def delete_schedule(self, job_id: str) -> dict:
        """
        DELETE /api/reports/schedule/{jobId}

        Returns ``{jobId, message, status}``.
        """
        return self._delete(f"/api/reports/schedule/{job_id}")

    def perform_action(self, job_id: str, action: str, **kwargs) -> dict:
        """
        Quartz doesn't have rich lifecycle actions.
        We approximate:
          - hold   → (no-op, Quartz pauses are handled differently)
          - cancel/delete → delete the schedule
          - mark_finished/mark_failed → (no-op at engine level)

        All local ScheduledJob state changes are handled by the view layer.
        """
        if action in ("cancel", "kill", "delete"):
            try:
                self.delete_schedule(job_id)
            except Exception as e:
                logger.warning("Quartz delete failed for %s: %s", job_id, e)
            return {"message": f"Schedule {job_id} deleted", "status": "SUCCESS"}

        # For actions Quartz can't handle, return a no-op success.
        return {"message": f"Action '{action}' acknowledged (Quartz)", "status": "SUCCESS"}

    def get_history(self, job_id: str, **params) -> dict:
        """Quartz doesn't expose run history via REST. Return empty."""
        return {"job_id": job_id, "total_entries": 0, "runs": []}

    def get_logs(self, job_id: str, **params) -> dict:
        """Quartz doesn't expose task logs via REST. Return empty."""
        return {"job_id": job_id, "logs": "", "task_instances": []}

    def health_check(self) -> bool:
        """Ping the Java service actuator or root endpoint."""
        try:
            url = f"{self.base_url}/api/reports/schedules"
            resp = http_requests.get(url, timeout=5)
            return resp.ok
        except Exception:
            return False
