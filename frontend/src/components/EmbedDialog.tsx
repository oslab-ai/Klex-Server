import React, { useState, useCallback, useRef } from 'react';

interface EmbedDialogProps {
    isOpen: boolean;
    onClose: () => void;
    reportId: number;
    reportName: string;
}

interface EndpointCard {
    key: string;
    title: string;
    description: string;
    method: 'GET' | 'POST';
    path: string;
    sampleBody?: string;
    sampleResponse?: string;
    icon: React.ReactNode;
    color: string;
}

const EmbedDialog: React.FC<EmbedDialogProps> = ({
    isOpen,
    onClose,
    reportId,
    reportName,
}) => {
    const [copiedKey, setCopiedKey] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<string>('compile');
    const dialogRef = useRef<HTMLDivElement>(null);

    const baseUrl = window.location.origin;

    const copyToClipboard = useCallback(async (text: string, key: string) => {
        try {
            await navigator.clipboard.writeText(text);
            setCopiedKey(key);
            setTimeout(() => setCopiedKey(null), 2000);
        } catch {
            // Fallback for older browsers
            const ta = document.createElement('textarea');
            ta.value = text;
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            document.body.removeChild(ta);
            setCopiedKey(key);
            setTimeout(() => setCopiedKey(null), 2000);
        }
    }, []);

    const endpoints: EndpointCard[] = [
        {
            key: 'compile',
            title: 'Compile Report',
            description: 'Generate the report output in PDF, XLSX, CSV, HTML, DOCX, PPTX or ODT format. Pass optional parameters and format.',
            method: 'POST',
            path: `/api/reports/${reportId}/compile/`,
            sampleBody: JSON.stringify(
                {
                    format: 'PDF',
                    parameters: { param_name: 'value' },
                },
                null,
                2,
            ),
            sampleResponse: JSON.stringify(
                {
                    execution_id: 42,
                    status: 'success',
                    output_url: `/api/reports/${reportId}/download/42/`,
                    message: 'PDF report compiled successfully',
                    format: 'pdf',
                },
                null,
                2,
            ),
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
            ),
            color: 'emerald',
        },
        {
            key: 'metadata',
            title: 'Report Metadata',
            description: 'Extract report metadata including fields, variables, data source info, and structure from the JRXML definition.',
            method: 'POST',
            path: `/api/reports/${reportId}/metadata/`,
            sampleBody: undefined,
            sampleResponse: JSON.stringify(
                {
                    reportName: reportName,
                    fields: ['field1', 'field2'],
                    variables: [],
                    dataSource: { type: 'jdbc' },
                },
                null,
                2,
            ),
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
            ),
            color: 'blue',
        },
        {
            key: 'schedule',
            title: 'Schedule Report',
            description: 'Create automated schedules to generate this report periodically with a cron expression, parameters, and email delivery.',
            method: 'POST',
            path: `/api/reports/schedule/`,
            sampleBody: JSON.stringify(
                {
                    report_id: reportId,
                    report_name: reportName,
                    cron_expression: '0 8 * * 1',
                    format: 'PDF',
                    parameters: {},
                    email_recipients: ['user@example.com'],
                },
                null,
                2,
            ),
            sampleResponse: JSON.stringify(
                {
                    id: 1,
                    status: 'scheduled',
                    message: 'Report scheduled successfully',
                },
                null,
                2,
            ),
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
            ),
            color: 'violet',
        },
    ];

    const activeEndpoint = endpoints.find((e) => e.key === activeTab)!;

    const buildCurlCommand = (ep: EndpointCard) => {
        const url = `${baseUrl}${ep.path}`;
        const parts = [
            `curl -X ${ep.method}`,
            `  "${url}"`,
            `  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"`,
            `  -H "Content-Type: application/json"`,
        ];
        if (ep.sampleBody) {
            parts.push(`  -d '${ep.sampleBody}'`);
        }
        return parts.join(' \\\n');
    };

    const buildFetchSnippet = (ep: EndpointCard) => {
        const url = `${baseUrl}${ep.path}`;
        let code = `const response = await fetch("${url}", {\n`;
        code += `  method: "${ep.method}",\n`;
        code += `  headers: {\n`;
        code += `    "Authorization": "Bearer <YOUR_ACCESS_TOKEN>",\n`;
        code += `    "Content-Type": "application/json",\n`;
        code += `  },\n`;
        if (ep.sampleBody) {
            code += `  body: JSON.stringify(${ep.sampleBody}),\n`;
        }
        code += `});\n\n`;
        code += `const data = await response.json();\nconsole.log(data);`;
        return code;
    };

    const buildPythonSnippet = (ep: EndpointCard) => {
        const url = `${baseUrl}${ep.path}`;
        let code = `import requests\n\n`;
        code += `url = "${url}"\n`;
        code += `headers = {\n`;
        code += `    "Authorization": "Bearer <YOUR_ACCESS_TOKEN>",\n`;
        code += `    "Content-Type": "application/json",\n`;
        code += `}\n`;

        if (ep.sampleBody) {
            code += `payload = ${ep.sampleBody}\n\n`;
            code += `response = requests.post(url, json=payload, headers=headers)\n`;
        } else {
            code += `\nresponse = requests.${ep.method.toLowerCase()}(url, headers=headers)\n`;
        }
        code += `print(response.json())`;
        return code;
    };

    if (!isOpen) return null;

    const colorMap: Record<string, {
        activeBg: string;
        activeText: string;
        badge: string;
        badgeInactive: string;
    }> = {
        emerald: {
            activeBg: 'bg-emerald-50 dark:bg-emerald-500/10',
            activeText: 'text-emerald-600 dark:text-emerald-400',
            badge: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-300',
            badgeInactive: 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-500',
        },
        blue: {
            activeBg: 'bg-blue-50 dark:bg-blue-500/10',
            activeText: 'text-blue-600 dark:text-blue-400',
            badge: 'bg-blue-100 text-blue-700 dark:bg-blue-500/20 dark:text-blue-300',
            badgeInactive: 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-500',
        },
        violet: {
            activeBg: 'bg-violet-50 dark:bg-violet-500/10',
            activeText: 'text-violet-600 dark:text-violet-400',
            badge: 'bg-violet-100 text-violet-700 dark:bg-violet-500/20 dark:text-violet-300',
            badgeInactive: 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-500',
        },
    };

    const colors = colorMap[activeEndpoint.color] || colorMap.emerald;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center"
            onClick={(e) => {
                if (e.target === e.currentTarget) onClose();
            }}
        >
            {/* Backdrop */}
            <div className="absolute inset-0 bg-black/40 dark:bg-black/60 backdrop-blur-sm" />

            {/* Dialog */}
            <div
                ref={dialogRef}
                className="relative w-full max-w-6xl max-h-[90vh] mx-4 overflow-hidden rounded-2xl shadow-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700/50"
            >
                {/* Header */}
                <div className="flex items-center justify-between px-8 py-5 border-b border-gray-200 dark:border-gray-700/50">
                    <div className="flex items-center gap-3">
                        <div
                            className="w-10 h-10 rounded-xl flex items-center justify-center"
                            style={{
                                background: 'linear-gradient(135deg, #6366f1, #818cf8)',
                            }}
                        >
                            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                            </svg>
                        </div>
                        <div>
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Embed & API Access</h2>
                            <p className="text-sm text-gray-500 dark:text-gray-400">
                                {reportName}
                                <span className="text-gray-300 dark:text-gray-600 mx-1.5">·</span>
                                <span className="text-gray-400 dark:text-gray-500">ID {reportId}</span>
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-lg text-gray-400 dark:text-gray-400 hover:text-gray-700 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700/50 transition-colors"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Horizontal tabs at the top */}
                <div className="flex items-center gap-1 px-8 border-b border-gray-200 dark:border-gray-700/50 bg-gray-50 dark:bg-gray-800/30">
                    {endpoints.map((ep) => {
                        const isActive = activeTab === ep.key;
                        const epColors = colorMap[ep.color] || colorMap.emerald;
                        return (
                            <button
                                key={ep.key}
                                onClick={() => setActiveTab(ep.key)}
                                className={`
                                    flex items-center gap-2 px-5 py-3.5 text-sm font-medium transition-all duration-150 border-b-2 -mb-px
                                    ${isActive
                                        ? `${epColors.activeText} border-current`
                                        : 'text-gray-500 dark:text-gray-500 border-transparent hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600'
                                    }
                                `}
                            >
                                {ep.icon}
                                {ep.title}
                                <span
                                    className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${
                                        isActive ? epColors.badge : epColors.badgeInactive
                                    }`}
                                >
                                    {ep.method}
                                </span>
                            </button>
                        );
                    })}
                </div>

                {/* Scrollable content area */}
                <div className="overflow-y-auto p-8 space-y-5" style={{ maxHeight: 'calc(90vh - 160px)' }}>
                    {/* Endpoint header */}
                    <div>
                        <div className="flex items-center gap-2 mb-2">
                            <span className={`px-2 py-0.5 rounded text-xs font-bold ${colors.badge}`}>
                                {activeEndpoint.method}
                            </span>
                            <code className="text-sm text-gray-700 dark:text-gray-300 font-mono break-all">
                                {baseUrl}
                                {activeEndpoint.path}
                            </code>
                            <button
                                onClick={() =>
                                    copyToClipboard(
                                        `${baseUrl}${activeEndpoint.path}`,
                                        `url-${activeEndpoint.key}`,
                                    )
                                }
                                className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-700/50 text-gray-400 dark:text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 transition-colors flex-shrink-0"
                                title="Copy URL"
                            >
                                {copiedKey === `url-${activeEndpoint.key}` ? (
                                    <svg className="w-4 h-4 text-emerald-500 dark:text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                ) : (
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                    </svg>
                                )}
                            </button>
                        </div>
                        <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">
                            {activeEndpoint.description}
                        </p>
                    </div>

                    {/* Request body + Response side by side when both exist */}
                    {activeEndpoint.sampleBody && activeEndpoint.sampleResponse ? (
                        <div className="grid grid-cols-2 gap-4">
                            <CodeBlock
                                title="Request Body"
                                code={activeEndpoint.sampleBody}
                                language="json"
                                copyKey={`body-${activeEndpoint.key}`}
                                copiedKey={copiedKey}
                                onCopy={copyToClipboard}
                            />
                            <CodeBlock
                                title="Sample Response"
                                code={activeEndpoint.sampleResponse}
                                language="json"
                                copyKey={`resp-${activeEndpoint.key}`}
                                copiedKey={copiedKey}
                                onCopy={copyToClipboard}
                            />
                        </div>
                    ) : (
                        <>
                            {activeEndpoint.sampleBody && (
                                <CodeBlock
                                    title="Request Body"
                                    code={activeEndpoint.sampleBody}
                                    language="json"
                                    copyKey={`body-${activeEndpoint.key}`}
                                    copiedKey={copiedKey}
                                    onCopy={copyToClipboard}
                                />
                            )}
                            {activeEndpoint.sampleResponse && (
                                <CodeBlock
                                    title="Sample Response"
                                    code={activeEndpoint.sampleResponse}
                                    language="json"
                                    copyKey={`resp-${activeEndpoint.key}`}
                                    copiedKey={copiedKey}
                                    onCopy={copyToClipboard}
                                />
                            )}
                        </>
                    )}

                    {/* Divider */}
                    <div className="border-t border-gray-200 dark:border-gray-700/50 pt-5">
                        <p className="text-[11px] font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-3">
                            Code Snippets
                        </p>
                    </div>

                    {/* cURL */}
                    <CodeBlock
                        title="cURL"
                        code={buildCurlCommand(activeEndpoint)}
                        language="bash"
                        copyKey={`curl-${activeEndpoint.key}`}
                        copiedKey={copiedKey}
                        onCopy={copyToClipboard}
                    />

                    {/* JavaScript */}
                    <CodeBlock
                        title="JavaScript (fetch)"
                        code={buildFetchSnippet(activeEndpoint)}
                        language="javascript"
                        copyKey={`js-${activeEndpoint.key}`}
                        copiedKey={copiedKey}
                        onCopy={copyToClipboard}
                    />

                    {/* Python */}
                    <CodeBlock
                        title="Python (requests)"
                        code={buildPythonSnippet(activeEndpoint)}
                        language="python"
                        copyKey={`py-${activeEndpoint.key}`}
                        copiedKey={copiedKey}
                        onCopy={copyToClipboard}
                    />
                </div>
            </div>
        </div>
    );
};

/* ──────────────────────────────────────────────
   Reusable code block with copy button
   ────────────────────────────────────────────── */
interface CodeBlockProps {
    title: string;
    code: string;
    language: string;
    copyKey: string;
    copiedKey: string | null;
    onCopy: (text: string, key: string) => void;
}

const CodeBlock: React.FC<CodeBlockProps> = ({
    title,
    code,
    language,
    copyKey,
    copiedKey,
    onCopy,
}) => {
    return (
        <div className="rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700/40">
            <div className="flex items-center justify-between px-4 py-2 bg-gray-50 dark:bg-gray-800/60 border-b border-gray-200 dark:border-gray-700/40">
                <div className="flex items-center gap-2">
                    <div className="flex gap-1">
                        <div className="w-2.5 h-2.5 rounded-full bg-red-400/60 dark:bg-red-500/60" />
                        <div className="w-2.5 h-2.5 rounded-full bg-yellow-400/60 dark:bg-yellow-500/60" />
                        <div className="w-2.5 h-2.5 rounded-full bg-green-400/60 dark:bg-green-500/60" />
                    </div>
                    <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{title}</span>
                    <span className="text-[10px] font-mono text-gray-400 dark:text-gray-600 border border-gray-200 dark:border-gray-700 rounded px-1.5 py-0.5">
                        {language}
                    </span>
                </div>
                <button
                    onClick={() => onCopy(code, copyKey)}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700/50 transition-colors"
                >
                    {copiedKey === copyKey ? (
                        <>
                            <svg className="w-3.5 h-3.5 text-emerald-500 dark:text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                            <span className="text-emerald-600 dark:text-emerald-400">Copied!</span>
                        </>
                    ) : (
                        <>
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                            </svg>
                            Copy
                        </>
                    )}
                </button>
            </div>
            <pre className="p-4 text-sm text-gray-800 dark:text-gray-300 overflow-x-auto bg-gray-100 dark:bg-gray-900/50 font-mono leading-relaxed">
                <code>{code}</code>
            </pre>
        </div>
    );
};

export default EmbedDialog;
