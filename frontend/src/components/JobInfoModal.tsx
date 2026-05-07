import { useState, useEffect } from 'react';
import {
    X, RefreshCw, Clock, Server, Hash, Layers,
    Calendar, Timer, Activity, Tag, FileText,
    Settings, AlertCircle,
} from 'lucide-react';
import Modal from './Modal';
import { jobsApi } from '../api';
import type { ScheduledJob, TaskInstance } from '../types';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    job: ScheduledJob | null;
    onRefresh?: () => void;
}

const STATUS_STYLES: Record<string, string> = {
    running: 'badge-primary',
    finished: 'badge-success',
    failed: 'badge-danger',
    on_hold: 'badge-warning',
};

const TABS = ['Overview', 'Task Instances', 'Configuration', 'Documentation'] as const;
type Tab = (typeof TABS)[number];

export default function JobInfoModal({ isOpen, onClose, job, onRefresh: _onRefresh }: Props) {
    const [activeTab, setActiveTab] = useState<Tab>('Overview');
    const [detailJob, setDetailJob] = useState<ScheduledJob | null>(null);
    const [taskInstances, setTaskInstances] = useState<TaskInstance[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isOpen && job) {
            fetchDetail();
        }
    }, [isOpen, job?.id]);

    const fetchDetail = async () => {
        if (!job) return;
        setLoading(true);
        try {
            const detail = await jobsApi.get(job.id);
            setDetailJob(detail);
            // Fetch task instances
            const logsData = await jobsApi.getLogs(job.id);
            setTaskInstances(logsData.task_instances || []);
        } catch (err) {
            console.error('Failed to fetch job details:', err);
            setDetailJob(job);
        } finally {
            setLoading(false);
        }
    };

    if (!job) return null;
    const displayJob = detailJob || job;

    const formatDate = (d: string | null) => {
        if (!d) return '—';
        return new Date(d).toLocaleString();
    };

    const detailItems = [
        { icon: <Hash size={14} />, label: 'DAG ID', value: displayJob.dag_id },
        { icon: <Layers size={14} />, label: 'Department', value: displayJob.department },
        { icon: <Activity size={14} />, label: 'Priority', value: displayJob.priority.toString() },
        { icon: <Calendar size={14} />, label: 'Cron Expression', value: displayJob.cron_expression || '—' },
        { icon: <Clock size={14} />, label: 'Frequency', value: displayJob.frequency_label || '—' },
        { icon: <Server size={14} />, label: 'Machine', value: displayJob.machine_name || '—' },
        { icon: <Timer size={14} />, label: 'Est. Runtime', value: displayJob.estimated_runtime_min ? `${displayJob.estimated_runtime_min} min` : '—' },
        { icon: <Timer size={14} />, label: 'Max Runtime', value: displayJob.max_runtime_min ? `${displayJob.max_runtime_min} min` : '—' },
        { icon: <Calendar size={14} />, label: 'Next Run', value: formatDate(displayJob.next_run) },
        { icon: <Calendar size={14} />, label: 'Last Run', value: formatDate(displayJob.last_run) },
        { icon: <Tag size={14} />, label: 'Exit Code', value: displayJob.exit_code || '—' },
        { icon: <FileText size={14} />, label: 'Term Description', value: displayJob.termination_description || '—' },
    ];

    const TI_STATE_STYLES: Record<string, string> = {
        success: 'badge-success',
        failed: 'badge-danger',
        running: 'badge-primary',
        queued: 'badge-warning',
        up_for_retry: 'badge-warning',
        skipped: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} maxWidth="max-w-4xl">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <div>
                    <div className="flex items-center gap-3">
                        <h2 className="text-xl font-bold text-gray-900 dark:text-white">{displayJob.schedule_name}</h2>
                        <span className={`badge ${STATUS_STYLES[displayJob.status] || 'badge-primary'}`}>
                            {displayJob.status === 'on_hold' ? 'On Hold' : displayJob.status.charAt(0).toUpperCase() + displayJob.status.slice(1)}
                        </span>
                    </div>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        {displayJob.report_name && <>Report: {displayJob.report_name} · </>}
                        Created by {displayJob.created_by_name || 'Unknown'} · {formatDate(displayJob.created_at)}
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <button onClick={fetchDetail} className="btn-ghost p-2 rounded-lg" title="Refresh">
                        <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
                    </button>
                    <button onClick={onClose} className="btn-ghost p-2 rounded-lg">
                        <X size={16} />
                    </button>
                </div>
            </div>

            {/* Airflow status banner */}
            {displayJob.airflow?.error && (
                <div className="mb-4 p-3 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 flex items-center gap-2 text-sm text-amber-700 dark:text-amber-300">
                    <AlertCircle size={16} />
                    <span>Airflow sync: {displayJob.airflow.error}</span>
                </div>
            )}

            {/* Tabs */}
            <div className="flex gap-1 mb-6 p-1 rounded-xl bg-gray-100 dark:bg-gray-700/50">
                {TABS.map(tab => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={`flex-1 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                            activeTab === tab
                                ? 'bg-white dark:bg-gray-800 text-gray-900 dark:text-white shadow-sm'
                                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                        }`}
                    >
                        {tab}
                    </button>
                ))}
            </div>

            {/* Tab Content */}
            <div className="min-h-[300px]">
                {activeTab === 'Overview' && (
                    <div className="grid grid-cols-2 gap-3">
                        {detailItems.map((item, i) => (
                            <div key={i} className="flex items-start gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-700/30">
                                <div className="text-gray-400 dark:text-gray-500 mt-0.5">{item.icon}</div>
                                <div>
                                    <p className="text-xs font-medium text-gray-500 dark:text-gray-400">{item.label}</p>
                                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 break-all">{item.value}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {activeTab === 'Task Instances' && (
                    <div>
                        {taskInstances.length === 0 ? (
                            <div className="text-center py-12 text-gray-500 dark:text-gray-400">
                                <Layers size={40} className="mx-auto mb-3 opacity-40" />
                                <p>No task instances found</p>
                            </div>
                        ) : (
                            <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700">
                                <table className="w-full text-sm">
                                    <thead>
                                        <tr className="bg-gray-50 dark:bg-gray-700/50">
                                            <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Task ID</th>
                                            <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">State</th>
                                            <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Operator</th>
                                            <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Duration</th>
                                            <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-300">Try #</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                                        {taskInstances.map((ti, i) => (
                                            <tr key={i} className="hover:bg-gray-50 dark:hover:bg-gray-700/30">
                                                <td className="px-4 py-3 font-mono text-xs">{ti.task_id}</td>
                                                <td className="px-4 py-3">
                                                    <span className={`badge ${TI_STATE_STYLES[ti.state] || 'badge-primary'}`}>{ti.state}</span>
                                                </td>
                                                <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{ti.operator}</td>
                                                <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{ti.duration ? `${ti.duration.toFixed(1)}s` : '—'}</td>
                                                <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{ti.try_number}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'Configuration' && (
                    <div className="space-y-4">
                        <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-700/30">
                            <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-200 mb-3 flex items-center gap-2">
                                <Settings size={14} /> Airflow Configuration
                            </h4>
                            {displayJob.airflow && !displayJob.airflow.error ? (
                                <div className="grid grid-cols-2 gap-3 text-sm">
                                    <div>
                                        <span className="text-gray-500">Schedule Interval:</span>
                                        <span className="ml-2 font-medium">{typeof displayJob.airflow.schedule_interval === 'object' ? JSON.stringify(displayJob.airflow.schedule_interval) : displayJob.airflow.schedule_interval || '—'}</span>
                                    </div>
                                    <div>
                                        <span className="text-gray-500">Paused:</span>
                                        <span className="ml-2 font-medium">{displayJob.airflow.is_paused ? 'Yes' : 'No'}</span>
                                    </div>
                                    <div>
                                        <span className="text-gray-500">Active:</span>
                                        <span className="ml-2 font-medium">{displayJob.airflow.is_active ? 'Yes' : 'No'}</span>
                                    </div>
                                    <div>
                                        <span className="text-gray-500">Owners:</span>
                                        <span className="ml-2 font-medium">{displayJob.airflow.owners?.join(', ') || '—'}</span>
                                    </div>
                                    {displayJob.airflow.tags && displayJob.airflow.tags.length > 0 && (
                                        <div className="col-span-2">
                                            <span className="text-gray-500">Tags:</span>
                                            <span className="ml-2 font-medium">{displayJob.airflow.tags.join(', ')}</span>
                                        </div>
                                    )}
                                    {displayJob.airflow.description && (
                                        <div className="col-span-2">
                                            <span className="text-gray-500">Description:</span>
                                            <p className="mt-1 font-medium">{displayJob.airflow.description}</p>
                                        </div>
                                    )}
                                </div>
                            ) : (
                                <p className="text-gray-500 text-sm">Airflow configuration not available</p>
                            )}
                        </div>
                    </div>
                )}

                {activeTab === 'Documentation' && (
                    <div className="text-center py-12 text-gray-500 dark:text-gray-400">
                        <FileText size={40} className="mx-auto mb-3 opacity-40" />
                        <p className="font-medium">No documentation available</p>
                        <p className="text-sm mt-1">Documentation can be added to the Airflow DAG definition</p>
                    </div>
                )}
            </div>

            {/* Footer */}
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
                <button onClick={fetchDetail} className="btn-secondary" disabled={loading}>
                    <RefreshCw size={14} className={`mr-2 ${loading ? 'animate-spin' : ''}`} /> Refresh
                </button>
                <button onClick={onClose} className="btn-primary">OK</button>
            </div>
        </Modal>
    );
}
