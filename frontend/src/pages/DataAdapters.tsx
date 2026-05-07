import { useState, useEffect, useRef } from 'react';
import { dataAdaptersApi } from '../api';
import type { DataAdapter } from '../types';
import Modal from '../components/Modal';

export default function DataAdapters() {
    const [adapters, setAdapters] = useState<DataAdapter[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);

    // Form state
    const [name, setName] = useState('');
    const [adapterType, setAdapterType] = useState<'jdbc' | 'csv' | 'json' | 'xml' | 'inmemory' | 'mock'>('jdbc');
    const [connectionDetails, setConnectionDetails] = useState({
        host: 'localhost',
        port: '5432',
        database: '',
        username: 'postgres',
        password: '',
        url: '',
    });
    const [csvFile, setCsvFile] = useState<File | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
    const [formError, setFormError] = useState('');
    const [saving, setSaving] = useState(false);
    const [testing, setTesting] = useState(false);
    const [showTemplates, setShowTemplates] = useState(false);
    const templatesDropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (templatesDropdownRef.current && !templatesDropdownRef.current.contains(event.target as Node)) {
                setShowTemplates(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const downloadTemplate = (driver: string, dbName: string) => {
        const yamlContent = `# Use postgres, Mysql\n# change type to csv, JSON if applicable\nname: ${dbName} Data Adapter\ntype: jdbc\n\nconfig:\n  driver: ${driver}\n  url: \${DB_URL_customers}\n  username: \${DB_USER_customers}\n  password: \${DB_PASSWORD_customers}\n`;
        const blob = new Blob([yamlContent], { type: 'text/yaml' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${driver}_template.yaml`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        setShowTemplates(false);
    };

    useEffect(() => {
        loadAdapters();
    }, []);

    const loadAdapters = async () => {
        try {
            const data = await dataAdaptersApi.list();
            setAdapters(data);
        } catch (err) {
            console.error('Failed to load adapters:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleTestConnection = async () => {
        setTesting(true);
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
                    ...(connectionDetails.url ? { url: connectionDetails.url } : {}),
                } : adapterType === 'mock' ? {} : { file_path: '' },
            });
            setTestResult(result);
        } catch {
            setTestResult({ success: false, message: 'Connection test failed' });
        } finally {
            setTesting(false);
        }
    };

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault();
        setFormError('');
        setSaving(true);

        try {
            if (adapterType === 'csv' || adapterType === 'json' || adapterType === 'xml') {
                if (!csvFile) {
                    setFormError(`Please select a ${adapterType.toUpperCase()} file to upload`);
                    setSaving(false);
                    return;
                }
                await dataAdaptersApi.createWithFile(name, adapterType, csvFile);
            } else {
                await dataAdaptersApi.create({
                    name,
                    adapter_type: adapterType,
                    connection_details: adapterType === 'jdbc' ? {
                        host: connectionDetails.host,
                        port: parseInt(connectionDetails.port),
                        database: connectionDetails.database,
                        username: connectionDetails.username,
                        password: connectionDetails.password,
                        ...(connectionDetails.url ? { url: connectionDetails.url } : {}),
                    } : {},
                });
            }
            await loadAdapters();
            resetForm();
        } catch (err: unknown) {
            const error = err as { response?: { data?: { error?: string } } };
            setFormError(error.response?.data?.error || 'Failed to create adapter');
        } finally {
            setSaving(false);
        }
    };

    const handleTestExisting = async (adapter: DataAdapter) => {
        try {
            const result = await dataAdaptersApi.test(adapter.id);
            alert(result.message);
        } catch {
            alert('Connection test failed');
        }
    };

    const handleDelete = async (adapter: DataAdapter) => {
        if (!confirm(`Are you sure you want to delete adapter "${adapter.name}"?`)) return;

        try {
            await dataAdaptersApi.delete(adapter.id);
            await loadAdapters();
        } catch (err) {
            console.error('Failed to delete adapter:', err);
        }
    };



    const resetForm = () => {
        setShowModal(false);
        setName('');
        setAdapterType('jdbc');
        setConnectionDetails({
            host: 'localhost',
            port: '5432',
            database: '',
            username: 'postgres',
            password: '',
            url: '',
        });
        setCsvFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
        setTestResult(null);
        setFormError('');
    };

    const formatFileSize = (bytes: number): string => {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    };

    if (loading) {
        return (
            <div className="fade-in">
                <div className="flex items-center justify-between mb-6">
                    <h1 className="text-2xl font-bold">Data Adapters</h1>
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
                <div>
                    <h1 className="text-2xl font-bold">Data Adapters</h1>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        Manage your data source connections. Report data sources are configured via YAML files in each report's repository.
                    </p>
                </div>
                <div className="flex items-center space-x-3">
                    <div className="relative" ref={templatesDropdownRef}>
                        <button 
                            onClick={() => setShowTemplates(!showTemplates)} 
                            className="btn-secondary flex items-center space-x-2"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                            </svg>
                            <span>Templates</span>
                        </button>
                        {showTemplates && (
                            <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-100 dark:border-gray-700 py-1 z-50">
                                <button
                                    onClick={() => downloadTemplate('postgres', 'PostgreSQL')}
                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50"
                                >
                                    PostgreSQL
                                </button>
                                <button
                                    onClick={() => downloadTemplate('mysql', 'MySQL')}
                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50"
                                >
                                    MySQL
                                </button>
                                <button
                                    onClick={() => downloadTemplate('sqlserver', 'SQL Server')}
                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50"
                                >
                                    SQL Server
                                </button>
                                <button
                                    onClick={() => downloadTemplate('mongodb', 'MongoDB')}
                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50"
                                >
                                    MongoDB
                                </button>
                            </div>
                        )}
                    </div>
                    <button onClick={() => setShowModal(true)} className="btn-primary flex items-center space-x-2">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        <span>Add Adapter</span>
                    </button>
                </div>
            </div>

            {adapters.length === 0 ? (
                <div className="text-center py-16 card">
                    <div className="w-16 h-16 bg-gray-200 dark:bg-gray-700 rounded-full flex items-center justify-center mx-auto mb-4">
                        <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
                        </svg>
                    </div>
                    <h2 className="text-xl font-semibold mb-2">No data adapters</h2>
                    <p className="text-gray-600 dark:text-gray-400 mb-4">
                        Add a data adapter to connect your reports to a data source.
                    </p>
                    <button onClick={() => setShowModal(true)} className="btn-primary">
                        Add Adapter
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {adapters.map((adapter) => (
                        <div
                            key={adapter.id}
                            className="card p-6 transition-all duration-200"
                        >
                            <div className="flex items-start justify-between mb-4">
                                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${adapter.adapter_type === 'jdbc'
                                    ? 'bg-blue-100 dark:bg-blue-900/30'
                                    : adapter.adapter_type === 'csv'
                                        ? 'bg-green-100 dark:bg-green-900/30'
                                        : adapter.adapter_type === 'json'
                                            ? 'bg-yellow-100 dark:bg-yellow-900/30'
                                            : adapter.adapter_type === 'xml'
                                                ? 'bg-orange-100 dark:bg-orange-900/30'
                                                : adapter.adapter_type === 'inmemory'
                                                    ? 'bg-cyan-100 dark:bg-cyan-900/30'
                                                    : 'bg-purple-100 dark:bg-purple-900/30'
                                    }`}>
                                    {adapter.adapter_type === 'csv' || adapter.adapter_type === 'json' || adapter.adapter_type === 'xml' ? (
                                        <svg className={`w-6 h-6 ${adapter.adapter_type === 'csv' ? 'text-green-600' : adapter.adapter_type === 'json' ? 'text-yellow-600' : 'text-orange-600'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                        </svg>
                                    ) : adapter.adapter_type === 'inmemory' ? (
                                        <svg className="w-6 h-6 text-cyan-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
                                        </svg>
                                    ) : (
                                        <svg className={`w-6 h-6 ${adapter.adapter_type === 'jdbc' ? 'text-blue-600' : 'text-purple-600'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
                                        </svg>
                                    )}
                                </div>


                            </div>

                            <h3 className="font-semibold text-lg">{adapter.name}</h3>
                            <p className="text-sm text-gray-500 mt-1">
                                {adapter.adapter_type.toUpperCase()}
                            </p>

                            {adapter.adapter_type === 'jdbc' && (
                                <p className="text-xs text-gray-400 mt-2">
                                    {(adapter.connection_details as { host?: string; database?: string }).host}:{(adapter.connection_details as { port?: number }).port}/{(adapter.connection_details as { database?: string }).database}
                                </p>
                            )}

                            {(adapter.adapter_type === 'csv' || adapter.adapter_type === 'json' || adapter.adapter_type === 'xml') && (
                                <div className="mt-2 flex items-center gap-2 text-xs text-gray-400">
                                    <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                    </svg>
                                    <span className="truncate">
                                        {(adapter.connection_details as { original_name?: string }).original_name || `${adapter.adapter_type.toUpperCase()} File`}
                                    </span>
                                    {(adapter.connection_details as { file_size?: number }).file_size && (
                                        <span className="text-gray-500 whitespace-nowrap">
                                            ({formatFileSize((adapter.connection_details as { file_size: number }).file_size)})
                                        </span>
                                    )}
                                </div>
                            )}

                            <div className="flex space-x-2 mt-4">
                                <button
                                    onClick={() => handleTestExisting(adapter)}
                                    className="btn-secondary text-sm flex-1"
                                >
                                    Test
                                </button>
                                <button
                                    onClick={() => handleDelete(adapter)}
                                    className="btn-ghost text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 text-sm"
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Create Adapter Modal */}
            <Modal isOpen={showModal} onClose={resetForm}>
                <h2 className="text-xl font-semibold mb-4">Create Data Adapter</h2>

                <form onSubmit={handleCreate} className="space-y-4">
                    {formError && (
                        <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
                            {formError}
                        </div>
                    )}

                    <div>
                        <label className="block text-sm font-medium mb-2">Name *</label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="input"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium mb-2">Type *</label>
                        <select
                            value={adapterType}
                            onChange={(e) => {
                                setAdapterType(e.target.value as typeof adapterType);
                                setCsvFile(null);
                                if (fileInputRef.current) fileInputRef.current.value = '';
                                setTestResult(null);
                            }}
                            className="input"
                        >
                            <option value="jdbc">JDBC (PostgreSQL)</option>
                            <option value="json">JSON File</option>
                            <option value="csv">CSV File</option>
                            <option value="xml">XML File</option>
                            <option value="inmemory">In-Memory</option>
                            <option value="mock">Mock Data</option>
                        </select>
                    </div>

                    {adapterType === 'jdbc' && (
                        <>
                            <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 text-xs text-blue-700 dark:text-blue-300">
                                <p className="font-medium mb-1">🐳 Running in Docker?</p>
                                <p>Use <code className="px-1 py-0.5 rounded bg-blue-100 dark:bg-blue-800/50 font-mono">db</code> to connect to the Docker Compose database, or <code className="px-1 py-0.5 rounded bg-blue-100 dark:bg-blue-800/50 font-mono">host.docker.internal</code> to reach your host machine's localhost.</p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-2">JDBC URL <span className="text-gray-400 font-normal">(optional)</span></label>
                                <input
                                    type="text"
                                    value={connectionDetails.url}
                                    onChange={(e) => setConnectionDetails({ ...connectionDetails, url: e.target.value })}
                                    className="input font-mono text-xs"
                                    placeholder="jdbc:postgresql://host.docker.internal:5432/mydb"
                                />
                                <p className="text-xs text-gray-500 mt-1">If provided, overrides host/port/database below.</p>
                            </div>
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
                            <div>
                                <label className="block text-sm font-medium mb-2">Password</label>
                                <input
                                    type="password"
                                    value={connectionDetails.password}
                                    onChange={(e) => setConnectionDetails({ ...connectionDetails, password: e.target.value })}
                                    className="input"
                                />
                            </div>
                        </>
                    )}

                    {adapterType === 'csv' && (
                        <div>
                            <label className="block text-sm font-medium mb-2">CSV File *</label>
                            <div
                                className={`relative border-2 border-dashed rounded-xl p-6 text-center transition-colors cursor-pointer
                                    ${csvFile
                                        ? 'border-green-300 dark:border-green-700 bg-green-50 dark:bg-green-900/20'
                                        : 'border-gray-300 dark:border-gray-600 hover:border-primary-400 dark:hover:border-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/10'
                                    }`}
                                onClick={() => fileInputRef.current?.click()}
                            >
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept=".csv,.tsv,.txt"
                                    className="hidden"
                                    onChange={(e) => {
                                        const file = e.target.files?.[0];
                                        if (file) {
                                            setCsvFile(file);
                                            if (!name) setName(file.name.replace(/\.[^/.]+$/, ''));
                                        }
                                    }}
                                />
                                {csvFile ? (
                                    <div className="space-y-2">
                                        <div className="w-12 h-12 bg-green-100 dark:bg-green-900/40 rounded-full flex items-center justify-center mx-auto">
                                            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                            </svg>
                                        </div>
                                        <p className="font-medium text-green-700 dark:text-green-300">{csvFile.name}</p>
                                        <p className="text-xs text-green-600 dark:text-green-400">{formatFileSize(csvFile.size)}</p>
                                        <button
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setCsvFile(null);
                                                if (fileInputRef.current) fileInputRef.current.value = '';
                                            }}
                                            className="text-xs text-red-500 hover:text-red-700 underline"
                                        >
                                            Remove file
                                        </button>
                                    </div>
                                ) : (
                                    <div className="space-y-2">
                                        <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center mx-auto">
                                            <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                            </svg>
                                        </div>
                                        <p className="text-sm font-medium">Click to upload CSV file</p>
                                        <p className="text-xs text-gray-500">Supports .csv, .tsv, .txt</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {(adapterType === 'json' || adapterType === 'xml') && (
                        <div>
                            <label className="block text-sm font-medium mb-2">{adapterType.toUpperCase()} File *</label>
                            <div
                                className={`relative border-2 border-dashed rounded-xl p-6 text-center transition-colors cursor-pointer
                                    ${csvFile
                                        ? 'border-green-300 dark:border-green-700 bg-green-50 dark:bg-green-900/20'
                                        : 'border-gray-300 dark:border-gray-600 hover:border-primary-400 dark:hover:border-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/10'
                                    }`}
                                onClick={() => fileInputRef.current?.click()}
                            >
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept={adapterType === 'json' ? '.json' : '.xml'}
                                    className="hidden"
                                    onChange={(e) => {
                                        const file = e.target.files?.[0];
                                        if (file) {
                                            setCsvFile(file);
                                            if (!name) setName(file.name.replace(/\.[^/.]+$/, ''));
                                        }
                                    }}
                                />
                                {csvFile ? (
                                    <div className="space-y-2">
                                        <div className="w-12 h-12 bg-green-100 dark:bg-green-900/40 rounded-full flex items-center justify-center mx-auto">
                                            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                            </svg>
                                        </div>
                                        <p className="font-medium text-green-700 dark:text-green-300">{csvFile.name}</p>
                                        <p className="text-xs text-green-600 dark:text-green-400">{formatFileSize(csvFile.size)}</p>
                                        <button
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setCsvFile(null);
                                                if (fileInputRef.current) fileInputRef.current.value = '';
                                            }}
                                            className="text-xs text-red-500 hover:text-red-700 underline"
                                        >
                                            Remove file
                                        </button>
                                    </div>
                                ) : (
                                    <div className="space-y-2">
                                        <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center mx-auto">
                                            <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                            </svg>
                                        </div>
                                        <p className="text-sm font-medium">Click to upload {adapterType.toUpperCase()} file</p>
                                        <p className="text-xs text-gray-500">Supports .{adapterType}</p>
                                    </div>
                                )}
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

                    <div className="flex space-x-3 pt-4">
                        <button type="button" onClick={resetForm} className="btn-secondary">
                            Cancel
                        </button>
                        {adapterType === 'jdbc' && (
                            <button
                                type="button"
                                onClick={handleTestConnection}
                                disabled={testing}
                                className="btn-secondary"
                            >
                                {testing ? 'Testing...' : 'Test'}
                            </button>
                        )}
                        <button type="submit" disabled={saving} className="btn-primary flex-1">
                            {saving ? 'Creating...' : 'Create'}
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}