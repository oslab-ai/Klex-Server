import { useEffect, useRef, useState } from 'react';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';
import { useTheme } from '../context/ThemeContext';

export default function Embeddings() {
    const { theme } = useTheme();
    const containerRef = useRef<HTMLDivElement>(null);
    const [specUrl] = useState(() => {
        const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000';
        return `${base}/api/reports/openapi.json`;
    });

    // Inject dark-mode overrides when theme changes
    useEffect(() => {
        const styleId = 'swagger-theme-overrides';
        let styleEl = document.getElementById(styleId) as HTMLStyleElement | null;
        if (!styleEl) {
            styleEl = document.createElement('style');
            styleEl.id = styleId;
            document.head.appendChild(styleEl);
        }

        if (theme === 'dark') {
            styleEl.textContent = `
                /* ── Swagger UI Dark Mode ─────────────────────────── */
                .swagger-ui .topbar { display: none !important; }

                .swagger-ui,
                .swagger-ui .wrapper,
                .swagger-ui .opblock-tag,
                .swagger-ui .opblock-description-wrapper,
                .swagger-ui .info .title,
                .swagger-ui .info .description p,
                .swagger-ui .opblock .opblock-summary-description,
                .swagger-ui .opblock .opblock-summary-path,
                .swagger-ui .response-col_description__inner p,
                .swagger-ui .parameter__name,
                .swagger-ui .parameter__type,
                .swagger-ui table thead tr td,
                .swagger-ui table thead tr th,
                .swagger-ui .model-title,
                .swagger-ui .model,
                .swagger-ui .model span,
                .swagger-ui section.models h4,
                .swagger-ui .response-col_status,
                .swagger-ui .tab li,
                .swagger-ui .opblock-section-header h4 {
                    color: #e2e8f0 !important;
                }

                .swagger-ui .scheme-container,
                .swagger-ui .opblock .opblock-section-header {
                    background: #1e293b !important;
                    border-color: #334155 !important;
                }

                .swagger-ui .opblock {
                    border-color: #334155 !important;
                    background: #0f172a !important;
                }

                .swagger-ui .opblock.opblock-get {
                    border-color: #3b82f6 !important;
                    background: rgba(59, 130, 246, 0.06) !important;
                }
                .swagger-ui .opblock.opblock-get .opblock-summary-method {
                    background: #3b82f6 !important;
                }
                .swagger-ui .opblock.opblock-get .opblock-summary {
                    border-color: rgba(59, 130, 246, 0.2) !important;
                }

                .swagger-ui .opblock.opblock-post {
                    border-color: #22c55e !important;
                    background: rgba(34, 197, 94, 0.06) !important;
                }
                .swagger-ui .opblock.opblock-post .opblock-summary-method {
                    background: #22c55e !important;
                }
                .swagger-ui .opblock.opblock-post .opblock-summary {
                    border-color: rgba(34, 197, 94, 0.2) !important;
                }

                .swagger-ui .opblock.opblock-delete {
                    border-color: #ef4444 !important;
                    background: rgba(239, 68, 68, 0.06) !important;
                }
                .swagger-ui .opblock.opblock-delete .opblock-summary-method {
                    background: #ef4444 !important;
                }
                .swagger-ui .opblock.opblock-delete .opblock-summary {
                    border-color: rgba(239, 68, 68, 0.2) !important;
                }

                .swagger-ui .opblock.opblock-patch {
                    border-color: #f59e0b !important;
                    background: rgba(245, 158, 11, 0.06) !important;
                }
                .swagger-ui .opblock.opblock-patch .opblock-summary-method {
                    background: #f59e0b !important;
                }
                .swagger-ui .opblock.opblock-patch .opblock-summary {
                    border-color: rgba(245, 158, 11, 0.2) !important;
                }

                .swagger-ui .opblock-body pre,
                .swagger-ui .highlight-code {
                    background: #1e293b !important;
                    color: #e2e8f0 !important;
                    border-radius: 8px !important;
                }

                .swagger-ui .model-box {
                    background: #1e293b !important;
                }

                .swagger-ui section.models {
                    border-color: #334155 !important;
                }

                .swagger-ui section.models.is-open h4 {
                    border-color: #334155 !important;
                }

                .swagger-ui input[type=text],
                .swagger-ui textarea,
                .swagger-ui select {
                    background: #1e293b !important;
                    color: #e2e8f0 !important;
                    border-color: #475569 !important;
                }

                .swagger-ui .btn {
                    color: #e2e8f0 !important;
                    border-color: #475569 !important;
                }

                .swagger-ui .btn.execute {
                    background: #6366f1 !important;
                    border-color: #6366f1 !important;
                    color: #fff !important;
                }

                .swagger-ui .responses-inner {
                    background: transparent !important;
                }

                .swagger-ui .markdown p,
                .swagger-ui .markdown h3,
                .swagger-ui .markdown h4,
                .swagger-ui .renderedMarkdown p {
                    color: #cbd5e1 !important;
                }

                .swagger-ui .info a {
                    color: #818cf8 !important;
                }

                .swagger-ui .opblock-tag:hover {
                    background: rgba(99, 102, 241, 0.08) !important;
                }

                .swagger-ui .opblock-tag {
                    border-bottom-color: #1e293b !important;
                }

                .swagger-ui .response-col_links {
                    color: #94a3b8 !important;
                }
            `;
        } else {
            styleEl.textContent = `
                /* ── Swagger UI Light Mode Overrides ──────────────── */
                .swagger-ui .topbar { display: none !important; }

                .swagger-ui .btn.execute {
                    background: #6366f1 !important;
                    border-color: #6366f1 !important;
                    color: #fff !important;
                }

                .swagger-ui .opblock-tag:hover {
                    background: rgba(99, 102, 241, 0.04) !important;
                }

                .swagger-ui .info a {
                    color: #6366f1 !important;
                }
            `;
        }

        return () => {
            // Cleanup only on unmount
        };
    }, [theme]);

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                        Embeddings
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">
                        API reference for embedding Klex reporting services into your applications
                    </p>
                </div>
                <a
                    href="https://github.com/teenybopper/KlexReportingService"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-2 px-4 py-2 bg-surface-100 dark:bg-surface-800 text-gray-700 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-surface-200 dark:hover:bg-surface-700 transition-colors border border-gray-200 dark:border-gray-700"
                >
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                    </svg>
                    Java Engine Repo
                </a>
            </div>

            {/* Service Group Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {[
                    { icon: '📄', label: 'Report Execution', desc: 'Compile & download reports', count: 6 },
                    { icon: '🔍', label: 'Metadata & Params', desc: 'JRXML structure extraction', count: 3 },
                    { icon: '📈', label: 'Audit & Stats', desc: 'Execution logs & perf metrics', count: 2 },
                    { icon: '⏱️', label: 'Scheduling', desc: 'Quartz / Airflow jobs', count: 10 },
                ].map((card) => (
                    <div
                        key={card.label}
                        className="bg-white dark:bg-surface-800 rounded-2xl p-4 border border-gray-100 dark:border-gray-800 shadow-sm hover:shadow-md transition-shadow"
                    >
                        <div className="flex items-start gap-3">
                            <span className="text-2xl">{card.icon}</span>
                            <div>
                                <h3 className="font-semibold text-gray-900 dark:text-gray-100 text-sm">
                                    {card.label}
                                </h3>
                                <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                                    {card.desc}
                                </p>
                                <span className="inline-block mt-2 text-xs font-medium px-2 py-0.5 rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400">
                                    {card.count} endpoints
                                </span>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Auth Banner */}
            <div className="bg-gradient-to-r from-primary-50 to-indigo-50 dark:from-primary-900/20 dark:to-indigo-900/20 rounded-2xl p-4 border border-primary-100 dark:border-primary-800/40">
                <div className="flex items-start gap-3">
                    <svg className="w-5 h-5 text-primary-600 dark:text-primary-400 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    <div>
                        <h4 className="text-sm font-semibold text-primary-800 dark:text-primary-300">
                            Authentication Required
                        </h4>
                        <p className="text-xs text-primary-700 dark:text-primary-400 mt-0.5">
                            All endpoints require a JWT Bearer token. Obtain one via{' '}
                            <code className="bg-primary-100 dark:bg-primary-900/40 px-1.5 py-0.5 rounded text-xs font-mono">
                                POST /api/auth/login/
                            </code>{' '}
                            and pass it in the <code className="bg-primary-100 dark:bg-primary-900/40 px-1.5 py-0.5 rounded text-xs font-mono">Authorization: Bearer &lt;token&gt;</code> header.
                        </p>
                    </div>
                </div>
            </div>

            {/* Swagger UI */}
            <div
                ref={containerRef}
                className="bg-white dark:bg-surface-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden"
            >
                <SwaggerUI
                    url={specUrl}
                    docExpansion="list"
                    defaultModelsExpandDepth={-1}
                    filter={true}
                    tryItOutEnabled={false}
                    deepLinking={true}
                />
            </div>
        </div>
    );
}
