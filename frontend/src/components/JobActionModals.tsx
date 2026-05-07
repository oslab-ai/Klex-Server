import { useState } from 'react';
import {
    AlertTriangle, Pause, XCircle, Skull,
    PlayCircle, RotateCcw, CheckCircle, Zap,
} from 'lucide-react';
import Modal from './Modal';
import type { JobAction } from '../types';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    action: JobAction | null;
    jobName: string;
    onConfirm: () => Promise<void>;
}

const ACTION_CONFIG: Record<string, { label: string; description: string; icon: React.ReactNode; color: string; btnClass: string }> = {
    hold: {
        label: 'Hold Job',
        description: 'This will pause the DAG in Airflow. No new runs will be triggered until the job is released.',
        icon: <Pause size={24} />,
        color: 'text-amber-600 dark:text-amber-400',
        btnClass: 'bg-amber-600 hover:bg-amber-700 text-white',
    },
    release: {
        label: 'Release Job',
        description: 'This will unpause the DAG in Airflow. The job will resume its scheduled runs.',
        icon: <PlayCircle size={24} />,
        color: 'text-green-600 dark:text-green-400',
        btnClass: 'bg-green-600 hover:bg-green-700 text-white',
    },
    cancel: {
        label: 'Cancel Job',
        description: 'This will mark the current running DAG run as failed. The schedule will continue.',
        icon: <XCircle size={24} />,
        color: 'text-red-600 dark:text-red-400',
        btnClass: 'bg-red-600 hover:bg-red-700 text-white',
    },
    kill: {
        label: 'Kill Job',
        description: 'This will forcefully stop the current run AND pause the DAG. Use with caution.',
        icon: <Skull size={24} />,
        color: 'text-red-600 dark:text-red-400',
        btnClass: 'bg-red-600 hover:bg-red-700 text-white',
    },
    force_start: {
        label: 'Force Start',
        description: 'This will trigger an immediate DAG run without affecting the regular schedule.',
        icon: <Zap size={24} />,
        color: 'text-violet-600 dark:text-violet-400',
        btnClass: 'bg-violet-600 hover:bg-violet-700 text-white',
    },
    restart: {
        label: 'Restart Job',
        description: 'This will clear the latest DAG run task instances and re-run them.',
        icon: <RotateCcw size={24} />,
        color: 'text-blue-600 dark:text-blue-400',
        btnClass: 'bg-blue-600 hover:bg-blue-700 text-white',
    },
    force_restart: {
        label: 'Force Restart',
        description: 'This will clear ALL task instances for the latest run and force a complete re-execution.',
        icon: <Zap size={24} />,
        color: 'text-blue-600 dark:text-blue-400',
        btnClass: 'bg-blue-600 hover:bg-blue-700 text-white',
    },
    skip: {
        label: 'Skip Run',
        description: 'This will mark the latest DAG run as successful, effectively skipping it.',
        icon: <CheckCircle size={24} />,
        color: 'text-gray-600 dark:text-gray-400',
        btnClass: 'bg-gray-600 hover:bg-gray-700 text-white',
    },
    mark_finished: {
        label: 'Mark Finished OK',
        description: 'This will set the latest DAG run state to success and update the job status to finished.',
        icon: <CheckCircle size={24} />,
        color: 'text-green-600 dark:text-green-400',
        btnClass: 'bg-green-600 hover:bg-green-700 text-white',
    },
    mark_failed: {
        label: 'Mark Failed',
        description: 'This will set the latest DAG run state to failed and update the job status.',
        icon: <AlertTriangle size={24} />,
        color: 'text-red-600 dark:text-red-400',
        btnClass: 'bg-red-600 hover:bg-red-700 text-white',
    },
};

export default function JobActionModal({ isOpen, onClose, action, jobName, onConfirm }: Props) {
    const [submitting, setSubmitting] = useState(false);

    if (!action) return null;
    const config = ACTION_CONFIG[action];
    if (!config) return null;

    const handleConfirm = async () => {
        setSubmitting(true);
        try {
            await onConfirm();
            onClose();
        } catch (err) {
            console.error(`Action ${action} failed:`, err);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} maxWidth="max-w-md">
            <div className="text-center">
                <div className={`inline-flex items-center justify-center w-16 h-16 rounded-2xl mb-4 ${config.color} bg-gray-100 dark:bg-gray-700/50`}>
                    {config.icon}
                </div>
                <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2">{config.label}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">
                    Job: <span className="font-semibold text-gray-700 dark:text-gray-200">{jobName}</span>
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">{config.description}</p>
            </div>

            <div className="flex gap-3">
                <button onClick={onClose} className="btn-secondary flex-1" disabled={submitting}>
                    Cancel
                </button>
                <button
                    onClick={handleConfirm}
                    className={`btn flex-1 ${config.btnClass}`}
                    disabled={submitting}
                >
                    {submitting ? 'Processing...' : config.label}
                </button>
            </div>
        </Modal>
    );
}
