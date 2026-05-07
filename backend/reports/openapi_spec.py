"""
OpenAPI 3.0 specification for Klex Django API.

Documents all Django endpoints that proxy to or wrap the
KlexReportingService Java engine.
"""

OPENAPI_SPEC = {
    "openapi": "3.0.3",
    "info": {
        "title": "Klex Reporting Server — Django API",
        "description": (
            "REST API reference for the Klex Reporting Server.\n\n"
            "Klex exposes a Django backend that wraps the KlexReportingService "
            "(Java/Spring Boot) engine. All endpoints listed here are served by the "
            "Django server — the Java engine is an internal implementation detail.\n\n"
            "### Authentication\n"
            "All endpoints require a valid JWT Bearer token obtained via "
            "`POST /api/auth/login/`.\n\n"
            "### Service Groups\n"
            "- **Reports** — CRUD, compilation, and download\n"
            "- **Metadata** — JRXML structure extraction\n"
            "- **Parameters** — Validation and UI-ready schema\n"
            "- **Audit** — Execution logs and performance stats\n"
            "- **Scheduling** — Quartz / Airflow job management\n"
            "- **Dispatch** — Airflow-only admission-controlled submission\n"
            "- **Audit Logs (Django)** — Application-level audit trail\n"
        ),
        "version": "1.0.0",
        "contact": {
            "name": "Klex Team",
            "url": "https://github.com/teenybopper/klex",
        },
    },
    "servers": [
        {
            "url": "/",
            "description": "Current server",
        },
    ],
    "tags": [
        {"name": "Reports", "description": "Report CRUD, compilation, and download"},
        {"name": "Metadata", "description": "JRXML metadata extraction"},
        {"name": "Parameters", "description": "Parameter validation and UI-ready metadata"},
        {"name": "Audit", "description": "Report execution audit logs and performance statistics"},
        {"name": "Scheduling", "description": "Unified job scheduling (Quartz / Airflow)"},
        {"name": "Dispatch", "description": "Airflow-only admission-controlled job submission"},
        {"name": "Audit Logs", "description": "Django application-level audit trail"},
        {"name": "Authentication", "description": "JWT authentication endpoints"},
        {"name": "Data Adapters", "description": "Data source connection management"},
    ],
    "components": {
        "securitySchemes": {
            "BearerAuth": {
                "type": "http",
                "scheme": "bearer",
                "bearerFormat": "JWT",
                "description": "JWT token obtained from POST /api/auth/login/",
            }
        },
        "schemas": {
            "Error": {
                "type": "object",
                "properties": {
                    "error": {"type": "string", "description": "Error message"},
                },
            },
            "Report": {
                "type": "object",
                "properties": {
                    "id": {"type": "integer"},
                    "report_name": {"type": "string"},
                    "display_name": {"type": "string", "nullable": True},
                    "path": {"type": "string", "description": "Path within the GitHub repo"},
                    "is_public": {"type": "boolean"},
                    "created_at": {"type": "string", "format": "date-time"},
                    "updated_at": {"type": "string", "format": "date-time"},
                },
            },
            "CompileRequest": {
                "type": "object",
                "properties": {
                    "format": {
                        "type": "string",
                        "enum": ["PDF", "XLSX", "CSV", "HTML", "DOCX", "PPTX", "ODT"],
                        "default": "PDF",
                        "description": "Output format for the compiled report",
                    },
                    "parameters": {
                        "type": "object",
                        "additionalProperties": {"type": "string"},
                        "description": "Report parameters to inject at compile time",
                    },
                },
            },
            "CompileResponse": {
                "type": "object",
                "properties": {
                    "execution_id": {"type": "integer"},
                    "status": {"type": "string", "example": "success"},
                    "output_url": {"type": "string", "example": "/api/reports/1/download/42/"},
                    "message": {"type": "string"},
                    "format": {"type": "string", "example": "pdf"},
                },
            },
            "Execution": {
                "type": "object",
                "properties": {
                    "id": {"type": "integer"},
                    "report": {"type": "integer"},
                    "invoked_by": {"type": "string"},
                    "status": {
                        "type": "string",
                        "enum": ["running", "success", "failure"],
                    },
                    "output_format": {"type": "string"},
                    "output_location": {"type": "string", "nullable": True},
                    "started_at": {"type": "string", "format": "date-time", "nullable": True},
                    "finished_at": {"type": "string", "format": "date-time", "nullable": True},
                    "created_at": {"type": "string", "format": "date-time"},
                },
            },
            "MetadataResponse": {
                "type": "object",
                "properties": {
                    "parameters": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string"},
                                "type": {"type": "string", "example": "java.util.Date"},
                                "defaultValue": {"type": "string", "nullable": True},
                                "description": {"type": "string", "nullable": True},
                            },
                        },
                    },
                    "fields": {"type": "array", "items": {"type": "string"}},
                    "queryString": {"type": "string", "nullable": True},
                    "bands": {"type": "array", "items": {"type": "string"}},
                    "charts": {"type": "array", "items": {"type": "string"}},
                    "subReports": {"type": "array", "items": {"type": "string"}},
                },
            },
            "ParameterValidationRequest": {
                "type": "object",
                "properties": {
                    "parameters": {
                        "type": "object",
                        "additionalProperties": {"type": "string"},
                        "description": "Parameter values to validate",
                    },
                },
            },
            "ParameterValidationResponse": {
                "type": "object",
                "properties": {
                    "valid": {"type": "boolean"},
                    "errors": {"type": "array", "items": {"type": "string"}},
                    "warnings": {"type": "array", "items": {"type": "string"}},
                },
            },
            "ParameterMetadata": {
                "type": "object",
                "properties": {
                    "reportId": {"type": "integer"},
                    "reportName": {"type": "string"},
                    "parameters": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string"},
                                "label": {"type": "string"},
                                "type": {
                                    "type": "string",
                                    "enum": ["string", "number", "boolean", "date", "timestamp"],
                                },
                                "required": {"type": "boolean"},
                                "defaultValue": {"type": "string", "nullable": True},
                                "forPrompting": {"type": "boolean"},
                                "widget": {
                                    "type": "string",
                                    "enum": [
                                        "TextField", "NumericField", "Switch",
                                        "DatePicker", "DateTimePicker",
                                        "Select", "MultiSelect",
                                    ],
                                },
                                "supportedFormats": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                    "nullable": True,
                                },
                                "placeholder": {"type": "string"},
                                "group": {"type": "string", "enum": ["Basic Filters", "Advanced Filters"]},
                                "allowedValues": {
                                    "type": "array",
                                    "items": {
                                        "type": "object",
                                        "properties": {
                                            "value": {"type": "string"},
                                            "label": {"type": "string"},
                                        },
                                    },
                                    "nullable": True,
                                },
                                "hidden": {"type": "boolean"},
                                "readOnly": {"type": "boolean"},
                                "order": {"type": "integer"},
                            },
                        },
                    },
                },
            },
            "AuditLogEntry": {
                "type": "object",
                "properties": {
                    "id": {"type": "integer"},
                    "reportPath": {"type": "string"},
                    "status": {"type": "string", "enum": ["SUCCESS", "ERROR"]},
                    "format": {"type": "string"},
                    "dataSourceType": {"type": "string"},
                    "compileTimeMs": {"type": "integer"},
                    "fillTimeMs": {"type": "integer"},
                    "exportTimeMs": {"type": "integer"},
                    "totalTimeMs": {"type": "integer"},
                    "timestamp": {"type": "string", "format": "date-time"},
                },
            },
            "AuditStats": {
                "type": "object",
                "properties": {
                    "totalRuns": {"type": "integer"},
                    "successCount": {"type": "integer"},
                    "errorCount": {"type": "integer"},
                    "errorRate": {"type": "number", "format": "float"},
                    "avgCompileTimeMs": {"type": "number"},
                    "avgFillTimeMs": {"type": "number"},
                    "avgExportTimeMs": {"type": "number"},
                    "avgTotalTimeMs": {"type": "number"},
                    "totalDataVolumeBytes": {"type": "integer"},
                    "top10SlowestReports": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "reportPath": {"type": "string"},
                                "avgTotalTimeMs": {"type": "number"},
                            },
                        },
                    },
                },
            },
            "ScheduleRequest": {
                "type": "object",
                "required": ["scheduleName", "cronExpression"],
                "properties": {
                    "report_id": {"type": "integer", "description": "ID of the report to schedule"},
                    "scheduleName": {"type": "string"},
                    "cronExpression": {"type": "string", "description": "Quartz cron expression (6 fields)"},
                    "department": {"type": "string", "default": "General"},
                    "priority": {"type": "integer", "default": 0},
                    "frequencyLabel": {"type": "string"},
                    "machineName": {"type": "string"},
                    "estimatedRuntimeMin": {"type": "integer", "nullable": True},
                    "maxRuntimeMin": {"type": "integer", "default": 30},
                },
            },
            "ScheduleResponse": {
                "type": "object",
                "properties": {
                    "id": {"type": "integer"},
                    "dag_id": {"type": "string"},
                    "engine": {"type": "string", "enum": ["quartz", "airflow"]},
                    "engine_response": {"type": "object"},
                    "message": {"type": "string"},
                    "status": {"type": "string"},
                },
            },
            "ScheduledJob": {
                "type": "object",
                "properties": {
                    "id": {"type": "integer"},
                    "dag_id": {"type": "string"},
                    "schedule_name": {"type": "string"},
                    "status": {
                        "type": "string",
                        "enum": ["running", "on_hold", "finished", "failed"],
                    },
                    "priority": {"type": "integer"},
                    "department": {"type": "string"},
                    "cron_expression": {"type": "string"},
                    "is_active": {"type": "boolean"},
                    "created_at": {"type": "string", "format": "date-time"},
                    "updated_at": {"type": "string", "format": "date-time"},
                },
            },
            "JobActionRequest": {
                "type": "object",
                "required": ["action"],
                "properties": {
                    "action": {
                        "type": "string",
                        "enum": [
                            "hold", "release", "cancel", "kill", "force_start",
                            "restart", "force_restart", "skip",
                            "mark_finished", "mark_failed",
                        ],
                    },
                },
            },
            "DispatchSubmitRequest": {
                "type": "object",
                "properties": {
                    "report_id": {"type": "integer"},
                    "dag_id": {"type": "string"},
                    "payload": {"type": "object"},
                    "priority": {"type": "string", "enum": ["CRITICAL", "HIGH", "NORMAL", "LOW", "BULK"]},
                    "workload_class": {"type": "string", "enum": ["LIGHT", "MEDIUM", "HEAVY", "EXTREME"]},
                    "overlap_policy": {"type": "string", "enum": ["SKIP", "QUEUE_ALL", "REPLACE"]},
                    "expected_runtime_seconds": {"type": "integer", "nullable": True},
                    "max_retries": {"type": "integer", "nullable": True},
                },
            },
            "DispatchSubmitResponse": {
                "type": "object",
                "properties": {
                    "id": {"type": "string"},
                    "dag_id": {"type": "string"},
                    "status": {"type": "string"},
                    "dedupe_key": {"type": "string"},
                    "airflow_run_id": {"type": "string", "nullable": True},
                    "message": {"type": "string"},
                },
            },
            "DispatchQueueSummary": {
                "type": "object",
                "properties": {
                    "status_counts": {"type": "object"},
                    "workload_counts": {"type": "object"},
                    "total_waiting": {"type": "integer"},
                    "total_active": {"type": "integer"},
                    "avg_wait_seconds": {"type": "number", "nullable": True},
                    "long_running_count": {"type": "integer"},
                    "engine": {"type": "string"},
                },
            },
            "DjangoAuditLog": {
                "type": "object",
                "properties": {
                    "id": {"type": "integer"},
                    "user": {"type": "integer"},
                    "action": {"type": "string"},
                    "created_at": {"type": "string", "format": "date-time"},
                },
            },
            "SchedulerEngineStatus": {
                "type": "object",
                "properties": {
                    "engine": {"type": "string", "enum": ["quartz", "airflow"]},
                    "healthy": {"type": "boolean"},
                },
            },
        },
    },
    "security": [{"BearerAuth": []}],
    "paths": {
        # ── Reports ──────────────────────────────────────────────
        "/api/reports/": {
            "get": {
                "tags": ["Reports"],
                "summary": "List reports",
                "description": (
                    "Returns reports visible to the current user. Admins see all reports; "
                    "regular users see public reports and those they have explicit permission for."
                ),
                "responses": {
                    "200": {
                        "description": "Array of reports",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "array",
                                    "items": {"$ref": "#/components/schemas/Report"},
                                }
                            }
                        },
                    },
                    "401": {"description": "Not authenticated"},
                },
            },
        },
        "/api/reports/{id}/": {
            "get": {
                "tags": ["Reports"],
                "summary": "Get report details",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {
                        "description": "Report details",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/Report"},
                            }
                        },
                    },
                    "404": {"description": "Report not found"},
                },
            },
        },
        "/api/reports/{id}/compile/": {
            "post": {
                "tags": ["Reports"],
                "summary": "Compile and generate a report",
                "description": (
                    "Downloads the JRXML template from GitHub, resolves the data source "
                    "configuration, and delegates compilation to the internal Java engine. "
                    "Returns an execution record with a download URL on success."
                ),
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "requestBody": {
                    "required": False,
                    "content": {
                        "application/json": {
                            "schema": {"$ref": "#/components/schemas/CompileRequest"},
                        }
                    },
                },
                "responses": {
                    "200": {
                        "description": "Report compiled successfully",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/CompileResponse"},
                            }
                        },
                    },
                    "400": {"description": "Unsupported format"},
                    "403": {"description": "Permission denied"},
                    "404": {"description": "Report not found"},
                    "500": {"description": "Compilation failed"},
                    "503": {"description": "Java engine unavailable"},
                },
            },
        },
        "/api/reports/{id}/download/": {
            "get": {
                "tags": ["Reports"],
                "summary": "Download latest compiled report",
                "description": "Serves the output file from the most recent successful execution.",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {
                        "description": "File download",
                        "content": {
                            "application/pdf": {"schema": {"type": "string", "format": "binary"}},
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": {
                                "schema": {"type": "string", "format": "binary"},
                            },
                            "text/csv": {"schema": {"type": "string", "format": "binary"}},
                        },
                    },
                    "404": {"description": "No compiled report available"},
                },
            },
        },
        "/api/reports/{id}/download/{execution_id}/": {
            "get": {
                "tags": ["Reports"],
                "summary": "Download a specific execution output",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                    {"name": "execution_id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {"description": "File download"},
                    "404": {"description": "Execution not found"},
                },
            },
        },
        "/api/reports/executions/": {
            "get": {
                "tags": ["Reports"],
                "summary": "List executions",
                "description": "Admins see all executions; users see only their own.",
                "responses": {
                    "200": {
                        "description": "Array of executions",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "array",
                                    "items": {"$ref": "#/components/schemas/Execution"},
                                }
                            }
                        },
                    },
                },
            },
        },
        "/api/reports/executions/{id}/": {
            "get": {
                "tags": ["Reports"],
                "summary": "Get execution details",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {
                        "description": "Execution details",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/Execution"},
                            }
                        },
                    },
                    "404": {"description": "Execution not found"},
                },
            },
        },
        # ── Metadata ─────────────────────────────────────────────
        "/api/reports/{id}/metadata/": {
            "post": {
                "tags": ["Metadata"],
                "summary": "Extract JRXML metadata",
                "description": (
                    "Downloads the JRXML template from GitHub and extracts structural "
                    "metadata — parameters, fields, SQL query, bands, charts, and sub-reports. "
                    "Useful for dynamically building frontend parameter forms."
                ),
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {
                        "description": "Metadata extracted",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/MetadataResponse"},
                            }
                        },
                    },
                    "404": {"description": "Report not found"},
                    "503": {"description": "Java engine unavailable"},
                },
            },
        },
        # ── Parameters ───────────────────────────────────────────
        "/api/reports/{id}/parameters/": {
            "post": {
                "tags": ["Parameters"],
                "summary": "Validate report parameters",
                "description": (
                    "Validates user-supplied parameter values against the JRXML parameter "
                    "definitions. Returns validation errors and warnings."
                ),
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "requestBody": {
                    "content": {
                        "application/json": {
                            "schema": {"$ref": "#/components/schemas/ParameterValidationRequest"},
                        }
                    },
                },
                "responses": {
                    "200": {
                        "description": "Validation result",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/ParameterValidationResponse"},
                            }
                        },
                    },
                    "404": {"description": "Report not found"},
                    "503": {"description": "Java engine unavailable"},
                },
            },
        },
        "/api/reports/{id}/parameter-metadata/": {
            "get": {
                "tags": ["Parameters"],
                "summary": "Get UI-ready parameter schema",
                "description": (
                    "Returns enriched parameter metadata with widget types, allowed values, "
                    "grouping, and dynamic options fetched from the data source. "
                    "Designed for automatic frontend form generation."
                ),
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {
                        "description": "Parameter metadata schema",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/ParameterMetadata"},
                            }
                        },
                    },
                    "404": {"description": "Report not found"},
                    "503": {"description": "Java engine unavailable"},
                },
            },
        },
        # ── Audit ────────────────────────────────────────────────
        "/api/reports/audit/logs/": {
            "get": {
                "tags": ["Audit"],
                "summary": "List report audit logs",
                "description": (
                    "Returns audit log entries from the Java reporting engine. "
                    "Admin-only. Supports filtering by status, format, and data source type."
                ),
                "parameters": [
                    {"name": "status", "in": "query", "schema": {"type": "string", "enum": ["SUCCESS", "ERROR"]}},
                    {"name": "format", "in": "query", "schema": {"type": "string", "enum": ["PDF", "XLSX", "CSV", "HTML"]}},
                    {"name": "dataSourceType", "in": "query", "schema": {"type": "string", "enum": ["jdbc", "csv", "json", "xml", "inmemory"]}},
                ],
                "responses": {
                    "200": {
                        "description": "Array of audit log entries",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "array",
                                    "items": {"$ref": "#/components/schemas/AuditLogEntry"},
                                }
                            }
                        },
                    },
                    "403": {"description": "Admin access required"},
                    "503": {"description": "Java engine unavailable"},
                },
            },
        },
        "/api/reports/audit/stats/": {
            "get": {
                "tags": ["Audit"],
                "summary": "Get aggregate performance statistics",
                "description": "Returns aggregate performance statistics across all report executions. Admin-only.",
                "responses": {
                    "200": {
                        "description": "Audit statistics",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/AuditStats"},
                            }
                        },
                    },
                    "403": {"description": "Admin access required"},
                    "503": {"description": "Java engine unavailable"},
                },
            },
        },
        # ── Scheduling ───────────────────────────────────────────
        "/api/reports/scheduler-engine/": {
            "get": {
                "tags": ["Scheduling"],
                "summary": "Get active scheduler engine",
                "description": "Returns the currently active scheduling engine (quartz or airflow) and its health status.",
                "responses": {
                    "200": {
                        "description": "Engine status",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/SchedulerEngineStatus"},
                            }
                        },
                    },
                },
            },
        },
        "/api/reports/schedule/": {
            "post": {
                "tags": ["Scheduling"],
                "summary": "Create a scheduled job",
                "description": (
                    "Creates a new scheduled report job. Delegates to the active scheduler engine "
                    "(Quartz or Airflow). Resolves report artefacts (JRXML, data source) automatically."
                ),
                "requestBody": {
                    "required": True,
                    "content": {
                        "application/json": {
                            "schema": {"$ref": "#/components/schemas/ScheduleRequest"},
                        }
                    },
                },
                "responses": {
                    "200": {
                        "description": "Schedule created",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/ScheduleResponse"},
                            }
                        },
                    },
                    "404": {"description": "Report not found"},
                    "503": {"description": "Scheduler engine unavailable"},
                },
            },
        },
        "/api/reports/schedule/{id}/": {
            "delete": {
                "tags": ["Scheduling"],
                "summary": "Delete a scheduled job",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {"description": "Job deleted"},
                    "404": {"description": "Job not found"},
                },
            },
        },
        "/api/reports/jobs/": {
            "get": {
                "tags": ["Scheduling"],
                "summary": "List all scheduled jobs",
                "description": "Lists all managed scheduled jobs with live status sync from the engine.",
                "parameters": [
                    {"name": "status", "in": "query", "schema": {"type": "string", "enum": ["running", "on_hold", "finished", "failed"]}},
                    {"name": "search", "in": "query", "schema": {"type": "string"}, "description": "Search by schedule name, DAG ID, or department"},
                ],
                "responses": {
                    "200": {
                        "description": "List of jobs",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "object",
                                    "properties": {
                                        "count": {"type": "integer"},
                                        "jobs": {"type": "array", "items": {"$ref": "#/components/schemas/ScheduledJob"}},
                                        "engine": {"type": "string"},
                                    },
                                }
                            }
                        },
                    },
                },
            },
        },
        "/api/reports/jobs/{id}/": {
            "get": {
                "tags": ["Scheduling"],
                "summary": "Get job detail",
                "description": "Retrieves details of a specific scheduled job, enriched with live engine data when using Airflow.",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {
                    "200": {"description": "Job detail with engine enrichment"},
                    "404": {"description": "Job not found"},
                },
            },
        },
        "/api/reports/jobs/{id}/action/": {
            "post": {
                "tags": ["Scheduling"],
                "summary": "Perform action on a job",
                "description": (
                    "Execute a lifecycle action on a scheduled job: hold, release, cancel, "
                    "kill, force_start, restart, force_restart, skip, mark_finished, mark_failed."
                ),
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "requestBody": {
                    "required": True,
                    "content": {
                        "application/json": {
                            "schema": {"$ref": "#/components/schemas/JobActionRequest"},
                        }
                    },
                },
                "responses": {
                    "200": {"description": "Action performed"},
                    "400": {"description": "Invalid action"},
                    "404": {"description": "Job not found"},
                },
            },
        },
        "/api/reports/jobs/{id}/priority/": {
            "patch": {
                "tags": ["Scheduling"],
                "summary": "Update job priority",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "requestBody": {
                    "content": {
                        "application/json": {
                            "schema": {
                                "type": "object",
                                "required": ["priority"],
                                "properties": {"priority": {"type": "integer"}},
                            }
                        }
                    },
                },
                "responses": {
                    "200": {"description": "Priority updated"},
                    "404": {"description": "Job not found"},
                },
            },
        },
        "/api/reports/jobs/{id}/comments/": {
            "get": {
                "tags": ["Scheduling"],
                "summary": "List job comments",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "responses": {"200": {"description": "List of comments"}},
            },
            "post": {
                "tags": ["Scheduling"],
                "summary": "Add a comment to a job",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                ],
                "requestBody": {
                    "content": {
                        "application/json": {
                            "schema": {
                                "type": "object",
                                "required": ["text"],
                                "properties": {"text": {"type": "string"}},
                            }
                        }
                    },
                },
                "responses": {
                    "201": {"description": "Comment added"},
                    "400": {"description": "Text required"},
                    "404": {"description": "Job not found"},
                },
            },
        },
        "/api/reports/jobs/{id}/history/": {
            "get": {
                "tags": ["Scheduling"],
                "summary": "Get job execution history",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                    {"name": "limit", "in": "query", "schema": {"type": "integer", "default": 25}},
                    {"name": "offset", "in": "query", "schema": {"type": "integer", "default": 0}},
                    {"name": "start_date", "in": "query", "schema": {"type": "string", "format": "date"}},
                    {"name": "end_date", "in": "query", "schema": {"type": "string", "format": "date"}},
                ],
                "responses": {"200": {"description": "Execution history"}},
            },
        },
        "/api/reports/jobs/{id}/logs/": {
            "get": {
                "tags": ["Scheduling"],
                "summary": "Get job logs",
                "parameters": [
                    {"name": "id", "in": "path", "required": True, "schema": {"type": "integer"}},
                    {"name": "run_id", "in": "query", "schema": {"type": "string"}},
                    {"name": "task_id", "in": "query", "schema": {"type": "string"}},
                ],
                "responses": {"200": {"description": "Job logs"}},
            },
        },
        # ── Dispatch (Airflow-only) ──────────────────────────────
        "/api/reports/dispatch/submit/": {
            "post": {
                "tags": ["Dispatch"],
                "summary": "Submit a report execution job",
                "description": (
                    "Submit a report execution job through the dispatch service with "
                    "admission control and deduplication. **Airflow-only** — returns 400 "
                    "if the active engine is Quartz."
                ),
                "requestBody": {
                    "required": True,
                    "content": {
                        "application/json": {
                            "schema": {"$ref": "#/components/schemas/DispatchSubmitRequest"},
                        }
                    },
                },
                "responses": {
                    "201": {
                        "description": "Job submitted",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/DispatchSubmitResponse"},
                            }
                        },
                    },
                    "200": {"description": "Job skipped (duplicate)"},
                    "400": {"description": "Airflow not active / missing dag_id"},
                    "404": {"description": "Report not found"},
                },
            },
        },
        "/api/reports/dispatch/summary/": {
            "get": {
                "tags": ["Dispatch"],
                "summary": "Get dispatch queue summary",
                "description": "Returns queue summary including status counts, workload distribution, and wait times. Admin-only.",
                "responses": {
                    "200": {
                        "description": "Queue summary",
                        "content": {
                            "application/json": {
                                "schema": {"$ref": "#/components/schemas/DispatchQueueSummary"},
                            }
                        },
                    },
                    "403": {"description": "Admin access required"},
                },
            },
        },
        # ── Django Audit Logs ────────────────────────────────────
        "/api/audit-logs/": {
            "get": {
                "tags": ["Audit Logs"],
                "summary": "List application audit logs",
                "description": "Returns Django-level audit logs (user actions). Admin-only. Supports filtering by action and user_id.",
                "parameters": [
                    {"name": "action", "in": "query", "schema": {"type": "string"}, "description": "Filter by action type"},
                    {"name": "user_id", "in": "query", "schema": {"type": "integer"}, "description": "Filter by user ID"},
                ],
                "responses": {
                    "200": {
                        "description": "Array of audit logs",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "array",
                                    "items": {"$ref": "#/components/schemas/DjangoAuditLog"},
                                }
                            }
                        },
                    },
                    "403": {"description": "Admin access required"},
                },
            },
        },
    },
}
