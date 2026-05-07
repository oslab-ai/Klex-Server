import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { reportsApi } from '../api';
import { getAccessToken } from '../api/client';
import type { Report, CompileResponse, MetadataResponse, ParameterMetadataResponse } from '../types';
import MetadataSidebar from '../components/MetadataSidebar';
import { ReportParameterForm } from '../components/ReportParameterForm';
import AuditLogSidebar from '../components/AuditLogSidebar';
import ScheduleModal from '../components/ScheduleModal';
import ParameterDialogModal from '../components/ParameterDialogModal';
import EmbedDialog from '../components/EmbedDialog';
import { useAuth } from '../context/AuthContext';

const EXPORT_FORMATS = [
    { key: 'PDF', label: 'PDF', icon: '📄', color: 'text-red-600' },
    { key: 'XLSX', label: 'Excel (XLSX)', icon: '📊', color: 'text-green-600' },
    { key: 'CSV', label: 'CSV', icon: '📋', color: 'text-blue-600' },
    { key: 'HTML', label: 'HTML', icon: '🌐', color: 'text-orange-600' },
    { key: 'DOCX', label: 'Word (DOCX)', icon: '📝', color: 'text-blue-700' },
    { key: 'PPTX', label: 'PowerPoint (PPTX)', icon: '📽️', color: 'text-amber-600' },
    { key: 'ODT', label: 'OpenDocument (ODT)', icon: '📃', color: 'text-purple-600' },
];

