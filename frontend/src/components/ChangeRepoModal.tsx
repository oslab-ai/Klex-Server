import { useState, useEffect } from 'react';
import { githubApi, reposApi } from '../api';
import type { GitHubRepo, Repo } from '../types';

interface ChangeRepoModalProps {
    isOpen: boolean;
    onClose: () => void;
    currentRepo: Repo | null;
}

type ModalStep = 'select' | 'confirm';

export default function ChangeRepoModal({ isOpen, onClose, currentRepo }: ChangeRepoModalProps) {
    const [step, setStep] = useState<ModalStep>('select');
    const [repos, setRepos] = useState<GitHubRepo[]>([]);
    const [selectedRepo, setSelectedRepo] = useState<GitHubRepo | null>(null);
    const [pathPrefix, setPathPrefix] = useState(currentRepo?.path_prefix || 'reports');
    const [loading, setLoading] = useState(false);
    const [reposLoading, setReposLoading] = useState(false);
    const [confirmText, setConfirmText] = useState('');
    const [error, setError] = useState('');
    const [result, setResult] = useState<{
        message: string;
        old_repo: string;
        deleted_data: { reports: number; permissions: number; executions: number; scheduled_jobs: number; report_groups: number };
        sync_results: { created: number; updated: number; total_reports: number };
    } | null>(null);

    // Load GitHub repos when modal opens
    useEffect(() => {
        if (isOpen) {
            loadRepos();
            // Reset state
            setStep('select');
            setSelectedRepo(null);
            setConfirmText('');
            setError('');
            setResult(null);
            setPathPrefix(currentRepo?.path_prefix || 'reports');
        }
    }, [isOpen, currentRepo]);

    const loadRepos = async () => {
        setReposLoading(true);
        try {
            const repoList = await githubApi.listRepos();
            setRepos(repoList);
        } catch (err) {
            console.error('Failed to load repos:', err);
            setError('Failed to load repositories from GitHub.');
        } finally {
            setReposLoading(false);
        }
    };

    const handleChange = async () => {
        if (!selectedRepo || !currentRepo) return;

        setLoading(true);
        setError('');

        try {
            const response = await reposApi.change(currentRepo.id, {
                owner: selectedRepo.owner,
                name: selectedRepo.name,
                branch: selectedRepo.default_branch,
                path_prefix: pathPrefix,
                git_remote_url: selectedRepo.clone_url,
            });

            setResult({
                message: response.message,
                old_repo: response.old_repo,
                deleted_data: response.deleted_data,
                sync_results: response.sync_results,
            });
        } catch (err: unknown) {
            const error = err as { response?: { data?: { error?: string } } };
            setError(error.response?.data?.error || 'Failed to change repository. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleSuccessClose = () => {
        onClose();
        window.location.reload();
    };

    if (!isOpen) return null;

    const isCurrentRepo = (repo: GitHubRepo) =>
        !!(currentRepo && repo.owner === currentRepo.owner && repo.name === currentRepo.name);

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 fade-in">
            <div className="glass-card p-6 max-w-lg w-full mx-4 animate-scale-in max-h-[90vh] overflow-y-auto">

                {/* Success state */}
                {result ? (
                    <div>
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-10 h-10 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                                <svg className="w-5 h-5 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold text-green-700 dark:text-green-300">Repository Changed</h3>
                        </div>

                        <div className="space-y-3 mb-6">
                            <div className="p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                                <p className="text-sm text-gray-500 mb-1">Previous repository</p>
                                <p className="font-medium line-through text-gray-400">{result.old_repo}</p>
                            </div>

                            <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800">
                                <p className="text-sm text-green-600 dark:text-green-400 mb-1">New repository</p>
                                <p className="font-medium">{selectedRepo?.full_name}</p>
                                <p className="text-sm text-gray-500 mt-1">
                                    {result.sync_results.created} reports imported
                                </p>
                            </div>

                            {(result.deleted_data.reports > 0 || result.deleted_data.permissions > 0) && (
                                <div className="p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                                    <p className="text-sm text-amber-600 dark:text-amber-400 mb-1">Cleaned up</p>
                                    <ul className="text-sm text-gray-600 dark:text-gray-400 space-y-1">
                                        {result.deleted_data.reports > 0 && <li>• {result.deleted_data.reports} report(s)</li>}
                                        {result.deleted_data.permissions > 0 && <li>• {result.deleted_data.permissions} permission(s)</li>}
                                        {result.deleted_data.executions > 0 && <li>• {result.deleted_data.executions} execution(s)</li>}
                                        {result.deleted_data.scheduled_jobs > 0 && <li>• {result.deleted_data.scheduled_jobs} scheduled job(s)</li>}
                                        {result.deleted_data.report_groups > 0 && <li>• {result.deleted_data.report_groups} report group(s)</li>}
                                    </ul>
                                </div>
                            )}
                        </div>

                        <button onClick={handleSuccessClose} className="btn-primary w-full">
                            Done
                        </button>
                    </div>
                ) : step === 'select' ? (
                    /* Step 1: Select new repo */
                    <div>
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-lg font-semibold">Change Repository</h3>
                            <button onClick={onClose} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        {/* Current repo info */}
                        {currentRepo && (
                            <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 mb-4">
                                <p className="text-xs text-blue-600 dark:text-blue-400 font-medium mb-1">Currently connected</p>
                                <p className="font-medium text-sm">{currentRepo.full_name}</p>
                                <p className="text-xs text-gray-500 mt-0.5">Branch: {currentRepo.branch} • Path: {currentRepo.path_prefix || '(root)'}</p>
                            </div>
                        )}

                        <div className="mb-4">
                            <label className="block text-sm font-medium mb-2">Reports Path Prefix</label>
                            <input
                                type="text"
                                value={pathPrefix}
                                onChange={(e) => setPathPrefix(e.target.value)}
                                className="input"
                                placeholder="reports"
                            />
                        </div>

                        <p className="text-sm text-gray-500 mb-3">Select a different repository:</p>

                        {error && (
                            <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm mb-3">
                                {error}
                            </div>
                        )}

                        {reposLoading ? (
                            <div className="space-y-2 mb-4">
                                {[1, 2, 3].map((i) => (
                                    <div key={i} className="skeleton h-14 rounded-lg" />
                                ))}
                            </div>
                        ) : (
                            <div className="max-h-56 overflow-y-auto space-y-2 mb-4">
                                {repos.map((repo) => {
                                    const isCurrent = isCurrentRepo(repo);
                                    return (
                                        <button
                                            key={repo.id}
                                            onClick={() => !isCurrent && setSelectedRepo(repo)}
                                            disabled={isCurrent}
                                            className={`w-full text-left p-3 rounded-lg border transition-all ${
                                                isCurrent
                                                    ? 'border-blue-300 bg-blue-50/50 dark:bg-blue-900/10 dark:border-blue-800 opacity-60 cursor-not-allowed'
                                                    : selectedRepo?.id === repo.id
                                                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                                                    : 'border-gray-200 dark:border-gray-700 hover:border-primary-300'
                                            }`}
                                        >
                                            <div className="flex items-center justify-between">
                                                <div>
                                                    <p className="font-medium text-sm">{repo.full_name}</p>
                                                    <p className="text-xs text-gray-500">{repo.default_branch}</p>
                                                </div>
                                                <div className="flex items-center gap-2">
                                                    {isCurrent && (
                                                        <span className="text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 px-2 py-0.5 rounded">
                                                            Current
                                                        </span>
                                                    )}
                                                    {repo.private && (
                                                        <span className="text-xs bg-gray-200 dark:bg-gray-700 px-2 py-0.5 rounded">
                                                            Private
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                        </button>
                                    );
                                })}
                            </div>
                        )}

                        <div className="flex space-x-3">
                            <button onClick={onClose} className="btn-secondary flex-1">
                                Cancel
                            </button>
                            <button
                                onClick={() => { setStep('confirm'); setError(''); }}
                                disabled={!selectedRepo}
                                className="btn-primary flex-1"
                            >
                                Next
                            </button>
                        </div>
                    </div>
                ) : (
                    /* Step 2: Confirm with warning */
                    <div>
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-10 h-10 rounded-full bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                                <svg className="w-5 h-5 text-amber-600 dark:text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold">Change Repository?</h3>
                        </div>

                        <div className="space-y-3 mb-4">
                            <div className="flex items-center gap-3">
                                <div className="flex-1 p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                                    <p className="text-xs text-gray-500 mb-1">Current</p>
                                    <p className="font-medium text-sm">{currentRepo?.full_name}</p>
                                </div>
                                <svg className="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                                </svg>
                                <div className="flex-1 p-3 rounded-lg bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800">
                                    <p className="text-xs text-primary-600 dark:text-primary-400 mb-1">New</p>
                                    <p className="font-medium text-sm">{selectedRepo?.full_name}</p>
                                </div>
                            </div>
                        </div>

                        {/* Warning box */}
                        <div className="p-4 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 mb-4">
                            <p className="text-sm font-semibold text-red-700 dark:text-red-300 mb-2">
                                ⚠️ This will permanently delete:
                            </p>
                            <ul className="text-sm text-red-600 dark:text-red-400 space-y-1">
                                <li>• All existing reports from the current repository</li>
                                <li>• All report permissions</li>
                                <li>• All execution history</li>
                                <li>• All scheduled jobs (unlinked)</li>
                                <li>• All report groups</li>
                            </ul>
                            <p className="text-sm text-red-600 dark:text-red-400 mt-2 font-medium">
                                This action cannot be undone.
                            </p>
                        </div>

                        {/* Confirmation input */}
                        <div className="mb-4">
                            <label className="block text-sm font-medium mb-2">
                                Type <span className="font-mono bg-gray-100 dark:bg-gray-800 px-1.5 py-0.5 rounded text-red-600 dark:text-red-400">CHANGE</span> to confirm:
                            </label>
                            <input
                                type="text"
                                value={confirmText}
                                onChange={(e) => setConfirmText(e.target.value)}
                                className="input"
                                placeholder="CHANGE"
                                autoFocus
                            />
                        </div>

                        {error && (
                            <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm mb-4">
                                {error}
                            </div>
                        )}

                        <div className="flex space-x-3">
                            <button
                                onClick={() => { setStep('select'); setConfirmText(''); setError(''); }}
                                className="btn-secondary flex-1"
                                disabled={loading}
                            >
                                Back
                            </button>
                            <button
                                onClick={handleChange}
                                disabled={confirmText !== 'CHANGE' || loading}
                                className="flex-1 px-4 py-2.5 rounded-xl font-medium transition-all duration-200 bg-red-600 hover:bg-red-700 text-white disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {loading ? (
                                    <span className="flex items-center justify-center gap-2">
                                        <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        Changing...
                                    </span>
                                ) : 'Change Repository'}
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
