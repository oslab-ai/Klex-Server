import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi, githubApi, reposApi, dataAdaptersApi } from '../api';
import type { GitHubRepo } from '../types';

type Step = 1 | 2 | 3 | 4;

export default function AdminSetup() {
    const [step, setStep] = useState<Step>(1);
    const [searchParams] = useSearchParams();
    const { setUser, user } = useAuth();
    const navigate = useNavigate();

    // Step 1: Signup
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [displayName, setDisplayName] = useState('');
    const [signupError, setSignupError] = useState('');
    const [signupLoading, setSignupLoading] = useState(false);

    // Step 2: GitHub
    const [githubConnected, setGithubConnected] = useState(false);
    const [githubLoading, setGithubLoading] = useState(false);

    // Step 3: Repo selection
    const [repos, setRepos] = useState<GitHubRepo[]>([]);
    const [selectedRepo, setSelectedRepo] = useState<GitHubRepo | null>(null);
    const [reposLoading, setReposLoading] = useState(false);
    const [pathPrefix, setPathPrefix] = useState('reports');
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);

    // Step 4: Data Adapter
    const [adapterType, setAdapterType] = useState<'jdbc' | 'csv' | 'mock'>('jdbc');
    const [adapterName, setAdapterName] = useState('Default Adapter');
    const [connectionDetails, setConnectionDetails] = useState({
        host: 'localhost',
        port: '5432',
        database: 'klex',
        username: 'postgres',
        password: '',
    });
    const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
    const [adapterLoading, setAdapterLoading] = useState(false);

    // Check if admin already exists
    useEffect(() => {
        const checkAdmin = async () => {
            try {
                const { admin_exists } = await authApi.checkAdminExists();
                if (admin_exists && !user) {
                    navigate('/login?error=Admin+account+already+exists.+Please+log+in.');
                }
            } catch {
                // Ignore errors
            }
        };
        checkAdmin();
    }, [navigate, user]);

    // Handle GitHub callback
    useEffect(() => {
        const githubConnectedParam = searchParams.get('github_connected');
        const githubError = searchParams.get('github_error');

        if (githubConnectedParam === 'true') {
            setGithubConnected(true);
            if (step === 2) {
                setStep(3);
                loadGitHubRepos();
            }
        } else if (githubError) {
            alert(`GitHub connection failed: ${githubError}`);
        }
    }, [searchParams, step]);

    // Check GitHub status when on step 2
    useEffect(() => {
        if (step === 2 && user) {
            checkGitHubStatus();
        }
    }, [step, user]);

    const checkGitHubStatus = async () => {
        try {
            const status = await githubApi.getStatus();
            setGithubConnected(status.connected);
            if (status.connected) {
                loadGitHubRepos();
            }
        } catch {
            // Ignore
        }
    };

    const loadGitHubRepos = async () => {
        setReposLoading(true);
        try {
            const repoList = await githubApi.listRepos();
            setRepos(repoList);
        } catch (err) {
            console.error('Failed to load repos:', err);
        } finally {
            setReposLoading(false);
        }
    };

    // Step 1: Create admin account
    const handleSignup = async (e: React.FormEvent) => {
        e.preventDefault();
        setSignupError('');
        setSignupLoading(true);

        try {
            const response = await authApi.adminSetup({
                username,
                email: email || undefined,
                password,
                display_name: displayName || undefined,
            });
            setUser(response.user);
            setStep(2);
        } catch (err: unknown) {
            const error = err as { response?: { data?: { error?: string; username?: string[]; email?: string[]; non_field_errors?: string[] } } };
            setSignupError(
                error.response?.data?.error ||
                error.response?.data?.email?.[0] ||
                error.response?.data?.username?.[0] ||
                error.response?.data?.non_field_errors?.[0] ||
                'Account creation failed. Please check your details and try again.'
            );
        } finally {
            setSignupLoading(false);
        }
    };

    // Step 2: Connect GitHub
    const handleGitHubConnect = async () => {
        setGithubLoading(true);
        try {
            const { auth_url } = await githubApi.getAuthUrl();
            window.location.href = auth_url;
        } catch (err) {
            console.error('Failed to get GitHub auth URL:', err);
            alert('Failed to connect to GitHub. Please try again.');
            setGithubLoading(false);
        }
    };

    // Step 3: Select and connect repo
    const handleRepoSelect = async () => {
        if (!selectedRepo) return;

        setReposLoading(true);
        try {
            const repo = await reposApi.create({
                owner: selectedRepo.owner,
                name: selectedRepo.name,
                git_remote_url: selectedRepo.clone_url,
                branch: selectedRepo.default_branch,
                path_prefix: pathPrefix,
            });

            // Sync the repo
            await reposApi.sync(repo.id);
            setStep(4);
        } catch (err) {
            console.error('Failed to connect repo:', err);
            alert('Failed to connect repository. Please try again.');
        } finally {
            setReposLoading(false);
        }
    };

    // Step 4: Test connection
    const handleTestConnection = async () => {
        setAdapterLoading(true);
        setTestResult(null);

        try {
            const result = await dataAdaptersApi.testConnection({
                adapter_type: adapterType,
                connection_details: adapterType === 'jdbc' ? {
                    host: connectionDetails.host,
                    port: parseInt(connectionDetails.port),
                    database: connectionDetails.database,
                    username: connectionDetails.username,
                    password: connectionDetails.password,
                } : adapterType === 'mock' ? {} : { file_path: '' },
            });
            setTestResult(result);
        } catch (err) {
            setTestResult({ success: false, message: 'Connection test failed' });
        } finally {
            setAdapterLoading(false);
        }
    };

    // Step 4: Save adapter and finish
    const handleFinish = async () => {
        setAdapterLoading(true);
        try {
            await dataAdaptersApi.create({
                name: adapterName,
                adapter_type: adapterType,
                connection_details: adapterType === 'jdbc' ? {
                    host: connectionDetails.host,
                    port: parseInt(connectionDetails.port),
                    database: connectionDetails.database,
                    username: connectionDetails.username,
                    password: connectionDetails.password,
                } : {},
            });
            navigate('/');
        } catch (err) {
            console.error('Failed to create adapter:', err);
            alert('Failed to create data adapter. Please try again.');
        } finally {
            setAdapterLoading(false);
        }
    };

    const handleSkipToReports = () => {
        navigate('/');
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-primary-50 via-gray-50 to-primary-100 dark:from-gray-900 dark:via-gray-800 dark:to-primary-900/20 py-12 px-4">
            <div className="max-w-2xl mx-auto">
                {/* Header */}
                <div className="text-center mb-8">
                    <div className="w-16 h-16 flex items-center justify-center mx-auto">
                        <img src="/logo_transparent.png" alt="Klex" className="w-full h-full object-contain" />
                    </div>
                    <h1 className="mt-4 text-3xl font-bold">Welcome to Klex</h1>
                    <p className="mt-2 text-gray-600 dark:text-gray-400">Let's set up your report server</p>
                </div>

                {/* Stepper */}
                <div className="flex items-center justify-center mb-8">
                    {[1, 2, 3, 4].map((s) => (
                        <div key={s} className="flex items-center">
                            <div
                                className={`step-indicator ${s === step ? 'active' : s < step ? 'completed' : 'inactive'
                                    }`}
                            >
                                {s < step ? (
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                ) : (
                                    s
                                )}
                            </div>
                            {s < 4 && (
                                <div className={`w-12 h-1 mx-2 rounded ${s < step ? 'bg-green-500' : 'bg-gray-200 dark:bg-gray-700'
                                    }`} />
                            )}
                        </div>
                    ))}
                </div>

                {/* Step content */}
                <div className="glass-card p-8 fade-in">
                    {/* Step 1: Admin Signup */}
                    {step === 1 && (
                        <div>
                            <h2 className="text-xl font-semibold mb-6">Create Admin Account</h2>
                            <form onSubmit={handleSignup} className="space-y-4">
                                {signupError && (
                                    <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
                                        {signupError}
                                    </div>
                                )}

                                <div>
                                    <label className="block text-sm font-medium mb-2">Username *</label>
                                    <input
                                        type="text"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        className="input"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">Email</label>
                                    <input
                                        type="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        className="input"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">Display Name</label>
                                    <input
                                        type="text"
                                        value={displayName}
                                        onChange={(e) => setDisplayName(e.target.value)}
                                        className="input"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">Password *</label>
                                    <input
                                        type="password"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        className="input"
                                        minLength={8}
                                        required
                                    />
                                </div>

                                <button type="submit" disabled={signupLoading} className="btn-primary w-full">
                                    {signupLoading ? 'Creating...' : 'Create Account'}
                                </button>
                            </form>
                        </div>
                    )}

                    {/* Step 2: GitHub Connect */}
                    {step === 2 && (
                        <div className="text-center">
                            <h2 className="text-xl font-semibold mb-6">Connect GitHub</h2>
                            <p className="text-gray-600 dark:text-gray-400 mb-6">
                                Connect your GitHub account to import reports from your repositories.
                            </p>

                            {githubConnected ? (
                                <div className="space-y-4">
                                    <div className="p-4 rounded-lg bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300">
                                        ✓ GitHub connected successfully!
                                    </div>
                                    <button onClick={() => { setStep(3); loadGitHubRepos(); }} className="btn-primary">
                                        Continue to Repository Selection
                                    </button>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    <button
                                        onClick={handleGitHubConnect}
                                        disabled={githubLoading}
                                        className="btn-primary flex items-center justify-center space-x-2 mx-auto"
                                    >
                                        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                                            <path fillRule="evenodd" d="M12 2C6.477 2 2 6.477 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0012 2z" clipRule="evenodd" />
                                        </svg>
                                        <span>{githubLoading ? 'Connecting...' : 'Connect GitHub'}</span>
                                    </button>
                                    <button onClick={handleSkipToReports} className="btn-ghost">
                                        Skip for now
                                    </button>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Step 3: Repository Selection */}
                    {step === 3 && (
                        <div>
                            <h2 className="text-xl font-semibold mb-6">Select Repository</h2>

                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-2">Reports Path Prefix</label>
                                <input
                                    type="text"
                                    value={pathPrefix}
                                    onChange={(e) => setPathPrefix(e.target.value)}
                                    className="input"
                                    placeholder="reports"
                                />
                                <p className="text-sm text-gray-500 mt-1">
                                    Folder in your repository where reports are stored
                                </p>
                            </div>

                            {reposLoading ? (
                                <div className="grid grid-cols-1 gap-3">
                                    {[1, 2, 3].map((i) => (
                                        <div key={i} className="skeleton h-16 rounded-lg" />
                                    ))}
                                </div>
                            ) : (
                                <div className="max-h-64 overflow-y-auto space-y-2 mb-4">
                                    {repos.map((repo) => (
                                        <button
                                            key={repo.id}
                                            onClick={() => setSelectedRepo(repo)}
                                            className={`w-full text-left p-4 rounded-lg border transition-all ${selectedRepo?.id === repo.id
                                                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                                                    : 'border-gray-200 dark:border-gray-700 hover:border-primary-300'
                                                }`}
                                        >
                                            <div className="flex items-center justify-between">
                                                <div>
                                                    <p className="font-medium">{repo.full_name}</p>
                                                    <p className="text-sm text-gray-500">{repo.default_branch}</p>
                                                </div>
                                                {repo.private && (
                                                    <span className="text-xs bg-gray-200 dark:bg-gray-700 px-2 py-1 rounded">
                                                        Private
                                                    </span>
                                                )}
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            )}

                            <div className="flex space-x-3">
                                <button onClick={() => setStep(2)} className="btn-secondary">
                                    Back
                                </button>
                                <button
                                    onClick={() => setShowConfirmDialog(true)}
                                    disabled={!selectedRepo || reposLoading}
                                    className="btn-primary flex-1"
                                >
                                    {reposLoading ? 'Connecting...' : 'Connect Repository'}
                                </button>
                            </div>
                            {/* Confirmation Dialog */}
                            {showConfirmDialog && selectedRepo && (
                                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 fade-in">
                                    <div className="glass-card p-6 max-w-md w-full mx-4 animate-scale-in">
                                        <h3 className="text-lg font-semibold mb-4">Confirm Repository Selection</h3>
                                        <p className="text-gray-600 dark:text-gray-400 mb-4">
                                            You are about to connect to:
                                        </p>
                                        <div className="p-4 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 mb-4 space-y-2">
                                            <div className="flex items-center gap-2">
                                                <span className="text-lg">📦</span>
                                                <span className="font-medium">{selectedRepo.full_name}</span>
                                                {selectedRepo.private && (
                                                    <span className="text-xs bg-gray-200 dark:bg-gray-700 px-2 py-0.5 rounded">Private</span>
                                                )}
                                            </div>
                                            <div className="flex items-center gap-2 text-sm text-gray-500">
                                                <span>🌿</span>
                                                <span>Branch: {selectedRepo.default_branch}</span>
                                            </div>
                                            <div className="flex items-center gap-2 text-sm text-gray-500">
                                                <span>📁</span>
                                                <span>Reports path: {pathPrefix || '(root)'}</span>
                                            </div>
                                        </div>
                                        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
                                            All reports will be imported from this repository. Are you sure you want to proceed?
                                        </p>
                                        <div className="flex space-x-3">
                                            <button
                                                onClick={() => setShowConfirmDialog(false)}
                                                className="btn-secondary flex-1"
                                            >
                                                Cancel
                                            </button>
                                            <button
                                                onClick={() => {
                                                    setShowConfirmDialog(false);
                                                    handleRepoSelect();
                                                }}
                                                disabled={reposLoading}
                                                className="btn-primary flex-1"
                                            >
                                                {reposLoading ? 'Connecting...' : 'Yes, Connect'}
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Step 4: Data Adapter */}
                    {step === 4 && (
                        <div>
                            <h2 className="text-xl font-semibold mb-6">Configure Data Adapter</h2>

                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium mb-2">Adapter Name</label>
                                    <input
                                        type="text"
                                        value={adapterName}
                                        onChange={(e) => setAdapterName(e.target.value)}
                                        className="input"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-2">Adapter Type</label>
                                    <select
                                        value={adapterType}
                                        onChange={(e) => setAdapterType(e.target.value as typeof adapterType)}
                                        className="input"
                                    >
                                        <option value="jdbc">JDBC (PostgreSQL)</option>
                                        <option value="csv">CSV File</option>
                                        <option value="mock">Mock Data</option>
                                    </select>
                                </div>

                                {adapterType === 'jdbc' && (
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-medium mb-2">Host</label>
                                            <input
                                                type="text"
                                                value={connectionDetails.host}
                                                onChange={(e) => setConnectionDetails({ ...connectionDetails, host: e.target.value })}
                                                className="input"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium mb-2">Port</label>
                                            <input
                                                type="text"
                                                value={connectionDetails.port}
                                                onChange={(e) => setConnectionDetails({ ...connectionDetails, port: e.target.value })}
                                                className="input"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium mb-2">Database</label>
                                            <input
                                                type="text"
                                                value={connectionDetails.database}
                                                onChange={(e) => setConnectionDetails({ ...connectionDetails, database: e.target.value })}
                                                className="input"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium mb-2">Username</label>
                                            <input
                                                type="text"
                                                value={connectionDetails.username}
                                                onChange={(e) => setConnectionDetails({ ...connectionDetails, username: e.target.value })}
                                                className="input"
                                            />
                                        </div>
                                        <div className="col-span-2">
                                            <label className="block text-sm font-medium mb-2">Password</label>
                                            <input
                                                type="password"
                                                value={connectionDetails.password}
                                                onChange={(e) => setConnectionDetails({ ...connectionDetails, password: e.target.value })}
                                                className="input"
                                            />
                                        </div>
                                    </div>
                                )}

                                {testResult && (
                                    <div className={`p-3 rounded-lg ${testResult.success
                                            ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                                            : 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300'
                                        }`}>
                                        {testResult.message}
                                    </div>
                                )}

                                <div className="flex space-x-3">
                                    <button onClick={() => setStep(3)} className="btn-secondary">
                                        Back
                                    </button>
                                    <button
                                        onClick={handleTestConnection}
                                        disabled={adapterLoading}
                                        className="btn-secondary"
                                    >
                                        Test Connection
                                    </button>
                                    <button
                                        onClick={handleFinish}
                                        disabled={adapterLoading}
                                        className="btn-primary flex-1"
                                    >
                                        {adapterLoading ? 'Saving...' : 'Finish Setup'}
                                    </button>
                                </div>

                                <button onClick={handleSkipToReports} className="btn-ghost w-full">
                                    Skip for now
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
