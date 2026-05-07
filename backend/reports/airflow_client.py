"""
Airflow REST API client.

Wraps Apache Airflow's stable REST API (/api/v2/) for DAG management,
triggering runs, pausing, clearing, and fetching logs.

Features:
- Shared ``requests.Session`` with connection pooling
- Automatic retry with exponential back-off (connection errors, timeouts,
  429, 5xx)
- Configurable timeouts via Django settings
- Custom exceptions (see ``reports.exceptions``)
- Correlation-ID logging on every request
- Health-check method
- Deterministic ``dag_run_id`` support for idempotent triggers
- Pagination / bulk-status helpers
"""
import logging
import uuid
from typing import Any, Optional

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from django.conf import settings

from .exceptions import (
    AirflowAPIError,
    AirflowClientError,
    AirflowConnectionError,
    AirflowNotFoundError,
    AirflowRateLimitError,
)

logger = logging.getLogger(__name__)

# ── default settings ────────────────────────────────────────────
_DEFAULT_TIMEOUT = 30          # seconds
_DEFAULT_MAX_RETRIES = 3
_DEFAULT_BACKOFF_FACTOR = 0.5  # 0s, 0.5s, 1s, 2s …
_RETRYABLE_STATUS_CODES = (429, 500, 502, 503, 504)


