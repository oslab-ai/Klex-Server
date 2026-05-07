import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import type { ParameterDetail, FieldInfo, FilterSection, FilterOption } from '../types';

/* ────────────────────────────────────────────────────────────
   Props
   ──────────────────────────────────────────────────────────── */
interface FiltersSidebarProps {
    parameters: ParameterDetail[];
    fields: FieldInfo[];
    loading: boolean;
    onApply: (values: Record<string, string>) => void;
    applying: boolean;
}

/* ────────────────────────────────────────────────────────────
   Helpers: auto-generate filter sections from params + fields
   ──────────────────────────────────────────────────────────── */
const DATE_PRESETS: { key: string; label: string }[] = [
    { key: 'last_7', label: 'Last 7 days' },
    { key: 'this_month', label: 'This month' },
    { key: 'last_quarter', label: 'Last quarter' },
    { key: 'ytd', label: 'YTD' },
];

function computeDatePreset(key: string): { from: string; to: string } {
    const now = new Date();
    const fmt = (d: Date) => d.toISOString().slice(0, 10);
    const to = fmt(now);
    switch (key) {
        case 'last_7': {
            const d = new Date(now);
            d.setDate(d.getDate() - 7);
            return { from: fmt(d), to };
        }
        case 'this_month': {
            const d = new Date(now.getFullYear(), now.getMonth(), 1);
            return { from: fmt(d), to };
        }
        case 'last_quarter': {
            const qMonth = Math.floor(now.getMonth() / 3) * 3 - 3;
            const from = new Date(now.getFullYear(), qMonth, 1);
            const toD = new Date(now.getFullYear(), qMonth + 3, 0);
            return { from: fmt(from), to: fmt(toD) };
        }
        case 'ytd': {
            return { from: `${now.getFullYear()}-01-01`, to };
        }
        default:
            return { from: '', to };
    }
}

