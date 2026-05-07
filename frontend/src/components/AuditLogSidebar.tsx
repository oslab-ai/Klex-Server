import { useState, useEffect } from 'react';
import { reportAuditApi } from '../api';
import type { ReportAuditEntry, AuditPerformanceStats } from '../types';

interface AuditLogSidebarProps {
    isOpen: boolean;
    onClose: () => void;
}

function formatMs(ms: number | null | undefined): string {
    if (ms == null) return '—';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
}

function formatBytes(bytes: number | null | undefined): string {
    if (bytes == null) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function formatTimestamp(ts: string): string {
    try {
        const d = new Date(ts);
        return d.toLocaleString(undefined, {
            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
    } catch {
        return ts;
    }
}

type TabType = 'logs' | 'stats';

export default function AuditLogSidebar({ isOpen, onClose }: AuditLogSidebarProps) {
    const [tab, setTab] = useState<TabType>('logs');
    const [logs, setLogs] = useState<ReportAuditEntry[]>([]);
    const [stats, setStats] = useState<AuditPerformanceStats | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [expandedLog, setExpandedLog] = useState<number | null>(null);

    // Filter state
    const [filterStatus, setFilterStatus] = useState<string>('');

    useEffect(() => {
        if (isOpen) {
            if (tab === 'logs') loadLogs();
            else loadStats();
        }
    }, [isOpen, tab, filterStatus]);

    const loadLogs = async () => {
        setLoading(true);
        setError(null);
        try {
            const params: Record<string, string> = {};
            if (filterStatus) params.status = filterStatus;
            const data = await reportAuditApi.getLogs(params);
            setLogs(data);
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string } }; message?: string };
            setError(e.response?.data?.error || e.message || 'Failed to load audit logs');
        } finally {
            setLoading(false);
        }
    };

    const loadStats = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await reportAuditApi.getStats();
            setStats(data);
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string } }; message?: string };
            setError(e.response?.data?.error || e.message || 'Failed to load audit stats');
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <>
            {/* Backdrop */}
            <div
                className="fixed inset-0 bg-black/30 backdrop-blur-sm z-40 transition-opacity"
                onClick={onClose}
            />

            {/* Sidebar */}
            <div className="fixed top-0 right-0 h-full w-[480px] max-w-full bg-white dark:bg-gray-800 shadow-2xl z-50 flex flex-col slide-in-right">
                {/* Header */}
                <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200 dark:border-gray-700">
                    <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                            <svg className="w-4 h-4 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                            </svg>
                        </div>
                        <h2 className="text-lg font-semibold">Audit & Logging</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Tabs */}
                <div className="flex border-b border-gray-200 dark:border-gray-700 px-5">
                    <button
                        onClick={() => setTab('logs')}
                        className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                            tab === 'logs'
                                ? 'border-amber-500 text-amber-700 dark:text-amber-300'
                                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                        }`}
                    >
                        📋 Execution Logs
                    </button>
                    <button
                        onClick={() => setTab('stats')}
                        className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                            tab === 'stats'
                                ? 'border-amber-500 text-amber-700 dark:text-amber-300'
                                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                        }`}
                    >
                        📊 Performance Stats
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-5 space-y-3">
                    {loading && (
                        <div className="flex items-center justify-center py-16">
                            <div className="text-center">
                                <div className="w-10 h-10 border-4 border-amber-500 border-t-transparent rounded-full animate-spin mx-auto" />
                                <p className="mt-3 text-sm text-gray-500">
                                    {tab === 'logs' ? 'Loading audit logs...' : 'Loading stats...'}
                                </p>
                            </div>
                        </div>
                    )}

                    {error && (
                        <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
                            {error}
                        </div>
                    )}

                    {/* === Logs Tab === */}
                    {tab === 'logs' && !loading && !error && (
                        <>
                            {/* Filter bar */}
                            <div className="flex items-center gap-2 pb-2">
                                <select
                                    value={filterStatus}
                                    onChange={(e) => setFilterStatus(e.target.value)}
                                    className="text-xs px-2 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 focus:outline-none focus:ring-1 focus:ring-amber-500"
                                >
                                    <option value="">All Statuses</option>
                                    <option value="SUCCESS">Success</option>
                                    <option value="ERROR">Failure</option>
                                </select>
                                <span className="text-xs text-gray-400 ml-auto">{logs.length} entries</span>
                            </div>

                            {logs.length === 0 ? (
                                <div className="text-center py-12 text-gray-500">
                                    <p className="text-3xl mb-2">📭</p>
                                    <p className="text-sm">No audit logs found</p>
                                </div>
                            ) : (
                                <div className="space-y-2">
                                    {logs.map((log) => (
                                        <div
                                            key={log.id}
                                            className={`rounded-xl border transition-all cursor-pointer ${
                                                log.status === 'ERROR'
                                                    ? 'border-red-200 dark:border-red-800 bg-red-50/50 dark:bg-red-900/10'
                                                    : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800/50'
                                            } ${expandedLog === log.id ? 'shadow-md' : 'hover:shadow-sm'}`}
                                            onClick={() => setExpandedLog(expandedLog === log.id ? null : log.id)}
                                        >
                                            {/* Log summary row */}
                                            <div className="flex items-center gap-3 px-4 py-3">
                                                <div className={`w-2 h-2 rounded-full flex-shrink-0 ${
                                                    log.status === 'SUCCESS' ? 'bg-green-500' : 'bg-red-500'
                                                }`} />
                                                <div className="flex-1 min-w-0">
                                                    <p className="text-sm font-medium truncate">
                                                        {log.reportName || log.reportPath?.split('/').pop() || 'Unknown'}
                                                    </p>
                                                    <div className="flex items-center gap-2 mt-0.5">
                                                        <span className="text-xs text-gray-400">{formatTimestamp(log.timestamp)}</span>
                                                        <span className={`text-xs px-1.5 py-0.5 rounded font-mono ${
                                                            log.outputFormat === 'PDF' ? 'bg-red-100 dark:bg-red-900/30 text-red-600'
                                                            : log.outputFormat === 'XLSX' ? 'bg-green-100 dark:bg-green-900/30 text-green-600'
                                                            : 'bg-blue-100 dark:bg-blue-900/30 text-blue-600'
                                                        }`}>
                                                            {log.outputFormat}
                                                        </span>
                                                        <span className="text-xs text-gray-400">
                                                            {log.dataSourceType}
                                                        </span>
                                                    </div>
                                                </div>
                                                <div className="text-right flex-shrink-0">
                                                    <p className="text-xs font-mono text-gray-500">
                                                        {formatMs(log.totalExecutionTimeMs)}
                                                    </p>
                                                    {log.reportPages != null && (
                                                        <p className="text-xs text-gray-400">{log.reportPages} pg</p>
                                                    )}
                                                </div>
                                                <svg className={`w-4 h-4 text-gray-400 flex-shrink-0 transition-transform ${expandedLog === log.id ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                                </svg>
                                            </div>

                                            {/* Expanded details */}
                                            {expandedLog === log.id && (
                                                <div className="px-4 pb-4 pt-1 border-t border-gray-100 dark:border-gray-700 space-y-3">
                                                    {/* Performance breakdown */}
                                                    <div>
                                                        <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">⏱ Performance</p>
                                                        <div className="grid grid-cols-3 gap-2">
                                                            <MetricCard label="Compile" value={formatMs(log.compileTimeMs)} />
                                                            <MetricCard label="Fill" value={formatMs(log.fillTimeMs)} />
                                                            <MetricCard label="Export" value={formatMs(log.exportTimeMs)} />
                                                        </div>
                                                    </div>

                                                    {/* Design metadata */}
                                                    {(log.fieldCount != null || log.parameterCount != null) && (
                                                        <div>
                                                            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">📐 Design</p>
                                                            <div className="grid grid-cols-2 gap-2 text-xs">
                                                                {log.fieldCount != null && (
                                                                    <div className="flex justify-between p-2 bg-gray-50 dark:bg-gray-900/40 rounded-lg">
                                                                        <span className="text-gray-500">Fields</span>
                                                                        <span className="font-medium">{log.fieldCount}</span>
                                                                    </div>
                                                                )}
                                                                {log.parameterCount != null && (
                                                                    <div className="flex justify-between p-2 bg-gray-50 dark:bg-gray-900/40 rounded-lg">
                                                                        <span className="text-gray-500">Parameters</span>
                                                                        <span className="font-medium">{log.parameterCount}</span>
                                                                    </div>
                                                                )}
                                                                {log.orientation && (
                                                                    <div className="flex justify-between p-2 bg-gray-50 dark:bg-gray-900/40 rounded-lg">
                                                                        <span className="text-gray-500">Orientation</span>
                                                                        <span className="font-medium">{log.orientation}</span>
                                                                    </div>
                                                                )}
                                                                {log.pageWidth != null && log.pageHeight != null && (
                                                                    <div className="flex justify-between p-2 bg-gray-50 dark:bg-gray-900/40 rounded-lg">
                                                                        <span className="text-gray-500">Page</span>
                                                                        <span className="font-medium">{log.pageWidth}×{log.pageHeight}</span>
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    )}

                                                    {/* Output */}
                                                    <div className="flex items-center justify-between text-xs text-gray-500">
                                                        <span>Output: {formatBytes(log.outputFileSizeBytes)}</span>
                                                        <span>Source: {log.sourceType}</span>
                                                    </div>

                                                    {/* Error message */}
                                                    {log.errorMessage && (
                                                        <div className="p-2.5 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-xs font-mono break-all">
                                                            {log.errorMessage}
                                                        </div>
                                                    )}

                                                    {/* User & ID */}
                                                    <div className="flex items-center justify-between text-xs text-gray-400">
                                                        <span>User: {log.userId || '—'}</span>
                                                        <span>ID: #{log.id}</span>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                    {/* === Stats Tab === */}
                    {tab === 'stats' && !loading && !error && stats && (
                        <>
                            {/* Summary cards */}
                            <div className="p-4 rounded-xl bg-gradient-to-r from-amber-50 to-orange-50 dark:from-amber-900/20 dark:to-orange-900/10 border border-amber-200 dark:border-amber-800">
                                <p className="text-xs font-medium text-amber-600 dark:text-amber-400 uppercase tracking-wider">Overview</p>
                                <div className="grid grid-cols-3 gap-4 mt-3">
                                    <div className="text-center">
                                        <p className="text-2xl font-bold">{stats.totalReports}</p>
                                        <p className="text-xs text-gray-500">Total Runs</p>
                                    </div>
                                    <div className="text-center">
                                        <p className="text-2xl font-bold text-green-600">{stats.successCount}</p>
                                        <p className="text-xs text-gray-500">Success</p>
                                    </div>
                                    <div className="text-center">
                                        <p className="text-2xl font-bold text-red-600">{stats.errorCount}</p>
                                        <p className="text-xs text-gray-500">Failures</p>
                                    </div>
                                </div>
                            </div>

                            {/* Error rate */}
                            <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-700">
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-sm font-medium">Failure Rate</span>
                                    <span className={`text-sm font-bold ${stats.errorRate > 0.1 ? 'text-red-600' : 'text-green-600'}`}>
                                        {(stats.errorRate * 100).toFixed(1)}%
                                    </span>
                                </div>
                                <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                                    <div
                                        className={`h-2 rounded-full transition-all ${stats.errorRate > 0.1 ? 'bg-red-500' : 'bg-green-500'}`}
                                        style={{ width: `${Math.min(stats.errorRate * 100, 100)}%` }}
                                    />
                                </div>
                            </div>

                            {/* Average timings */}
                            <div className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
                                <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-700">
                                    <h3 className="text-sm font-semibold flex items-center gap-2">
                                        <span>⏱</span> Average Timings
                                    </h3>
                                </div>
                                <div className="p-3 grid grid-cols-2 gap-2">
                                    <MetricCard label="Total" value={formatMs(stats.avgTotalExecutionTimeMs)} />
                                    <MetricCard label="Compile" value={formatMs(stats.avgCompileTimeMs)} />
                                    <MetricCard label="Fill" value={formatMs(stats.avgFillTimeMs)} />
                                    <MetricCard label="Export" value={formatMs(stats.avgExportTimeMs)} />
                                </div>
                            </div>

                            {/* Data volume */}
                            <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-700">
                                <div className="flex items-center justify-between">
                                    <span className="text-sm font-medium">💾 Total Data Volume</span>
                                    <span className="text-sm font-bold text-blue-600">{formatBytes(stats.totalDataVolumeBytes)}</span>
                                </div>
                            </div>

                            {/* Slowest reports */}
                            {stats.slowestReports && stats.slowestReports.length > 0 && (
                                <div className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
                                    <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-700">
                                        <h3 className="text-sm font-semibold flex items-center gap-2">
                                            <span>🐌</span> Slowest Reports
                                        </h3>
                                    </div>
                                    <div className="divide-y divide-gray-100 dark:divide-gray-700">
                                        {stats.slowestReports.map((r, i) => (
                                            <div key={r.id} className="px-4 py-2.5 flex items-center gap-3">
                                                <span className="text-xs font-bold text-gray-400 w-5">#{i + 1}</span>
                                                <div className="flex-1 min-w-0">
                                                    <p className="text-sm font-medium truncate">{r.reportName || r.reportPath?.split('/').pop()}</p>
                                                    <p className="text-xs text-gray-400">{formatTimestamp(r.timestamp)}</p>
                                                </div>
                                                <span className="text-sm font-mono font-medium text-red-600">
                                                    {formatMs(r.totalExecutionTimeMs)}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </>
                    )}

                    {tab === 'stats' && !loading && !error && !stats && (
                        <div className="text-center py-12 text-gray-500">
                            <p className="text-3xl mb-2">📊</p>
                            <p className="text-sm">No performance data available</p>
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}

function MetricCard({ label, value }: { label: string; value: string }) {
    return (
        <div className="p-2.5 rounded-lg bg-gray-50 dark:bg-gray-900/40 text-center">
            <p className="text-xs text-gray-500">{label}</p>
            <p className="text-sm font-mono font-semibold mt-0.5">{value}</p>
        </div>
    );
}
