// User types
export interface User {
    id: string;
    username: string;
    email: string | null;
    display_name: string | null;
    is_admin: boolean;
    is_super_admin: boolean;
    is_active: boolean;
    organization: number | null;
    organization_name: string | null;
    created_at: string;
}

// Organization types
export interface Organization {
    id: number;
    name: string;
    description: string;
    member_count: number;
    created_at: string;
}

export interface OrgReportPermission {
    report_id: number;
    report_name: string;
    path: string;
    is_public: boolean;
    can_access: boolean;
    can_schedule: boolean;
}

// Report Group types (server-side folders with permissions)
export interface ReportGroup {
    id: number;
    name: string;
    color: string;
    description: string;
    report_ids: number[];
    report_count: number;
    created_by: string | null;
    created_at: string;
    updated_at: string;
}

export interface GroupUserPerm {
    user_id: string;
    username: string;
    display_name: string | null;
    is_admin: boolean;
    can_access: boolean;
    can_schedule: boolean;
}

export interface GroupOrgPerm {
    organization_id: number;
    name: string;
    can_access: boolean;
    can_schedule: boolean;
}

export interface GroupPermissions {
    group_id: number;
    group_name: string;
    users: GroupUserPerm[];
    organizations: GroupOrgPerm[];
}

// Auth types
export interface AuthResponse {
    access: string;
    refresh: string;
    user: User;
}

export interface LoginCredentials {
    username: string;
    password: string;
}

export interface SignupData {
    username: string;
    email?: string;
    password: string;
    display_name?: string;
}

// Repo types
export interface Repo {
    id: number;
    owner: string;
    name: string;
    path_prefix: string;
    branch: string;
    last_synced_at: string | null;
    git_remote_url: string | null;
    created_at: string;
    full_name: string;
}

export interface GitHubRepo {
    id: number;
    name: string;
    full_name: string;
    owner: string;
    private: boolean;
    html_url: string;
    clone_url: string;
    default_branch: string;
}

// Report types
export interface Report {
    id: number;
    repo: number;
    repo_name: string;
    path: string;
    report_name: string;
    display_name: string | null;
    latest_commit: string | null;
    is_public: boolean;
    created_at: string;
    groups: number[];
}

export interface ReportPermission {
    id: number;
    user: string;
    user_name: string;
    report: number;
    report_name: string;
    can_access: boolean;
    can_schedule: boolean;
    created_at: string;
}

export interface UserReportPermission {
    report_id: number;
    report_name: string;
    path: string;
    is_public: boolean;
    can_access: boolean;
    can_schedule: boolean;
    org_can_access?: boolean;
    org_can_schedule?: boolean;
    group_can_access?: boolean;
    group_can_schedule?: boolean;
}

export interface Execution {
    id: number;
    report: number;
    report_name: string;
    invoked_by: string;
    invoked_by_name: string;
    status: 'queued' | 'running' | 'success' | 'failure';
    started_at: string | null;
    finished_at: string | null;
    output_location: string | null;
    created_at: string;
}

export interface CompileResponse {
    execution_id: number;
    status: string;
    output_url: string;
    message: string;
    format?: string;
}

// Data Adapter types
export interface DataAdapter {
    id: number;
    name: string;
    adapter_type: 'jdbc' | 'csv' | 'json' | 'xml' | 'inmemory' | 'mock';
    connection_details: Record<string, unknown>;
    is_active: boolean;
    created_by: string;
    created_by_name: string;
    created_at: string;
    updated_at: string;
}

export interface TestConnectionResponse {
    success: boolean;
    message: string;
}

// Audit types
export interface AuditLog {
    id: number;
    user: string;
    user_name: string;
    action: string;
    meta: Record<string, unknown>;
    ts: string;
}

// Java Audit Service types
export interface ReportAuditEntry {
    id: number;
    userId: string;
    reportPath: string;
    sourceType: string;
    dataSourceType: string;
    outputFormat: string;
    outputPath: string;
    status: string;
    errorMessage: string | null;
    reportName: string;
    queryLanguage: string | null;
    fieldCount: number | null;
    parameterCount: number | null;
    columnCount: number | null;
    orientation: string | null;
    pageWidth: number | null;
    pageHeight: number | null;
    compileTimeMs: number | null;
    fillTimeMs: number | null;
    exportTimeMs: number | null;
    totalExecutionTimeMs: number | null;
    reportPages: number | null;
    outputFileSizeBytes: number | null;
    timestamp: string;
}