function buildFilterSections(params: ParameterDetail[], fields: FieldInfo[]): FilterSection[] {
    const sections: FilterSection[] = [];
    const consumed = new Set<string>();

    // 1. Column visibility (always present when there are fields)
    if (fields.length > 0) {
        sections.push({
            id: 'columns',
            type: 'column_visibility',
            label: 'Visible Columns',
            paramKey: 'VISIBLE_COLUMNS',
            options: fields.map(f => ({ value: f.name, label: prettifyName(f.name) })),
        });
    }

    // Walk through parameters and auto-assign filter types
    for (const p of params) {
        if (consumed.has(p.name)) continue;
        const lower = p.name.toLowerCase();
        const type = (p.expectedType || '').toLowerCase();

        // Date range pair detection
        if ((lower.includes('date_from') || lower.includes('from_date') || lower.includes('start_date')) && !consumed.has(p.name)) {
            const toName = findPairParam(params, p.name, ['date_to', 'to_date', 'end_date']);
            if (toName) {
                consumed.add(p.name);
                consumed.add(toName);
                sections.push({
                    id: `dr_${p.name}`,
                    type: 'date_range',
                    label: prettifyName(p.name.replace(/_from|_start|from_|start_/i, '')).replace(/date$/i, '') + 'Date Range',
                    paramKey: p.name,
                    paramKeySecondary: toName,
                    presets: DATE_PRESETS,
                });
                continue;
            }
        }

        // Single date param
        if (type.includes('date') || type.includes('timestamp')) {
            consumed.add(p.name);
            sections.push({
                id: `date_${p.name}`,
                type: 'date_range',
                label: prettifyName(p.name),
                paramKey: p.name,
                presets: [],
            });
            continue;
        }

        // Boolean
        if (type.includes('boolean')) {
            consumed.add(p.name);
            sections.push({
                id: `bool_${p.name}`,
                type: 'boolean_toggle',
                label: prettifyName(p.name),
                paramKey: p.name,
                defaultValue: p.defaultValue || 'false',
            });
            continue;
        }

        // Numeric range detection
        if ((lower.includes('min_') || lower.includes('_min')) && !consumed.has(p.name)) {
            const maxName = findPairParam(params, p.name, ['max_', '_max']);
            if (maxName) {
                consumed.add(p.name);
                consumed.add(maxName);
                sections.push({
                    id: `nr_${p.name}`,
                    type: 'numeric_range',
                    label: prettifyName(p.name.replace(/min_|_min/i, '')) + ' Range',
                    paramKey: p.name,
                    paramKeySecondary: maxName,
                    min: 0,
                    max: 100000,
                });
                continue;
            }
        }

        // Numeric (single)
        if (type.includes('integer') || type.includes('long') || type.includes('double') || type.includes('float') || type.includes('bigdecimal') || type.includes('short') || type.includes('number')) {
            // Check for special names
            if (lower.includes('limit') || lower.includes('max_record') || lower.includes('page_size') || lower.includes('row_count')) {
                consumed.add(p.name);
                sections.push({
                    id: `rl_${p.name}`,
                    type: 'record_limit',
                    label: 'Record Limit',
                    paramKey: p.name,
                    defaultValue: p.defaultValue || '100',
                });
                continue;
            }
            consumed.add(p.name);
            sections.push({
                id: `num_${p.name}`,
                type: 'numeric_range',
                label: prettifyName(p.name),
                paramKey: p.name,
                min: 0,
                max: 100000,
            });
            continue;
        }

        // Multi-select / list types
        if (type.includes('list') || type.includes('collection') || type.includes('set') || lower.includes('_list') || lower.includes('_options') || lower.includes('status')) {
            consumed.add(p.name);
            sections.push({
                id: `ms_${p.name}`,
                type: 'multi_select',
                label: prettifyName(p.name),
                paramKey: p.name,
                options: guessOptionsFromName(p.name),
            });
            continue;
        }

        // Sort / Order
        if (lower.includes('sort') || lower.includes('order_by')) {
            const dirName = findPairParam(params, p.name, ['sort_dir', 'sort_order', 'order_dir', 'sort_direction']);
            consumed.add(p.name);
            if (dirName) consumed.add(dirName);
            sections.push({
                id: `sort_${p.name}`,
                type: 'sort_order',
                label: 'Sort Order',
                paramKey: p.name,
                paramKeySecondary: dirName || undefined,
                options: fields.length > 0
                    ? fields.map(f => ({ value: f.name, label: prettifyName(f.name) }))
                    : [{ value: 'name', label: 'Name' }, { value: 'date', label: 'Date' }, { value: 'amount', label: 'Amount' }],
            });
            continue;
        }

        // Group by
        if (lower.includes('group')) {
            consumed.add(p.name);
            sections.push({
                id: `gb_${p.name}`,
                type: 'group_by',
                label: 'Group By',
                paramKey: p.name,
                options: fields.length > 0
                    ? [{ value: '', label: 'None' }, ...fields.map(f => ({ value: f.name, label: prettifyName(f.name) }))]
                    : [{ value: '', label: 'None' }],
            });
            continue;
        }

        // Fallback: text search
        consumed.add(p.name);
        sections.push({
            id: `txt_${p.name}`,
            type: 'text_search',
            label: prettifyName(p.name),
            paramKey: p.name,
            defaultValue: p.defaultValue || '',
        });
    }

    // Always add display options section
    sections.push({
        id: 'display_opts',
        type: 'display_option',
        label: 'Display Options',
        paramKey: 'SHOW_TOTALS',
        options: [
            { value: 'SHOW_TOTALS', label: 'Show Totals Row' },
            { value: 'SHOW_CHARTS', label: 'Show Charts' },
            { value: 'LANDSCAPE', label: 'Landscape Orientation' },
            { value: 'COMPACT_VIEW', label: 'Compact View' },
        ],
    });

    // Always add record limit if none was auto-detected
    if (!sections.some(s => s.type === 'record_limit')) {
        sections.push({
            id: 'record_limit_default',
            type: 'record_limit',
            label: 'Record Limit',
            paramKey: 'RECORD_LIMIT',
            defaultValue: '100',
        });
    }

    // Always add sort if none was auto-detected
    if (!sections.some(s => s.type === 'sort_order')) {
        sections.push({
            id: 'sort_default',
            type: 'sort_order',
            label: 'Sort Order',
            paramKey: 'SORT_COLUMN',
            paramKeySecondary: 'SORT_DIR',
            options: fields.length > 0
                ? fields.map(f => ({ value: f.name, label: prettifyName(f.name) }))
                : [],
        });
    }

    return sections;
}

