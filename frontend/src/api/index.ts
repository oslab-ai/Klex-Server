import api, { setTokens, clearTokens } from './client';
import type {
    User,
    AuthResponse,
    LoginCredentials,
    SignupData,
    Repo,
    GitHubRepo,
    Report,
    ReportPermission,
    UserReportPermission,
    Execution,
    CompileResponse,
    DataAdapter,
    TestConnectionResponse,
    AuditLog,
    ReportAuditEntry,
    AuditPerformanceStats,
    GitHubStatus,
    Organization,
    OrgReportPermission,
    ReportGroup,
    GroupPermissions,
    MetadataResponse,
    ValidationResponse,
    ParameterMetadataResponse,
    ScheduleRequest,
    ScheduleResponse,
    ScheduledJob,
    DagRun,
    TaskInstance,
    JobComment,
    JobAction,
    DispatchSubmitRequest,
    DispatchSubmitResponse,
    DispatchQueueSummary,
} from '../types';

// Auth API
export const authApi = {
    login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
        const { data } = await api.post<AuthResponse>('/api/auth/login/', credentials);
        setTokens(data.access, data.refresh);
        return data;
    },

    logout: async (): Promise<void> => {
        try {
            const refresh = localStorage.getItem('klex_refresh_token');
            await api.post('/api/auth/logout/', { refresh });
        } catch {
            // Ignore errors on logout
        }
        clearTokens();
    },

    me: async (): Promise<User> => {
        const { data } = await api.get<User>('/api/auth/me/');
        return data;
    },

    checkAdminExists: async (): Promise<{ admin_exists: boolean }> => {
        const { data } = await api.get<{ admin_exists: boolean }>('/api/auth/admin-setup/');
        return data;
    },

    adminSetup: async (userData: SignupData): Promise<AuthResponse> => {
        const { data } = await api.post<AuthResponse>('/api/auth/admin-setup/', userData);
        setTokens(data.access, data.refresh);
        return data;
    },
};

// Users API
export const usersApi = {
    list: async (): Promise<User[]> => {
        const { data } = await api.get<User[]>('/api/auth/users/');
        return data;
    },

    create: async (userData: SignupData): Promise<User> => {
        const { data } = await api.post<User>('/api/auth/users/', userData);
        return data;
    },

    update: async (id: string, userData: Partial<User>): Promise<User> => {
        const { data } = await api.patch<User>(`/api/auth/users/${id}/`, userData);
        return data;
    },

    delete: async (id: string): Promise<void> => {
        await api.delete(`/api/auth/users/${id}/`);
    },

    getPermissions: async (id: string): Promise<UserReportPermission[]> => {
        const { data } = await api.get<UserReportPermission[]>(`/api/auth/users/${id}/permissions/`);
        return data;
    },

    updatePermissions: async (id: string, permissions: UserReportPermission[]): Promise<void> => {
        await api.put(`/api/auth/users/${id}/permissions/`, { permissions });
    },
};

// Organizations API
export const organizationsApi = {
    list: async (): Promise<Organization[]> => {
        const { data } = await api.get<Organization[]>('/api/auth/organizations/');
        return data;
    },

    create: async (orgData: Partial<Organization>): Promise<Organization> => {
        const { data } = await api.post<Organization>('/api/auth/organizations/', orgData);
        return data;
    },

    update: async (id: number, orgData: Partial<Organization>): Promise<Organization> => {
        const { data } = await api.patch<Organization>(`/api/auth/organizations/${id}/`, orgData);
        return data;
    },

    delete: async (id: number): Promise<void> => {
        await api.delete(`/api/auth/organizations/${id}/`);
    },

    getMembers: async (id: number): Promise<User[]> => {
        const { data } = await api.get<User[]>(`/api/auth/organizations/${id}/members/`);
        return data;
    },

    getPermissions: async (id: number): Promise<OrgReportPermission[]> => {
        const { data } = await api.get<OrgReportPermission[]>(`/api/auth/organizations/${id}/permissions/`);
        return data;
    },

    updatePermissions: async (id: number, permissions: OrgReportPermission[]): Promise<void> => {
        await api.put(`/api/auth/organizations/${id}/permissions/`, { permissions });
    },
};

