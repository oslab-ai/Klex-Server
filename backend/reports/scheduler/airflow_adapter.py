"""
Airflow Scheduler Adapter — wraps the existing AirflowClient behind
the common :class:`SchedulerClient` interface.

This adapter is loaded **only** when ``scheduler_config.yaml`` has
``active_engine: airflow``.  It delegates to the existing
``reports.airflow_client.AirflowClient`` and
``reports.dispatch_service.ReportDispatchService``.
"""
from __future__ import annotations

import logging
from typing import Any

from django.conf import settings

from . import SchedulerClient

logger = logging.getLogger(__name__)


class AirflowSchedulerAdapter(SchedulerClient):
    """Adapter that implements SchedulerClient using the Airflow integration."""

    def __init__(self, config: dict | None = None):
        config = config or {}
        # Lazy-import to avoid pulling Airflow deps when using Quartz
        from reports.airflow_client import AirflowClient
        self._airflow = AirflowClient()
        self._config = config

    @property
    def engine_name(self) -> str:
        return "airflow"

    def _get_dispatch_service(self):
        from reports.dispatch_service import ReportDispatchService
        return ReportDispatchService(client=self._airflow)

    # ── SchedulerClient contract ─────────────────────────────────

    def create_schedule(self, schedule_data: dict) -> dict:
        """
        Route through the dispatch service for admission control,
        then trigger the Airflow DAG.
        """
        from reports.models import Report
        from reports.enums import PriorityTier, WorkloadClass, OverlapPolicy

        service = self._get_dispatch_service()

        dag_id = schedule_data.get("dag_id") or "klex_report_dispatcher"
        report_id = schedule_data.get("report_id")
        occurrences = schedule_data.get("occurrences", 1)
        report = None
        if report_id:
            try:
                report = Report.objects.get(id=report_id)
            except Report.DoesNotExist:
                pass

        priority_map = {
            0: PriorityTier.NORMAL,
            1: PriorityTier.LOW,
            2: PriorityTier.NORMAL,
            3: PriorityTier.HIGH,
            4: PriorityTier.CRITICAL,
        }
        priority_val = schedule_data.get("priority", 0)
        dispatch_priority = priority_map.get(
            int(priority_val) if isinstance(priority_val, (int, str)) else 0,
            PriorityTier.NORMAL,
        )

        conf = {
            "report_id": report_id,
            "schedule_name": schedule_data.get("scheduleName", ""),
            "output_formats": schedule_data.get("outputFormats", {}),
            "output_timezone": schedule_data.get("outputTimeZone", "UTC"),
            "delivery_method": schedule_data.get("deliveryMethod", "EMAIL"),
            "mail_notification": schedule_data.get("mailNotification", {}),
            "parameters": schedule_data.get("parameters", {})
        }
        if "report_ids" in schedule_data:
            conf["report_ids"] = schedule_data["report_ids"]
        if "reportOutputFormats" in schedule_data:
            conf["reportOutputFormats"] = schedule_data["reportOutputFormats"]
        # Include pre-resolved fields if present
        if "report_unit_uri" in schedule_data:
            conf["report_unit_uri"] = schedule_data["report_unit_uri"]
        if "reportUnitUris" in schedule_data:
            conf["reportUnitUris"] = schedule_data["reportUnitUris"]
        if "data_adapter" in schedule_data:
            conf["data_adapter"] = schedule_data["data_adapter"]
        if schedule_data.get("_organization"):
            conf["_organization_id"] = str(schedule_data["_organization"].id)

        import uuid
        job_id = f"klex_sched_{uuid.uuid4().hex[:8]}"

        return {
            "jobId": job_id,
            "dag_id": job_id,
            "status": "SUCCESS",
            "message": f"Schedule mapped to Airflow DAG {job_id}",
        }

    def list_schedules(self) -> dict:
        """List DAGs from Airflow."""
        try:
            result = self._airflow.list_dags(only_active=True)
            return {
                "count": len(result.get("dags", [])),
                "schedules": result.get("dags", []),
                "status": "SUCCESS",
            }
        except Exception as e:
            logger.error("Airflow list_dags failed: %s", e)
            return {"count": 0, "schedules": [], "status": "ERROR", "error": str(e)}

    def get_schedule(self, job_id: str) -> dict:
        """Get DAG info from Airflow."""
        try:
            dag_info = self._airflow.get_dag(job_id)
            return {"jobId": job_id, "status": "SUCCESS", "schedule": dag_info}
        except Exception as e:
            return {"jobId": job_id, "status": "ERROR", "error": str(e)}

    def delete_schedule(self, job_id: str) -> dict:
        """Delete DAG from Airflow."""
        try:
            self._airflow.delete_dag(job_id)
            return {"jobId": job_id, "message": "DAG deleted", "status": "SUCCESS"}
        except Exception as e:
            logger.warning("Airflow DAG deletion failed for %s: %s", job_id, e)
            return {"jobId": job_id, "status": "ERROR", "error": str(e)}

    def perform_action(self, job_id: str, action: str, **kwargs) -> dict:
        """Delegate lifecycle actions to the Airflow client."""
        from reports.exceptions import (
            AirflowClientError,
            AirflowConnectionError,
            AirflowNotFoundError,
        )

        warnings: list[str] = []

        def _try(fn, *args, **kw):
            try:
                return fn(*args, **kw), None
            except (AirflowNotFoundError, AirflowConnectionError) as exc:
                msg = f"Airflow sync skipped: {exc}"
                warnings.append(msg)
                return None, msg
            except AirflowClientError as exc:
                msg = f"Airflow sync failed: {exc}"
                warnings.append(msg)
                return None, msg

        result: dict[str, Any] = {}

        if action == "hold":
            _try(self._airflow.pause_dag, job_id)
            result = {"message": f"{job_id} paused"}
        elif action == "release":
            _try(self._airflow.unpause_dag, job_id)
            result = {"message": f"{job_id} released"}
        elif action in ("cancel", "kill"):
            runs_result, _ = _try(self._airflow.list_dag_runs, job_id, limit=1)
            if runs_result:
                for run in runs_result.get("dag_runs", []):
                    if run.get("state") in ("running", "queued"):
                        _try(self._airflow.set_dag_run_state, job_id, run["dag_run_id"], "failed")
            if action == "kill":
                _try(self._airflow.pause_dag, job_id)
            result = {"message": f"{job_id} {action}led"}
        elif action == "force_start":
            dag_run, _ = _try(
                self._airflow.trigger_dag, job_id,
                conf={"force_start": True, "triggered_by": kwargs.get("username", "system")},
            )
            result = {"message": f"{job_id} force started"}
            if dag_run:
                result["dag_run"] = dag_run
        elif action in ("restart", "force_restart"):
            runs_result, _ = _try(self._airflow.list_dag_runs, job_id, limit=1)
            if runs_result:
                dag_runs = runs_result.get("dag_runs", [])
                if dag_runs:
                    _try(self._airflow.clear_dag_run, job_id, dag_runs[0]["dag_run_id"])
            result = {"message": f"{job_id} restarted"}
        elif action == "skip":
            runs_result, _ = _try(self._airflow.list_dag_runs, job_id, limit=1)
            if runs_result:
                for run in runs_result.get("dag_runs", []):
                    _try(self._airflow.set_dag_run_state, job_id, run["dag_run_id"], "success")
            result = {"message": f"{job_id} skipped"}
        elif action in ("mark_finished", "mark_failed"):
            state = "success" if action == "mark_finished" else "failed"
            runs_result, _ = _try(self._airflow.list_dag_runs, job_id, limit=1)
            if runs_result:
                for run in runs_result.get("dag_runs", []):
                    _try(self._airflow.set_dag_run_state, job_id, run["dag_run_id"], state)
            result = {"message": f"{job_id} {action.replace('_', ' ')}"}
        else:
            result = {"message": f"Unknown action '{action}'"}

        result["status"] = "SUCCESS"
        if warnings:
            result["warnings"] = warnings
        return result

    def get_history(self, job_id: str, **params) -> dict:
        """Fetch DAG run history from Airflow."""
        from reports.exceptions import AirflowNotFoundError, AirflowConnectionError
        try:
            result = self._airflow.list_dag_runs(
                job_id,
                limit=params.get("limit", 25),
                offset=params.get("offset", 0),
                start_date_gte=params.get("start_date"),
                start_date_lte=params.get("end_date"),
            )
            runs = [
                {
                    "dag_run_id": r.get("dag_run_id"),
                    "state": r.get("state"),
                    "start_date": r.get("start_date"),
                    "end_date": r.get("end_date"),
                    "logical_date": r.get("logical_date"),
                    "external_trigger": r.get("external_trigger"),
                    "conf": r.get("conf", {}),
                    "note": r.get("note"),
                }
                for r in result.get("dag_runs", [])
            ]
            return {
                "dag_id": job_id,
                "total_entries": result.get("total_entries", 0),
                "runs": runs,
            }
        except (AirflowNotFoundError, AirflowConnectionError):
            return {"dag_id": job_id, "total_entries": 0, "runs": []}

    def get_logs(self, job_id: str, **params) -> dict:
        """Fetch task logs from Airflow."""
        from reports.exceptions import AirflowNotFoundError, AirflowConnectionError
        run_id = params.get("run_id")
        task_id = params.get("task_id")

        if not run_id:
            try:
                runs = self._airflow.list_dag_runs(job_id, limit=1)
                dag_runs = runs.get("dag_runs", [])
                if not dag_runs:
                    return {"logs": "", "task_instances": []}
                run_id = dag_runs[0]["dag_run_id"]
            except (AirflowNotFoundError, AirflowConnectionError):
                return {"logs": "", "task_instances": [], "message": "DAG not found in Airflow"}

        try:
            ti_result = self._airflow.list_task_instances(job_id, run_id)
            task_instances = [
                {
                    "task_id": ti.get("task_id"),
                    "state": ti.get("state"),
                    "start_date": ti.get("start_date"),
                    "end_date": ti.get("end_date"),
                    "duration": ti.get("duration"),
                    "try_number": ti.get("try_number"),
                    "operator": ti.get("operator"),
                }
                for ti in ti_result.get("task_instances", [])
            ]

            logs = ""
            if task_id:
                try:
                    logs = self._airflow.get_task_logs(job_id, run_id, task_id)
                except Exception:
                    logs = "Failed to fetch logs for this task."

            return {
                "dag_id": job_id,
                "run_id": run_id,
                "task_instances": task_instances,
                "logs": logs,
            }
        except Exception as e:
            return {"error": str(e), "logs": "", "task_instances": []}

    def health_check(self) -> bool:
        return self._airflow.health_check()