function prettifyName(name: string): string {
    return name
        .replace(/([A-Z])/g, ' $1')
        .replace(/[_-]/g, ' ')
        .replace(/\b\w/g, c => c.toUpperCase())
        .trim();
}

function findPairParam(params: ParameterDetail[], currentName: string, patterns: string[]): string | null {
    const base = currentName.toLowerCase();
    for (const p of params) {
        if (p.name === currentName) continue;
        const lower = p.name.toLowerCase();
        for (const pat of patterns) {
            if (lower.includes(pat) && shareBase(base, lower)) return p.name;
        }
    }
    return null;
}

function shareBase(a: string, b: string): boolean {
    const clean = (s: string) => s.replace(/min_|_min|max_|_max|_from|from_|_to|to_|start_|_start|end_|_end|_date|date_|_dir|dir_|_order|order_/gi, '');
    return clean(a) === clean(b) || true; // simplified: accept any pair
}

function guessOptionsFromName(name: string): FilterOption[] {
    const lower = name.toLowerCase();
    if (lower.includes('status')) {
        return ['Active', 'Pending', 'Closed', 'Cancelled', 'Draft'].map(v => ({ value: v, label: v }));
    }
    if (lower.includes('type') || lower.includes('category')) {
        return ['Type A', 'Type B', 'Type C'].map(v => ({ value: v, label: v }));
    }
    if (lower.includes('region') || lower.includes('location')) {
        return ['North', 'South', 'East', 'West'].map(v => ({ value: v, label: v }));
    }
    return ['Option 1', 'Option 2', 'Option 3'].map(v => ({ value: v, label: v }));
}


/* ────────────────────────────────────────────────────────────
   Section icons (SVG paths)
   ──────────────────────────────────────────────────────────── */
const SECTION_ICONS: Record<string, string> = {
    column_visibility: 'M3 10h18M3 14h18M3 18h18M3 6h18',
    text_search: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z',
    date_range: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
    multi_select: 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A2 2 0 013 12V7a4 4 0 014-4z',
    numeric_range: 'M4 6h16M4 12h16M4 18h7',
    boolean_toggle: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
    group_by: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
    sort_order: 'M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0l4 4m-4-4v12',
    record_limit: 'M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z',
    display_option: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z',
};

/* ────────────────────────────────────────────────────────────
   Main Component
   ──────────────────────────────────────────────────────────── */
