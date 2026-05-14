import { useState, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { reposApi } from '../api';

const SIDEBAR_KEY = 'klex_sidebar_collapsed';

interface NavItem {
    path: string;
    label: string;
    icon: React.ReactNode;
    adminOnly?: boolean;
}

export default function Sidebar() {
    const { user, logout } = useAuth();
    const { theme, toggleTheme } = useTheme();
    const location = useLocation();
    const [collapsed, setCollapsed] = useState(() => {
        return localStorage.getItem(SIDEBAR_KEY) === 'true';
    });
    const [syncing, setSyncing] = useState(false);
    const [userMenuOpen, setUserMenuOpen] = useState(false);
    const userMenuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        localStorage.setItem(SIDEBAR_KEY, String(collapsed));
    }, [collapsed]);

    // Close user menu on outside click
    useEffect(() => {
        const handleClick = (e: MouseEvent) => {
            if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
                setUserMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, []);

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

    const isActive = (path: string) => {
        if (path === '/') return location.pathname === '/';
        return location.pathname.startsWith(path);
    };

    const mainNavItems: NavItem[] = [
        {
            path: '/',
            label: 'Reports',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
            ),
        },
        {
            path: '/embeddings',
            label: 'Embeddings',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                </svg>
            ),
        },
    ];

    const adminNavItems: NavItem[] = [
        {
            path: '/data-adapters',
            label: 'Data Adapters',
            adminOnly: true,
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
                </svg>
            ),
        },
        {
            path: '/users',
            label: 'Users',
            adminOnly: true,
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
                </svg>
            ),
        },
        {
            path: '/permissions',
            label: 'Permissions',
            adminOnly: true,
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
            ),
        },
        {
            path: '/schedules',
            label: 'Schedules',
            adminOnly: true,
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
            ),
        },
    ];

    const renderNavItem = (item: NavItem) => (
        <Link
            key={item.path}
            to={item.path}
            className={`sidebar-item group relative ${isActive(item.path) ? 'sidebar-item-active' : ''}`}
            title={collapsed ? item.label : undefined}
        >
            {isActive(item.path) && (
                <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-primary-600 dark:bg-primary-400 rounded-r-full" />
            )}
            <span className="flex-shrink-0">{item.icon}</span>
            {!collapsed && <span className="truncate">{item.label}</span>}
        </Link>
    );

    return (
        <aside
            className="sidebar"
            style={{ width: collapsed ? 72 : 260 }}
        >
            {/* Logo */}
            <div className="px-4 py-5 flex items-center justify-center border-b border-gray-100 dark:border-gray-800">
                <div className={`${collapsed ? 'w-14 h-14' : 'w-full h-15'} flex items-center justify-center flex-shrink-0`}>
                    <img src={theme === 'light' ? '/logo-nobg.png' : '/logo-nobg_light.png'} alt="Klex" className="w-full h-full object-contain" />
                </div>
            </div>

            {/* Navigation */}
            <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
                {/* Main */}
                {!collapsed && <div className="sidebar-section-label">Main</div>}
                {mainNavItems.map(renderNavItem)}

                {/* Sync button */}
                {user?.is_admin && (
                    <button
                        onClick={handleSync}
                        disabled={syncing}
                        className={`sidebar-item w-full ${syncing ? 'opacity-60' : ''}`}
                        title={collapsed ? (syncing ? 'Syncing...' : 'Sync') : undefined}
                    >
                        <svg className={`w-5 h-5 flex-shrink-0 ${syncing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                        </svg>
                        {!collapsed && <span>{syncing ? 'Syncing...' : 'Sync Repos'}</span>}
                    </button>
                )}

                {/* Admin */}
                {user?.is_admin && (
                    <>
                        <div className="pt-4" />
                        {!collapsed && <div className="sidebar-section-label">Admin</div>}
                        {adminNavItems.map(renderNavItem)}
                    </>
                )}

                {/* Service Status */}
                <div className="pt-4" />
                {!collapsed && <div className="sidebar-section-label">Services</div>}
                <div className={`sidebar-item cursor-default hover:bg-transparent dark:hover:bg-transparent ${collapsed ? 'justify-center' : ''}`}>
                    <div className="status-dot status-dot-active flex-shrink-0" />
                    {!collapsed && <span className="text-xs text-gray-500 dark:text-gray-400">Reporting Engine</span>}
                </div>
                <div className={`sidebar-item cursor-default hover:bg-transparent dark:hover:bg-transparent ${collapsed ? 'justify-center' : ''}`}>
                    <div className="status-dot status-dot-active flex-shrink-0" />
                    {!collapsed && <span className="text-xs text-gray-500 dark:text-gray-400">Scheduler</span>}
                </div>
            </nav>

            {/* Bottom section */}
            <div className="border-t border-gray-100 dark:border-gray-800 px-3 py-3 space-y-2">
                {/* Theme toggle */}
                <button
                    onClick={toggleTheme}
                    className="sidebar-item w-full"
                    title={collapsed ? (theme === 'light' ? 'Dark mode' : 'Light mode') : undefined}
                >
                    {theme === 'light' ? (
                        <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                        </svg>
                    ) : (
                        <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                        </svg>
                    )}
                    {!collapsed && <span>{theme === 'light' ? 'Dark Mode' : 'Light Mode'}</span>}
                </button>

                {/* Collapse toggle */}
                <button
                    onClick={() => setCollapsed(!collapsed)}
                    className="sidebar-item w-full"
                    title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
                >
                    <svg className={`w-5 h-5 flex-shrink-0 transition-transform duration-300 ${collapsed ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
                    </svg>
                    {!collapsed && <span>Collapse</span>}
                </button>

                {/* User */}
                <div className="relative" ref={userMenuRef}>
                    <button
                        onClick={() => setUserMenuOpen(!userMenuOpen)}
                        className={`sidebar-item w-full ${collapsed ? 'justify-center' : ''}`}
                    >
                        <div className="w-8 h-8 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center flex-shrink-0">
                            <span className="text-white font-semibold text-sm">
                                {user?.username?.charAt(0).toUpperCase()}
                            </span>
                        </div>
                        {!collapsed && (
                            <div className="flex-1 text-left min-w-0">
                                <div className="text-sm font-semibold truncate">{user?.display_name || user?.username}</div>
                                <div className="text-[11px] text-gray-400 truncate">{user?.is_admin ? 'Admin' : 'User'}</div>
                            </div>
                        )}
                    </button>

                    {userMenuOpen && (
                        <div className="absolute bottom-full left-0 mb-2 w-48 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-xl py-1 animate-scale-in z-50">
                            <div className="px-4 py-2 border-b border-gray-100 dark:border-gray-700">
                                <p className="text-sm font-semibold">{user?.display_name || user?.username}</p>
                                <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                            </div>
                            <button
                                onClick={() => {
                                    setUserMenuOpen(false);
                                    logout();
                                }}
                                className="w-full text-left px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors flex items-center gap-2"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                                </svg>
                                Logout
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </aside>
    );
}