export interface AuditPerformanceStats {
    totalReports: number;
    successCount: number;
    errorCount: number;
    errorRate: number;
    avgTotalExecutionTimeMs: number | null;
    avgCompileTimeMs: number | null;
    avgFillTimeMs: number | null;
    avgExportTimeMs: number | null;
    totalDataVolumeBytes: number;
    slowestReports: ReportAuditEntry[];
}

// GitHub status
export interface GitHubStatus {
    connected: boolean;
    scope?: string;
}

// API Error
export interface ApiError {
    error?: string;
    detail?: string;
    message?: string;
}

// Metadata types (from KlexReportingService)
export interface ParameterInfo {
    name: string;
    className: string;
    defaultValueExpression: string | null;
    forPrompting: boolean;
}

export interface FieldInfo {
    name: string;
    className: string;
    description: string | null;
}

export interface VariableInfo {
    name: string;
    className: string;
    calculation: string | null;
    expression: string | null;
    resetType: string | null;
}

export interface BandInfo {
    type: string;
    height: number;
    elementCount: number;
}

export interface MetadataResponse {
    reportName: string;
    pageWidth: number;
    pageHeight: number;
    columnWidth: number;
    leftMargin: number;
    rightMargin: number;
    topMargin: number;
    bottomMargin: number;
    queryLanguage: string | null;
    queryText: string | null;
    dataAdapterName: string | null;
    parameters: ParameterInfo[];
    fields: FieldInfo[];
    variables: VariableInfo[];
    bands: BandInfo[];
    subReports: string[];
    charts: string[];
}

// Parameter validation types (from KlexReportingService)
export interface ParameterDetail {
    name: string;
    expectedType: string;
    suppliedValue: string | null;
    valid: boolean;
    defaultValue: string | null;
    forPrompting: boolean;
    error: string | null;
    description: string | null;
    nestedTypeName: string | null;
}

export interface ValidationResponse {
    valid: boolean;
    parameterDetails: ParameterDetail[];
    missingRequired: string[];
    errors: string[];
}

// Scheduler types (Quartz or Airflow — driven by scheduler_config.yaml)
export interface ScheduledJob {
    id: number;
    dag_id: string;   // Quartz: jobId, Airflow: dag_id
    schedule_name: string;
    report: number | null;
    report_name: string | null;
    created_by: string | null;
    created_by_name: string | null;
    status: 'running' | 'finished' | 'failed' | 'on_hold';
    priority: number;
    department: string;
    cron_expression: string;
    frequency_label: string;
    machine_name: string;
    estimated_runtime_min: number | null;
    max_runtime_min: number | null;
    next_run: string | null;
    last_run: string | null;
    is_active: boolean;
    termination_description: string;
    exit_code: string;
    comment_count: number;
    created_at: string;
    updated_at: string;
    engine?: 'quartz' | 'airflow';
    airflow?: {
        is_paused?: boolean;
        is_active?: boolean;
        schedule_interval?: string | null;
        next_dagrun?: string | null;
        last_parsed_time?: string | null;
        owners?: string[];
        tags?: string[];
        description?: string | null;
        error?: string;
    };
}

export interface DagRun {
    dag_run_id: string;
    state: string;
    start_date: string | null;
    end_date: string | null;
    logical_date: string | null;
    external_trigger: boolean;
    conf: Record<string, unknown>;
    note: string | null;
}

export interface TaskInstance {
    task_id: string;
    state: string;
    start_date: string | null;
    end_date: string | null;
    duration: number | null;
    try_number: number;
    operator: string;
}

export interface JobComment {
    id: number;
    job: number;
    user: string | null;
    user_name: string | null;
    text: string;
    created_at: string;
}

export type JobAction =
    | 'hold' | 'release' | 'cancel' | 'kill'
    | 'force_start' | 'restart' | 'force_restart'
    | 'skip' | 'mark_finished' | 'mark_failed';

