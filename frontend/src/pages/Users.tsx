import { useState, useEffect } from 'react';
import { usersApi, organizationsApi } from '../api';
import type { User, UserReportPermission, Organization } from '../types';
import Modal from '../components/Modal';
import { useAuth } from '../context/AuthContext';

type PermissionFilter = 'all' | 'public' | 'private';

export default function Users() {
    const { user: currentUser } = useAuth();
    const [users, setUsers] = useState<User[]>([]);
    const [organizations, setOrganizations] = useState<Organization[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingUser, setEditingUser] = useState<User | null>(null);

    // Form state
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [displayName, setDisplayName] = useState('');
    const [isAdmin, setIsAdmin] = useState(false);
    const [isActive, setIsActive] = useState(true);
    const [orgId, setOrgId] = useState<number | null>(null);
    const [formError, setFormError] = useState('');
    const [saving, setSaving] = useState(false);

    // Permissions modal state
    const [showPermissionsModal, setShowPermissionsModal] = useState(false);
    const [permissionsUser, setPermissionsUser] = useState<User | null>(null);
    const [permissions, setPermissions] = useState<UserReportPermission[]>([]);
    const [loadingPermissions, setLoadingPermissions] = useState(false);
    const [savingPermissions, setSavingPermissions] = useState(false);
    const [permissionFilter, setPermissionFilter] = useState<PermissionFilter>('all');
    const [permissionSearch, setPermissionSearch] = useState('');

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const [usersData, orgsData] = await Promise.all([
                usersApi.list(),
                organizationsApi.list(),
            ]);
            setUsers(usersData);
            setOrganizations(orgsData);
        } catch (err) {
            console.error('Failed to load data:', err);
        } finally {
            setLoading(false);
        }
    };

    const loadUsers = async () => {
        try {
            const data = await usersApi.list();
            setUsers(data);
        } catch (err) {
            console.error('Failed to load users:', err);
        }
    };

    // ─── Edit user ──────────────────────────────────────────
    const handleEditUser = (user: User) => {
        setEditingUser(user);
        setUsername(user.username);
        setEmail(user.email || '');
        setDisplayName(user.display_name || '');
        setPassword('');
        setIsAdmin(user.is_admin);
        setIsActive(user.is_active);
        setOrgId(user.organization);
        setFormError('');
        setShowModal(true);
    };

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault();
        setFormError('');
        setSaving(true);

        try {
            await usersApi.create({
                username,
                email: email || undefined,
                password,
                display_name: displayName || undefined,
            });
            await loadUsers();
            resetForm();
        } catch (err: unknown) {
            const error = err as { response?: { data?: { error?: string; username?: string[]; email?: string[] } } };
            setFormError(
                error.response?.data?.error ||
                error.response?.data?.email?.[0] ||
                error.response?.data?.username?.[0] ||
                'Failed to create user'
            );
        } finally {
            setSaving(false);
        }
    };

    const handleUpdate = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingUser) return;
        setFormError('');
        setSaving(true);

        try {
            const payload: Record<string, unknown> = {
                username,
                email: email || '',
                display_name: displayName || '',
                is_admin: isAdmin,
                is_active: isActive,
                organization: orgId,
            };
            if (password) {
                payload.password = password;
            }
            await usersApi.update(editingUser.id, payload as Partial<User>);
            await loadUsers();
            resetForm();
        } catch (err: unknown) {
            const error = err as { response?: { data?: { error?: string; username?: string[]; email?: string[] } } };
            setFormError(
                error.response?.data?.error ||
                error.response?.data?.email?.[0] ||
                error.response?.data?.username?.[0] ||
                'Failed to update user'
            );
        } finally {
            setSaving(false);
        }
    };

    const handleToggleActive = async (user: User) => {
        const action = user.is_active ? 'disable' : 'enable';
        if (!confirm(`Are you sure you want to ${action} user "${user.display_name || user.username}"?`)) return;

        try {
            await usersApi.update(user.id, { is_active: !user.is_active });
            await loadUsers();
        } catch (err) {
            console.error('Failed to update user:', err);
        }
    };

    const handleDemoteFromAdmin = async (user: User) => {
        if (!confirm(`Are you sure you want to remove Admin privileges from "${user.display_name || user.username}"?`)) return;

        try {
            await usersApi.update(user.id, { is_admin: false });
            await loadUsers();
        } catch (err) {
            console.error('Failed to demote user:', err);
        }
    };

    const handleDelete = async (user: User) => {
        if (!confirm(`Are you sure you want to delete user "${user.username}"?`)) return;

        try {
            await usersApi.delete(user.id);
            await loadUsers();
        } catch (err: unknown) {
            const error = err as { response?: { data?: { error?: string } } };
            alert(error.response?.data?.error || 'Failed to delete user');
        }
    };

    const handleEditPermissions = async (user: User) => {
        setPermissionsUser(user);
        setShowPermissionsModal(true);
        setLoadingPermissions(true);
        setPermissionFilter('all');
        setPermissionSearch('');

        try {
            const perms = await usersApi.getPermissions(user.id);
            setPermissions(perms);
        } catch (err) {
            console.error('Failed to load permissions:', err);
            setPermissions([]);
        } finally {
            setLoadingPermissions(false);
        }
    };

    const handleToggleAccess = (reportId: number) => {
        setPermissions(prev => prev.map(p =>
            p.report_id === reportId
                ? { ...p, can_access: !p.can_access }
                : p
        ));
    };

    const handleSelectAllPermissions = () => {
        setPermissions(prev => prev.map(p => ({ ...p, can_access: true })));
    };

    const handleDeselectAllPermissions = () => {
        setPermissions(prev => prev.map(p => ({ ...p, can_access: false })));
    };

    const handleSavePermissions = async () => {
        if (!permissionsUser) return;

        setSavingPermissions(true);
        try {
            await usersApi.updatePermissions(permissionsUser.id, permissions);
            setShowPermissionsModal(false);
            setPermissionsUser(null);
        } catch (err) {
            console.error('Failed to save permissions:', err);
            alert('Failed to save permissions');
        } finally {
            setSavingPermissions(false);
        }
    };

	const closeModal = () => {
		setShowModal(false);
	}

    const resetForm = () => {
        setShowModal(false);
        setEditingUser(null);
        setUsername('');
        setEmail('');
        setPassword('');
        setDisplayName('');
        setIsAdmin(false);
        setIsActive(true);
        setOrgId(null);
        setFormError('');
    };

    const filteredPermissions = permissions.filter(p => {
        // Apply filter
        if (permissionFilter === 'public' && !p.is_public) return false;
        if (permissionFilter === 'private' && p.is_public) return false;

        // Apply search
        if (permissionSearch.trim()) {
            const search = permissionSearch.toLowerCase();
            return p.report_name.toLowerCase().includes(search) ||
                p.path.toLowerCase().includes(search);
        }
        return true;
    });

    const allSelected = permissions.length > 0 && permissions.every(p => p.can_access);
    const noneSelected = permissions.length > 0 && permissions.every(p => !p.can_access);

    if (loading) {
        return (
            <div className="fade-in">
                <div className="flex items-center justify-between mb-6">
                    <h1 className="text-2xl font-bold">User Management</h1>
                </div>
                <div className="card">
                    <div className="skeleton h-64 rounded-xl" />
                </div>
            </div>
        );
    }

    return (
        <div className="fade-in">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold">User Management</h1>
                <button onClick={() => setShowModal(true)} className="btn-primary flex items-center space-x-2">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    <span>Add User</span>
                </button>
            </div>

            <div className="card overflow-hidden">
                <table className="w-full">
                    <thead className="bg-gray-50 dark:bg-gray-800/50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                User
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Role
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Status
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Created
                            </th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Actions
                            </th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                        {users.map((user) => (
                            <tr key={user.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <div className="flex items-center">
                                        <div className="w-10 h-10 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center">
                                            <span className="text-white font-medium">
                                                {user.username.charAt(0).toUpperCase()}
                                            </span>
                                        </div>
                                        <div className="ml-4">
                                            <div className="font-medium">{user.display_name || user.username}</div>
                                            <div className="text-sm text-gray-500">{user.email || user.username}</div>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                        user.is_super_admin
                                            ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-300'
                                            : user.is_admin
                                            ? 'bg-purple-100 text-purple-800 dark:bg-purple-900/50 dark:text-purple-300'
                                            : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'
                                        }`}>
                                        {user.is_super_admin ? '⭐ Super Admin' : user.is_admin ? 'Admin' : 'User'}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${user.is_active
                                        ? 'bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-300'
                                        : 'bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-300'
                                        }`}>
                                        {user.is_active ? 'Active' : 'Disabled'}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {new Date(user.created_at).toLocaleDateString()}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-right">
                                    <div className="flex items-center justify-end gap-2">
                                        {/* Edit – super admin only, not on super admin row */}
                                        {currentUser?.is_super_admin && !user.is_super_admin && (
                                            <button
                                                onClick={() => handleEditUser(user)}
                                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-indigo-50 text-indigo-700 hover:bg-indigo-100 dark:bg-indigo-900/30 dark:text-indigo-300 dark:hover:bg-indigo-900/50 transition-colors"
                                                title="Edit User"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                </svg>
                                                Edit
                                            </button>
                                        )}

                                        {/* Permissions – non-admin, non-super-admin users only */}
                                        {!user.is_admin && (
                                            <button
                                                onClick={() => handleEditPermissions(user)}
                                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 dark:bg-blue-900/30 dark:text-blue-300 dark:hover:bg-blue-900/50 transition-colors"
                                                title="Edit Permissions"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                                                </svg>
                                                Permissions
                                            </button>
                                        )}

                                        {/* Demote: only super_admin can demote an admin (not themselves) */}
                                        {currentUser?.is_super_admin && user.is_admin && !user.is_super_admin && (
                                            <button
                                                onClick={() => handleDemoteFromAdmin(user)}
                                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors bg-orange-50 text-orange-700 hover:bg-orange-100 dark:bg-orange-900/30 dark:text-orange-300 dark:hover:bg-orange-900/50"
                                                title="Remove Admin"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6" />
                                                </svg>
                                                Remove Admin
                                            </button>
                                        )}

                                        {/* Enable / Disable — skip for super admin */}
                                        {!user.is_super_admin && (
                                            <button
                                                onClick={() => handleToggleActive(user)}
                                                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${user.is_active
                                                        ? 'bg-orange-50 text-orange-700 hover:bg-orange-100 dark:bg-orange-900/30 dark:text-orange-300 dark:hover:bg-orange-900/50'
                                                        : 'bg-green-50 text-green-700 hover:bg-green-100 dark:bg-green-900/30 dark:text-green-300 dark:hover:bg-green-900/50'
                                                    }`}
                                                title={user.is_active ? 'Disable User' : 'Enable User'}
                                            >
                                                {user.is_active ? (
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                                                    </svg>
                                                ) : (
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                    </svg>
                                                )}
                                                {user.is_active ? 'Disable' : 'Enable'}
                                            </button>
                                        )}

                                        {/* Delete — not allowed for the super admin */}
                                        {!user.is_super_admin && (
                                            <button
                                                onClick={() => handleDelete(user)}
                                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50 transition-colors"
                                                title="Delete User"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                </svg>
                                                Delete
                                            </button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Create / Edit User Modal */}
            <Modal isOpen={showModal} onClose={closeModal} maxWidth="max-w-md">
                <h2 className="text-xl font-semibold mb-4">
                    {editingUser ? 'Edit User' : 'Create New User'}
                </h2>

                <form onSubmit={editingUser ? handleUpdate : handleCreate} className="space-y-4">
                    {formError && (
                        <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
                            {formError}
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
                        <label className="block text-sm font-medium mb-2">
                            Password {editingUser ? '' : '*'}
                        </label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="input"
                            minLength={8}
                            required={!editingUser}
                            placeholder={editingUser ? 'Leave blank to keep unchanged' : ''}
                        />
                    </div>

                    {/* Extra fields visible only in edit mode */}
                    {editingUser && (
                        <>
                            <div>
                                <label className="block text-sm font-medium mb-2">Organization</label>
                                <select
                                    value={orgId ?? ''}
                                    onChange={(e) => setOrgId(e.target.value ? Number(e.target.value) : null)}
                                    className="input"
                                >
                                    <option value="">None</option>
                                    {organizations.map(org => (
                                        <option key={org.id} value={org.id}>{org.name}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="flex items-center justify-between py-2">
                                <span className="text-sm font-medium">Admin</span>
                                <button
                                    type="button"
                                    onClick={() => setIsAdmin(!isAdmin)}
                                    className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 ${
                                        isAdmin ? 'bg-purple-500' : 'bg-gray-300 dark:bg-gray-600'
                                    }`}
                                >
                                    <span className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                                        isAdmin ? 'translate-x-5' : 'translate-x-0'
                                    }`} />
                                </button>
                            </div>

                            <div className="flex items-center justify-between py-2">
                                <span className="text-sm font-medium">Active</span>
                                <button
                                    type="button"
                                    onClick={() => setIsActive(!isActive)}
                                    className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 ${
                                        isActive ? 'bg-green-500' : 'bg-gray-300 dark:bg-gray-600'
                                    }`}
                                >
                                    <span className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                                        isActive ? 'translate-x-5' : 'translate-x-0'
                                    }`} />
                                </button>
                            </div>
                        </>
                    )}

                    <div className="flex space-x-3 pt-4">
                        <button type="button" onClick={resetForm} className="btn-secondary flex-1">
                            Cancel
                        </button>
                        <button type="submit" disabled={saving} className="btn-primary flex-1">
                            {saving
                                ? (editingUser ? 'Saving...' : 'Creating...')
                                : (editingUser ? 'Save Changes' : 'Create User')
                            }
                        </button>
                    </div>
                </form>
            </Modal>

            {/* Permissions Modal */}
            <Modal
                isOpen={showPermissionsModal}
                onClose={() => { setShowPermissionsModal(false); setPermissionsUser(null); }}
                maxWidth="max-w-2xl"
            >
                <h2 className="text-xl font-semibold mb-2">
                    Edit Permissions: {permissionsUser?.display_name || permissionsUser?.username}
                </h2>
                <p className="text-sm text-gray-500 mb-4">
                    Select which reports this user can access
                </p>

                {/* Filters */}
                <div className="flex flex-wrap gap-3 mb-4">
                    <div className="flex-1 min-w-48">
                        <input
                            type="text"
                            placeholder="Search reports..."
                            value={permissionSearch}
                            onChange={(e) => setPermissionSearch(e.target.value)}
                            className="input w-full"
                        />
                    </div>
                    <div className="flex items-center space-x-1 bg-gray-100 dark:bg-gray-800 rounded-lg p-1">
                        <button
                            onClick={() => setPermissionFilter('all')}
                            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${permissionFilter === 'all'
                                ? 'bg-white dark:bg-gray-700 shadow'
                                : 'hover:bg-gray-200 dark:hover:bg-gray-600'
                                }`}
                        >
                            All
                        </button>
                        <button
                            onClick={() => setPermissionFilter('public')}
                            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${permissionFilter === 'public'
                                ? 'bg-white dark:bg-gray-700 shadow'
                                : 'hover:bg-gray-200 dark:hover:bg-gray-600'
                                }`}
                        >
                            Public
                        </button>
                        <button
                            onClick={() => setPermissionFilter('private')}
                            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${permissionFilter === 'private'
                                ? 'bg-white dark:bg-gray-700 shadow'
                                : 'hover:bg-gray-200 dark:hover:bg-gray-600'
                                }`}
                        >
                            Private
                        </button>
                    </div>
                </div>

                {/* Select All / Deselect All */}
                {!loadingPermissions && permissions.length > 0 && (
                    <div className="flex items-center gap-2 mb-3">
                        <button
                            type="button"
                            onClick={handleSelectAllPermissions}
                            disabled={allSelected}
                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                                allSelected
                                    ? 'bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500 cursor-not-allowed'
                                    : 'bg-green-50 text-green-700 hover:bg-green-100 dark:bg-green-900/30 dark:text-green-300 dark:hover:bg-green-900/50'
                            }`}
                        >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            Select All
                        </button>
                        <button
                            type="button"
                            onClick={handleDeselectAllPermissions}
                            disabled={noneSelected}
                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                                noneSelected
                                    ? 'bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500 cursor-not-allowed'
                                    : 'bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50'
                            }`}
                        >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            Deselect All
                        </button>
                        <span className="text-xs text-gray-500 ml-auto">
                            {permissions.filter(p => p.can_access).length} of {permissions.length} selected
                        </span>
                    </div>
                )}

                {loadingPermissions ? (
                    <div className="py-8 text-center text-gray-500">Loading reports...</div>
                ) : filteredPermissions.length === 0 ? (
                    <div className="py-8 text-center text-gray-500">No reports found</div>
                ) : (
                    <div className="max-h-96 overflow-y-auto border border-gray-200 dark:border-gray-700 rounded-lg divide-y divide-gray-200 dark:divide-gray-700">
                        {filteredPermissions.map((perm) => (
                            <label
                                key={perm.report_id}
                                className="flex items-center px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 cursor-pointer"
                            >
                                <input
                                    type="checkbox"
                                    checked={perm.can_access}
                                    onChange={() => handleToggleAccess(perm.report_id)}
                                    className="w-5 h-5 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                />
                                <div className="ml-3 flex-1">
                                    <div className="font-medium">{perm.report_name}</div>
                                    <div className="text-sm text-gray-500">{perm.path}</div>
                                </div>
                                <span className={`text-xs px-2 py-0.5 rounded-full ${perm.is_public
                                    ? 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300'
                                    : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                                    }`}>
                                    {perm.is_public ? 'Public' : 'Private'}
                                </span>
                            </label>
                        ))}
                    </div>
                )}

                <div className="flex space-x-3 pt-4 mt-4 border-t border-gray-200 dark:border-gray-700">
                    <button
                        type="button"
                        onClick={() => { setShowPermissionsModal(false); setPermissionsUser(null); }}
                        className="btn-secondary flex-1"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSavePermissions}
                        disabled={savingPermissions || loadingPermissions}
                        className="btn-primary flex-1"
                    >
                        {savingPermissions ? 'Saving...' : 'Save Permissions'}
                    </button>
                </div>
            </Modal>
        </div>
    );
}