export default function ReportViewer() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();

    // ── Core state ──
    const [report, setReport] = useState<Report | null>(null);
    const [pdfUrl, setPdfUrl] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const pdfUrlRef = useRef<string | null>(null);

    // ── Simple loading flag (shown as a spinner overlay) ──
    const [initializing, setInitializing] = useState(true);
    const [compiling, setCompiling] = useState(false);

    // ── Parameter metadata (fetched ONCE, shared with sidebar + dialog) ──
    const [paramMetadata, setParamMetadata] = useState<ParameterMetadataResponse | null>(null);
    const [hasVisibleParams, setHasVisibleParams] = useState(false);
    const [showParamDialog, setShowParamDialog] = useState(false);
    const [initialParamValues, setInitialParamValues] = useState<Record<string, string> | undefined>(undefined);
    const lastParamsRef = useRef<Record<string, string> | undefined>(undefined);

    // ── Viewer controls ──
    const [zoom, setZoom] = useState(100);
    const [rotation, setRotation] = useState(0);

    // ── Sidebar / modal state ──
    const [showAudit, setShowAudit] = useState(false);
    const [showSchedule, setShowSchedule] = useState(false);
    const [showEmbed, setShowEmbed] = useState(false);
    const [showExportMenu, setShowExportMenu] = useState(false);
    const [exporting, setExporting] = useState(false);
    const exportMenuRef = useRef<HTMLDivElement>(null);
    const iframeRef = useRef<HTMLIFrameElement>(null);
    const [showMetadata, setShowMetadata] = useState(false);
    const [metadata, setMetadata] = useState<MetadataResponse | null>(null);
    const [metadataLoading, setMetadataLoading] = useState(false);
    const [metadataError, setMetadataError] = useState<string | null>(null);

    // ── Helper: download compiled output as blob URL ──
    const downloadPdf = useCallback(async (outputUrl: string): Promise<string> => {
        const baseUrl = window.location.origin;
        const downloadPath = outputUrl.startsWith('http')
            ? outputUrl
            : `${baseUrl}${outputUrl}`;

        const token = getAccessToken();
        const response = await fetch(downloadPath, {
            headers: { 'Authorization': `Bearer ${token}` },
        });

        if (!response.ok) {
            throw new Error(`Failed to download PDF: ${response.statusText}`);
        }

        const blob = await response.blob();
        return URL.createObjectURL(blob);
    }, []);

    // ═══════════════════════════════════════════════════════════
    // ── SINGLE initialization flow
    // ── Uses cancelled flag to prevent stale updates from
    // ── React StrictMode's double-invoke in dev mode
    // ═══════════════════════════════════════════════════════════
    useEffect(() => {
        if (!id) return;

        let cancelled = false;
        const reportId = parseInt(id);

        const init = async () => {
            setInitializing(true);
            setError(null);
            setPdfUrl(null);
            if (pdfUrlRef.current) URL.revokeObjectURL(pdfUrlRef.current);
            pdfUrlRef.current = null;

            try {
                // 1. Fetch report info
                const data = await reportsApi.get(reportId);
                if (cancelled) return;
                setReport(data);

                // 2. Check if report has parameters
                let paramMeta: ParameterMetadataResponse | null = null;
                try {
                    paramMeta = await reportsApi.getParameterMetadata(reportId);
                    if (cancelled) return;
                    setParamMetadata(paramMeta);
                } catch {
                    if (cancelled) return;
                    // No parameters or endpoint unavailable — proceed
                }

                const visibleParams = paramMeta?.parameters.filter(p => !p.hidden) || [];
                if (cancelled) return;
                setHasVisibleParams(visibleParams.length > 0);

                if (visibleParams.length > 0) {
                    // Has parameters → stop loading, show dialog
                    setInitializing(false);
                    setShowParamDialog(true);
                    return;
                }

                // 3. No parameters → compile directly
                setCompiling(true);
                setInitializing(false);

                const result: CompileResponse = await reportsApi.compile(reportId);
                if (cancelled) return;
                if (result.output_url) {
                    const blobUrl = await downloadPdf(result.output_url);
                    if (cancelled) {
                        URL.revokeObjectURL(blobUrl);
                        return;
                    }
                    pdfUrlRef.current = blobUrl;
                    setPdfUrl(blobUrl);
                }
            } catch (err: unknown) {
                if (cancelled) return;
                const e = err as { response?: { data?: { error?: string } }; message?: string };
                setError(e.response?.data?.error || e.message || 'Failed to load report');
            } finally {
                if (!cancelled) {
                    setInitializing(false);
                    setCompiling(false);
                }
            }
        };

        init();

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    // Cleanup blob URL on unmount
    useEffect(() => {
        return () => {
            if (pdfUrlRef.current) URL.revokeObjectURL(pdfUrlRef.current);
        };
    }, []);

    // Close export menu on outside click
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (exportMenuRef.current && !exportMenuRef.current.contains(e.target as Node)) {
                setShowExportMenu(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // ═══════════════════════════
    // ── Handlers
    // ═══════════════════════════

    // Compile helper (used for re-runs and parameter submissions)
    const compileReport = useCallback(async (reportId: number, params?: Record<string, string>) => {
        setCompiling(true);
        setError(null);
        lastParamsRef.current = params;

        try {
            const result: CompileResponse = await reportsApi.compile(reportId, params);
            if (result.output_url) {
                if (pdfUrlRef.current) URL.revokeObjectURL(pdfUrlRef.current);
                const blobUrl = await downloadPdf(result.output_url);
                pdfUrlRef.current = blobUrl;
                setPdfUrl(blobUrl);
            }
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string } }; message?: string };
            setError(e.response?.data?.error || e.message || 'Failed to compile report');
        } finally {
            setCompiling(false);
        }
    }, [downloadPdf]);

    // Sidebar "Run Report"
    const handleApplyParameters = useCallback(async (values: Record<string, string>) => {
        if (!id) return;
        setInitialParamValues(values);
        await compileReport(parseInt(id), values);
    }, [id, compileReport]);

    // Initial parameter dialog submit
    const handleDialogSubmit = useCallback(async (params: Record<string, string>) => {
        if (!id) return;
        setShowParamDialog(false);
        setInitialParamValues(params);
        await compileReport(parseInt(id), params);
    }, [id, compileReport]);

    // Dialog cancel — go back to reports list
    const handleDialogCancel = useCallback(() => {
        setShowParamDialog(false);
        navigate('/');
    }, [navigate]);

    // Toolbar "Re-run"
    const handleRecompile = useCallback(async () => {
        if (!id) return;
        setError(null);
        await compileReport(parseInt(id), lastParamsRef.current);
    }, [id, compileReport]);

    // Full retry — restart the entire init
    const handleRetry = useCallback(() => {
        if (!id) return;
        setError(null);
        setPdfUrl(null);
        if (pdfUrlRef.current) URL.revokeObjectURL(pdfUrlRef.current);
        pdfUrlRef.current = null;

        const reportId = parseInt(id);
        const retry = async () => {
            setInitializing(true);
            try {
                const data = await reportsApi.get(reportId);
                setReport(data);

                let paramMeta: ParameterMetadataResponse | null = null;
                try {
                    paramMeta = await reportsApi.getParameterMetadata(reportId);
                    setParamMetadata(paramMeta);
                } catch { /* proceed */ }

                const visibleParams = paramMeta?.parameters.filter(p => !p.hidden) || [];
                setHasVisibleParams(visibleParams.length > 0);

                if (visibleParams.length > 0) {
                    setInitializing(false);
                    setShowParamDialog(true);
                    return;
                }

                setCompiling(true);
                setInitializing(false);
                const result: CompileResponse = await reportsApi.compile(reportId);
                if (result.output_url) {
                    const blobUrl = await downloadPdf(result.output_url);
                    pdfUrlRef.current = blobUrl;
                    setPdfUrl(blobUrl);
                }
            } catch (err: unknown) {
                const e = err as { response?: { data?: { error?: string } }; message?: string };
                setError(e.response?.data?.error || e.message || 'Failed to load report');
            } finally {
                setInitializing(false);
                setCompiling(false);
            }
        };
        retry();
    }, [id, downloadPdf]);

    const handlePrint = () => {
        if (iframeRef.current?.contentWindow) {
            iframeRef.current.contentWindow.focus();
            iframeRef.current.contentWindow.print();
        } else {
            window.print();
        }
    };

    const handleZoomIn = () => setZoom(prev => Math.min(prev + 25, 300));
    const handleZoomOut = () => setZoom(prev => Math.max(prev - 25, 25));
    const handleResetZoom = () => setZoom(100);
    const handleRotate = () => setRotation(prev => (prev + 90) % 360);

    const handleExport = async (format: string) => {
        if (!id) return;
        setShowExportMenu(false);
        setExporting(true);
        setError(null);

        try {
            const reportId = parseInt(id);
            const result = await reportsApi.compile(reportId, lastParamsRef.current, format);
            if (result.output_url) {
                const blobUrl = await downloadPdf(result.output_url);
                const link = document.createElement('a');
                link.href = blobUrl;
                link.download = `${report?.report_name || 'report'}.${format.toLowerCase()}`;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                URL.revokeObjectURL(blobUrl);
            }
        } catch (err: unknown) {
            console.error('Export failed:', err);
            const error = err as { response?: { data?: { error?: string } }; message?: string };
            setError(error.response?.data?.error || error.message || `Failed to export report as ${format}`);
        } finally {
            setExporting(false);
        }
    };

    const handleShowMetadata = async () => {
        setShowMetadata(true);
        if (metadata) return;
        setMetadataLoading(true);
        setMetadataError(null);
        try {
            const data = await reportsApi.getMetadata(parseInt(id!));
            setMetadata(data);
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string } }; message?: string };
            setMetadataError(e.response?.data?.error || e.message || 'Failed to extract metadata');
        } finally {
            setMetadataLoading(false);
        }
    };

    // Suppress unused variable warnings for zoom/rotation controls
    void handleZoomIn;
    void handleZoomOut;
    void handleResetZoom;
    void handleRotate;

    // ═════════════════════════════
    // ── RENDER
    // ═════════════════════════════

    // ── Simple full-screen spinner while initializing or compiling with no content yet ──
    if (initializing || (compiling && !pdfUrl)) {
        return (
            <div
                className="fade-in"
                style={{
                    height: 'calc(100vh - 80px)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: 'linear-gradient(145deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)',
                }}
            >
                <div style={{ textAlign: 'center' }}>
                    {/* Spinner ring */}
                    <div style={{ position: 'relative', width: '56px', height: '56px', margin: '0 auto 24px' }}>
                        <div
                            style={{
                                position: 'absolute',
                                inset: 0,
                                borderRadius: '50%',
                                border: '3px solid rgba(99, 102, 241, 0.15)',
                            }}
                        />
                        <div
                            className="animate-spin"
                            style={{
                                position: 'absolute',
                                inset: 0,
                                borderRadius: '50%',
                                border: '3px solid transparent',
                                borderTopColor: '#818cf8',
                            }}
                        />
                    </div>

                    <p
                        style={{
                            fontSize: '0.95rem',
                            fontWeight: 500,
                            color: '#94a3b8',
                            letterSpacing: '0.01em',
                        }}
                    >
                        Loading your report…
                    </p>
                </div>
            </div>
        );
    }

    // ── Fatal error (no report data) ──
    if (error && !report) {
        return (
            <div className="text-center py-16">
                <div className="w-16 h-16 bg-red-100 dark:bg-red-900/30 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                </div>
                <h2 className="text-xl font-semibold mb-2">Error</h2>
                <p className="text-gray-600 dark:text-gray-400 mb-4">{error}</p>
                <button onClick={() => navigate('/')} className="btn-primary">
                    Back to Reports
                </button>
            </div>
        );
    }

    // ═══════════════════════════════════════
    // ── Main viewer layout
    // ═══════════════════════════════════════
    return (
        <div className="fade-in flex h-[calc(100vh-80px)]" style={{ gap: '0px' }}>
            {/* ── Left sidebar: Report Parameters ── */}
            {hasVisibleParams && id && paramMetadata && (
                <ReportParameterForm
                    reportId={parseInt(id)}
                    onRunReport={handleApplyParameters}
                    running={compiling}
                    initialValues={initialParamValues}
                    preloadedMetadata={paramMetadata}
                />
            )}

            {/* ── Right: toolbar + content ── */}
            <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
                {/* Toolbar */}
                <div className="flex items-center justify-between px-5 py-3 bg-white dark:bg-gray-800/90 border-b border-gray-100 dark:border-gray-800 flex-shrink-0">
                    {/* Left: Back button + report name */}
                    <div className="flex items-center space-x-3">
                        <button
                            onClick={() => navigate('/')}
                            className="inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                            </svg>
                            Back to Reports
                        </button>
                        <div className="hidden sm:block h-6 w-px bg-gray-300 dark:bg-gray-600" />
                        <div className="hidden sm:block">
                            <h1 className="text-sm font-semibold truncate max-w-[200px]">
                                {report?.display_name || report?.report_name}
                            </h1>
                            <p className="text-xs text-gray-500 truncate max-w-[200px]">
                                {report?.repo_name} / {report?.path}
                            </p>
                        </div>
                    </div>

                    {/* Right: Actions */}
                    <div className="flex items-center space-x-2">
                        <button
                            onClick={handleRecompile}
                            disabled={compiling}
                            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 hover:bg-primary-100 dark:hover:bg-primary-900/50 transition-colors disabled:opacity-50"
                            title="Re-run Report"
                        >
                            {compiling ? (
                                <div className="w-4 h-4 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
                            ) : (
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                </svg>
                            )}
                            <span className="hidden sm:inline">{compiling ? 'Compiling...' : 'Re-run'}</span>
                        </button>

                        {/* Print */}
                        <button
                            onClick={handlePrint}
                            disabled={!pdfUrl || compiling}
                            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-slate-50 dark:bg-slate-800/60 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                            title="Print Report"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
                            </svg>
                            <span className="hidden sm:inline">Print</span>
                        </button>

                        {/* Audit - admin only */}
                        {user?.is_admin && (
                            <button
                                onClick={() => setShowAudit(true)}
                                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 hover:bg-amber-100 dark:hover:bg-amber-900/50 transition-colors"
                                title="Audit & Logging"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                                </svg>
                                <span className="hidden sm:inline">Audit</span>
                            </button>
                        )}

                        {/* Info */}
                        <button
                            onClick={handleShowMetadata}
                            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/50 transition-colors"
                            title="Report Metadata"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <span className="hidden sm:inline">Info</span>
                        </button>

                        {/* Schedule */}
                        <button
                            onClick={() => setShowSchedule(true)}
                            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-violet-50 dark:bg-violet-900/30 text-violet-700 dark:text-violet-300 hover:bg-violet-100 dark:hover:bg-violet-900/50 transition-colors"
                            title="Schedule Report"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <span className="hidden sm:inline">Schedule</span>
                        </button>

                        {/* Embed */}
                        <button
                            onClick={() => setShowEmbed(true)}
                            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-cyan-50 dark:bg-cyan-900/30 text-cyan-700 dark:text-cyan-300 hover:bg-cyan-100 dark:hover:bg-cyan-900/50 transition-colors"
                            title="Embed & API Access"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                            </svg>
                        </button>

                        {/* Export */}
                        <div className="relative" ref={exportMenuRef}>
                            <button
                                onClick={() => setShowExportMenu(prev => !prev)}
                                disabled={exporting || compiling}
                                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 hover:bg-green-100 dark:hover:bg-green-900/50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                                title="Export Report"
                            >
                                {exporting ? (
                                    <div className="w-4 h-4 border-2 border-green-500 border-t-transparent rounded-full animate-spin" />
                                ) : (
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                    </svg>
                                )}
                                <span className="hidden sm:inline">{exporting ? 'Exporting...' : 'Export'}</span>
                                <svg className={`w-3 h-3 ml-1 transition-transform ${showExportMenu ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                </svg>
                            </button>

                            {showExportMenu && (
                                <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-md shadow-lg z-10">
                                    <div className="py-1">
                                        {EXPORT_FORMATS.map(format => (
                                            <button
                                                key={format.key}
                                                onClick={() => handleExport(format.key)}
                                                className="flex items-center w-full px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-600"
                                            >
                                                <span className={`mr-2 ${format.color}`}>{format.icon}</span>
                                                {format.label}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* Error bar */}
                {error && (
                    <div className="mx-4 mt-3 p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm flex items-center gap-2 flex-shrink-0">
                        <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                        </svg>
                        <span>{error}</span>
                        <button
                            onClick={handleRetry}
                            className="ml-auto text-xs font-medium bg-red-200 dark:bg-red-800 px-2 py-1 rounded hover:bg-red-300 dark:hover:bg-red-700 transition-colors"
                        >
                            Retry
                        </button>
                    </div>
                )}

                {/* Content Area */}
                <div className="flex-1 overflow-auto bg-gray-100 dark:bg-gray-900">
                    {compiling ? (
                        <div className="h-full flex items-center justify-center">
                            <div className="text-center">
                                <div className="relative">
                                    <div className="w-16 h-16 border-4 border-primary-200 dark:border-primary-800 rounded-full" />
                                    <div className="absolute top-0 left-0 w-16 h-16 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
                                </div>
                                <h3 className="mt-6 text-lg font-semibold">Compiling Report</h3>
                                <p className="mt-2 text-gray-500 dark:text-gray-400 text-sm">
                                    Please wait while the report is being compiled...
                                </p>
                                <div className="mt-4 flex items-center justify-center gap-1">
                                    <div className="w-2 h-2 bg-primary-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                                    <div className="w-2 h-2 bg-primary-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                    <div className="w-2 h-2 bg-primary-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                                </div>
                            </div>
                        </div>
                    ) : pdfUrl ? (
                        <div className="h-full flex items-start justify-center overflow-auto p-4">
                            <iframe
                                ref={iframeRef}
                                src={pdfUrl}
                                className="bg-white shadow-2xl rounded-lg border border-gray-200 dark:border-gray-700"
                                style={{
                                    width: `${zoom}%`,
                                    height: `${zoom}%`,
                                    minWidth: '300px',
                                    minHeight: '600px',
                                    transform: `rotate(${rotation}deg)`,
                                    transformOrigin: 'center center',
                                    transition: 'transform 0.3s ease, width 0.3s ease, height 0.3s ease',
                                }}
                                title="Report PDF"
                            />
                        </div>
                    ) : !error ? (
                        <div className="h-full flex items-center justify-center">
                            <div className="text-center">
                                <div className="w-20 h-20 bg-gray-200 dark:bg-gray-700 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                    </svg>
                                </div>
                                <h3 className="text-lg font-semibold mb-2">No output yet</h3>
                                <p className="text-gray-600 dark:text-gray-400 mb-4">
                                    Report compilation has not started.
                                </p>
                                <button onClick={handleRetry} className="btn-primary">
                                    Run Report
                                </button>
                            </div>
                        </div>
                    ) : null}
                </div>
            </div>

            {/* Metadata Sidebar */}
            <MetadataSidebar
                isOpen={showMetadata}
                onClose={() => setShowMetadata(false)}
                metadata={metadata}
                loading={metadataLoading}
                error={metadataError}
            />

            {/* Audit Log Sidebar */}
            {user?.is_admin && (
                <AuditLogSidebar
                    isOpen={showAudit}
                    onClose={() => setShowAudit(false)}
                />
            )}

            {/* Schedule Modal */}
            <ScheduleModal
                isOpen={showSchedule}
                onClose={() => setShowSchedule(false)}
                reportName={report?.display_name || report?.report_name || ''}
                reportUri={report?.path || ''}
                reportId={report?.id}
            />

            {/* Parameter Dialog Modal */}
            {paramMetadata && id && (
                <ParameterDialogModal
                    open={showParamDialog}
                    reportId={parseInt(id)}
                    reportName={report?.display_name || report?.report_name || ''}
                    metadata={paramMetadata}
                    onSubmit={handleDialogSubmit}
                    onCancel={handleDialogCancel}
                    submitting={compiling}
                />
            )}

            {/* Embed & API Dialog */}
            {id && (
                <EmbedDialog
                    isOpen={showEmbed}
                    onClose={() => setShowEmbed(false)}
                    reportId={parseInt(id)}
                    reportName={report?.display_name || report?.report_name || ''}
                />
            )}
        </div>
    );
}