// Dispatch Service types
export type OverlapPolicyType = 'QUEUE_ALL' | 'SKIP_IF_RUNNING' | 'LATEST_ONLY' | 'COALESCE';
export type WorkloadClassType = 'INTERACTIVE' | 'LIGHT' | 'MEDIUM' | 'HEAVY';
export type PriorityTierType = 'CRITICAL' | 'HIGH' | 'NORMAL' | 'LOW';
export type DispatchJobStatus =
    | 'RECEIVED' | 'WAITING' | 'ADMITTED' | 'TRIGGERED'
    | 'RUNNING' | 'SUCCESS' | 'FAILED_RETRYABLE' | 'FAILED_FINAL'
    | 'SKIPPED' | 'EXPIRED' | 'CANCELLED';

export interface DispatchSubmitRequest {
    dag_id?: string;
    report_id?: number;
    payload?: Record<string, unknown>;
    priority?: PriorityTierType;
    workload_class?: WorkloadClassType;
    overlap_policy?: OverlapPolicyType;
    expected_runtime_seconds?: number;
    max_retries?: number;
}

export interface DispatchSubmitResponse {
    id: string;
    dag_id: string;
    status: DispatchJobStatus;
    dedupe_key: string;
    airflow_run_id: string | null;
    message: string;
}

export interface DispatchQueueSummary {
    status_counts: Record<string, number>;
    workload_counts: Record<string, number>;
    total_waiting: number;
    total_active: number;
    avg_wait_seconds: number | null;
    long_running_count: number;
    timestamp: string;
}

// Legacy aliases for backward compatibility
export interface ScheduleRequest {
    reportUnitUri?: string;
    scheduleName: string;
    outputFormats: { outputFormat: string[] };
    outputTimeZone: string;
    trigger?: Record<string, unknown>;
    deliveryMethod: string;
    report_id?: number;
    dag_id?: string;
    cronExpression?: string;
    department?: string;
    priority?: number;
    mailNotification?: {
        messageText: string;
        subject: string;
        toAddresses: { address: string[] };
        sendToOrganization?: boolean;
        sendToOrganizations?: number[];
    };
    parameters?: Record<string, string>;
}

export interface ScheduleResponse {
    id: number;
    dag_id: string;
    dag_run: Record<string, unknown>;
    message: string;
    status: string;
}

// Filter sidebar types
export type FilterType =
    | 'column_visibility'
    | 'text_search'
    | 'date_range'
    | 'multi_select'
    | 'numeric_range'
    | 'boolean_toggle'
    | 'group_by'
    | 'sort_order'
    | 'record_limit'
    | 'display_option';

export interface FilterOption {
    value: string;
    label: string;
}

export interface FilterSection {
    id: string;
    type: FilterType;
    label: string;
    icon?: string;
    /** Parameter name(s) this filter maps to when serialized */
    paramKey: string;
    /** Secondary param key (e.g. DATE_TO for date ranges, SORT_DIR for sort) */
    paramKeySecondary?: string;
    /** Available options for multi-select, column visibility, group-by, sort */
    options?: FilterOption[];
    /** Default value */
    defaultValue?: string;
    /** For numeric range: min/max bounds */
    min?: number;
    max?: number;
    /** For date range: available presets */
    presets?: { key: string; label: string }[];
}

// Parameter Metadata types (from Django ReportParameterMetadataView)
export interface AllowedValue {
    value: string;
    label: string;
}

export interface ParameterMetadata {
    name: string;
    label: string;
    type: 'string' | 'number' | 'boolean' | 'date' | 'timestamp';
    required: boolean;
    defaultValue: string | null;
    forPrompting: boolean;
    widget: 'TextField' | 'NumericField' | 'Switch' | 'DatePicker' | 'DateTimePicker' | 'Select' | 'MultiSelect';
    supportedFormats: string[] | null;
    placeholder: string;
    group: string;
    operator: string | null;
    allowedValues: AllowedValue[] | null;
    hidden: boolean;
    readOnly: boolean;
    dependencies: string[];
    order: number;
}

export interface ParameterMetadataResponse {
    reportId: number;
    reportName: string;
    parameters: ParameterMetadata[];
}
