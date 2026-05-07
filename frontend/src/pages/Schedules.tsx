import { useState, useEffect, useCallback } from 'react';
import {
    Calendar, Search, RefreshCw,
    Activity, CheckCircle, XCircle, PauseCircle, Layers,
    ArrowUpDown, Clock, Server, Tag, Zap, Timer, AlertTriangle,
} from 'lucide-react';
import { jobsApi, dispatchApi } from '../api';
import type { ScheduledJob, JobAction, DispatchQueueSummary } from '../types';
import JobContextMenu from '../components/JobContextMenu';
import JobInfoModal from '../components/JobInfoModal';
import JobHistoryModal from '../components/JobHistoryModal';
import JobCommentModal from '../components/JobCommentModal';
import JobActionModal from '../components/JobActionModals';
import ScheduleModal from '../components/ScheduleModal';
import { useAuth } from '../context/AuthContext';

/* ──── status helpers ──────────────────────────────────── */
const STATUS_CONFIG: Record<string, { label: string; dotClass: string; badgeClass: string }> = {
    running: { label: 'Running', dotClass: 'status-dot status-dot-active', badgeClass: 'badge-primary' },
    finished: { label: 'Finished', dotClass: 'status-dot bg-green-500', badgeClass: 'badge-success' },
    failed: { label: 'Failed', dotClass: 'status-dot bg-red-500', badgeClass: 'badge-danger' },
    on_hold: { label: 'On Hold', dotClass: 'status-dot bg-amber-500', badgeClass: 'badge-warning' },
};

