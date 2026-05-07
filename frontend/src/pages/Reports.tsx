import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { reportsApi, githubApi, reposApi, groupsApi } from '../api';
import type { Report, GitHubRepo, ReportGroup } from '../types';
import Modal from '../components/Modal';
import ScheduleModal from '../components/ScheduleModal';
import ReportGroupModal, { getGroupColorConfig } from '../components/ReportGroupModal';
import type { GroupFormData } from '../components/ReportGroupModal';
import { useAuth } from '../context/AuthContext';

type ViewMode = 'tile' | 'list';

export default function Reports() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [viewMode, setViewMode] = useState<ViewMode>(() => {
        return (localStorage.getItem('klex_view_mode') as ViewMode) || 'tile';
    });
    const [githubConnected, setGithubConnected] = useState(false);
    const [connectingGithub, setConnectingGithub] = useState(false);
    const [showRepoModal, setShowRepoModal] = useState(false);
    const [repos, setRepos] = useState<GitHubRepo[]>([]);
    const [selectedRepo, setSelectedRepo] = useState<GitHubRepo | null>(null);
    const [pathPrefix, setPathPrefix] = useState('');
    const [reposLoading, setReposLoading] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [contextMenu, setContextMenu] = useState<{ x: number; y: number; report: Report } | null>(null);
    const [scheduleReport, setScheduleReport] = useState<Report | null>(null);

    // ── Group state ──
    const [groups, setGroups] = useState<ReportGroup[]>([]);
    const [collapsedGroups, setCollapsedGroups] = useState<Set<number>>(new Set());
    const [showGroupModal, setShowGroupModal] = useState(false);
    const [editingGroup, setEditingGroup] = useState<ReportGroup | null>(null);
    const [showGroupSubmenu, setShowGroupSubmenu] = useState(false);
    const [savingGroup, setSavingGroup] = useState(false);

    useEffect(() => {
        loadReports();
        checkGitHubStatus();
        loadGroups();
    }, []);

    useEffect(() => {
        localStorage.setItem('klex_view_mode', viewMode);
    }, [viewMode]);

    const loadGroups = async () => {
        try {
            const data = await groupsApi.list();
            setGroups(data);
        } catch (err) {
            console.error('Failed to load groups:', err);
        }
    };

    const closeContextMenu = useCallback(() => {
        setContextMenu(null);
        setShowGroupSubmenu(false);
    }, []);

    useEffect(() => {
        if (!contextMenu) return;
        const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') closeContextMenu(); };
        window.addEventListener('click', closeContextMenu);
        window.addEventListener('scroll', closeContextMenu, true);
        window.addEventListener('keydown', onKey);
        return () => {
            window.removeEventListener('click', closeContextMenu);
            window.removeEventListener('scroll', closeContextMenu, true);
            window.removeEventListener('keydown', onKey);
        };
    }, [contextMenu, closeContextMenu]);

    const handleContextMenu = (e: React.MouseEvent, report: Report) => {
        e.preventDefault();
        setContextMenu({ x: e.clientX, y: e.clientY, report });
        setShowGroupSubmenu(false);
    };

    const checkGitHubStatus = async () => {
        try {
            const status = await githubApi.getStatus();
            setGithubConnected(status.connected);
        } catch { /* Ignore */ }
    };

    const loadReports = async () => {
        try {
            const data = await reportsApi.list();
            setReports(data);
        } catch (err) {
            console.error('Failed to load reports:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleConnectGitHub = async () => {
        setConnectingGithub(true);
        try {
            const { auth_url } = await githubApi.getAuthUrl();
            window.location.href = auth_url;
        } catch (err) {
            console.error('Failed to get GitHub auth URL:', err);
            alert('Failed to connect to GitHub. Please try again.');
            setConnectingGithub(false);
        }
    };

    const handleSelectRepository = async () => {
        setReposLoading(true);
        try {
            const repoList = await githubApi.listRepos();
            setRepos(repoList);
            setShowRepoModal(true);
        } catch (err) {
            console.error('Failed to load repos:', err);
            alert('Failed to load repositories. Please try again.');
        } finally {
            setReposLoading(false);
        }
    };

    const handleConnectRepo = async () => {
        if (!selectedRepo) return;
        setReposLoading(true);
        try {
            const existingRepos = await reposApi.list();
            const existingRepo = existingRepos.find(
                r => r.owner === selectedRepo.owner && r.name === selectedRepo.name
            );
            let repoId: number;
            if (existingRepo) {
                repoId = existingRepo.id;
            } else {
                const newRepo = await reposApi.create({
                    owner: selectedRepo.owner,
                    name: selectedRepo.name,
                    git_remote_url: selectedRepo.clone_url,
                    branch: selectedRepo.default_branch,
                    path_prefix: pathPrefix,
                });
                repoId = newRepo.id;
            }
            await reposApi.sync(repoId);
            setShowRepoModal(false);
            setSelectedRepo(null);
            loadReports();
        } catch (err: unknown) {
            console.error('Failed to connect repo:', err);
            const errorMessage = (err as { response?: { data?: { error?: string; detail?: string } } })?.response?.data?.error
                || (err as { response?: { data?: { error?: string; detail?: string } } })?.response?.data?.detail
                || 'Failed to connect repository. Please try again.';
            alert(errorMessage);
        } finally {
            setReposLoading(false);
        }
    };

    // ── Group handlers ──
    const handleSaveGroup = async (formData: GroupFormData) => {
        setSavingGroup(true);
        try {
            if (editingGroup) {
                const updated = await groupsApi.update(editingGroup.id, formData);
                setGroups(prev => prev.map(g => g.id === updated.id ? updated : g));
            } else {
                const created = await groupsApi.create(formData);
                setGroups(prev => [...prev, created]);
            }
            setShowGroupModal(false);
            setEditingGroup(null);
        } catch (err) {
            console.error('Failed to save group:', err);
        } finally {
            setSavingGroup(false);
        }
    };

    const handleDeleteGroup = async (groupId: number) => {
        try {
            await groupsApi.delete(groupId);
            setGroups(prev => prev.filter(g => g.id !== groupId));
        } catch (err) {
            console.error('Failed to delete group:', err);
        }
    };

    const handleToggleGroupCollapse = (groupId: number) => {
        setCollapsedGroups(prev => {
            const next = new Set(prev);
            if (next.has(groupId)) next.delete(groupId);
            else next.add(groupId);
            return next;
        });
    };

    const handleAddToGroup = async (reportId: number, groupId: number) => {
        const group = groups.find(g => g.id === groupId);
        if (!group) return;
        // Remove from any other group first, then add to target
        const otherGroups = groups.filter(g => g.id !== groupId && g.report_ids.includes(reportId));
        for (const og of otherGroups) {
            const newIds = og.report_ids.filter(id => id !== reportId);
            try {
                await groupsApi.setReports(og.id, newIds);
                setGroups(prev => prev.map(g => g.id === og.id ? { ...g, report_ids: newIds, report_count: newIds.length } : g));
            } catch (err) { console.error('Failed to remove from group:', err); }
        }
        const newIds = [...group.report_ids.filter(id => id !== reportId), reportId];
        try {
            await groupsApi.setReports(groupId, newIds);
            setGroups(prev => prev.map(g => g.id === groupId ? { ...g, report_ids: newIds, report_count: newIds.length } : g));
        } catch (err) { console.error('Failed to add to group:', err); }
        closeContextMenu();
    };

    const handleRemoveFromGroup = async (reportId: number) => {
        const group = groups.find(g => g.report_ids.includes(reportId));
        if (!group) return;
        const newIds = group.report_ids.filter(id => id !== reportId);
        try {
            await groupsApi.setReports(group.id, newIds);
            setGroups(prev => prev.map(g => g.id === group.id ? { ...g, report_ids: newIds, report_count: newIds.length } : g));
        } catch (err) { console.error('Failed to remove from group:', err); }
        closeContextMenu();
    };

    // Which group does a report belong to?
    const getReportGroup = useCallback((reportId: number): ReportGroup | null => {
        return groups.find(g => g.report_ids.includes(reportId)) || null;
    }, [groups]);

    const filteredReports = useMemo(() => {
        return reports.filter(report => {
            if (!searchQuery.trim()) return true;
            const query = searchQuery.toLowerCase();
            const name = (report.display_name || report.report_name).toLowerCase();
            const path = report.path?.toLowerCase() || '';
            return name.includes(query) || path.includes(query);
        });
    }, [reports, searchQuery]);

    // Split filtered reports into grouped and ungrouped
    const { groupedSections, ungroupedReports } = useMemo(() => {
        const allGroupedIds = new Set(groups.flatMap(g => g.report_ids));
        const ungrouped = filteredReports.filter(r => !allGroupedIds.has(r.id));

        const sections = groups.map(group => ({
            group,
            reports: filteredReports.filter(r => group.report_ids.includes(r.id)),
        })).filter(s => s.reports.length > 0 || !searchQuery.trim());

        return { groupedSections: sections, ungroupedReports: ungrouped };
    }, [filteredReports, groups, searchQuery]);

    // Color accents for report cards
    const cardAccents = [
        'from-primary-500 to-primary-600',
        'from-violet-500 to-purple-600',
        'from-emerald-500 to-teal-600',
        'from-amber-500 to-orange-600',
        'from-rose-500 to-pink-600',
        'from-cyan-500 to-blue-600',
    ];

    // ── Render helpers ──
    const renderTileCard = (report: Report, index: number) => (
        <Link
            key={report.id}
            to={`/reports/${report.id}`}
            className="card-interactive p-5 group relative overflow-hidden"
            onContextMenu={(e) => handleContextMenu(e, report)}
        >
            {/* Accent top border */}
            <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${cardAccents[index % cardAccents.length]} opacity-60 group-hover:opacity-100 transition-opacity`} />

            <div className="flex items-start justify-between">
                <div className={`w-11 h-11 bg-gradient-to-br ${cardAccents[index % cardAccents.length]} rounded-xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-300`}>
                    <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                </div>
                <div className="flex items-center gap-1.5">
                    {getReportGroup(report.id) && (
                        <span className={`text-[10px] px-1.5 py-0.5 rounded-md font-medium ${getGroupColorConfig(getReportGroup(report.id)!.color).badge}`}>
                            {getReportGroup(report.id)!.name}
                        </span>
                    )}
                    {report.is_public && (
                        <span className="badge badge-success">Public</span>
                    )}
                </div>
            </div>

            <h3 className="mt-4 text-base font-bold group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors truncate">
                {report.display_name || report.report_name}
            </h3>

            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400 truncate">
                {report.repo_name}
            </p>

            <div className="mt-3 flex items-center text-xs text-gray-400">
                <svg className="w-3.5 h-3.5 mr-1.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                </svg>
                <span className="truncate">{report.path}</span>
            </div>
        </Link>
    );

    const renderListRow = (report: Report, index: number) => (
        <tr
            key={report.id}
            onClick={() => navigate(`/reports/${report.id}`)}
            onContextMenu={(e) => handleContextMenu(e, report)}
            className="hover:bg-primary-50/50 dark:hover:bg-primary-900/10 transition-colors cursor-pointer"
        >
            <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 bg-gradient-to-br ${cardAccents[index % cardAccents.length]} rounded-lg flex items-center justify-center`}>
                        <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                    </div>
                    <span className="font-semibold text-sm">{report.display_name || report.report_name}</span>
                </div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{report.repo_name}</td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{report.path}</td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {getReportGroup(report.id) ? (
                    <span className={`text-xs px-2 py-0.5 rounded-md font-medium ${getGroupColorConfig(getReportGroup(report.id)!.color).badge}`}>
                        {getReportGroup(report.id)!.name}
                    </span>
                ) : '—'}
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {new Date(report.created_at).toLocaleDateString()}
            </td>
        </tr>
    );

    const renderGroupHeader = (group: ReportGroup, reportCount: number) => {
        const colorCfg = getGroupColorConfig(group.color);
        return (
            <div className={`flex items-center justify-between px-4 py-2.5 rounded-xl border ${colorCfg.border} bg-gradient-to-r ${colorCfg.banner} mb-3 transition-all`}>
                <button
                    onClick={() => handleToggleGroupCollapse(group.id)}
                    className="flex items-center gap-2.5 flex-1 min-w-0"
                >
                    <svg
                        className={`w-4 h-4 ${colorCfg.text} transition-transform duration-200 ${collapsedGroups.has(group.id) ? '-rotate-90' : ''}`}
                        fill="none" stroke="currentColor" viewBox="0 0 24 24"
                    >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
                    </svg>
                    <div className={`w-3 h-3 rounded-full ${colorCfg.bg} flex-shrink-0`} />
                    <span className={`text-sm font-bold ${colorCfg.text} truncate`}>
                        {group.name}
                    </span>
                    <span className={`text-xs px-1.5 py-0.5 rounded-md font-medium ${colorCfg.badge} flex-shrink-0`}>
                        {reportCount}
                    </span>
                </button>
                <div className="flex items-center gap-1 ml-2 flex-shrink-0">
                    <button
                        onClick={() => { setEditingGroup(group); setShowGroupModal(true); }}
                        className="p-1.5 rounded-lg hover:bg-white/10 dark:hover:bg-white/5 transition-colors"
                        title="Edit group"
                    >
                        <svg className={`w-3.5 h-3.5 ${colorCfg.text}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                    </button>
                    <button
                        onClick={() => {
                            if (confirm(`Delete group "${group.name}"? Reports will become ungrouped.`)) {
                                handleDeleteGroup(group.id);
                            }
                        }}
                        className="p-1.5 rounded-lg hover:bg-red-500/10 transition-colors"
                        title="Delete group"
                    >
                        <svg className="w-3.5 h-3.5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                    </button>
                </div>
            </div>
        );
    };

    if (loading) {
        return (
            <div className="fade-in">
                <div className="mb-8">
                    <div className="skeleton h-8 w-64 mb-2" />
                    <div className="skeleton h-4 w-40" />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    {[1, 2, 3, 4, 5, 6].map((i) => (
                        <div key={i} className="skeleton h-44 rounded-2xl" />
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="fade-in">
            {/* Header */}
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                    Welcome back, <span className="bg-gradient-to-r from-primary-600 to-primary-400 bg-clip-text text-transparent">{user?.display_name || user?.username}</span>
                </h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">Manage and view your reports</p>
            </div>

            {/* Stats row */}
            {reports.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
                    <div className="stat-card">
                        <div className="stat-card-icon bg-primary-100 dark:bg-primary-900/30">
                            <svg className="w-6 h-6 text-primary-600 dark:text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        </div>
                        <div>
                            <p className="text-2xl font-bold">{reports.length}</p>
                            <p className="text-xs text-gray-500 dark:text-gray-400">Total Reports</p>
                        </div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-card-icon bg-violet-100 dark:bg-violet-900/30">
                            <svg className="w-6 h-6 text-violet-600 dark:text-violet-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                            </svg>
                        </div>
                        <div>
                            <p className="text-2xl font-bold">{new Set(reports.map(r => r.repo_name)).size}</p>
                            <p className="text-xs text-gray-500 dark:text-gray-400">Repositories</p>
                        </div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-card-icon bg-emerald-100 dark:bg-emerald-900/30">
                            <svg className="w-6 h-6 text-emerald-600 dark:text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                            </svg>
                        </div>
                        <div>
                            <p className="text-2xl font-bold">{reports.filter(r => r.is_public).length}</p>
                            <p className="text-xs text-gray-500 dark:text-gray-400">Public</p>
                        </div>
                    </div>
                </div>
            )}

            {/* Toolbar */}
            <div className="flex items-center justify-between mb-6">
                <div className="relative flex-1 max-w-md">
                    <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <input
                        type="text"
                        placeholder="Search reports..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="input input-icon"
                    />
                </div>
                <div className="flex items-center gap-2 ml-4">
                    {/* Create Group button */}
                    {reports.length > 0 && (
                        <button
                            onClick={() => { setEditingGroup(null); setShowGroupModal(true); }}
                            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm font-medium bg-violet-50 dark:bg-violet-900/20 text-violet-700 dark:text-violet-300 hover:bg-violet-100 dark:hover:bg-violet-900/40 border border-violet-200 dark:border-violet-800/50 transition-all"
                            title="Create a report group"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                            </svg>
                            <svg className="w-3 h-3 -ml-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M12 4v16m8-8H4" />
                            </svg>
                            <span className="hidden sm:inline">Group</span>
                        </button>
                    )}

                    {/* View toggle */}
                    <div className="flex items-center bg-gray-100 dark:bg-gray-800 rounded-xl p-1 gap-0.5">
                        <button
                            onClick={() => setViewMode('tile')}
                            className={`p-2 rounded-lg transition-all ${viewMode === 'tile'
                                ? 'bg-white dark:bg-gray-700 shadow-sm text-primary-600'
                                : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                            }`}
                            aria-label="Tile view"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                            </svg>
                        </button>
                        <button
                            onClick={() => setViewMode('list')}
                            className={`p-2 rounded-lg transition-all ${viewMode === 'list'
                                ? 'bg-white dark:bg-gray-700 shadow-sm text-primary-600'
                                : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                            }`}
                            aria-label="List view"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>

            {reports.length === 0 ? (
                <div className="text-center py-20 card">
                    <div className="w-20 h-20 bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-700 dark:to-gray-800 rounded-full flex items-center justify-center mx-auto mb-5">
                        <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                    </div>
                    <h2 className="text-xl font-bold mb-2">No reports yet</h2>
                    <p className="text-gray-500 dark:text-gray-400 mb-6 max-w-sm mx-auto">
                        Connect a GitHub repository to import your JRXML Reports.
                    </p>
                    {!githubConnected ? (
                        <button onClick={handleConnectGitHub} disabled={connectingGithub} className="btn-primary inline-flex items-center gap-2">
                            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                                <path fillRule="evenodd" d="M12 2C6.477 2 2 6.477 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0012 2z" clipRule="evenodd" />
                            </svg>
                            {connectingGithub ? 'Connecting...' : 'Connect GitHub'}
                        </button>
                    ) : (
                        <button onClick={handleSelectRepository} disabled={reposLoading} className="btn-primary inline-flex items-center gap-2">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                            </svg>
                            {reposLoading ? 'Loading...' : 'Select Repository'}
                        </button>
                    )}
                </div>
            ) : (
                <div className="space-y-6">
                    {/* ── Grouped Sections ── */}
                    {groupedSections.map(({ group, reports: groupReports }) => (
                        <div key={group.id}>
                            {renderGroupHeader(group, groupReports.length)}

                            {!collapsedGroups.has(group.id) && groupReports.length > 0 && (
                                viewMode === 'tile' ? (
                                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 pl-3">
                                        {groupReports.map((report, index) => renderTileCard(report, index))}
                                    </div>
                                ) : (
                                    <div className="card overflow-hidden ml-3">
                                        <table className="w-full">
                                            <thead className="bg-gray-50 dark:bg-gray-800/50">
                                                <tr>
                                                    <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Report</th>
                                                    <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Repository</th>
                                                    <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Path</th>
                                                    <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Group</th>
                                                    <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Created</th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                                                {groupReports.map((report, index) => renderListRow(report, index))}
                                            </tbody>
                                        </table>
                                    </div>
                                )
                            )}

                            {!collapsedGroups.has(group.id) && groupReports.length === 0 && (
                                <div className="text-center py-6 text-sm text-gray-400 dark:text-gray-500 italic ml-3">
                                    No reports in this group. Right-click a report to add it.
                                </div>
                            )}
                        </div>
                    ))}

                    {/* ── Ungrouped ── */}
                    {ungroupedReports.length > 0 && (
                        <div>
                            {groups.length > 0 && (
                                <div className="flex items-center gap-2 mb-3 px-1">
                                    <div className="w-3 h-3 rounded-full bg-gray-400 dark:bg-gray-500" />
                                    <span className="text-sm font-bold text-gray-500 dark:text-gray-400">Ungrouped</span>
                                    <span className="text-xs px-1.5 py-0.5 rounded-md bg-gray-100 dark:bg-gray-800 text-gray-500 font-medium">
                                        {ungroupedReports.length}
                                    </span>
                                </div>
                            )}

                            {viewMode === 'tile' ? (
                                <div className={`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 ${groups.length > 0 ? 'pl-3' : ''}`}>
                                    {ungroupedReports.map((report, index) => renderTileCard(report, index))}
                                </div>
                            ) : (
                                <div className={`card overflow-hidden ${groups.length > 0 ? 'ml-3' : ''}`}>
                                    <table className="w-full">
                                        <thead className="bg-gray-50 dark:bg-gray-800/50">
                                            <tr>
                                                <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Report</th>
                                                <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Repository</th>
                                                <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Path</th>
                                                <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Group</th>
                                                <th className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Created</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                                            {ungroupedReports.map((report, index) => renderListRow(report, index))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}

            {/* Repository Selection Modal */}
            <Modal isOpen={showRepoModal} onClose={() => setShowRepoModal(false)}>
                <h2 className="text-xl font-bold mb-4">Select Repository</h2>
                <div className="mb-4">
                    <label className="block text-sm font-semibold mb-2">Reports Path Prefix</label>
                    <input
                        type="text"
                        value={pathPrefix}
                        onChange={(e) => setPathPrefix(e.target.value)}
                        className="input"
                        placeholder="Leave empty for repo root"
                    />
                    <p className="text-xs text-gray-500 mt-1.5">Folder in your repository where reports are stored</p>
                </div>
                <div className="max-h-64 overflow-y-auto space-y-2 mb-4">
                    {repos.map((repo) => (
                        <button
                            key={repo.id}
                            onClick={() => setSelectedRepo(repo)}
                            className={`w-full text-left p-4 rounded-xl border transition-all ${selectedRepo?.id === repo.id
                                ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 shadow-glow-sm'
                                : 'border-gray-200 dark:border-gray-700 hover:border-primary-300'
                            }`}
                        >
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="font-semibold">{repo.full_name}</p>
                                    <p className="text-sm text-gray-500">{repo.default_branch}</p>
                                </div>
                                {repo.private && <span className="badge badge-warning">Private</span>}
                            </div>
                        </button>
                    ))}
                </div>
                <div className="flex gap-3">
                    <button onClick={() => setShowRepoModal(false)} className="btn-secondary">Cancel</button>
                    <button onClick={handleConnectRepo} disabled={!selectedRepo || reposLoading} className="btn-primary flex-1">
                        {reposLoading ? 'Connecting...' : 'Connect Repository'}
                    </button>
                </div>
            </Modal>

            {/* Context Menu */}
            {contextMenu && (
                <div className="context-menu" style={{ top: contextMenu.y, left: contextMenu.x }} onClick={e => e.stopPropagation()}>
                    <button
                        className="context-menu-item"
                        onClick={() => { navigate(`/reports/${contextMenu.report.id}`); setContextMenu(null); }}
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                        Open Report
                    </button>
                    <div className="context-menu-divider" />
                    <button
                        className="context-menu-item context-menu-item-accent"
                        onClick={() => { setScheduleReport(contextMenu.report); setContextMenu(null); }}
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        Schedule Report
                    </button>

                    {/* ── Group submenu ── */}
                    <div className="context-menu-divider" />

                    {/* Add to Group */}
                    <div className="relative">
                        <button
                            className="context-menu-item"
                            onClick={(e) => { e.stopPropagation(); setShowGroupSubmenu(!showGroupSubmenu); }}
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                            </svg>
                            Add to Group
                            <svg className="w-3 h-3 ml-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                            </svg>
                        </button>

                        {showGroupSubmenu && (
                            <div
                                className="absolute left-full top-0 ml-1 w-48 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-xl py-1 z-50"
                                onClick={(e) => e.stopPropagation()}
                            >
                                {groups.length > 0 ? (
                                    groups.map(g => {
                                        const isInThisGroup = g.report_ids.includes(contextMenu.report.id);
                                        const colorCfg = getGroupColorConfig(g.color);
                                        return (
                                            <button
                                                key={g.id}
                                                className={`w-full text-left px-3 py-2 text-sm flex items-center gap-2 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors ${isInThisGroup ? 'opacity-50' : ''}`}
                                                disabled={isInThisGroup}
                                                onClick={() => handleAddToGroup(contextMenu.report.id, g.id)}
                                            >
                                                <div className={`w-2.5 h-2.5 rounded-full ${colorCfg.bg}`} />
                                                <span className="truncate">{g.name}</span>
                                                {isInThisGroup && <span className="text-[10px] ml-auto text-gray-400">current</span>}
                                            </button>
                                        );
                                    })
                                ) : (
                                    <div className="px-3 py-2 text-xs text-gray-400 italic">No groups yet</div>
                                )}
                                <div className="border-t border-gray-200 dark:border-gray-700 my-1" />
                                <button
                                    className="w-full text-left px-3 py-2 text-sm flex items-center gap-2 text-violet-600 dark:text-violet-400 hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors"
                                    onClick={() => {
                                        closeContextMenu();
                                        setEditingGroup(null);
                                        setShowGroupModal(true);
                                    }}
                                >
                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                    </svg>
                                    Create New Group
                                </button>
                            </div>
                        )}
                    </div>

                    {/* Remove from Group (only if report is in a group) */}
                    {getReportGroup(contextMenu.report.id) && (
                        <button
                            className="context-menu-item text-red-500 dark:text-red-400"
                            onClick={() => handleRemoveFromGroup(contextMenu.report.id)}
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                            </svg>
                            Remove from "{getReportGroup(contextMenu.report.id)!.name}"
                        </button>
                    )}
                </div>
            )}

            {/* Schedule Modal */}
            <ScheduleModal
                isOpen={!!scheduleReport}
                onClose={() => setScheduleReport(null)}
                reportName={scheduleReport?.display_name || scheduleReport?.report_name || ''}
                reportUri={scheduleReport?.path || ''}
                reportId={scheduleReport?.id}
            />

            {/* Group Create/Edit Modal */}
            <ReportGroupModal
                isOpen={showGroupModal}
                onClose={() => { setShowGroupModal(false); setEditingGroup(null); }}
                onSave={handleSaveGroup}
                editingGroup={editingGroup}
                saving={savingGroup}
            />
        </div>
    );
}
