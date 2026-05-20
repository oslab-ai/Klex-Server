import { useEffect, useRef } from 'react';
import {
    Info, History, MessageSquare, FileText,
    Pause, XCircle, SkipForward, Skull, PlayCircle,
    RotateCcw, Zap, CheckCircle, AlertTriangle,
    Edit, Trash2,
} from 'lucide-react';
import type { ScheduledJob, JobAction } from '../types';

interface ContextMenuProps {
    isOpen: boolean;
    position: { x: number; y: number };
    job: ScheduledJob | null;
    onClose: () => void;
    onAction: (action: string, job: ScheduledJob) => void;
    engine?: string;
}

interface MenuItem {
    id: string;
    label: string;
    icon: React.ReactNode;
    action?: JobAction | string;
    className?: string;
    dividerAfter?: boolean;
    disabled?: boolean;
    /** If true, this item requires Airflow and will be disabled on Quartz */
    airflowOnly?: boolean;
}

/* Actions that have no Quartz equivalent and require Airflow */
const AIRFLOW_ONLY_ACTIONS = new Set([
    'cancel', 'skip', 'kill',
    'force_start', 'restart', 'force_restart',
    'view_history', 'view_logs',
]);

export default function JobContextMenu({ isOpen, position, job, onClose, onAction, engine = 'quartz' }: ContextMenuProps) {
    const menuRef = useRef<HTMLDivElement>(null);
    const isQuartz = engine === 'quartz';

    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
                onClose();
            }
        };
        const handleEsc = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
            document.addEventListener('keydown', handleEsc);
        }
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('keydown', handleEsc);
        };
    }, [isOpen, onClose]);

    // Adjust position to stay within viewport
    useEffect(() => {
        if (isOpen && menuRef.current) {
            const menu = menuRef.current;
            const rect = menu.getBoundingClientRect();
            const vw = window.innerWidth;
            const vh = window.innerHeight;
            if (rect.right > vw) {
                menu.style.left = `${vw - rect.width - 8}px`;
            }
            if (rect.bottom > vh) {
                menu.style.top = `${vh - rect.height - 8}px`;
            }
        }
    }, [isOpen, position]);

    if (!isOpen || !job) return null;

    const isOnHold = job.status === 'on_hold';
    const isRunning = job.status === 'running';

    const sections: { label: string; items: MenuItem[] }[] = [
        {
            label: 'Information',
            items: [
                { id: 'info', label: 'Job Information', icon: <Info size={15} />, action: 'view_info' },
                { id: 'history', label: 'Job History', icon: <History size={15} />, action: 'view_history', airflowOnly: true },
                { id: 'comment', label: 'Comment', icon: <MessageSquare size={15} />, action: 'view_comments' },
                { id: 'output', label: 'View Job Output', icon: <FileText size={15} />, action: 'view_logs', airflowOnly: true },
            ],
        },
        {
            label: 'Control',
            items: [
                { id: 'hold', label: isOnHold ? 'Release' : 'Hold', icon: <Pause size={15} />, action: isOnHold ? 'release' : 'hold' },
                { id: 'cancel', label: 'Cancel', icon: <XCircle size={15} />, action: 'cancel', disabled: !isRunning, airflowOnly: true },
                { id: 'skip', label: 'Skip', icon: <SkipForward size={15} />, action: 'skip', disabled: !isRunning, airflowOnly: true },
                { id: 'kill', label: 'Kill', icon: <Skull size={15} />, action: 'kill', className: 'text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30', disabled: !isRunning, airflowOnly: true },
            ],
        },
        {
            label: 'Execute',
            items: [
                { id: 'force_start', label: 'Force Start', icon: <PlayCircle size={15} />, action: 'force_start', className: 'context-menu-item-accent', airflowOnly: true },
                { id: 'restart', label: 'Restart', icon: <RotateCcw size={15} />, action: 'restart', airflowOnly: true },
                { id: 'force_restart', label: 'Force Restart', icon: <Zap size={15} />, action: 'force_restart', airflowOnly: true },
            ],
        },
        {
            label: 'Status',
            items: [
                { id: 'mark_finished', label: 'Mark Finished OK', icon: <CheckCircle size={15} />, action: 'mark_finished', className: 'text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30' },
                { id: 'mark_failed', label: 'Mark Failed', icon: <AlertTriangle size={15} />, action: 'mark_failed', className: 'text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30' },
            ],
        },
        {
            label: 'Maintenance',
            items: [
                { id: 'edit', label: 'Edit Job', icon: <Edit size={15} />, action: 'edit' },
                { id: 'edit_in_airflow', label: 'Edit in Airflow UI', icon: <Edit size={15} />, action: 'edit_in_airflow', airflowOnly: true },
                { id: 'delete', label: 'Delete Job', icon: <Trash2 size={15} />, action: 'delete', className: 'text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30' },
            ],
        },
    ];

    return (
        <div
            ref={menuRef}
            className="context-menu"
            style={{ left: position.x, top: position.y }}
        >
            {/* Job name header */}
            <div className="px-4 py-2 border-b border-gray-100 dark:border-gray-700/50">
                <p className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Job Actions</p>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate mt-0.5">{job.schedule_name}</p>
            </div>

            {sections.map((section, sIdx) => {
                // If all items in the section are airflow-only and we're on quartz,
                // still show the section but with disabled items
                return (
                    <div key={section.label}>
                        {sIdx > 0 && <div className="context-menu-divider" />}
                        <div className="px-4 pt-2 pb-1">
                            <span className="text-[10px] font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">{section.label}</span>
                        </div>
                        {section.items.map(item => {
                            const isAirflowBlocked = isQuartz && (item.airflowOnly || AIRFLOW_ONLY_ACTIONS.has(item.action || item.id));
                            const isDisabled = item.disabled || isAirflowBlocked;

                            return (
                                <div key={item.id} className="relative group/item">
                                    <button
                                        className={`context-menu-item ${isAirflowBlocked ? 'opacity-40 cursor-not-allowed' : item.className || ''} ${item.disabled && !isAirflowBlocked ? 'opacity-40 cursor-not-allowed' : ''}`}
                                        onClick={() => {
                                            if (isDisabled) return;
                                            onAction(item.action || item.id, job);
                                            onClose();
                                        }}
                                        disabled={isDisabled}
                                    >
                                        {item.icon}
                                        <span className="flex-1 text-left">{item.label}</span>
                                        {isAirflowBlocked && (
                                            <span className="text-[9px] font-medium text-gray-400 dark:text-gray-500 bg-gray-100 dark:bg-gray-700 px-1.5 py-0.5 rounded ml-1 whitespace-nowrap">
                                                Airflow only
                                            </span>
                                        )}
                                    </button>
                                </div>
                            );
                        })}
                    </div>
                );
            })}
        </div>
    );
}
