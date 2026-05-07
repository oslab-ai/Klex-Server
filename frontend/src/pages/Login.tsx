import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
    const [usernameOrEmail, setUsernameOrEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [showForgotMsg, setShowForgotMsg] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const searchParams = new URLSearchParams(window.location.search);
        const errorParam = searchParams.get('error');
        if (errorParam) {
            setError(errorParam);
            window.history.replaceState({}, document.title, window.location.pathname);
        }
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            await login(usernameOrEmail, password);
            navigate('/');
        } catch (err: unknown) {
            const error = err as { response?: { data?: { error?: string; detail?: string } } };
            setError(error.response?.data?.error || error.response?.data?.detail || 'Login failed. Please check your credentials.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex">
            {/* Left Panel — Branding */}
            <div className="hidden lg:flex lg:w-[55%] relative bg-gradient-to-br from-primary-950 via-primary-900 to-primary-800 overflow-hidden">
                {/* Animated orbs */}
                <div className="absolute w-96 h-96 bg-primary-500/20 rounded-full -top-20 -left-20 blur-3xl animate-float" />
                <div className="absolute w-72 h-72 bg-violet-500/15 rounded-full bottom-20 right-10 blur-3xl animate-float-delayed" />
                <div className="absolute w-48 h-48 bg-accent-400/10 rounded-full top-1/2 left-1/3 blur-2xl animate-float" />

                {/* Grid pattern overlay */}
                <div
                    className="absolute inset-0 opacity-[0.03]"
                    style={{
                        backgroundImage: 'radial-gradient(circle at 1px 1px, white 1px, transparent 0)',
                        backgroundSize: '40px 40px',
                    }}
                />

                {/* Content */}
                <div className="relative flex flex-col justify-center px-16 z-10">
                    {/* Logo */}
                    <div className="mb-12">
                        <div className="w-full max-w-[280px] h-20 flex items-center justify-start">
                            <img src="/logo-nobg_light.png" alt="Klex" className="w-full h-full object-contain drop-shadow-lg" />
                        </div>
                    </div>

                    {/* Hero text */}
                    <h1 className="text-4xl font-bold text-white leading-tight mb-4">
                        Report {" "}
                        <span className="bg-gradient-to-r from-accent-300 to-accent-500 bg-clip-text text-transparent">
                            Server
                        </span>
                    </h1>

                    <p className="text-primary-200 text-lg leading-relaxed mb-10 max-w-md">
                        Generate, schedule, and deliver beautiful reports.
                    </p>

                    {/* Feature pills */}
                    <div className="flex flex-wrap gap-3 mb-12">
                        {['Report Generation', 'Scheduling', 'Multi-format Export', 'Data Adapters'].map((feature, i) => (
                            <div
                                key={feature}
                                className="px-4 py-2 rounded-full bg-white/10 backdrop-blur-sm border border-white/10 text-sm text-primary-100 font-medium animate-slide-up"
                                style={{ animationDelay: `${i * 100}ms`, animationFillMode: 'backwards' }}
                            >
                                {feature}
                            </div>
                        ))}
                    </div>

                    {/* Service badges */}
                    <div className="flex items-center gap-4 text-primary-300 text-xs">
                        <div className="flex items-center gap-2">
                            <div className="status-dot status-dot-active" />
                            <span>Reporting Engine</span>
                        </div>
                        <div className="w-px h-4 bg-primary-700" />
                        <div className="flex items-center gap-2">
                            <div className="status-dot status-dot-active" />
                            <span>Scheduler</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Right Panel — Login Form */}
            <div className="flex-1 flex items-center justify-center bg-white dark:bg-gray-900 px-6">
                <div className="w-full max-w-sm">
                    {/* Mobile logo */}
                    <div className="lg:hidden text-center mb-8">
                        <div className="w-40 h-14 flex items-center justify-center mx-auto">
                            <img src="/logo-nobg.png" alt="Klex" className="w-full h-full object-contain dark:hidden" />
                            <img src="/logo-nobg_light.png" alt="Klex" className="w-full h-full object-contain hidden dark:block" />
                        </div>
                        <h1 className="mt-4 text-2xl font-bold bg-gradient-to-r from-primary-600 to-primary-400 bg-clip-text text-transparent">
                            Klex
                        </h1>
                    </div>

                    <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-1">Welcome back</h2>
                    <p className="text-gray-500 dark:text-gray-400 mb-8">Sign in to your account</p>

                    <form onSubmit={handleSubmit} className="space-y-5">
                        {error && (
                            <div className="p-3 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-sm flex items-center gap-2">
                                <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                                {error}
                            </div>
                        )}

                        <div>
                            <label htmlFor="username" className="block text-sm font-semibold mb-2 text-gray-700 dark:text-gray-300">
                                Username or Email
                            </label>
                            <div className="relative">
                                <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                </svg>
                                <input
                                    id="username"
                                    type="text"
                                    value={usernameOrEmail}
                                    onChange={(e) => setUsernameOrEmail(e.target.value)}
                                    className="input input-icon"
                                    placeholder="Enter your username or email"
                                    required
                                />
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center justify-between mb-2">
                                <label htmlFor="password" className="block text-sm font-semibold text-gray-700 dark:text-gray-300">
                                    Password
                                </label>
                                <button
                                    type="button"
                                    onClick={() => setShowForgotMsg(true)}
                                    className="text-xs text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300 font-medium transition-colors"
                                >
                                    Forgot password?
                                </button>
                            </div>
                            {showForgotMsg && (
                                <div className="mb-2 p-2.5 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 text-blue-700 dark:text-blue-300 text-xs">
                                    Please contact your administrator to reset your password.
                                </div>
                            )}
                            <div className="relative">
                                <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                                </svg>
                                <input
                                    id="password"
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    className="input input-icon"
                                    placeholder="Enter your password"
                                    required
                                />
                            </div>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="btn-primary w-full flex items-center justify-center h-11"
                        >
                            {loading ? (
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                            ) : (
                                'Sign In'
                            )}
                        </button>
                    </form>

                    <div className="mt-8 text-center">
                        <a
                            href="/admin-setup"
                            className="text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300 font-medium transition-colors"
                        >
                            First time? Set up admin account →
                        </a>
                    </div>
                </div>
            </div>
        </div>
    );
}
