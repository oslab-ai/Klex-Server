import { useState, useEffect, useMemo } from 'react';
import { reportsApi, organizationsApi, usersApi, groupsApi } from '../api';
import type { Report, Organization, User, OrgReportPermission, UserReportPermission, ReportGroup, GroupPermissions } from '../types';
import { getGroupColorConfig } from '../components/ReportGroupModal';
import Modal from '../components/Modal';

type TabKey = 'public' | 'organization' | 'user' | 'group';

export default function Permissions() {
    const [activeTab, setActiveTab] = useState<TabKey>('public');
    const [reports, setReports] = useState<Report[]>([]);
    const [organizations, setOrganizations] = useState<Organization[]>([]);
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');

    // Public tab
    const [togglingPublic, setTogglingPublic] = useState<number | null>(null);

    // Org tab
    const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null);
    const [orgPermissions, setOrgPermissions] = useState<OrgReportPermission[]>([]);
    const [loadingOrgPerms, setLoadingOrgPerms] = useState(false);
    const [savingOrgPerms, setSavingOrgPerms] = useState(false);
    const [orgMembers, setOrgMembers] = useState<User[]>([]);

    // User tab
    const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
    const [userPermissions, setUserPermissions] = useState<UserReportPermission[]>([]);
    const [loadingUserPerms, setLoadingUserPerms] = useState(false);
    const [savingUserPerms, setSavingUserPerms] = useState(false);

    // Org CRUD modal
    const [showOrgModal, setShowOrgModal] = useState(false);
    const [orgName, setOrgName] = useState('');
    const [orgDescription, setOrgDescription] = useState('');
    const [orgFormError, setOrgFormError] = useState('');
    const [savingOrg, setSavingOrg] = useState(false);

    // Success toast
    const [toast, setToast] = useState('');

    // Group tab
    const [groups, setGroups] = useState<ReportGroup[]>([]);
    const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
    const [groupPermissions, setGroupPermissions] = useState<GroupPermissions | null>(null);
    const [loadingGroupPerms, setLoadingGroupPerms] = useState(false);
    const [savingGroupPerms, setSavingGroupPerms] = useState(false);

    useEffect(() => {
        loadInitialData();
    }, []);

    const loadInitialData = async () => {
        try {
            const [reportsData, orgsData, usersData, groupsData] = await Promise.all([
                reportsApi.list(),
                organizationsApi.list(),
                usersApi.list(),
                groupsApi.list(),
            ]);
            setReports(reportsData);
            setOrganizations(orgsData);
            setUsers(usersData);
            setGroups(groupsData);
        } catch (err) {
            console.error('Failed to load data:', err);
        } finally {
            setLoading(false);
        }
    };

    const showToast = (msg: string) => {
        setToast(msg);
        setTimeout(() => setToast(''), 3000);
    };

    // ─── Public Tab ──────────────────────────────────────
    const handleTogglePublic = async (report: Report) => {
        setTogglingPublic(report.id);
        try {
            await reportsApi.togglePublic(report.id, !report.is_public);
            setReports(prev => prev.map(r =>
                r.id === report.id ? { ...r, is_public: !r.is_public } : r
            ));
            showToast(`"${report.display_name || report.report_name}" is now ${!report.is_public ? 'public' : 'private'}`);
        } catch (err) {
            console.error('Failed to toggle public:', err);
        } finally {
            setTogglingPublic(null);
        }
    };

    // ─── Organization Tab ────────────────────────────────
    const handleSelectOrg = async (orgId: number) => {
        setSelectedOrgId(orgId);
        setLoadingOrgPerms(true);
        try {
            const [permsData, membersData] = await Promise.all([
                organizationsApi.getPermissions(orgId),
                organizationsApi.getMembers(orgId),
            ]);
            setOrgPermissions(permsData);
            setOrgMembers(membersData);
        } catch (err) {
            console.error('Failed to load org permissions:', err);
            setOrgPermissions([]);
            setOrgMembers([]);
        } finally {
            setLoadingOrgPerms(false);
        }
    };

    const handleToggleOrgAccess = (reportId: number) => {
        setOrgPermissions(prev => prev.map(p =>
            p.report_id === reportId ? { ...p, can_access: !p.can_access } : p
        ));
    };

    const handleSelectAllOrg = () => setOrgPermissions(prev => prev.map(p => ({ ...p, can_access: true })));
    const handleDeselectAllOrg = () => setOrgPermissions(prev => prev.map(p => ({ ...p, can_access: false })));

    const handleSaveOrgPermissions = async () => {
        if (!selectedOrgId) return;
        setSavingOrgPerms(true);
        try {
            await organizationsApi.updatePermissions(selectedOrgId, orgPermissions);
            showToast('Organization permissions saved');
        } catch (err) {
            console.error('Failed to save org permissions:', err);
        } finally {
            setSavingOrgPerms(false);
        }
    };

    const handleCreateOrg = async (e: React.FormEvent) => {
        e.preventDefault();
        setOrgFormError('');
        setSavingOrg(true);
        try {
            const newOrg = await organizationsApi.create({ name: orgName, description: orgDescription });
            setOrganizations(prev => [...prev, newOrg]);
            setShowOrgModal(false);
            setOrgName('');
            setOrgDescription('');
            showToast(`Organization "${newOrg.name}" created`);
        } catch (err: unknown) {
            const error = err as { response?: { data?: { name?: string[]; error?: string } } };
            setOrgFormError(error.response?.data?.name?.[0] || error.response?.data?.error || 'Failed to create organization');
        } finally {
            setSavingOrg(false);
        }
    };

    const handleDeleteOrg = async (org: Organization) => {
        if (!confirm(`Delete organization "${org.name}"? This will remove all org-level permissions.`)) return;
        try {
            await organizationsApi.delete(org.id);
            setOrganizations(prev => prev.filter(o => o.id !== org.id));
            if (selectedOrgId === org.id) {
                setSelectedOrgId(null);
                setOrgPermissions([]);
                setOrgMembers([]);
            }
            showToast(`Organization "${org.name}" deleted`);
        } catch (err) {
            console.error('Failed to delete org:', err);
        }
    };

    // ─── User Tab ────────────────────────────────────────
    const handleSelectUser = async (userId: string) => {
        setSelectedUserId(userId);
        setLoadingUserPerms(true);
        try {
            const perms = await usersApi.getPermissions(userId);
            setUserPermissions(perms);
        } catch (err) {
            console.error('Failed to load user permissions:', err);
            setUserPermissions([]);
        } finally {
            setLoadingUserPerms(false);
        }
    };

    const handleToggleUserAccess = (reportId: number) => {
        setUserPermissions(prev => prev.map(p =>
            p.report_id === reportId ? { ...p, can_access: !p.can_access } : p
        ));
    };

    const handleSelectAllUser = () => setUserPermissions(prev => prev.map(p => ({ ...p, can_access: true })));
    const handleDeselectAllUser = () => setUserPermissions(prev => prev.map(p => ({ ...p, can_access: false })));

    const handleSaveUserPermissions = async () => {
        if (!selectedUserId) return;
        setSavingUserPerms(true);
        try {
            await usersApi.updatePermissions(selectedUserId, userPermissions);
            showToast('User permissions saved');
        } catch (err) {
            console.error('Failed to save user permissions:', err);
        } finally {
            setSavingUserPerms(false);
        }
    };

    // ─── Filtered data ───────────────────────────────────
    const filteredReports = useMemo(() => {
        if (!search.trim()) return reports;
        const s = search.toLowerCase();
        return reports.filter(r =>
            (r.display_name || r.report_name).toLowerCase().includes(s) ||
            r.path.toLowerCase().includes(s)
        );
    }, [reports, search]);

    const filteredOrgPerms = useMemo(() => {
        if (!search.trim()) return orgPermissions;
        const s = search.toLowerCase();
        return orgPermissions.filter(p =>
            p.report_name.toLowerCase().includes(s) || p.path.toLowerCase().includes(s)
        );
    }, [orgPermissions, search]);

    const filteredUserPerms = useMemo(() => {
        if (!search.trim()) return userPermissions;
        const s = search.toLowerCase();
        return userPermissions.filter(p =>
            p.report_name.toLowerCase().includes(s) || p.path.toLowerCase().includes(s)
        );
    }, [userPermissions, search]);

    const nonAdminUsers = useMemo(() => users.filter(u => !u.is_admin), [users]);

    const selectedUser = useMemo(
        () => users.find(u => u.id === selectedUserId),
        [users, selectedUserId]
    );

    const selectedOrg = useMemo(
        () => organizations.find(o => o.id === selectedOrgId),
        [organizations, selectedOrgId]
    );

    const selectedGroup = useMemo(
        () => groups.find(g => g.id === selectedGroupId),
        [groups, selectedGroupId]
    );

    // ─── Group Tab ──────────────────────────────────────
    const handleSelectGroup = async (groupId: number) => {
        setSelectedGroupId(groupId);
        setLoadingGroupPerms(true);
        try {
            const perms = await groupsApi.getPermissions(groupId);
            setGroupPermissions(perms);
        } catch (err) {
            console.error('Failed to load group permissions:', err);
            setGroupPermissions(null);
        } finally {
            setLoadingGroupPerms(false);
        }
    };

    const handleToggleGroupUserAccess = (userId: string) => {
        if (!groupPermissions) return;
        setGroupPermissions({
            ...groupPermissions,
            users: groupPermissions.users.map(u =>
                u.user_id === userId ? { ...u, can_access: !u.can_access } : u
            ),
        });
    };

    const handleToggleGroupOrgAccess = (orgId: number) => {
        if (!groupPermissions) return;
        setGroupPermissions({
            ...groupPermissions,
            organizations: groupPermissions.organizations.map(o =>
                o.organization_id === orgId ? { ...o, can_access: !o.can_access } : o
            ),
        });
    };

    const handleSelectAllGroupUsers = () => {
        if (!groupPermissions) return;
        setGroupPermissions({
            ...groupPermissions,
            users: groupPermissions.users.map(u => ({ ...u, can_access: true })),
        });
    };

    const handleDeselectAllGroupUsers = () => {
        if (!groupPermissions) return;
        setGroupPermissions({
            ...groupPermissions,
            users: groupPermissions.users.map(u => ({ ...u, can_access: false })),
        });
    };

    const handleSelectAllGroupOrgs = () => {
        if (!groupPermissions) return;
        setGroupPermissions({
            ...groupPermissions,
            organizations: groupPermissions.organizations.map(o => ({ ...o, can_access: true })),
        });
    };

    const handleDeselectAllGroupOrgs = () => {
        if (!groupPermissions) return;
        setGroupPermissions({
            ...groupPermissions,
            organizations: groupPermissions.organizations.map(o => ({ ...o, can_access: false })),
        });
    };

    const handleSaveGroupPermissions = async () => {
        if (!selectedGroupId || !groupPermissions) return;
        setSavingGroupPerms(true);
        try {
            await groupsApi.updatePermissions(selectedGroupId, {
                users: groupPermissions.users,
                organizations: groupPermissions.organizations,
            });
            showToast('Group permissions saved');
        } catch (err) {
            console.error('Failed to save group permissions:', err);
        } finally {
            setSavingGroupPerms(false);
        }
    };

    const filteredGroupUsers = useMemo(() => {
        if (!groupPermissions || !search.trim()) return groupPermissions?.users || [];
        const s = search.toLowerCase();
        return groupPermissions.users.filter(u =>
            u.username.toLowerCase().includes(s) ||
            (u.display_name || '').toLowerCase().includes(s)
        );
    }, [groupPermissions, search]);

    const filteredGroupOrgs = useMemo(() => {
        if (!groupPermissions || !search.trim()) return groupPermissions?.organizations || [];
        const s = search.toLowerCase();
        return groupPermissions.organizations.filter(o =>
            o.name.toLowerCase().includes(s)
        );
    }, [groupPermissions, search]);

    // ─── Tab config ──────────────────────────────────────
    const tabs: { key: TabKey; label: string; icon: JSX.Element; desc: string }[] = [
        {
            key: 'public',
            label: 'Public Reports',
            desc: 'Visible to all authenticated users',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
            ),
        },
        {
            key: 'organization',
            label: 'Organization',
            desc: 'Access for all org members',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                </svg>
            ),
        },
        {
            key: 'user',
            label: 'User',
            desc: 'Individual user access',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
            ),
        },
        {
            key: 'group',
            label: 'Group',
            desc: 'Bulk group access',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                </svg>
            ),
        },
    ];

    if (loading) {
        return (
            <div className="fade-in">
                <div className="flex items-center justify-between mb-6">
                    <h1 className="text-2xl font-bold">Permissions Management</h1>
                </div>
                <div className="card">
                    <div className="skeleton h-96 rounded-xl" />
                </div>
            </div>
        );
    }

    return (
        <div className="fade-in">
            {/* Toast */}
            {toast && (
                <div className="fixed top-4 right-4 z-50 px-4 py-3 rounded-lg bg-green-600 text-white shadow-lg fade-in flex items-center gap-2">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    {toast}
                </div>
            )}

            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold">Permissions Management</h1>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        Manage report access across four levels: Public, Organization, Group, and User
                    </p>
                </div>
            </div>

            {/* Permission Hierarchy Visual */}
            <div className="card p-4 mb-6">
                <div className="flex items-center gap-6 text-sm">
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-green-500"></div>
                        <span className="text-gray-600 dark:text-gray-300">Public — Everyone</span>
                    </div>
                    <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                        <span className="text-gray-600 dark:text-gray-300">Organization — Org Members</span>
                    </div>
                    <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-amber-500"></div>
                        <span className="text-gray-600 dark:text-gray-300">Group — All reports in group</span>
                    </div>
                    <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-purple-500"></div>
                        <span className="text-gray-600 dark:text-gray-300">User — Individual</span>
                    </div>
                </div>
            </div>

            {/* Tabs */}
            <div className="flex space-x-1 bg-gray-100 dark:bg-gray-800 rounded-xl p-1 mb-6">
                {tabs.map(tab => (
                    <button
                        key={tab.key}
                        onClick={() => { setActiveTab(tab.key); setSearch(''); }}
                        className={`flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-lg text-sm font-medium transition-all duration-200 ${activeTab === tab.key
                            ? 'bg-white dark:bg-gray-700 shadow-md text-primary-600 dark:text-primary-400'
                            : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-200/50 dark:hover:bg-gray-700/50'
                            }`}
                    >
                        {tab.icon}
                        <div className="text-left">
                            <div>{tab.label}</div>
                            <div className="text-xs opacity-60 hidden sm:block">{tab.desc}</div>
                        </div>
                    </button>
                ))}
            </div>

            {/* Search bar */}
            <div className="mb-4">
                <div className="relative">
                    <svg className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <input
                        type="text"
                        placeholder="Search reports..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        className="input input-icon"
                    />
                </div>
            </div>

            {/* ─── Public Tab Content ─── */}
            {activeTab === 'public' && (
                <div className="card overflow-hidden">
                    <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                        <div className="flex items-center justify-between">
                            <div>
                                <h3 className="font-semibold">Public Visibility</h3>
                                <p className="text-sm text-gray-500">Toggle whether each report is visible to all authenticated users</p>
                            </div>
                            <div className="flex items-center gap-3">
                                <div className="flex items-center gap-1.5">
                                    <button
                                        onClick={async () => { for (const r of reports.filter(r => !r.is_public)) { await reportsApi.togglePublic(r.id, true); } setReports(prev => prev.map(r => ({ ...r, is_public: true }))); showToast('All reports made public'); }}
                                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-green-50 text-green-700 hover:bg-green-100 dark:bg-green-900/30 dark:text-green-300 dark:hover:bg-green-900/50 transition-colors"
                                    >
                                        Select All
                                    </button>
                                    <button
                                        onClick={async () => { for (const r of reports.filter(r => r.is_public)) { await reportsApi.togglePublic(r.id, false); } setReports(prev => prev.map(r => ({ ...r, is_public: false }))); showToast('All reports made private'); }}
                                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50 transition-colors"
                                    >
                                        Deselect All
                                    </button>
                                </div>
                                <span className="text-sm text-gray-500">
                                    {reports.filter(r => r.is_public).length} of {reports.length} public
                                </span>
                            </div>
                        </div>
                    </div>
                    <div className="divide-y divide-gray-200 dark:divide-gray-700">
                        {filteredReports.length === 0 ? (
                            <div className="py-12 text-center text-gray-500">No reports found</div>
                        ) : (
                            filteredReports.map(report => (
                                <div
                                    key={report.id}
                                    className="flex items-center px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-colors"
                                >
                                    <div className="flex-1 min-w-0">
                                        <div className="font-medium truncate">{report.display_name || report.report_name}</div>
                                        <div className="text-sm text-gray-500 truncate">{report.path}</div>
                                    </div>
                                    <div className="flex items-center gap-3 ml-4">
                                        <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${report.is_public
                                            ? 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300'
                                            : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                                            }`}>
                                            {report.is_public ? 'Public' : 'Private'}
                                        </span>
                                        <button
                                            onClick={() => handleTogglePublic(report)}
                                            disabled={togglingPublic === report.id}
                                            className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 ${report.is_public ? 'bg-green-500' : 'bg-gray-300 dark:bg-gray-600'
                                                } ${togglingPublic === report.id ? 'opacity-50' : ''}`}
                                        >
                                            <span
                                                className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${report.is_public ? 'translate-x-5' : 'translate-x-0'
                                                    }`}
                                            />
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}

            {/* ─── Organization Tab Content ─── */}
            {activeTab === 'organization' && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Org list */}
                    <div className="lg:col-span-1">
                        <div className="card overflow-hidden">
                            <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50 flex items-center justify-between">
                                <h3 className="font-semibold text-sm">Organizations</h3>
                                <button
                                    onClick={() => setShowOrgModal(true)}
                                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-primary-50 text-primary-700 hover:bg-primary-100 dark:bg-primary-900/30 dark:text-primary-300 dark:hover:bg-primary-900/50 transition-colors"
                                >
                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                    </svg>
                                    New
                                </button>
                            </div>
                            <div className="divide-y divide-gray-200 dark:divide-gray-700 max-h-96 overflow-y-auto">
                                {organizations.length === 0 ? (
                                    <div className="py-8 px-4 text-center text-gray-500 text-sm">
                                        No organizations yet
                                    </div>
                                ) : (
                                    organizations.map(org => (
                                        <div
                                            key={org.id}
                                            className={`flex items-center px-4 py-3 cursor-pointer transition-colors group ${selectedOrgId === org.id
                                                ? 'bg-primary-50 dark:bg-primary-900/20 border-l-3 border-l-primary-500'
                                                : 'hover:bg-gray-50 dark:hover:bg-gray-800/30'
                                                }`}
                                            onClick={() => handleSelectOrg(org.id)}
                                        >
                                            <div className="w-9 h-9 bg-gradient-to-br from-blue-400 to-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
                                                <span className="text-white text-sm font-bold">{org.name.charAt(0).toUpperCase()}</span>
                                            </div>
                                            <div className="ml-3 flex-1 min-w-0">
                                                <div className="font-medium text-sm truncate">{org.name}</div>
                                                <div className="text-xs text-gray-500">{org.member_count} member{org.member_count !== 1 ? 's' : ''}</div>
                                            </div>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleDeleteOrg(org); }}
                                                className="opacity-0 group-hover:opacity-100 p-1 rounded text-gray-400 hover:text-red-500 transition-all"
                                                title="Delete Organization"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                </svg>
                                            </button>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Org permissions */}
                    <div className="lg:col-span-2">
                        {!selectedOrgId ? (
                            <div className="card p-12 text-center">
                                <svg className="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                                </svg>
                                <p className="text-gray-500">Select an organization to manage its report permissions</p>
                            </div>
                        ) : (
                            <div className="card overflow-hidden">
                                <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <h3 className="font-semibold">{selectedOrg?.name} — Report Access</h3>
                                            <p className="text-sm text-gray-500">
                                                All {orgMembers.length} member{orgMembers.length !== 1 ? 's' : ''} will inherit these permissions
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <button
                                                onClick={handleSelectAllOrg}
                                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-green-50 text-green-700 hover:bg-green-100 dark:bg-green-900/30 dark:text-green-300 dark:hover:bg-green-900/50 transition-colors"
                                            >
                                                Select All
                                            </button>
                                            <button
                                                onClick={handleDeselectAllOrg}
                                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50 transition-colors"
                                            >
                                                Deselect All
                                            </button>
                                            <button
                                                onClick={handleSaveOrgPermissions}
                                                disabled={savingOrgPerms || loadingOrgPerms}
                                                className="btn-primary text-sm"
                                            >
                                                {savingOrgPerms ? 'Saving...' : 'Save'}
                                            </button>
                                        </div>
                                    </div>
                                    {/* Members preview */}
                                    {orgMembers.length > 0 && (
                                        <div className="flex items-center gap-1 mt-3">
                                            <span className="text-xs text-gray-500 mr-1">Members:</span>
                                            <div className="flex -space-x-2">
                                                {orgMembers.slice(0, 5).map(m => (
                                                    <div
                                                        key={m.id}
                                                        className="w-7 h-7 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center ring-2 ring-white dark:ring-gray-800"
                                                        title={m.display_name || m.username}
                                                    >
                                                        <span className="text-white text-xs font-medium">{(m.display_name || m.username).charAt(0).toUpperCase()}</span>
                                                    </div>
                                                ))}
                                            </div>
                                            {orgMembers.length > 5 && (
                                                <span className="text-xs text-gray-500 ml-2">+{orgMembers.length - 5} more</span>
                                            )}
                                        </div>
                                    )}
                                </div>

                                {loadingOrgPerms ? (
                                    <div className="py-12 text-center text-gray-500">Loading permissions...</div>
                                ) : (
                                    <div className="divide-y divide-gray-200 dark:divide-gray-700 max-h-[28rem] overflow-y-auto">
                                        {filteredOrgPerms.length === 0 ? (
                                            <div className="py-12 text-center text-gray-500">No reports found</div>
                                        ) : (
                                            filteredOrgPerms.map(perm => (
                                                <label
                                                    key={perm.report_id}
                                                    className="flex items-center px-6 py-3.5 hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer transition-colors"
                                                >
                                                    <input
                                                        type="checkbox"
                                                        checked={perm.can_access}
                                                        onChange={() => handleToggleOrgAccess(perm.report_id)}
                                                        className="w-5 h-5 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                                    />
                                                    <div className="ml-3 flex-1 min-w-0">
                                                        <div className="font-medium text-sm">{perm.report_name}</div>
                                                        <div className="text-xs text-gray-500">{perm.path}</div>
                                                    </div>
                                                    {perm.is_public && (
                                                        <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300 ml-2">
                                                            Public
                                                        </span>
                                                    )}
                                                </label>
                                            ))
                                        )}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* ─── User Tab Content ─── */}
            {activeTab === 'user' && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* User list */}
                    <div className="lg:col-span-1">
                        <div className="card overflow-hidden">
                            <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                                <h3 className="font-semibold text-sm">Users</h3>
                            </div>
                            <div className="divide-y divide-gray-200 dark:divide-gray-700 max-h-96 overflow-y-auto">
                                {nonAdminUsers.length === 0 ? (
                                    <div className="py-8 px-4 text-center text-gray-500 text-sm">
                                        No non-admin users
                                    </div>
                                ) : (
                                    nonAdminUsers.map(user => (
                                        <div
                                            key={user.id}
                                            className={`flex items-center px-4 py-3 cursor-pointer transition-colors ${selectedUserId === user.id
                                                ? 'bg-primary-50 dark:bg-primary-900/20 border-l-3 border-l-primary-500'
                                                : 'hover:bg-gray-50 dark:hover:bg-gray-800/30'
                                                }`}
                                            onClick={() => handleSelectUser(user.id)}
                                        >
                                            <div className="w-9 h-9 bg-gradient-to-br from-purple-400 to-purple-600 rounded-full flex items-center justify-center flex-shrink-0">
                                                <span className="text-white text-sm font-bold">{(user.display_name || user.username).charAt(0).toUpperCase()}</span>
                                            </div>
                                            <div className="ml-3 flex-1 min-w-0">
                                                <div className="font-medium text-sm truncate">{user.display_name || user.username}</div>
                                                <div className="text-xs text-gray-500 truncate">
                                                    {user.organization_name ? (
                                                        <span className="inline-flex items-center gap-1">
                                                            <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
                                                            {user.organization_name}
                                                        </span>
                                                    ) : (
                                                        <span className="text-gray-400">No organization</span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

                    {/* User permissions */}
                    <div className="lg:col-span-2">
                        {!selectedUserId ? (
                            <div className="card p-12 text-center">
                                <svg className="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                </svg>
                                <p className="text-gray-500">Select a user to manage their individual report permissions</p>
                            </div>
                        ) : (
                            <div className="card overflow-hidden">
                                <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <h3 className="font-semibold">{selectedUser?.display_name || selectedUser?.username} — Report Access</h3>
                                            <p className="text-sm text-gray-500">
                                                {selectedUser?.organization_name
                                                    ? `Org: ${selectedUser.organization_name} • Blue badge = inherited from org`
                                                    : 'Not in any organization'}
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <button
                                                onClick={handleSelectAllUser}
                                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-green-50 text-green-700 hover:bg-green-100 dark:bg-green-900/30 dark:text-green-300 dark:hover:bg-green-900/50 transition-colors"
                                            >
                                                Select All
                                            </button>
                                            <button
                                                onClick={handleDeselectAllUser}
                                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50 transition-colors"
                                            >
                                                Deselect All
                                            </button>
                                            <button
                                                onClick={handleSaveUserPermissions}
                                                disabled={savingUserPerms || loadingUserPerms}
                                                className="btn-primary text-sm"
                                            >
                                                {savingUserPerms ? 'Saving...' : 'Save'}
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                {loadingUserPerms ? (
                                    <div className="py-12 text-center text-gray-500">Loading permissions...</div>
                                ) : (
                                    <div className="divide-y divide-gray-200 dark:divide-gray-700 max-h-[28rem] overflow-y-auto">
                                        {filteredUserPerms.length === 0 ? (
                                            <div className="py-12 text-center text-gray-500">No reports found</div>
                                        ) : (
                                            filteredUserPerms.map(perm => (
                                                <label
                                                    key={perm.report_id}
                                                    className="flex items-center px-6 py-3.5 hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer transition-colors"
                                                >
                                                    <input
                                                        type="checkbox"
                                                        checked={perm.can_access}
                                                        onChange={() => handleToggleUserAccess(perm.report_id)}
                                                        className="w-5 h-5 rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                                                    />
                                                    <div className="ml-3 flex-1 min-w-0">
                                                        <div className="font-medium text-sm">{perm.report_name}</div>
                                                        <div className="text-xs text-gray-500">{perm.path}</div>
                                                    </div>
                                                    <div className="flex items-center gap-2 ml-2">
                                                        {perm.is_public && (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300">
                                                                Public
                                                            </span>
                                                        )}
                                                        {perm.org_can_access && (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300 flex items-center gap-1">
                                                                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
                                                                </svg>
                                                                Org
                                                            </span>
                                                        )}
                                                    </div>
                                                </label>
                                            ))
                                        )}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* ─── Group Tab Content ─── */}
            {activeTab === 'group' && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Group list */}
                    <div className="lg:col-span-1">
                        <div className="card overflow-hidden">
                            <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                                <h3 className="font-semibold text-sm">Report Groups</h3>
                            </div>
                            <div className="divide-y divide-gray-200 dark:divide-gray-700 max-h-96 overflow-y-auto">
                                {groups.length === 0 ? (
                                    <div className="py-8 px-4 text-center text-gray-500 text-sm">
                                        No groups yet. Create groups from the Reports page.
                                    </div>
                                ) : (
                                    groups.map(group => {
                                        const colorCfg = getGroupColorConfig(group.color);
                                        return (
                                            <div
                                                key={group.id}
                                                className={`flex items-center px-4 py-3 cursor-pointer transition-colors ${selectedGroupId === group.id
                                                    ? 'bg-primary-50 dark:bg-primary-900/20 border-l-3 border-l-primary-500'
                                                    : 'hover:bg-gray-50 dark:hover:bg-gray-800/30'
                                                    }`}
                                                onClick={() => handleSelectGroup(group.id)}
                                            >
                                                <div className={`w-9 h-9 ${colorCfg.bg} rounded-lg flex items-center justify-center flex-shrink-0`}>
                                                    <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                                    </svg>
                                                </div>
                                                <div className="ml-3 flex-1 min-w-0">
                                                    <div className="font-medium text-sm truncate">{group.name}</div>
                                                    <div className="text-xs text-gray-500">{group.report_count} report{group.report_count !== 1 ? 's' : ''}</div>
                                                </div>
                                            </div>
                                        );
                                    })
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Group permissions */}
                    <div className="lg:col-span-2">
                        {!selectedGroupId ? (
                            <div className="card p-12 text-center">
                                <svg className="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                </svg>
                                <p className="text-gray-500">Select a group to manage who can access all its reports</p>
                            </div>
                        ) : (
                            <div className="card overflow-hidden">
                                <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <div className="flex items-center gap-2">
                                                <div className={`w-3 h-3 rounded-full ${getGroupColorConfig(selectedGroup?.color || 'violet').bg}`} />
                                                <h3 className="font-semibold">{selectedGroup?.name} — Permissions</h3>
                                            </div>
                                            <p className="text-sm text-gray-500 mt-0.5">
                                                {selectedGroup?.report_count} report{selectedGroup?.report_count !== 1 ? 's' : ''} in this group
                                            </p>
                                        </div>
                                        <button
                                            onClick={handleSaveGroupPermissions}
                                            disabled={savingGroupPerms || loadingGroupPerms}
                                            className="btn-primary text-sm"
                                        >
                                            {savingGroupPerms ? 'Saving...' : 'Save'}
                                        </button>
                                    </div>
                                </div>

                                {loadingGroupPerms ? (
                                    <div className="py-12 text-center text-gray-500">Loading permissions...</div>
                                ) : (
                                    <div className="max-h-[32rem] overflow-y-auto">
                                        {/* Users section */}
                                        <div className="border-b border-gray-200 dark:border-gray-700">
                                            <div className="px-6 py-3 bg-gray-50/50 dark:bg-gray-800/30 flex items-center justify-between">
                                                <div className="flex items-center gap-2">
                                                    <svg className="w-4 h-4 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                                    </svg>
                                                    <span className="text-sm font-semibold">Users</span>
                                                    <span className="text-xs text-gray-500">
                                                        ({groupPermissions?.users.filter(u => u.can_access).length || 0} / {groupPermissions?.users.length || 0})
                                                    </span>
                                                </div>
                                                <div className="flex items-center gap-1.5">
                                                    <button
                                                        onClick={handleSelectAllGroupUsers}
                                                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-green-50 text-green-700 hover:bg-green-100 dark:bg-green-900/30 dark:text-green-300 dark:hover:bg-green-900/50 transition-colors"
                                                    >
                                                        All
                                                    </button>
                                                    <button
                                                        onClick={handleDeselectAllGroupUsers}
                                                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50 transition-colors"
                                                    >
                                                        None
                                                    </button>
                                                </div>
                                            </div>
                                            <div className="divide-y divide-gray-100 dark:divide-gray-800">
                                                {filteredGroupUsers.length === 0 ? (
                                                    <div className="py-6 text-center text-gray-400 text-sm">No users found</div>
                                                ) : (
                                                    filteredGroupUsers.map(u => (
                                                        <label
                                                            key={u.user_id}
                                                            className="flex items-center px-6 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer transition-colors"
                                                        >
                                                            <input
                                                                type="checkbox"
                                                                checked={u.can_access}
                                                                onChange={() => handleToggleGroupUserAccess(u.user_id)}
                                                                className="w-4.5 h-4.5 rounded border-gray-300 text-amber-600 focus:ring-amber-500"
                                                            />
                                                            <div className="ml-3 flex-1 min-w-0">
                                                                <div className="font-medium text-sm">{u.display_name || u.username}</div>
                                                                <div className="text-xs text-gray-500">@{u.username}</div>
                                                            </div>
                                                            {u.is_admin && (
                                                                <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300">
                                                                    Admin
                                                                </span>
                                                            )}
                                                        </label>
                                                    ))
                                                )}
                                            </div>
                                        </div>

                                        {/* Organizations section */}
                                        <div>
                                            <div className="px-6 py-3 bg-gray-50/50 dark:bg-gray-800/30 flex items-center justify-between">
                                                <div className="flex items-center gap-2">
                                                    <svg className="w-4 h-4 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                                                    </svg>
                                                    <span className="text-sm font-semibold">Organizations</span>
                                                    <span className="text-xs text-gray-500">
                                                        ({groupPermissions?.organizations.filter(o => o.can_access).length || 0} / {groupPermissions?.organizations.length || 0})
                                                    </span>
                                                </div>
                                                <div className="flex items-center gap-1.5">
                                                    <button
                                                        onClick={handleSelectAllGroupOrgs}
                                                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-green-50 text-green-700 hover:bg-green-100 dark:bg-green-900/30 dark:text-green-300 dark:hover:bg-green-900/50 transition-colors"
                                                    >
                                                        All
                                                    </button>
                                                    <button
                                                        onClick={handleDeselectAllGroupOrgs}
                                                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50 transition-colors"
                                                    >
                                                        None
                                                    </button>
                                                </div>
                                            </div>
                                            <div className="divide-y divide-gray-100 dark:divide-gray-800">
                                                {filteredGroupOrgs.length === 0 ? (
                                                    <div className="py-6 text-center text-gray-400 text-sm">No organizations found</div>
                                                ) : (
                                                    filteredGroupOrgs.map(o => (
                                                        <label
                                                            key={o.organization_id}
                                                            className="flex items-center px-6 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer transition-colors"
                                                        >
                                                            <input
                                                                type="checkbox"
                                                                checked={o.can_access}
                                                                onChange={() => handleToggleGroupOrgAccess(o.organization_id)}
                                                                className="w-4.5 h-4.5 rounded border-gray-300 text-amber-600 focus:ring-amber-500"
                                                            />
                                                            <div className="ml-3 flex-1 min-w-0">
                                                                <div className="font-medium text-sm">{o.name}</div>
                                                            </div>
                                                        </label>
                                                    ))
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* ─── Create Organization Modal ─── */}
            <Modal isOpen={showOrgModal} onClose={() => { setShowOrgModal(false); setOrgFormError(''); }} maxWidth="max-w-md">
                <h2 className="text-xl font-semibold mb-4">Create Organization</h2>
                <form onSubmit={handleCreateOrg} className="space-y-4">
                    {orgFormError && (
                        <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
                            {orgFormError}
                        </div>
                    )}
                    <div>
                        <label className="block text-sm font-medium mb-2">Name *</label>
                        <input
                            type="text"
                            value={orgName}
                            onChange={e => setOrgName(e.target.value)}
                            className="input"
                            required
                            placeholder="e.g. Engineering Team"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-2">Description</label>
                        <textarea
                            value={orgDescription}
                            onChange={e => setOrgDescription(e.target.value)}
                            className="input"
                            rows={3}
                            placeholder="Optional description..."
                        />
                    </div>
                    <div className="flex space-x-3 pt-4">
                        <button type="button" onClick={() => { setShowOrgModal(false); setOrgFormError(''); }} className="btn-secondary flex-1">
                            Cancel
                        </button>
                        <button type="submit" disabled={savingOrg} className="btn-primary flex-1">
                            {savingOrg ? 'Creating...' : 'Create'}
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}