// Report Groups API (server-side folders with permissions)
export const groupsApi = {
    list: async (): Promise<ReportGroup[]> => {
        const { data } = await api.get<ReportGroup[]>('/api/auth/groups/');
        return data;
    },

    create: async (groupData: { name: string; color?: string; description?: string; report_ids?: number[] }): Promise<ReportGroup> => {
        const { data } = await api.post<ReportGroup>('/api/auth/groups/', groupData);
        return data;
    },

    update: async (id: number, groupData: Partial<ReportGroup>): Promise<ReportGroup> => {
        const { data } = await api.put<ReportGroup>(`/api/auth/groups/${id}/`, groupData);
        return data;
    },

    delete: async (id: number): Promise<void> => {
        await api.delete(`/api/auth/groups/${id}/`);
    },

    setReports: async (id: number, reportIds: number[]): Promise<{ group_id: number; report_ids: number[]; report_count: number }> => {
        const { data } = await api.put(`/api/auth/groups/${id}/reports/`, { report_ids: reportIds });
        return data;
    },

    getPermissions: async (id: number): Promise<GroupPermissions> => {
        const { data } = await api.get<GroupPermissions>(`/api/auth/groups/${id}/permissions/`);
        return data;
    },

    updatePermissions: async (id: number, permissions: { users?: GroupPermissions['users']; organizations?: GroupPermissions['organizations'] }): Promise<void> => {
        await api.put(`/api/auth/groups/${id}/permissions/`, permissions);
    },
};

// GitHub API
export const githubApi = {
    getAuthUrl: async (): Promise<{ auth_url: string }> => {
        const { data } = await api.get<{ auth_url: string }>('/api/github/auth/');
        return data;
    },

    getStatus: async (): Promise<GitHubStatus> => {
        const { data } = await api.get<GitHubStatus>('/api/github/status/');
        return data;
    },

    listRepos: async (): Promise<GitHubRepo[]> => {
        const { data } = await api.get<GitHubRepo[]>('/api/github/repos/');
        return data;
    },
};

// Repos API
export const reposApi = {
    list: async (): Promise<Repo[]> => {
        const { data } = await api.get<Repo[]>('/api/repos/');
        return data;
    },

    create: async (repoData: Partial<Repo>): Promise<Repo> => {
        const { data } = await api.post<Repo>('/api/repos/', repoData);
        return data;
    },

    sync: async (id: number): Promise<{ message: string; created: number; updated: number }> => {
        const { data } = await api.post(`/api/repos/${id}/sync/`);
        return data;
    },

    delete: async (id: number): Promise<void> => {
        await api.delete(`/api/repos/${id}/`);
    },
};

// Reports API
export const reportsApi = {
    list: async (): Promise<Report[]> => {
        const { data } = await api.get<Report[]>('/api/reports/');
        return data;
    },

    get: async (id: number): Promise<Report> => {
        const { data } = await api.get<Report>(`/api/reports/${id}/`);
        return data;
    },

    compile: async (id: number, parameters?: Record<string, string>, format?: string): Promise<CompileResponse> => {
        const payload: Record<string, unknown> = {};
        if (parameters) payload.parameters = parameters;
        if (format) payload.format = format;
        const { data } = await api.post<CompileResponse>(`/api/reports/${id}/compile/`, payload);
        return data;
    },

    getDownloadUrl: (id: number, executionId: number): string => {
        const baseUrl = api.defaults.baseURL || '';
        return `${baseUrl}/api/reports/${id}/download/${executionId}/`;
    },

    togglePublic: async (id: number, isPublic: boolean): Promise<Report> => {
        const { data } = await api.patch<Report>(`/api/auth/reports/${id}/toggle-public/`, { is_public: isPublic });
        return data;
    },

    getMetadata: async (id: number): Promise<MetadataResponse> => {
        const { data } = await api.post<MetadataResponse>(`/api/reports/${id}/metadata/`);
        return data;
    },

    getParameters: async (id: number): Promise<ValidationResponse> => {
        const { data } = await api.post<ValidationResponse>(`/api/reports/${id}/parameters/`);
        return data;
    },

    getParameterMetadata: async (id: number): Promise<ParameterMetadataResponse> => {
        const { data } = await api.get<ParameterMetadataResponse>(`/api/reports/${id}/parameter-metadata/`);
        return data;
    },

    validateParameters: async (id: number, parameters: Record<string, string>): Promise<ValidationResponse> => {
        const { data } = await api.post<ValidationResponse>(`/api/reports/${id}/parameters/`, { parameters });
        return data;
    },
};



