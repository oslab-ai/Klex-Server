import { useState, useEffect } from 'react';
import { X, Calendar, Clock, TrendingUp, RefreshCw } from 'lucide-react';
import Modal from './Modal';
import { jobsApi } from '../api';
import type { ScheduledJob, DagRun } from '../types';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    job: ScheduledJob | null;
}

const STATE_STYLES: Record<string, string> = {
    success: 'badge-success',
    failed: 'badge-danger',
    running: 'badge-primary',
    queued: 'badge-warning',
};

export default function JobHistoryModal({ isOpen, onClose, job }: Props) {
    const [runs, setRuns] = useState<DagRun[]>([]);
    const [totalEntries, setTotalEntries] = useState(0);
    const [loading, setLoading] = useState(false);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    useEffect(() => {
        if (isOpen && job) {
            fetchHistory();
        }
    }, [isOpen, job?.id]);

    const fetchHistory = async () => {
        if (!job) return;
        setLoading(true);
        try {
            const params: Record<string, string | number> = { limit: 50 };
            if (startDate) params.start_date = startDate;
            if (endDate) params.end_date = endDate;
            const result = await jobsApi.getHistory(job.id, params as any);
            setRuns(result.runs);
            setTotalEntries(result.total_entries);
        } catch (err) {
            console.error('Failed to fetch history:', err);
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (d: string | null) => {
        if (!d) return '—';
        return new Date(d).toLocaleString();
    };

    const getDuration = (start: string | null, end: string | null) => {
        if (!start || !end) return '—';
        const ms = new Date(end).getTime() - new Date(start).getTime();
        const seconds = Math.floor(ms / 1000);
        if (seconds < 60) return `${seconds}s`;
        const minutes = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${minutes}m ${secs}s`;
    };

    if (!job) return null;

    // Calculate max duration for the bar chart
    const durations = runs.map(r => {
        if (!r.start_date || !r.end_date) return 0;
        return (new Date(r.end_date).getTime() - new Date(r.start_date).getTime()) / 1000;
    });
    const maxDuration = Math.max(...durations, 1);

    return (
        <Modal isOpen={isOpen} onClose={onClose} maxWidth="max-w-4xl">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h2 className="text-xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
                        <History size={20} className="text-primary-600" />
                        Execution History
                    </h2>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        {job.schedule_name} · {totalEntries} total runs
                    </p>
                </div>
                <button onClick={onClose} className="btn-ghost p-2 rounded-lg"><X size={16} /></button>
            </div>

            {/* Filters */}
            <div className="flex items-center gap-4 mb-4">
                <div className="flex items-center gap-2">
                    <Calendar size={14} className="text-gray-400" />
                    <input
                        type="date"
                        value={startDate}
                        onChange={e => setStartDate(e.target.value)}
                        className="input py-1.5 text-xs w-36"
                        placeholder="Start Date"
                    />
                </div>
                <span className="text-gray-400">to</span>
                <div className="flex items-center gap-2">
                    <Calendar size={14} className="text-gray-400" />
                    <input
                        type="date"
                        value={endDate}
                        onChange={e => setEndDate(e.target.value)}
                        className="input py-1.5 text-xs w-36"
                        placeholder="End Date"
                    />
                </div>
                <button onClick={fetchHistory} className="btn-secondary py-1.5 text-xs" disabled={loading}>
                    <RefreshCw size={12} className={`mr-1 ${loading ? 'animate-spin' : ''}`} /> Filter
                </button>
            </div>

            {/* Runtime Trend */}
            {durations.filter(d => d > 0).length > 1 && (
                <div className="mb-4 p-4 rounded-xl bg-gray-50 dark:bg-gray-700/30">
                    <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1">
                        <TrendingUp size={12} /> Runtime Trend
                    </h4>
                    <div className="flex items-end gap-1 h-16">
                        {durations.slice(0, 30).reverse().map((d, i) => {
                            const height = d > 0 ? Math.max((d / maxDuration) * 100, 4) : 0;
                            const run = runs[runs.length - 1 - i];
                            const state = run?.state || 'unknown';
                            const barColor = state === 'success'
                                ? 'bg-green-400 dark:bg-green-500'
                                : state === 'failed'
                                    ? 'bg-red-400 dark:bg-red-500'
                                    : 'bg-primary-400 dark:bg-primary-500';
                            return (
                                <div
                                    key={i}
                                    className={`flex-1 rounded-t-sm ${barColor} transition-all hover:opacity-80`}
                                    style={{ height: `${height}%` }}
                                    title={`${getDuration(run?.start_date, run?.end_date)} - ${state}`}
                                />
                            );
                        })}
                    </div>
                </div>
            )}

            {/* History Table */}
            <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700">
                {loading ? (
                    <div className="p-8 text-center text-gray-500">
                        <RefreshCw size={24} className="mx-auto mb-2 animate-spin" />
                        Loading history...
                    </div>
                ) : runs.length === 0 ? (
                    <div className="p-8 text-center text-gray-500 dark:text-gray-400">
                        <Clock size={32} className="mx-auto mb-2 opacity-40" />
                        <p>No execution history found</p>
                    </div>
                ) : (
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="bg-gray-50 dark:bg-gray-700/50">
                                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Run ID</th>
                                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">State</th>
                                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Start Time</th>
                                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">End Time</th>
                                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Duration</th>
                                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Trigger</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                            {runs.map((run, i) => (
                                <tr key={i} className="hover:bg-gray-50 dark:hover:bg-gray-700/30">
                                    <td className="px-4 py-3 font-mono text-xs truncate max-w-[200px]" title={run.dag_run_id}>
                                        {run.dag_run_id}
                                    </td>
                                    <td className="px-4 py-3">
                                        <span className={`badge ${STATE_STYLES[run.state] || 'badge-primary'}`}>{run.state}</span>
                                    </td>
                                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{formatDate(run.start_date)}</td>
                                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{formatDate(run.end_date)}</td>
                                    <td className="px-4 py-3 font-medium text-gray-700 dark:text-gray-200">
                                        {getDuration(run.start_date, run.end_date)}
                                    </td>
                                    <td className="px-4 py-3 text-gray-500">
                                        {run.external_trigger ? 'Manual' : 'Scheduled'}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Footer */}
            <div className="flex justify-end mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
                <button onClick={onClose} className="btn-primary">Close</button>
            </div>
        </Modal>
    );
}

function History({ size, className }: { size: number; className?: string }) {
    return <Clock size={size} className={className} />;
}