const PRIORITY_BADGES: Record<number, { label: string; cls: string }> = {
    0: { label: 'Normal', cls: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300' },
    1: { label: 'Low', cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300' },
    2: { label: 'Medium', cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300' },
    3: { label: 'High', cls: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-300' },
    4: { label: 'Critical', cls: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300' },
};

function getPriorityBadge(p: number) {
    return PRIORITY_BADGES[p] || PRIORITY_BADGES[0];
}

function formatDate(d: string | null) {
    if (!d) return '—';
    return new Date(d).toLocaleString('en-US', {
        month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
    });
}

/* getDuration is available for future use in duration column */
// function getDuration(start: string | null, end: string | null) { ... }

/* ──── component ───────────────────────────────────────── */
export default function Schedules() {
    const [jobs, setJobs] = useState<ScheduledJob[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [statusFilter, setStatusFilter] = useState<string | null>(null);
    const [sortBy, setSortBy] = useState<'priority' | 'name' | 'updated'>('priority');

    // Modals
    const [showScheduleModal, setShowScheduleModal] = useState(false);
    const [selectedJob, setSelectedJob] = useState<ScheduledJob | null>(null);
    const [showInfoModal, setShowInfoModal] = useState(false);
    const [showHistoryModal, setShowHistoryModal] = useState(false);
    const [showCommentModal, setShowCommentModal] = useState(false);
    const [actionModal, setActionModal] = useState<{ action: JobAction; job: ScheduledJob } | null>(null);

    // Context menu
    const [contextMenu, setContextMenu] = useState<{ x: number; y: number; job: ScheduledJob } | null>(null);

    // Dispatch queue
    const [dispatchSummary, setDispatchSummary] = useState<DispatchQueueSummary | null>(null);

    // Engine info
    const [activeEngine, setActiveEngine] = useState<string>('quartz');
    const [engineHealthy, setEngineHealthy] = useState<boolean>(true);
    const { user } = useAuth();

    /* ─ fetch ─ */
    const fetchJobs = useCallback(async () => {
        setLoading(true);
        try {
            const params: Record<string, string> = {};
            if (statusFilter) params.status = statusFilter;
            if (searchQuery) params.search = searchQuery;
            const result = await jobsApi.list(params);
            setJobs(result.jobs);
        } catch (err) {
            console.error('Failed to load jobs:', err);
        } finally {
            setLoading(false);
        }
    }, [statusFilter, searchQuery]);

    useEffect(() => { fetchJobs(); }, [fetchJobs]);

    /* ─ fetch dispatch summary ─ */
    const fetchDispatchSummary = useCallback(async () => {
        if (activeEngine !== 'airflow') return;
        try {
            const summary = await dispatchApi.summary();
            setDispatchSummary(summary);
        } catch {
            // Silently ignore — admin-only endpoint, may 403 for non-admins
        }
    }, [activeEngine]);

    /* ─ fetch engine info ─ */
    useEffect(() => {
        jobsApi.getEngine().then(({ engine, healthy }) => {
            setActiveEngine(engine);
            setEngineHealthy(healthy);
        }).catch(() => { /* ignore */ });
    }, []);

    useEffect(() => {
        if (activeEngine !== 'airflow') {
            setDispatchSummary(null);
            return;
        }
        fetchDispatchSummary();
        const interval = window.setInterval(fetchDispatchSummary, 30_000);
        return () => window.clearInterval(interval);
    }, [fetchDispatchSummary, activeEngine]);

    /* ─ engine actions ─ */
    const handleSwitchEngine = async () => {
        const target = activeEngine === 'quartz' ? 'airflow' : 'quartz';
        if (!confirm(`Are you sure you want to trigger a ZERO-DOWNTIME migration to ${target.toUpperCase()}?\n\nThis will migrate all active schedules and clean them up from the current engine.`)) return;

        try {
            setLoading(true);
            const res = await jobsApi.switchEngine(target);
            if (res.status === 'success' || res.status === 'partial') {
                alert(`Migration ${res.status}! Switched to ${target}. Migrated ${res.migration_report?.migrated || 0} schedules.`);
                setActiveEngine(target);
                fetchJobs();
                fetchDispatchSummary();
            } else {
                alert(`Migration returned status: ${res.status}. Check logs.`);
                fetchJobs();
            }
        } catch (err: any) {
            const data = err.response?.data;
            let errMsg = data?.error || data?.message || err.message;
            if (data?.issues?.length) {
                errMsg += '\n\nPre-flight Issues:\n• ' + data.issues.join('\n• ');
            }
            alert(errMsg);
            fetchJobs();
        }
    };

    const handleOpenAirflow = async () => {
        try {
            const res = await jobsApi.getAirflowUiUrl();
            if (res.available && res.dags_url) {
                window.open(res.dags_url, '_blank', 'noopener,noreferrer');
            } else {
                alert(res.message || 'Airflow UI is not available.');
            }
        } catch (err) {
            alert('Failed to get Airflow UI URL.');
        }
    };

    /* ─ sort ─ */
    const sortedJobs = [...jobs].sort((a, b) => {
        if (sortBy === 'priority') return b.priority - a.priority;
        if (sortBy === 'name') return a.schedule_name.localeCompare(b.schedule_name);
        return new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime();
    });

    /* ─ stats ─ */
    const stats = {
        total: jobs.length,
        running: jobs.filter(j => j.status === 'running').length,
        finished: jobs.filter(j => j.status === 'finished').length,
        failed: jobs.filter(j => j.status === 'failed').length,
        on_hold: jobs.filter(j => j.status === 'on_hold').length,
    };

    /* ─ context menu handler ─ */
    const handleContextMenu = (e: React.MouseEvent, job: ScheduledJob) => {
        e.preventDefault();
        setContextMenu({ x: e.clientX, y: e.clientY, job });
    };

    /* ─ action dispatcher ─ */
    const handleAction = (action: string, job: ScheduledJob) => {
        setSelectedJob(job);
        switch (action) {
            case 'view_info':
                setShowInfoModal(true);
                break;
            case 'view_history':
                setShowHistoryModal(true);
                break;
            case 'view_comments':
                setShowCommentModal(true);
                break;
            case 'view_logs':
                setShowInfoModal(true); // opens info modal on task instances tab
                break;
            case 'edit':
                // TODO: edit modal
                break;
            case 'delete':
                if (window.confirm(`Delete job "${job.schedule_name}"?`)) {
                    jobsApi.delete(job.id).then(() => fetchJobs());
                }
                break;
            default:
                // Job actions that need confirmation
                setActionModal({ action: action as JobAction, job });
                break;
        }
    };

    const handleConfirmAction = async () => {
        if (!actionModal) return;
        await jobsApi.performAction(actionModal.job.id, actionModal.action);
        fetchJobs();
    };

    const handleRowClick = (job: ScheduledJob) => {
        setSelectedJob(job);
        setShowInfoModal(true);
    };

    /* ─ render ─ */
    return (
        <div className="fade-in">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white shadow-glow-sm">
                            <Calendar size={20} />
                        </div>
                        Job Management
                    </h1>
                </div>
                <div className="flex items-center gap-3">
                    {/* Engine badge */}
                    <span className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold ${
                        activeEngine === 'airflow'
                            ? 'bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-300'
                            : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                    }`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${engineHealthy ? 'bg-green-500' : 'bg-red-500'}`} />
                        {activeEngine === 'airflow' ? 'Airflow' : 'Quartz'}
                    </span>
                    {user?.is_super_admin && (
                        <button onClick={handleSwitchEngine} className="btn-secondary px-4 py-2.5 text-sm" disabled={loading}>
                            Migrate to {activeEngine === 'quartz' ? 'Airflow' : 'Quartz'}
                        </button>
                    )}
                    {activeEngine === 'airflow' && (
                        <button onClick={handleOpenAirflow} className="btn-secondary px-4 py-2.5 text-sm text-sky-700 dark:text-sky-300 border-sky-300 dark:border-sky-700 hover:bg-sky-50 dark:hover:bg-sky-900/30">
                            Airflow DAGs
                        </button>
                    )}
                    <button onClick={() => { fetchJobs(); fetchDispatchSummary(); }} className="btn-secondary px-5 py-2.5 text-sm" disabled={loading}>
                        Refresh
                    </button>
                    <button onClick={() => setShowScheduleModal(true)} className="btn-primary px-5 py-2.5 text-sm">
                        New Schedule
                    </button>
                </div>
            </div>

            {/* Stats Row */}
            <div className="grid grid-cols-5 gap-4 mb-4">
                {[
                    { label: 'Total Jobs', value: stats.total, icon: <Layers size={20} />, color: 'from-primary-500 to-primary-600', textColor: 'text-primary-600 dark:text-primary-400' },
                    { label: 'Running', value: stats.running, icon: <Activity size={20} />, color: 'from-blue-500 to-blue-600', textColor: 'text-blue-600 dark:text-blue-400' },
                    { label: 'Finished', value: stats.finished, icon: <CheckCircle size={20} />, color: 'from-green-500 to-green-600', textColor: 'text-green-600 dark:text-green-400' },
                    { label: 'Failed', value: stats.failed, icon: <XCircle size={20} />, color: 'from-red-500 to-red-600', textColor: 'text-red-600 dark:text-red-400' },
                    { label: 'On Hold', value: stats.on_hold, icon: <PauseCircle size={20} />, color: 'from-amber-500 to-amber-600', textColor: 'text-amber-600 dark:text-amber-400' },
                ].map((stat) => (
                    <div key={stat.label} className="stat-card">
                        <div className={`stat-card-icon bg-gradient-to-br ${stat.color} text-white`}>
                            {stat.icon}
                        </div>
                        <div>
                            <p className="text-2xl font-bold text-gray-900 dark:text-white">{stat.value}</p>
                            <p className="text-xs font-medium text-gray-500 dark:text-gray-400">{stat.label}</p>
                        </div>
                    </div>
                ))}
            </div>

            {/* Dispatch Queue Status — only for Airflow engine */}
            {activeEngine === 'airflow' && dispatchSummary && (
                <div className="glass-card p-4 mb-4">
                    <div className="flex items-center gap-2 mb-3">
                        <Zap size={16} className="text-violet-500" />
                        <span className="text-sm font-semibold text-gray-700 dark:text-gray-200">Dispatch Queue</span>
                        <span className="text-[10px] text-gray-400 ml-auto">Auto-refreshes every 30s</span>
                    </div>
                    <div className="grid grid-cols-4 gap-3">
                        <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-amber-50 dark:bg-amber-900/15 border border-amber-200/50 dark:border-amber-800/30">
                            <Timer size={16} className="text-amber-500 shrink-0" />
                            <div>
                                <p className="text-lg font-bold text-gray-900 dark:text-white">{dispatchSummary.total_waiting}</p>
                                <p className="text-[10px] font-medium text-gray-500 dark:text-gray-400">Queued</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-blue-50 dark:bg-blue-900/15 border border-blue-200/50 dark:border-blue-800/30">
                            <Activity size={16} className="text-blue-500 shrink-0" />
                            <div>
                                <p className="text-lg font-bold text-gray-900 dark:text-white">{dispatchSummary.total_active}</p>
                                <p className="text-[10px] font-medium text-gray-500 dark:text-gray-400">Active</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-violet-50 dark:bg-violet-900/15 border border-violet-200/50 dark:border-violet-800/30">
                            <Clock size={16} className="text-violet-500 shrink-0" />
                            <div>
                                <p className="text-lg font-bold text-gray-900 dark:text-white">
                                    {dispatchSummary.avg_wait_seconds != null
                                        ? `${Math.round(dispatchSummary.avg_wait_seconds)}s`
                                        : '—'}
                                </p>
                                <p className="text-[10px] font-medium text-gray-500 dark:text-gray-400">Avg Wait</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-red-50 dark:bg-red-900/15 border border-red-200/50 dark:border-red-800/30">
                            <AlertTriangle size={16} className="text-red-500 shrink-0" />
                            <div>
                                <p className="text-lg font-bold text-gray-900 dark:text-white">{dispatchSummary.long_running_count}</p>
                                <p className="text-[10px] font-medium text-gray-500 dark:text-gray-400">Long Running</p>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Filter Bar */}
            <div className="glass-card p-4 mb-4 flex items-center gap-4 flex-wrap">
                {/* Search */}
                <div className="relative flex-1 min-w-[240px]">
                    <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Search jobs by name, DAG ID, or department..."
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                        className="input input-icon"
                    />
                </div>

                {/* Status chips */}
                <div className="flex gap-2">
                    {[
                        { key: null, label: 'All' },
                        { key: 'running', label: 'Running' },
                        { key: 'finished', label: 'Finished' },
                        { key: 'failed', label: 'Failed' },
                        { key: 'on_hold', label: 'On Hold' },
                    ].map(chip => (
                        <button
                            key={chip.key || 'all'}
                            onClick={() => setStatusFilter(chip.key)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                                statusFilter === chip.key
                                    ? 'bg-primary-600 text-white shadow-glow-sm'
                                    : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                            }`}
                        >
                            {chip.label}
                        </button>
                    ))}
                </div>

                {/* Sort */}
                <div className="flex items-center gap-2">
                    <ArrowUpDown size={14} className="text-gray-400" />
                    <select
                        value={sortBy}
                        onChange={e => setSortBy(e.target.value as typeof sortBy)}
                        className="input py-1.5 text-xs w-32"
                    >
                        <option value="priority">Priority</option>
                        <option value="name">Name</option>
                        <option value="updated">Last Updated</option>
                    </select>
                </div>
            </div>

            {/* Job Table */}
            <div className="glass-card overflow-hidden">
                {loading ? (
                    <div className="p-12 text-center">
                        <RefreshCw size={32} className="mx-auto mb-3 animate-spin text-primary-500" />
                        <p className="text-gray-500 dark:text-gray-400">Loading jobs...</p>
                    </div>
                ) : sortedJobs.length === 0 ? (
                    <div className="p-12 text-center">
                        <Calendar size={48} className="mx-auto mb-4 text-gray-300 dark:text-gray-600" />
                        <h3 className="text-lg font-semibold text-gray-700 dark:text-gray-300 mb-2">No Jobs Found</h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                            {searchQuery || statusFilter ? 'Try adjusting your filters.' : 'Create your first scheduled job to get started.'}
                        </p>
                        {!searchQuery && !statusFilter && (
                            <button onClick={() => setShowScheduleModal(true)} className="btn-primary">
                            New Schedule
                            </button>
                        )}
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="bg-gray-50/80 dark:bg-gray-700/30 border-b border-gray-200 dark:border-gray-700">
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300 w-[100px]">Status</th>
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Schedule Name</th>
                                    {activeEngine === 'airflow' && (
                                        <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">DAG ID</th>
                                    )}
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Department</th>
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300 w-[90px]">Priority</th>
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Machine</th>
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Last Run</th>
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Next Run</th>
                                    <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Cron</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100 dark:divide-gray-700/50">
                                {sortedJobs.map((job, idx) => {
                                    const sc = STATUS_CONFIG[job.status] || STATUS_CONFIG.running;
                                    const pb = getPriorityBadge(job.priority);
                                    return (
                                        <tr
                                            key={job.id}
                                            className="hover:bg-primary-50/40 dark:hover:bg-primary-900/10 cursor-pointer transition-colors group"
                                            style={{ animationDelay: `${idx * 30}ms` }}
                                            onClick={() => handleRowClick(job)}
                                            onContextMenu={e => handleContextMenu(e, job)}
                                        >
                                            <td className="px-4 py-3">
                                                <div className="flex items-center gap-2">
                                                    <div className={sc.dotClass} />
                                                    <span className={`badge ${sc.badgeClass} text-[11px]`}>{sc.label}</span>
                                                </div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <div>
                                                    <p className="font-semibold text-gray-900 dark:text-white group-hover:text-primary-700 dark:group-hover:text-primary-300 transition-colors">
                                                        {job.schedule_name}
                                                    </p>
                                                    {job.report_name && (
                                                        <p className="text-xs text-gray-400">{job.report_name}</p>
                                                    )}
                                                </div>
                                            </td>
                                            {activeEngine === 'airflow' && (
                                                <td className="px-4 py-3">
                                                    <code className="text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded">
                                                        {job.dag_id}
                                                    </code>
                                                </td>
                                            )}
                                            <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                                                <div className="flex items-center gap-1.5">
                                                    <Tag size={12} className="text-gray-400" />
                                                    {job.department}
                                                </div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className={`badge ${pb.cls} text-[11px]`}>{pb.label}</span>
                                            </td>
                                            <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                                                <div className="flex items-center gap-1.5">
                                                    <Server size={12} className="text-gray-400" />
                                                    {job.machine_name || '—'}
                                                </div>
                                            </td>
                                            <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                                                <div className="flex items-center gap-1.5">
                                                    <Clock size={12} className="text-gray-400" />
                                                    {formatDate(job.last_run)}
                                                </div>
                                            </td>
                                            <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                                                {formatDate(job.next_run)}
                                            </td>
                                            <td className="px-4 py-3">
                                                {job.cron_expression ? (
                                                    <code className="text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded">
                                                        {job.cron_expression}
                                                    </code>
                                                ) : '—'}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Context Menu */}
            <JobContextMenu
                isOpen={!!contextMenu}
                position={contextMenu ? { x: contextMenu.x, y: contextMenu.y } : { x: 0, y: 0 }}
                job={contextMenu?.job || null}
                onClose={() => setContextMenu(null)}
                onAction={handleAction}
                engine={activeEngine}
            />

            {/* Modals */}
            <JobInfoModal
                isOpen={showInfoModal}
                onClose={() => setShowInfoModal(false)}
                job={selectedJob}
                onRefresh={fetchJobs}
            />
            <JobHistoryModal
                isOpen={showHistoryModal}
                onClose={() => setShowHistoryModal(false)}
                job={selectedJob}
            />
            <JobCommentModal
                isOpen={showCommentModal}
                onClose={() => setShowCommentModal(false)}
                job={selectedJob}
            />
            <JobActionModal
                isOpen={!!actionModal}
                onClose={() => setActionModal(null)}
                action={actionModal?.action || null}
                jobName={actionModal?.job.schedule_name || ''}
                onConfirm={handleConfirmAction}
            />
            <ScheduleModal
                isOpen={showScheduleModal}
                onClose={() => {
                    setShowScheduleModal(false);
                    fetchJobs();
                }}
                reportName=""
                reportUri=""
            />
        </div>
    );
}
