"""
Custom exceptions for Airflow API client.

Hierarchy:
    AirflowClientError
    ├── AirflowConnectionError   — network / timeout
    ├── AirflowAPIError          — non-2xx HTTP response
    │   ├── AirflowRateLimitError  — 429
    │   └── AirflowNotFoundError   — 404
    └── (generic catch-all)
"""


class AirflowClientError(Exception):
    """Base exception for all Airflow client errors."""

    def __init__(self, message: str = "", *, details: dict | None = None):
        self.details = details or {}
        super().__init__(message)


class AirflowConnectionError(AirflowClientError):
    """Raised when the Airflow API cannot be reached (network / timeout)."""


class AirflowAPIError(AirflowClientError):
    """Raised when Airflow returns a non-2xx HTTP response."""

    def __init__(
        self,
        message: str = "",
        *,
        status_code: int | None = None,
        response_body: str = "",
        details: dict | None = None,
    ):
        self.status_code = status_code
        self.response_body = response_body
        super().__init__(message, details=details)

    def __str__(self) -> str:
        base = super().__str__()
        if self.status_code:
            return f"[HTTP {self.status_code}] {base}"
        return base


class AirflowRateLimitError(AirflowAPIError):
    """Raised on HTTP 429 — Too Many Requests."""


class AirflowNotFoundError(AirflowAPIError):
    """Raised on HTTP 404 — resource not found in Airflow."""
