import { useState } from 'react';
import type { ParameterDetail } from '../types';

interface ParametersSidebarProps {
    parameters: ParameterDetail[];
    loading: boolean;
    onApply: (values: Record<string, string>) => void;
    applying: boolean;
}

/**
 * Maps a Java type to a suitable HTML input type.
 */
function getInputType(javaType: string): string {
    const t = javaType.toLowerCase();
    if (t.includes('integer') || t.includes('long') || t.includes('double') || t.includes('float') || t.includes('bigdecimal') || t.includes('short') || t.includes('number')) {
        return 'number';
    }
    if (t.includes('boolean')) {
        return 'checkbox';
    }
    if (t.includes('date') || t.includes('timestamp')) {
        return 'date';
    }
    return 'text';
}

/**
 * Returns a short human-readable label for a Java type.
 */
function typeLabel(javaType: string): string {
    const parts = javaType.split('.');
    return parts[parts.length - 1];
}

export default function ParametersSidebar({ parameters, loading, onApply, applying }: ParametersSidebarProps) {
    const [values, setValues] = useState<Record<string, string>>({});
    const [collapsed, setCollapsed] = useState(false);

    const handleChange = (name: string, value: string) => {
        setValues(prev => ({ ...prev, [name]: value }));
    };

    const handleApply = () => {
        const filtered: Record<string, string> = {};
        for (const [k, v] of Object.entries(values)) {
            if (v !== '') filtered[k] = v;
        }
        onApply(filtered);
    };

    const handleReset = () => {
        setValues({});
    };

    const filledCount = Object.values(values).filter(v => v !== '').length;

    return (
        <div
            className={`h-full flex-shrink-0 transition-all duration-300 ease-in-out relative ${
                collapsed ? 'w-12' : 'w-80'
            }`}
            style={{
                background: collapsed ? 'transparent' : undefined,
            }}
        >
            {/* Collapsed state — thin vertical strip */}
            {collapsed && (
                <div className="h-full flex flex-col items-center bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-sm">
                    <button
                        onClick={() => setCollapsed(false)}
                        className="mt-3 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors group"
                        title="Open Filters"
                    >
                        <svg className="w-5 h-5 text-gray-500 group-hover:text-primary-600 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                        </svg>
                    </button>

                    {/* Vertical "FILTERS" text */}
                    <div className="mt-4 flex flex-col items-center gap-0.5">
                        {'FILTERS'.split('').map((char, i) => (
                            <span key={i} className="text-[10px] font-bold text-gray-400 dark:text-gray-500 leading-none tracking-widest">
                                {char}
                            </span>
                        ))}
                    </div>

                    {/* Active filter indicator */}
                    {filledCount > 0 && (
                        <div className="mt-3 w-5 h-5 rounded-full bg-primary-500 text-white text-[10px] font-bold flex items-center justify-center">
                            {filledCount}
                        </div>
                    )}
                </div>
            )}

            {/* Expanded state */}
            {!collapsed && (
                <div className="h-full flex flex-col bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg shadow-gray-200/50 dark:shadow-black/20 overflow-hidden">
                    {/* Header */}
                    <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-primary-50 to-indigo-50 dark:from-gray-800 dark:to-gray-800">
                        <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-lg bg-primary-100 dark:bg-primary-900/40 flex items-center justify-center">
                                <svg className="w-4 h-4 text-primary-600 dark:text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                                </svg>
                            </div>
                            <div>
                                <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Filters</h3>
                                <p className="text-[11px] text-gray-500 dark:text-gray-400">
                                    {loading ? 'Loading...' : `${parameters.length} parameter${parameters.length !== 1 ? 's' : ''}`}
                                </p>
                            </div>
                        </div>
                        <button
                            onClick={() => setCollapsed(true)}
                            className="p-1.5 rounded-lg hover:bg-white/60 dark:hover:bg-gray-700 transition-colors"
                            title="Collapse sidebar"
                        >
                            <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
                            </svg>
                        </button>
                    </div>

                    {/* Content */}
                    <div className="flex-1 overflow-y-auto p-4">
                        {loading ? (
                            /* Loading skeleton */
                            <div className="space-y-4">
                                {[1, 2, 3, 4].map(i => (
                                    <div key={i} className="animate-pulse">
                                        <div className="h-3.5 w-24 bg-gray-200 dark:bg-gray-700 rounded mb-2" />
                                        <div className="h-9 w-full bg-gray-200 dark:bg-gray-700 rounded-lg" />
                                    </div>
                                ))}
                            </div>
                        ) : parameters.length === 0 ? (
                            /* No parameters */
                            <div className="flex flex-col items-center justify-center py-10 text-center">
                                <div className="w-14 h-14 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center mb-3">
                                    <svg className="w-7 h-7 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m6.75 12H9.75m0 0l2.25 2.25M9.75 14.25l2.25-2.25M6 20.25h12A2.25 2.25 0 0020.25 18V6.75A2.25 2.25 0 0018 4.5H6A2.25 2.25 0 003.75 6.75v11.25A2.25 2.25 0 006 20.25z" />
                                    </svg>
                                </div>
                                <p className="text-sm font-medium text-gray-600 dark:text-gray-400">No parameters</p>
                                <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">This report has no configurable parameters</p>
                            </div>
                        ) : (
                            /* Parameter list */
                            <div className="space-y-4">
                                {parameters.map((param, index) => {
                                    const inputType = getInputType(param.expectedType);
                                    const hasValue = values[param.name] && values[param.name] !== '';

                                    return (
                                        <div
                                            key={param.name}
                                            className={`group rounded-xl border transition-all duration-200 ${
                                                hasValue
                                                    ? 'border-primary-200 dark:border-primary-800 bg-primary-50/50 dark:bg-primary-900/10'
                                                    : 'border-gray-200 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-900/30 hover:border-gray-300 dark:hover:border-gray-600'
                                            } p-3`}
                                            style={{ animationDelay: `${index * 50}ms` }}
                                        >
                                            <div className="flex items-center justify-between mb-2">
                                                <label className="text-xs font-semibold text-gray-700 dark:text-gray-300 truncate">
                                                    {param.name}
                                                </label>
                                                <span className={`text-[10px] px-1.5 py-0.5 rounded font-medium ${
                                                    hasValue
                                                        ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300'
                                                        : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
                                                }`}>
                                                    {typeLabel(param.expectedType)}
                                                </span>
                                            </div>

                                            {inputType === 'checkbox' ? (
                                                <label className="flex items-center gap-2.5 cursor-pointer py-1">
                                                    <div className="relative">
                                                        <input
                                                            type="checkbox"
                                                            checked={values[param.name] === 'true'}
                                                            onChange={(e) => handleChange(param.name, e.target.checked ? 'true' : 'false')}
                                                            className="sr-only peer"
                                                        />
                                                        <div className="w-9 h-5 bg-gray-200 dark:bg-gray-600 rounded-full peer-checked:bg-primary-500 transition-colors" />
                                                        <div className="absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform peer-checked:translate-x-4" />
                                                    </div>
                                                    <span className="text-sm text-gray-600 dark:text-gray-400">
                                                        {values[param.name] === 'true' ? 'True' : 'False'}
                                                    </span>
                                                </label>
                                            ) : (
                                                <input
                                                    type={inputType}
                                                    value={values[param.name] || ''}
                                                    onChange={(e) => handleChange(param.name, e.target.value)}
                                                    placeholder={param.defaultValue || `Enter ${typeLabel(param.expectedType).toLowerCase()}`}
                                                    className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 dark:focus:border-primary-400 transition-all outline-none"
                                                />
                                            )}

                                            {param.defaultValue && (
                                                <p className="text-[11px] text-gray-400 mt-1.5 flex items-center gap-1">
                                                    <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                    </svg>
                                                    Default: {param.defaultValue}
                                                </p>
                                            )}

                                            {param.description && (
                                                <p className="text-[11px] text-gray-400 mt-1 italic truncate" title={param.description}>
                                                    {param.description}
                                                </p>
                                            )}

                                            {param.nestedTypeName && (
                                                <p className="text-[10px] text-gray-400 mt-1 font-mono">
                                                    Nested: {param.nestedTypeName.split('.').pop()}
                                                </p>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>

                    {/* Footer actions — always visible when parameters exist */}
                    {parameters.length > 0 && (
                        <div className="border-t border-gray-200 dark:border-gray-700 p-4 bg-gray-50/80 dark:bg-gray-900/50">
                            {filledCount > 0 && (
                                <p className="text-[11px] text-primary-600 dark:text-primary-400 mb-2 font-medium text-center">
                                    {filledCount} filter{filledCount !== 1 ? 's' : ''} active
                                </p>
                            )}
                            <button
                                onClick={handleApply}
                                disabled={applying}
                                className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold bg-gradient-to-r from-primary-600 to-indigo-600 hover:from-primary-700 hover:to-indigo-700 text-white shadow-md shadow-primary-500/25 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 hover:shadow-lg hover:shadow-primary-500/30"
                            >
                                {applying ? (
                                    <>
                                        <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                        </svg>
                                        Compiling...
                                    </>
                                ) : (
                                    <>
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        Apply &amp; Re-run
                                    </>
                                )}
                            </button>
                            {filledCount > 0 && (
                                <button
                                    onClick={handleReset}
                                    className="w-full mt-2 inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-gray-500 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                                >
                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                    </svg>
                                    Clear all filters
                                </button>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
