import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { reposApi } from '../api';

export default function Navbar() {
    const { user, logout } = useAuth();
    const { theme, toggleTheme } = useTheme();
    const location = useLocation();
    const [menuOpen, setMenuOpen] = useState(false);
    const [syncing, setSyncing] = useState(false);

    const handleSync = async () => {
        if (syncing) return;
        setSyncing(true);
        try {
            const repos = await reposApi.list();
            if (repos.length > 0) {
                await reposApi.sync(repos[0].id);
                window.location.reload();
            } else {
                alert('No repository connected. Please connect a GitHub repository first.');
            }
        } catch (err: unknown) {
            console.error('Sync failed:', err);
            const errorMessage = (err as { response?: { data?: { error?: string; detail?: string } } })?.response?.data?.error
                || (err as { response?: { data?: { error?: string; detail?: string } } })?.response?.data?.detail
                || 'Sync failed. Please try again.';
            alert(errorMessage);
        } finally {
            setSyncing(false);
        }
    };

    const isActive = (path: string) => location.pathname === path;

    return (
        <nav className="glass-card sticky top-0 z-50 border-b border-gray-200/50 dark:border-gray-700/50">
            <div className="mx-auto px-7">
                <div className="flex items-center justify-between h-16">
                    {/* Logo */}
                    <Link to="/" className="flex items-center space-x-2">
                        <div className="w-8 h-8 flex items-center justify-center">
                            <img src="/logo-nobg.png" alt="Klex" className="w-full h-full object-contain" />
                        </div>
                        <span className="font-bold text-xl bg-gradient-to-r from-primary-600 to-primary-400 bg-clip-text text-transparent">
                            Klex
                        </span>
                    </Link>

                    {/* Desktop Navigation */}
                    <div className="hidden md:flex items-center space-x-4">
                        <Link
                            to="/"
                            className={`px-3 py-2 rounded-lg transition-colors ${isActive('/')
                                ? 'bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-300'
                                : 'hover:bg-gray-100 dark:hover:bg-gray-800'
                                }`}
                        >
                            Reports
                        </Link>

                        {user?.is_admin && (
                            <>
                                <button
                                    onClick={handleSync}
                                    disabled={syncing}
                                    className="btn-secondary flex items-center space-x-2"
                                >
                                    <svg className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                    </svg>
                                    <span>{syncing ? 'Syncing...' : 'Sync'}</span>
                                </button>

                                <Link
                                    to="/data-adapters"
                                    className={`px-3 py-2 rounded-lg transition-colors ${isActive('/data-adapters')
                                        ? 'bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-300'
                                        : 'hover:bg-gray-100 dark:hover:bg-gray-800'
                                        }`}
                                >
                                    Data Adapters
                                </Link>

                                <Link
                                    to="/users"
                                    className={`px-3 py-2 rounded-lg transition-colors ${isActive('/users')
                                        ? 'bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-300'
                                        : 'hover:bg-gray-100 dark:hover:bg-gray-800'
                                        }`}
                                >
                                    Users
                                </Link>

                                <Link
                                    to="/permissions"
                                    className={`px-3 py-2 rounded-lg transition-colors ${isActive('/permissions')
                                        ? 'bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-300'
                                        : 'hover:bg-gray-100 dark:hover:bg-gray-800'
                                        }`}
                                >
                                    Permissions
                                </Link>
                            </>
                        )}
                    </div>

                    {/* Right side */}
                    <div className="flex items-center space-x-4">
                        {/* Theme toggle */}
                        <button
                            onClick={toggleTheme}
                            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                            aria-label="Toggle theme"
                        >
                            {theme === 'light' ? (
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                                </svg>
                            ) : (
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                                </svg>
                            )}
                        </button>

                        {/* User menu */}
                        <div className="relative">
                            <button
                                onClick={() => setMenuOpen(!menuOpen)}
                                className="flex items-center space-x-2 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                            >
                                <div className="w-8 h-8 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center">
                                    <span className="text-white font-medium text-sm">
                                        {user?.username?.charAt(0).toUpperCase()}
                                    </span>
                                </div>
                                <span className="hidden sm:block text-sm font-medium">{user?.username}</span>
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                </svg>
                            </button>

                            {menuOpen && (
                                <div className="absolute right-0 mt-2 w-56 glass-card py-2 fade-in">
                                    <div className="px-4 py-2 border-b border-gray-200 dark:border-gray-700">
                                        <p className="text-sm font-medium">{user?.display_name || user?.username}</p>
                                        <p className="text-xs text-gray-500">{user?.email}</p>
                                        <span className={`inline-block mt-1 px-2 py-0.5 text-xs rounded-full ${
                                            user?.is_super_admin
                                                ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300'
                                                : user?.is_admin
                                                ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/50 dark:text-purple-300'
                                                : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                                            }`}>
                                            {user?.is_super_admin ? 'Super Admin' : user?.is_admin ? 'Admin' : 'User'}
                                        </span>
                                    </div>
                                    <button
                                        onClick={() => {
                                            setMenuOpen(false);
                                            logout();
                                        }}
                                        className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                                    >
                                        Logout
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </nav>
    );
}