export default function FiltersSidebar({ parameters, fields, loading, onApply, applying }: FiltersSidebarProps) {
    const [collapsed, setCollapsed] = useState(false);
    const [values, setValues] = useState<Record<string, string>>({});
    const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set());
    const [multiSelectOpen, setMultiSelectOpen] = useState<string | null>(null);
    const msRef = useRef<HTMLDivElement>(null);

    const sections = useMemo(() => buildFilterSections(parameters, fields), [parameters, fields]);

    // Auto-expand first 3 sections
    useEffect(() => {
        if (sections.length > 0 && expandedSections.size === 0) {
            setExpandedSections(new Set(sections.slice(0, 3).map(s => s.id)));
        }
    }, [sections]); // eslint-disable-line react-hooks/exhaustive-deps

    // Close multi-select dropdown on outside click
    useEffect(() => {
        const handleClick = (e: MouseEvent) => {
            if (msRef.current && !msRef.current.contains(e.target as Node)) {
                setMultiSelectOpen(null);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, []);

    const toggleSection = useCallback((id: string) => {
        setExpandedSections(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    }, []);

    const set = useCallback((key: string, val: string) => {
        setValues(prev => ({ ...prev, [key]: val }));
    }, []);

    const get = useCallback((key: string) => values[key] || '', [values]);

    const handleApply = () => {
        const out: Record<string, string> = {};
        for (const [k, v] of Object.entries(values)) {
            if (v !== '') out[k] = v;
        }
        onApply(out);
    };

    const handleReset = () => setValues({});

    const filledCount = Object.values(values).filter(v => v !== '').length;

    /* ── Collapsed strip ─────────────────────── */
    if (collapsed) {
        return (
            <div className="h-full w-12 flex-shrink-0 flex flex-col items-center bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-sm transition-all duration-300">
                <button
                    onClick={() => setCollapsed(false)}
                    className="mt-3 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors group"
                    title="Open Filters"
                >
                    <svg className="w-5 h-5 text-gray-500 group-hover:text-primary-600 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                    </svg>
                </button>
                <div className="mt-4 flex flex-col items-center gap-0.5">
                    {'FILTERS'.split('').map((char, i) => (
                        <span key={i} className="text-[10px] font-bold text-gray-400 dark:text-gray-500 leading-none tracking-widest">{char}</span>
                    ))}
                </div>
                {filledCount > 0 && (
                    <div className="mt-3 w-5 h-5 rounded-full bg-primary-500 text-white text-[10px] font-bold flex items-center justify-center animate-pulse">
                        {filledCount}
                    </div>
                )}
            </div>
        );
    }

    /* ── Expanded panel ───────────────────────── */
    return (
        <div className="h-full w-80 flex-shrink-0 transition-all duration-300 ease-in-out">
            <div className="h-full flex flex-col bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg shadow-gray-200/50 dark:shadow-black/20 overflow-hidden">

                {/* ── Header ── */}
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
                                {loading ? 'Loading...' : `${sections.length} filter section${sections.length !== 1 ? 's' : ''}`}
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

                {/* ── Content ── */}
                <div className="flex-1 overflow-y-auto">
                    {loading ? (
                        <div className="p-4 space-y-4">
                            {[1, 2, 3, 4].map(i => (
                                <div key={i} className="animate-pulse">
                                    <div className="h-3.5 w-24 bg-gray-200 dark:bg-gray-700 rounded mb-2" />
                                    <div className="h-9 w-full bg-gray-200 dark:bg-gray-700 rounded-lg" />
                                </div>
                            ))}
                        </div>
                    ) : sections.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-10 text-center px-4">
                            <div className="w-14 h-14 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center mb-3">
                                <svg className="w-7 h-7 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                                </svg>
                            </div>
                            <p className="text-sm font-medium text-gray-600 dark:text-gray-400">No filters available</p>
                            <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">This report has no configurable filters</p>
                        </div>
                    ) : (
                        <div className="divide-y divide-gray-100 dark:divide-gray-700/50">
                            {sections.map(section => {
                                const isOpen = expandedSections.has(section.id);
                                const iconPath = SECTION_ICONS[section.type] || SECTION_ICONS['text_search'];
                                return (
                                    <div key={section.id}>
                                        {/* Section header (collapsible) */}
                                        <button
                                            onClick={() => toggleSection(section.id)}
                                            className="w-full flex items-center gap-2 px-4 py-2.5 hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors text-left"
                                        >
                                            <svg className="w-3.5 h-3.5 text-primary-500 dark:text-primary-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={iconPath} />
                                            </svg>
                                            <span className="text-xs font-semibold text-gray-700 dark:text-gray-300 flex-1 truncate">{section.label}</span>
                                            {/* Active indicator dot */}
                                            {get(section.paramKey) && (
                                                <span className="w-1.5 h-1.5 rounded-full bg-primary-500 flex-shrink-0" />
                                            )}
                                            <svg className={`w-3 h-3 text-gray-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                            </svg>
                                        </button>

                                        {/* Section body */}
                                        <div className={`overflow-hidden transition-all duration-200 ${isOpen ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'}`}>
                                            <div className="px-4 pb-3 pt-1">
                                                {/* ── Column Visibility ── */}
                                                {section.type === 'column_visibility' && (
                                                    <ColumnVisibilityFilter
                                                        options={section.options || []}
                                                        value={get(section.paramKey)}
                                                        onChange={v => set(section.paramKey, v)}
                                                    />
                                                )}

                                                {/* ── Text Search ── */}
                                                {section.type === 'text_search' && (
                                                    <div className="relative">
                                                        <svg className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                                        </svg>
                                                        <input
                                                            type="text"
                                                            value={get(section.paramKey)}
                                                            onChange={e => set(section.paramKey, e.target.value)}
                                                            placeholder={`Search ${section.label.toLowerCase()}...`}
                                                            className="w-full pl-8 pr-3 py-2 text-sm rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 dark:focus:border-primary-400 transition-all outline-none"
                                                        />
                                                    </div>
                                                )}

                                                {/* ── Date Range ── */}
                                                {section.type === 'date_range' && (
                                                    <DateRangeFilter
                                                        section={section}
                                                        valueFrom={get(section.paramKey)}
                                                        valueTo={get(section.paramKeySecondary || '')}
                                                        onChangeFrom={v => set(section.paramKey, v)}
                                                        onChangeTo={v => section.paramKeySecondary && set(section.paramKeySecondary, v)}
                                                        presets={section.presets || []}
                                                    />
                                                )}

                                                {/* ── Multi-Select ── */}
                                                {section.type === 'multi_select' && (
                                                    <div ref={multiSelectOpen === section.id ? msRef : undefined} className="relative">
                                                        <MultiSelectFilter
                                                            options={section.options || []}
                                                            value={get(section.paramKey)}
                                                            onChange={v => set(section.paramKey, v)}
                                                            isOpen={multiSelectOpen === section.id}
                                                            onToggle={() => setMultiSelectOpen(prev => prev === section.id ? null : section.id)}
                                                        />
                                                    </div>
                                                )}

                                                {/* ── Numeric Range ── */}
                                                {section.type === 'numeric_range' && (
                                                    <NumericRangeFilter
                                                        section={section}
                                                        valueMin={get(section.paramKey)}
                                                        valueMax={get(section.paramKeySecondary || section.paramKey)}
                                                        onChangeMin={v => set(section.paramKey, v)}
                                                        onChangeMax={v => set(section.paramKeySecondary || section.paramKey, v)}
                                                    />
                                                )}

                                                {/* ── Boolean Toggle ── */}
                                                {section.type === 'boolean_toggle' && (
                                                    <BooleanToggleFilter
                                                        value={get(section.paramKey) || section.defaultValue || 'false'}
                                                        onChange={v => set(section.paramKey, v)}
                                                    />
                                                )}

                                                {/* ── Group By ── */}
                                                {section.type === 'group_by' && (
                                                    <GroupByFilter
                                                        options={section.options || []}
                                                        value={get(section.paramKey)}
                                                        onChange={v => set(section.paramKey, v)}
                                                    />
                                                )}

                                                {/* ── Sort Order ── */}
                                                {section.type === 'sort_order' && (
                                                    <SortOrderFilter
                                                        options={section.options || []}
                                                        column={get(section.paramKey)}
                                                        direction={get(section.paramKeySecondary || 'SORT_DIR') || 'ASC'}
                                                        onColumnChange={v => set(section.paramKey, v)}
                                                        onDirectionChange={v => set(section.paramKeySecondary || 'SORT_DIR', v)}
                                                    />
                                                )}

                                                {/* ── Record Limit ── */}
                                                {section.type === 'record_limit' && (
                                                    <RecordLimitFilter
                                                        value={get(section.paramKey) || section.defaultValue || '100'}
                                                        onChange={v => set(section.paramKey, v)}
                                                    />
                                                )}

                                                {/* ── Display Options ── */}
                                                {section.type === 'display_option' && (
                                                    <DisplayOptionsFilter
                                                        options={section.options || []}
                                                        values={values}
                                                        onChange={set}
                                                    />
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* ── Footer actions ── */}
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
            </div>
        </div>
    );
}


/* ════════════════════════════════════════════════════════════
   Sub-components for each filter type
   ════════════════════════════════════════════════════════════ */

/* ── 1. Column Visibility ── */
function ColumnVisibilityFilter({ options, value, onChange }: {
    options: FilterOption[];
    value: string;
    onChange: (v: string) => void;
}) {
    const selected = useMemo(() => new Set(value ? value.split(',') : options.map(o => o.value)), [value, options]);
    const toggle = (val: string) => {
        const next = new Set(selected);
        next.has(val) ? next.delete(val) : next.add(val);
        onChange(Array.from(next).join(','));
    };
    const allSelected = selected.size === options.length;
    const toggleAll = () => {
        onChange(allSelected ? '' : options.map(o => o.value).join(','));
    };

    return (
        <div className="space-y-1.5">
            <button onClick={toggleAll} className="text-[10px] font-medium text-primary-600 dark:text-primary-400 hover:underline mb-1">
                {allSelected ? 'Deselect All' : 'Select All'}
            </button>
            <div className="grid grid-cols-2 gap-1">
                {options.map(opt => (
                    <label key={opt.value} className="flex items-center gap-1.5 px-2 py-1.5 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors cursor-pointer">
                        <input
                            type="checkbox"
                            checked={selected.has(opt.value)}
                            onChange={() => toggle(opt.value)}
                            className="w-3.5 h-3.5 rounded border-gray-300 dark:border-gray-600 text-primary-600 focus:ring-primary-500/20"
                        />
                        <span className="text-[11px] text-gray-700 dark:text-gray-300 truncate">{opt.label}</span>
                    </label>
                ))}
            </div>
        </div>
    );
}

/* ── 2. Date Range ── */
function DateRangeFilter({ section, valueFrom, valueTo, onChangeFrom, onChangeTo, presets }: {
    section: FilterSection;
    valueFrom: string;
    valueTo: string;
    onChangeFrom: (v: string) => void;
    onChangeTo: (v: string) => void;
    presets: { key: string; label: string }[];
}) {
    const applyPreset = (key: string) => {
        const { from, to } = computeDatePreset(key);
        onChangeFrom(from);
        onChangeTo(to);
    };
    const hasPair = !!section.paramKeySecondary;

    return (
        <div className="space-y-2">
            {presets.length > 0 && (
                <div className="flex flex-wrap gap-1">
                    {presets.map(p => (
                        <button
                            key={p.key}
                            onClick={() => applyPreset(p.key)}
                            className="px-2 py-1 text-[10px] font-medium rounded-md bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-primary-100 hover:text-primary-700 dark:hover:bg-primary-900/30 dark:hover:text-primary-300 transition-colors"
                        >
                            {p.label}
                        </button>
                    ))}
                </div>
            )}
            <div className={`grid gap-2 ${hasPair ? 'grid-cols-2' : 'grid-cols-1'}`}>
                <div>
                    <label className="text-[10px] text-gray-500 dark:text-gray-400 mb-0.5 block">{hasPair ? 'From' : 'Date'}</label>
                    <input
                        type="date"
                        value={valueFrom}
                        onChange={e => onChangeFrom(e.target.value)}
                        className="w-full px-2 py-1.5 text-xs rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                    />
                </div>
                {hasPair && (
                    <div>
                        <label className="text-[10px] text-gray-500 dark:text-gray-400 mb-0.5 block">To</label>
                        <input
                            type="date"
                            value={valueTo}
                            onChange={e => onChangeTo(e.target.value)}
                            className="w-full px-2 py-1.5 text-xs rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                        />
                    </div>
                )}
            </div>
        </div>
    );
}

/* ── 3. Multi-Select ── */
function MultiSelectFilter({ options, value, onChange, isOpen, onToggle }: {
    options: FilterOption[];
    value: string;
    onChange: (v: string) => void;
    isOpen: boolean;
    onToggle: () => void;
}) {
    const selected = useMemo(() => new Set(value ? value.split(',').filter(Boolean) : []), [value]);
    const toggle = (val: string) => {
        const next = new Set(selected);
        next.has(val) ? next.delete(val) : next.add(val);
        onChange(Array.from(next).join(','));
    };

    return (
        <div>
            <button
                onClick={onToggle}
                className="w-full flex items-center justify-between px-3 py-2 text-sm rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 hover:border-gray-300 dark:hover:border-gray-500 transition-all"
            >
                <span className={selected.size === 0 ? 'text-gray-400 dark:text-gray-500 text-xs' : 'text-xs'}>
                    {selected.size === 0 ? 'Select options...' : `${selected.size} selected`}
                </span>
                <svg className={`w-3 h-3 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
            </button>

            {isOpen && (
                <div className="mt-1 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 shadow-lg max-h-40 overflow-y-auto">
                    {options.map(opt => (
                        <label key={opt.value} className="flex items-center gap-2 px-3 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer transition-colors">
                            <input
                                type="checkbox"
                                checked={selected.has(opt.value)}
                                onChange={() => toggle(opt.value)}
                                className="w-3.5 h-3.5 rounded border-gray-300 dark:border-gray-600 text-primary-600 focus:ring-primary-500/20"
                            />
                            <span className="text-xs text-gray-700 dark:text-gray-300">{opt.label}</span>
                        </label>
                    ))}
                </div>
            )}

            {/* Selected tags */}
            {selected.size > 0 && (
                <div className="flex flex-wrap gap-1 mt-1.5">
                    {Array.from(selected).map(v => {
                        const label = options.find(o => o.value === v)?.label || v;
                        return (
                            <span key={v} className="inline-flex items-center gap-1 px-1.5 py-0.5 text-[10px] rounded-md bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 font-medium">
                                {label}
                                <button onClick={() => toggle(v)} className="hover:text-red-500 transition-colors">×</button>
                            </span>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

/* ── 4. Numeric Range ── */
function NumericRangeFilter({ section, valueMin, valueMax, onChangeMin, onChangeMax }: {
    section: FilterSection;
    valueMin: string;
    valueMax: string;
    onChangeMin: (v: string) => void;
    onChangeMax: (v: string) => void;
}) {
    const hasPair = !!section.paramKeySecondary;
    return (
        <div className={`grid gap-2 ${hasPair ? 'grid-cols-2' : 'grid-cols-1'}`}>
            <div>
                <label className="text-[10px] text-gray-500 dark:text-gray-400 mb-0.5 block">{hasPair ? 'Min' : 'Value'}</label>
                <input
                    type="number"
                    value={valueMin}
                    onChange={e => onChangeMin(e.target.value)}
                    placeholder={String(section.min ?? 0)}
                    className="w-full px-2 py-1.5 text-xs rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                />
            </div>
            {hasPair && (
                <div>
                    <label className="text-[10px] text-gray-500 dark:text-gray-400 mb-0.5 block">Max</label>
                    <input
                        type="number"
                        value={valueMax}
                        onChange={e => onChangeMax(e.target.value)}
                        placeholder={String(section.max ?? 100000)}
                        className="w-full px-2 py-1.5 text-xs rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                    />
                </div>
            )}
        </div>
    );
}

/* ── 5. Boolean Toggle ── */
function BooleanToggleFilter({ value, onChange }: { value: string; onChange: (v: string) => void }) {
    const isOn = value === 'true';
    return (
        <label className="flex items-center gap-2.5 cursor-pointer py-1">
            <div className="relative">
                <input
                    type="checkbox"
                    checked={isOn}
                    onChange={e => onChange(e.target.checked ? 'true' : 'false')}
                    className="sr-only peer"
                />
                <div className="w-9 h-5 bg-gray-200 dark:bg-gray-600 rounded-full peer-checked:bg-primary-500 transition-colors" />
                <div className="absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform peer-checked:translate-x-4" />
            </div>
            <span className="text-sm text-gray-600 dark:text-gray-400">{isOn ? 'Enabled' : 'Disabled'}</span>
        </label>
    );
}

/* ── 6. Group By ── */
function GroupByFilter({ options, value, onChange }: {
    options: FilterOption[];
    value: string;
    onChange: (v: string) => void;
}) {
    return (
        <div className="space-y-1">
            {options.map(opt => (
                <label key={opt.value} className="flex items-center gap-2 px-2 py-1.5 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700/50 cursor-pointer transition-colors">
                    <input
                        type="radio"
                        name="group_by"
                        checked={value === opt.value}
                        onChange={() => onChange(opt.value)}
                        className="w-3.5 h-3.5 text-primary-600 border-gray-300 dark:border-gray-600 focus:ring-primary-500/20"
                    />
                    <span className="text-xs text-gray-700 dark:text-gray-300">{opt.label}</span>
                </label>
            ))}
        </div>
    );
}

/* ── 7. Sort Order ── */
function SortOrderFilter({ options, column, direction, onColumnChange, onDirectionChange }: {
    options: FilterOption[];
    column: string;
    direction: string;
    onColumnChange: (v: string) => void;
    onDirectionChange: (v: string) => void;
}) {
    return (
        <div className="space-y-2">
            <select
                value={column}
                onChange={e => onColumnChange(e.target.value)}
                className="w-full px-2 py-1.5 text-xs rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
            >
                <option value="">None</option>
                {options.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
            </select>
            {column && (
                <div className="flex gap-1">
                    <button
                        onClick={() => onDirectionChange('ASC')}
                        className={`flex-1 inline-flex items-center justify-center gap-1 px-2 py-1.5 rounded-lg text-[11px] font-medium transition-all ${
                            direction === 'ASC'
                                ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 border border-primary-300 dark:border-primary-700'
                                : 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 border border-gray-200 dark:border-gray-700 hover:border-gray-300'
                        }`}
                    >
                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0l4 4m-4-4v12" />
                        </svg>
                        Ascending
                    </button>
                    <button
                        onClick={() => onDirectionChange('DESC')}
                        className={`flex-1 inline-flex items-center justify-center gap-1 px-2 py-1.5 rounded-lg text-[11px] font-medium transition-all ${
                            direction === 'DESC'
                                ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 border border-primary-300 dark:border-primary-700'
                                : 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 border border-gray-200 dark:border-gray-700 hover:border-gray-300'
                        }`}
                    >
                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h13M3 8h9m-9 4h9m5-4v12m0 0l-4-4m4 4l4-4" />
                        </svg>
                        Descending
                    </button>
                </div>
            )}
        </div>
    );
}

/* ── 8. Record Limit ── */
function RecordLimitFilter({ value, onChange }: { value: string; onChange: (v: string) => void }) {
    const presets = ['50', '100', '500', '1000', ''];
    const labels: Record<string, string> = { '50': '50', '100': '100', '500': '500', '1000': '1K', '': 'All' };

    return (
        <div className="space-y-2">
            <div className="flex gap-1">
                {presets.map(p => (
                    <button
                        key={p}
                        onClick={() => onChange(p)}
                        className={`flex-1 py-1.5 rounded-lg text-[11px] font-medium transition-all ${
                            value === p
                                ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 border border-primary-300 dark:border-primary-700'
                                : 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 border border-gray-200 dark:border-gray-700 hover:border-gray-300'
                        }`}
                    >
                        {labels[p]}
                    </button>
                ))}
            </div>
            <div className="flex items-center gap-2">
                <span className="text-[10px] text-gray-500 dark:text-gray-400">Custom:</span>
                <input
                    type="number"
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    min="1"
                    placeholder="Custom limit"
                    className="flex-1 px-2 py-1 text-xs rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                />
            </div>
        </div>
    );
}

/* ── 9. Display Options ── */
function DisplayOptionsFilter({ options, values, onChange }: {
    options: FilterOption[];
    values: Record<string, string>;
    onChange: (key: string, val: string) => void;
}) {
    return (
        <div className="space-y-1.5">
            {options.map(opt => {
                const isOn = values[opt.value] === 'true';
                return (
                    <label key={opt.value} className="flex items-center justify-between gap-2 px-2 py-1.5 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700/50 cursor-pointer transition-colors">
                        <span className="text-xs text-gray-700 dark:text-gray-300">{opt.label}</span>
                        <div className="relative">
                            <input
                                type="checkbox"
                                checked={isOn}
                                onChange={e => onChange(opt.value, e.target.checked ? 'true' : 'false')}
                                className="sr-only peer"
                            />
                            <div className="w-8 h-4.5 bg-gray-200 dark:bg-gray-600 rounded-full peer-checked:bg-primary-500 transition-colors" style={{ width: '2rem', height: '1.125rem' }} />
                            <div className="absolute top-0.5 left-0.5 w-3.5 h-3.5 bg-white rounded-full shadow transition-transform peer-checked:translate-x-3.5" style={{ width: '0.875rem', height: '0.875rem' }} />
                        </div>
                    </label>
                );
            })}
        </div>
    );
}
