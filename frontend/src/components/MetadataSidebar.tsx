import type { MetadataResponse } from '../types';

interface MetadataSidebarProps {
    isOpen: boolean;
    onClose: () => void;
    metadata: MetadataResponse | null;
    loading: boolean;
    error: string | null;
}

export default function MetadataSidebar({ isOpen, onClose, metadata, loading, error }: MetadataSidebarProps) {
    if (!isOpen) return null;

    return (
        <>
            {/* Backdrop */}
            <div
                className="fixed inset-0 bg-black/30 backdrop-blur-sm z-40 transition-opacity"
                onClick={onClose}
            />

            {/* Sidebar */}
            <div className="fixed top-0 right-0 h-full w-[420px] max-w-full bg-white dark:bg-gray-800 shadow-2xl z-50 flex flex-col slide-in-right">
                {/* Header */}
                <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200 dark:border-gray-700">
                    <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                            <svg className="w-4 h-4 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h2 className="text-lg font-semibold">Report Metadata</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-5 space-y-4">
                    {loading && (
                        <div className="flex items-center justify-center py-16">
                            <div className="text-center">
                                <div className="w-10 h-10 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto" />
                                <p className="mt-3 text-sm text-gray-500">Extracting metadata...</p>
                            </div>
                        </div>
                    )}

                    {error && (
                        <div className="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
                            {error}
                        </div>
                    )}

                    {metadata && !loading && (
                        <>
                            {/* Report Name */}
                            <div className="p-4 rounded-xl bg-gradient-to-r from-primary-50 to-primary-100 dark:from-primary-900/20 dark:to-primary-900/10 border border-primary-200 dark:border-primary-800">
                                <p className="text-xs font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">Report</p>
                                <p className="text-lg font-semibold mt-1">{metadata.reportName || 'Unnamed'}</p>
                            </div>

                            {/* Page Layout */}
                            <Section title="Page Layout" icon="📐">
                                <div className="grid grid-cols-2 gap-3">
                                    <MetaItem label="Width" value={`${metadata.pageWidth}px`} />
                                    <MetaItem label="Height" value={`${metadata.pageHeight}px`} />
                                    <MetaItem label="Column Width" value={`${metadata.columnWidth}px`} />
                                    <MetaItem label="Margins" value={`${metadata.topMargin} ${metadata.rightMargin} ${metadata.bottomMargin} ${metadata.leftMargin}`} />
                                </div>
                            </Section>

                            {/* Data Source */}
                            {(metadata.queryLanguage || metadata.dataAdapterName) && (
                                <Section title="Data Source" icon="🔌">
                                    {metadata.dataAdapterName && (
                                        <MetaItem label="Adapter" value={metadata.dataAdapterName} />
                                    )}
                                    {metadata.queryLanguage && (
                                        <MetaItem label="Language" value={metadata.queryLanguage} />
                                    )}
                                    {metadata.queryText && (
                                        <div className="mt-2">
                                            <p className="text-xs text-gray-500 mb-1">Query</p>
                                            <pre className="text-xs bg-gray-100 dark:bg-gray-900 p-3 rounded-lg overflow-x-auto font-mono max-h-32 overflow-y-auto">
                                                {metadata.queryText}
                                            </pre>
                                        </div>
                                    )}
                                </Section>
                            )}

                            {/* Parameters */}
                            {metadata.parameters.length > 0 && (
                                <Section title={`Parameters (${metadata.parameters.length})`} icon="⚙️">
                                    <div className="space-y-2">
                                        {metadata.parameters.map((p, i) => (
                                            <div key={i} className="p-2.5 rounded-lg bg-gray-50 dark:bg-gray-900/50 border border-gray-100 dark:border-gray-700/50">
                                                <div className="flex items-center justify-between">
                                                    <span className="text-sm font-medium">{p.name}</span>
                                                    <span className="text-xs px-1.5 py-0.5 rounded bg-gray-200 dark:bg-gray-700 font-mono">
                                                        {p.className?.split('.').pop()}
                                                    </span>
                                                </div>
                                                {p.defaultValueExpression && (
                                                    <p className="text-xs text-gray-500 mt-1 font-mono truncate" title={p.defaultValueExpression}>
                                                        = {p.defaultValueExpression}
                                                    </p>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </Section>
                            )}

                            {/* Fields */}
                            {metadata.fields.length > 0 && (
                                <Section title={`Fields (${metadata.fields.length})`} icon="📋">
                                    <div className="space-y-1.5">
                                        {metadata.fields.map((f, i) => (
                                            <div key={i} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-900/30 transition-colors">
                                                <div>
                                                    <span className="text-sm font-medium">{f.name}</span>
                                                    {f.description && (
                                                        <p className="text-xs text-gray-400">{f.description}</p>
                                                    )}
                                                </div>
                                                <span className="text-xs px-1.5 py-0.5 rounded bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 font-mono">
                                                    {f.className?.split('.').pop()}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </Section>
                            )}

                            {/* Variables */}
                            {metadata.variables.length > 0 && (
                                <Section title={`Variables (${metadata.variables.length})`} icon="🔢">
                                    <div className="space-y-1.5">
                                        {metadata.variables.map((v, i) => (
                                            <div key={i} className="p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-900/30 transition-colors">
                                                <div className="flex items-center justify-between">
                                                    <span className="text-sm font-medium">{v.name}</span>
                                                    <div className="flex items-center gap-1">
                                                        {v.calculation && (
                                                            <span className="text-xs px-1.5 py-0.5 rounded bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300">
                                                                {v.calculation}
                                                            </span>
                                                        )}
                                                        <span className="text-xs px-1.5 py-0.5 rounded bg-gray-200 dark:bg-gray-700 font-mono">
                                                            {v.className?.split('.').pop()}
                                                        </span>
                                                    </div>
                                                </div>
                                                {v.expression && (
                                                    <p className="text-xs text-gray-500 mt-1 font-mono truncate" title={v.expression}>
                                                        {v.expression}
                                                    </p>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </Section>
                            )}

                            {/* Bands */}
                            {metadata.bands.length > 0 && (
                                <Section title={`Bands (${metadata.bands.length})`} icon="📊">
                                    <div className="space-y-1.5">
                                        {metadata.bands.map((b, i) => (
                                            <div key={i} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-900/30 transition-colors">
                                                <span className="text-sm font-medium capitalize">{b.type}</span>
                                                <div className="flex items-center gap-2 text-xs text-gray-500">
                                                    <span>{b.height}px</span>
                                                    <span className="px-1.5 py-0.5 rounded bg-gray-200 dark:bg-gray-700">
                                                        {b.elementCount} elements
                                                    </span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </Section>
                            )}

                            {/* Charts */}
                            {metadata.charts.length > 0 && (
                                <Section title={`Charts (${metadata.charts.length})`} icon="📈">
                                    <div className="flex flex-wrap gap-2">
                                        {metadata.charts.map((c, i) => (
                                            <span key={i} className="text-xs px-2.5 py-1 rounded-full bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 font-medium">
                                                {c}
                                            </span>
                                        ))}
                                    </div>
                                </Section>
                            )}

                            {/* Sub-reports */}
                            {metadata.subReports.length > 0 && (
                                <Section title={`Sub-Reports (${metadata.subReports.length})`} icon="📑">
                                    <div className="space-y-1.5">
                                        {metadata.subReports.map((s, i) => (
                                            <div key={i} className="p-2 rounded-lg bg-gray-50 dark:bg-gray-900/50">
                                                <p className="text-xs font-mono text-gray-600 dark:text-gray-400 break-all">{s}</p>
                                            </div>
                                        ))}
                                    </div>
                                </Section>
                            )}
                        </>
                    )}
                </div>
            </div>
        </>
    );
}

function Section({ title, icon, children }: { title: string; icon: string; children: React.ReactNode }) {
    return (
        <div className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
            <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-700">
                <h3 className="text-sm font-semibold flex items-center gap-2">
                    <span>{icon}</span>
                    {title}
                </h3>
            </div>
            <div className="p-3">{children}</div>
        </div>
    );
}

function MetaItem({ label, value }: { label: string; value: string }) {
    return (
        <div>
            <p className="text-xs text-gray-500">{label}</p>
            <p className="text-sm font-medium">{value}</p>
        </div>
    );
}