// Permissions API
export const permissionsApi = {
    list: async (): Promise<ReportPermission[]> => {
        const { data } = await api.get<ReportPermission[]>('/api/reports/permissions/');
        return data;
    },

    create: async (permission: Partial<ReportPermission>): Promise<ReportPermission> => {
        const { data } = await api.post<ReportPermission>('/api/reports/permissions/', permission);
        return data;
    },

    update: async (id: number, permission: Partial<ReportPermission>): Promise<ReportPermission> => {
        const { data } = await api.patch<ReportPermission>(`/api/reports/permissions/${id}/`, permission);
        return data;
    },

    delete: async (id: number): Promise<void> => {
        await api.delete(`/api/reports/permissions/${id}/`);
    },
};

// Executions API
export const executionsApi = {
    list: async (): Promise<Execution[]> => {
        const { data } = await api.get<Execution[]>('/api/reports/executions/');
        return data;
    },

    get: async (id: number): Promise<Execution> => {
        const { data } = await api.get<Execution>(`/api/reports/executions/${id}/`);
        return data;
    },
};

// Data Adapters API
export const dataAdaptersApi = {
    list: async (): Promise<DataAdapter[]> => {
        const { data } = await api.get<DataAdapter[]>('/api/data-adapters/');
        return data;
    },

    create: async (adapter: Partial<DataAdapter>): Promise<DataAdapter> => {
        const { data } = await api.post<DataAdapter>('/api/data-adapters/', adapter);
        return data;
    },

    createWithFile: async (name: string, adapterType: string, dataFile: File): Promise<DataAdapter> => {
        const formData = new FormData();
        formData.append('name', name);
        formData.append('adapter_type', adapterType);
        // CSV uses 'csv_file', JSON/XML use 'data_file'
        const fileKey = adapterType === 'csv' ? 'csv_file' : 'data_file';
        formData.append(fileKey, dataFile);
        const { data } = await api.post<DataAdapter>('/api/data-adapters/', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
        return data;
    },

    update: async (id: number, adapter: Partial<DataAdapter>): Promise<DataAdapter> => {
        const { data } = await api.patch<DataAdapter>(`/api/data-adapters/${id}/`, adapter);
        return data;
    },

    delete: async (id: number): Promise<void> => {
        await api.delete(`/api/data-adapters/${id}/`);
    },

    test: async (id: number): Promise<TestConnectionResponse> => {
        const { data } = await api.post<TestConnectionResponse>(`/api/data-adapters/${id}/test/`);
        return data;
    },

    testConnection: async (adapterData: { adapter_type: string; connection_details: Record<string, unknown> }): Promise<TestConnectionResponse> => {
        const { data } = await api.post<TestConnectionResponse>('/api/data-adapters/test/', adapterData);
        return data;
    },
};

// Audit API
export const auditApi = {
    list: async (params?: { action?: string; user_id?: string }): Promise<AuditLog[]> => {
        const { data } = await api.get<AuditLog[]>('/api/audit-logs/', { params });
        return data;
    },
};

// Report Audit API (Java service proxy)
export const reportAuditApi = {
    getLogs: async (params?: { status?: string; format?: string; dataSourceType?: string }): Promise<ReportAuditEntry[]> => {
        const { data } = await api.get<ReportAuditEntry[]>('/api/reports/audit/logs/', { params });
        return data;
    },

    getStats: async (): Promise<AuditPerformanceStats> => {
        const { data } = await api.get<AuditPerformanceStats>('/api/reports/audit/stats/');
        return data;
    },
};

// Jobs API (Unified scheduling — Quartz or Airflow)
export const jobsApi = {
    getEngine: async (): Promise<{ engine: string; healthy: boolean }> => {
        const { data } = await api.get('/api/reports/scheduler-engine/');
        return data;
    },

    list: async (params?: { status?: string; search?: string }): Promise<{ count: number; jobs: ScheduledJob[]; engine: string }> => {
        const { data } = await api.get('/api/reports/jobs/', { params });
        return data;
    },

    get: async (id: number): Promise<ScheduledJob> => {
        const { data } = await api.get<ScheduledJob>(`/api/reports/jobs/${id}/`);
        return data;
    },

    create: async (scheduleData: ScheduleRequest): Promise<ScheduleResponse> => {
        const { data } = await api.post<ScheduleResponse>('/api/reports/schedule/', scheduleData);
        return data;
    },

    delete: async (id: number): Promise<{ message: string; status: string }> => {
        const { data } = await api.delete(`/api/reports/schedule/${id}/`);
        return data;
    },

    performAction: async (id: number, action: JobAction): Promise<{ message: string; status: string; job_status: string }> => {
        const { data } = await api.post(`/api/reports/jobs/${id}/action/`, { action });
        return data;
    },

    updatePriority: async (id: number, priority: number): Promise<{ id: number; priority: number; message: string }> => {
        const { data } = await api.patch(`/api/reports/jobs/${id}/priority/`, { priority });
        return data;
    },

    getComments: async (id: number): Promise<{ comments: JobComment[] }> => {
        const { data } = await api.get(`/api/reports/jobs/${id}/comments/`);
        return data;
    },

    addComment: async (id: number, text: string): Promise<JobComment> => {
        const { data } = await api.post<JobComment>(`/api/reports/jobs/${id}/comments/`, { text });
        return data;
    },

    getHistory: async (id: number, params?: { limit?: number; offset?: number; start_date?: string; end_date?: string }): Promise<{ dag_id: string; total_entries: number; runs: DagRun[] }> => {
        const { data } = await api.get(`/api/reports/jobs/${id}/history/`, { params });
        return data;
    },

    getLogs: async (id: number, params?: { run_id?: string; task_id?: string }): Promise<{ dag_id: string; run_id: string; task_instances: TaskInstance[]; logs: string }> => {
        const { data } = await api.get(`/api/reports/jobs/${id}/logs/`, { params });
        return data;
    },

    switchEngine: async (target_engine: string, opts: { dry_run?: boolean; force?: boolean } = {}): Promise<any> => {
        const { data } = await api.post('/api/reports/scheduler-engine/switch/', { target_engine, ...opts });
        return data;
    },

    getAirflowUiUrl: async (): Promise<{ available: boolean; engine: string; ui_url?: string; dags_url?: string; message?: string }> => {
        const { data } = await api.get('/api/reports/airflow-ui/');
        return data;
    },
};

// Dispatch Service API (capacity-aware job queue)
export const dispatchApi = {
    submit: async (data: DispatchSubmitRequest): Promise<DispatchSubmitResponse> => {
        const { data: response } = await api.post<DispatchSubmitResponse>('/api/reports/dispatch/submit/', data);
        return response;
    },

    summary: async (): Promise<DispatchQueueSummary> => {
        const { data } = await api.get<DispatchQueueSummary>('/api/reports/dispatch/summary/');
        return data;
    },
};

// Legacy alias
export const schedulerApi = jobsApi;
export * from './chat';