class AirflowClient:
    """Production-ready wrapper around the Airflow REST API v2 (Airflow 3.x)."""

    # Token is cached for this many seconds before refresh
    _TOKEN_TTL = 20 * 60  # 20 minutes

    def __init__(self) -> None:
        self.base_url: str = getattr(
            settings, "AIRFLOW_API_URL", "http://localhost:8080/api/v2"
        )
        self._username = getattr(settings, "AIRFLOW_USERNAME", "airflow")
        self._password = getattr(settings, "AIRFLOW_PASSWORD", "airflow")
        self.timeout: int = getattr(
            settings, "AIRFLOW_TIMEOUT", _DEFAULT_TIMEOUT
        )

        # JWT token cache
        self._token: str | None = None
        self._token_expires_at: float = 0.0

        # Derive auth endpoint from base URL (e.g. http://localhost:8080)
        # base_url is like "http://localhost:8080/api/v2", strip to host
        from urllib.parse import urlparse
        parsed = urlparse(self.base_url)
        self._auth_url = f"{parsed.scheme}://{parsed.netloc}/auth/token"

        # ── shared session with retry strategy ──────────────────
        max_retries = getattr(
            settings, "AIRFLOW_MAX_RETRIES", _DEFAULT_MAX_RETRIES
        )
        backoff = getattr(
            settings, "AIRFLOW_BACKOFF_FACTOR", _DEFAULT_BACKOFF_FACTOR
        )

        retry_strategy = Retry(
            total=max_retries,
            backoff_factor=backoff,
            status_forcelist=list(_RETRYABLE_STATUS_CODES),
            allowed_methods=["GET", "POST", "PATCH", "DELETE"],
            raise_on_status=False,  # we do our own status handling
        )
        adapter = HTTPAdapter(max_retries=retry_strategy)

        self._session = requests.Session()
        self._session.mount("http://", adapter)
        self._session.mount("https://", adapter)
        self._session.headers.update(
            {
                "Content-Type": "application/json",
                "Accept": "application/json",
            }
        )

    # ── JWT token management ─────────────────────────────────────

    def _obtain_token(self) -> str:
        """Obtain a fresh JWT token from Airflow 3.x auth endpoint."""
        import time
        try:
            resp = requests.post(
                self._auth_url,
                json={"username": self._username, "password": self._password},
                timeout=self.timeout,
            )
            if not resp.ok:
                raise AirflowAPIError(
                    f"Failed to obtain Airflow token: {resp.status_code}",
                    status_code=resp.status_code,
                    response_body=resp.text[:500],
                )
            data = resp.json()
            self._token = data["access_token"]
            self._token_expires_at = time.time() + self._TOKEN_TTL
            return self._token
        except requests.exceptions.ConnectionError as exc:
            raise AirflowConnectionError(
                f"Cannot reach Airflow auth endpoint: {exc}"
            ) from exc

    def _ensure_token(self) -> str:
        """Return a valid JWT token, refreshing if expired."""
        import time
        if self._token and time.time() < self._token_expires_at:
            return self._token
        return self._obtain_token()

    # ── internal helpers ─────────────────────────────────────────

    @staticmethod
    def _correlation_id() -> str:
        return uuid.uuid4().hex[:12]

    def _handle_response(self, resp: requests.Response, *, correlation_id: str) -> None:
        """Raise typed exception based on status code."""
        if resp.ok:
            return

        body = resp.text[:500]

        if resp.status_code == 404:
            raise AirflowNotFoundError(
                f"Resource not found: {resp.url}",
                status_code=404,
                response_body=body,
            )
        if resp.status_code == 429:
            raise AirflowRateLimitError(
                "Airflow rate limit exceeded",
                status_code=429,
                response_body=body,
            )
        raise AirflowAPIError(
            f"Airflow API error: {resp.status_code}",
            status_code=resp.status_code,
            response_body=body,
        )

    def _request(
        self,
        method: str,
        path: str,
        *,
        params: dict | None = None,
        json: dict | None = None,
        headers: dict | None = None,
        timeout: int | None = None,
    ) -> requests.Response:
        url = f"{self.base_url}{path}"
        cid = self._correlation_id()

        # Attach JWT bearer token
        token = self._ensure_token()
        req_headers = {
            "X-Request-ID": cid,
            "Authorization": f"Bearer {token}",
        }
        if headers:
            req_headers.update(headers)

        logger.debug(
            "airflow_request",
            extra={
                "correlation_id": cid,
                "method": method,
                "url": url,
                "params": params,
            },
        )

        try:
            resp = self._session.request(
                method,
                url,
                params=params,
                json=json,
                headers=req_headers,
                timeout=timeout or self.timeout,
            )
        except requests.exceptions.ConnectionError as exc:
            logger.error(
                "airflow_connection_error",
                extra={"correlation_id": cid, "url": url, "error": str(exc)},
            )
            raise AirflowConnectionError(
                f"Cannot reach Airflow at {url}: {exc}"
            ) from exc
        except requests.exceptions.Timeout as exc:
            logger.error(
                "airflow_timeout",
                extra={"correlation_id": cid, "url": url, "error": str(exc)},
            )
            raise AirflowConnectionError(
                f"Airflow request timed out: {url}"
            ) from exc
        except requests.exceptions.RequestException as exc:
            logger.error(
                "airflow_request_exception",
                extra={"correlation_id": cid, "url": url, "error": str(exc)},
            )
            raise AirflowClientError(
                f"Airflow request failed: {exc}"
            ) from exc

        logger.debug(
            "airflow_response",
            extra={
                "correlation_id": cid,
                "status_code": resp.status_code,
                "url": url,
            },
        )

        self._handle_response(resp, correlation_id=cid)
        return resp

    def _get(self, path: str, params: dict | None = None) -> dict:
        return self._request("GET", path, params=params).json()

    def _post(self, path: str, json: dict | None = None) -> dict:
        return self._request("POST", path, json=json or {}).json()

    def _patch(
        self,
        path: str,
        json: dict | None = None,
        params: dict | None = None,
    ) -> dict:
        return self._request("PATCH", path, json=json or {}, params=params).json()

    def _delete(self, path: str) -> dict | None:
        resp = self._request("DELETE", path)
        if resp.content:
            return resp.json()
        return None

    # ── health check ─────────────────────────────────────────────

    def health_check(self) -> bool:
        """Return True if Airflow API is reachable and responding."""
        try:
            self._get("/monitor/health")
            return True
        except AirflowClientError:
            return False

    # ── DAGs ─────────────────────────────────────────────────────

    def list_dags(
        self,
        limit: int = 100,
        offset: int = 0,
        only_active: bool = True,
    ) -> dict:
        """GET /dags — list all DAGs."""
        params: dict[str, Any] = {"limit": limit, "offset": offset}
        if only_active:
            params["only_active"] = "true"
        return self._get("/dags", params=params)

    def get_dag(self, dag_id: str) -> dict:
        """GET /dags/{dag_id}"""
        return self._get(f"/dags/{dag_id}")

    def pause_dag(self, dag_id: str) -> dict:
        """PATCH /dags/{dag_id} — pause."""
        return self._patch(f"/dags/{dag_id}", json={"is_paused": True})

    def unpause_dag(self, dag_id: str) -> dict:
        """PATCH /dags/{dag_id} — unpause."""
        return self._patch(f"/dags/{dag_id}", json={"is_paused": False})

    def delete_dag(self, dag_id: str) -> dict | None:
        """DELETE /dags/{dag_id}"""
        return self._delete(f"/dags/{dag_id}")

    # ── DAG Runs ─────────────────────────────────────────────────

    def trigger_dag(
        self,
        dag_id: str,
        conf: dict | None = None,
        logical_date: str | None = None,
        dag_run_id: str | None = None,
    ) -> dict:
        """
        POST /dags/{dag_id}/dagRuns — trigger a new run.

        Args:
            dag_run_id: Optional deterministic run ID for idempotent
                        triggers.  If omitted Airflow will auto-generate one.
        """
        payload: dict[str, Any] = {}
        if logical_date:
            payload["logical_date"] = logical_date
        if dag_run_id:
            payload["dag_run_id"] = dag_run_id
        if conf:
            payload["conf"] = conf
        return self._post(f"/dags/{dag_id}/dagRuns", json=payload)

    def list_dag_runs(
        self,
        dag_id: str,
        limit: int = 25,
        offset: int = 0,
        start_date_gte: str | None = None,
        start_date_lte: str | None = None,
        order_by: str = "-start_date",
        state: str | None = None,
    ) -> dict:
        """GET /dags/{dag_id}/dagRuns"""
        params: dict[str, Any] = {
            "limit": limit,
            "offset": offset,
            "order_by": order_by,
        }
        if start_date_gte:
            params["start_date_gte"] = start_date_gte
        if start_date_lte:
            params["start_date_lte"] = start_date_lte
        if state:
            params["state"] = state
        return self._get(f"/dags/{dag_id}/dagRuns", params=params)

    def list_active_dag_runs(self, dag_id: str, limit: int = 100) -> list[dict]:
        """Return only running / queued DAG runs for *dag_id*."""
        active: list[dict] = []
        for run_state in ("running", "queued"):
            result = self.list_dag_runs(dag_id, limit=limit, state=run_state)
            active.extend(result.get("dag_runs", []))
        return active

    def get_dag_run(self, dag_id: str, run_id: str) -> dict:
        """GET /dags/{dag_id}/dagRuns/{run_id}"""
        return self._get(f"/dags/{dag_id}/dagRuns/{run_id}")

    def get_dag_runs_bulk(
        self,
        dag_id: str,
        run_ids: list[str],
    ) -> dict[str, dict]:
        """
        Fetch status for multiple run IDs efficiently.

        Returns a dict mapping ``dag_run_id → run_dict``.
        """
        result: dict[str, dict] = {}
        # Airflow doesn't have a native bulk endpoint, so we paginate and
        # filter client-side.  For small sets this is acceptable; for very
        # large sets callers should batch externally.
        remaining = set(run_ids)
        offset = 0
        page_size = 100
        while remaining:
            page = self.list_dag_runs(dag_id, limit=page_size, offset=offset)
            runs = page.get("dag_runs", [])
            if not runs:
                break
            for run in runs:
                rid = run.get("dag_run_id")
                if rid in remaining:
                    result[rid] = run
                    remaining.discard(rid)
            offset += page_size
            # Safety: don't paginate indefinitely
            if offset > page.get("total_entries", 0):
                break
        return result

    def set_dag_run_state(
        self, dag_id: str, run_id: str, state: str
    ) -> dict:
        """PATCH /dags/{dag_id}/dagRuns/{run_id} — update state."""
        return self._patch(
            f"/dags/{dag_id}/dagRuns/{run_id}",
            json={"state": state},
        )

    def clear_dag_run(
        self,
        dag_id: str,
        run_id: str,
        dry_run: bool = False,
        only_failed: bool = True,
    ) -> dict:
        """
        POST /dags/{dag_id}/clearTaskInstances — restart a run.

        Defaults to ``only_failed=True`` so only failed tasks are re-run
        rather than blindly resetting all tasks.
        """
        payload = {
            "dag_run_id": run_id,
            "dry_run": dry_run,
            "only_failed": only_failed,
        }
        return self._post(f"/dags/{dag_id}/clearTaskInstances", json=payload)

    def delete_dag_run(self, dag_id: str, run_id: str) -> dict | None:
        """DELETE /dags/{dag_id}/dagRuns/{run_id}"""
        return self._delete(f"/dags/{dag_id}/dagRuns/{run_id}")

    # ── Task Instances ───────────────────────────────────────────

    def list_task_instances(self, dag_id: str, run_id: str) -> dict:
        """GET /dags/{dag_id}/dagRuns/{run_id}/taskInstances"""
        return self._get(f"/dags/{dag_id}/dagRuns/{run_id}/taskInstances")

    def get_task_logs(
        self,
        dag_id: str,
        run_id: str,
        task_id: str,
        try_number: int = 1,
    ) -> str:
        """GET .../taskInstances/{task_id}/logs/{try_number}"""
        path = (
            f"/dags/{dag_id}/dagRuns/{run_id}"
            f"/taskInstances/{task_id}/logs/{try_number}"
        )
        resp = self._request(
            "GET",
            path,
            headers={"Accept": "text/plain"},
        )
        return resp.text


# ── Module-level singleton ───────────────────────────────────────
airflow_client = AirflowClient()
