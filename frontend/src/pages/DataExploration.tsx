import { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import { dataAdaptersApi } from '../api';
import type { DataAdapter, TableColumn, PivotFieldConfig, ExplorationQuery, FilterConfig, AggregateType, ShowAsMode, ConditionalFormatRule, JoinConfig, ExplorationResult } from '../types';
import { useAuth } from '../context/AuthContext';
import { buildPivotModel, formatPivotValue, pivotToCSV, pivotTo2DArray, getValueLabel } from '../utils/pivotEngine';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import * as XLSX from 'xlsx';
import { computeZScores, getCellColor } from '../utils/statsEngine';
import ZScorePanel from '../components/ZScorePanel';
import Modal from '../components/Modal';

const AVAILABLE_AGGREGATES: { label: string; value: AggregateType }[] = [
    { label: 'Sum', value: 'SUM' },
    { label: 'Average', value: 'AVERAGE' },
    { label: 'Count', value: 'COUNT' },
    { label: 'Count (All)', value: 'COUNTA' },
    { label: 'Distinct Count', value: 'COUNTUNIQUE' },
    { label: 'Min', value: 'MIN' },
    { label: 'Max', value: 'MAX' },
    { label: 'Median', value: 'MEDIAN' },
    { label: 'Product', value: 'PRODUCT' },
    { label: 'Std Dev (Sample)', value: 'STDEV' },
    { label: 'Std Dev (Pop)', value: 'STDEVP' },
    { label: 'Variance (Sample)', value: 'VAR' },
    { label: 'Variance (Pop)', value: 'VARP' },
];

const SHOW_AS_OPTIONS: { label: string; value: ShowAsMode }[] = [
    { label: 'Default', value: 'default' },
    { label: '% of Row Total', value: 'pct_of_row' },
    { label: '% of Column Total', value: 'pct_of_column' },
    { label: '% of Grand Total', value: 'pct_of_grand_total' },
];

const AVAILABLE_OPERATORS = [
    { label: 'Equals (=)', value: '=' },
    { label: 'Not Equals (!=)', value: '!=' },
    { label: 'Greater Than (>)', value: '>' },
    { label: 'Less Than (<)', value: '<' },
    { label: 'Greater or Equal (>=)', value: '>=' },
    { label: 'Less or Equal (<=)', value: '<=' },
    { label: 'Contains (LIKE)', value: 'LIKE' },
    { label: 'Is Null', value: 'IS NULL' },
    { label: 'Is Not Null', value: 'IS NOT NULL' },
    { label: 'In List (comma separated)', value: 'IN' },
    { label: 'Not In List (comma separated)', value: 'NOT IN' },
] as const;

function genId(): string {
    return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
}

function getAllSubtotalKeys(data: Record<string, unknown>[], rowFields: PivotFieldConfig[]): Set<string> {
    const keys = new Set<string>();
    if (rowFields.length <= 1) return keys;
    for (const row of data) {
        const pathParts: string[] = [];
        for (let i = 0; i < rowFields.length - 1; i++) {
            pathParts.push(String(row[rowFields[i].fieldName] ?? '(empty)'));
            keys.add(pathParts.join('||'));
        }
    }
    return keys;
}

const LOCAL_STORAGE_KEY = 'klex_data_exploration_state';
const SAVED_EXPLORATIONS_KEY = 'klex_saved_explorations';

interface SavedExploration {
    id: string;
    name: string;
    createdAt: string;
    updatedAt: string;
    selectedAdapterId: number | '';
    selectedTable: string;
    joins?: JoinConfig[];
    fields: PivotFieldConfig[];
    rowLimit: number | null;
    sortColumn: string | null;
    sortDirection: 'asc' | 'desc';
}

interface PersistentState {
    activeView: 'dashboard' | 'playground';
    activeExplorationId: string | null;
    explorationName: string;
    selectedAdapterId: number | '';
    selectedTable: string;
    joins?: JoinConfig[];
    fields: PivotFieldConfig[];
    rowLimit: number | null;
    sortColumn: string | null;
    sortDirection: 'asc' | 'desc';
}

export default function DataExploration() {
    const isInitialLoadRef = useRef(true);

    // Load saved state from localStorage
    const getSavedState = (): Partial<PersistentState> => {
        try {
            const saved = localStorage.getItem(LOCAL_STORAGE_KEY);
            return saved ? JSON.parse(saved) : {};
        } catch (e) {
            console.error('Failed to load saved state from localStorage', e);
            return {};
        }
    };

    const savedState = getSavedState();

    // View tracking
    const [activeView, setActiveView] = useState<'dashboard' | 'playground'>(
        savedState.activeView || 'dashboard'
    );
    const [activeExplorationId, setActiveExplorationId] = useState<string | null>(
        savedState.activeExplorationId || null
    );
    const [explorationName, setExplorationName] = useState<string>(
        savedState.explorationName || 'New Sheet'
    );
    const [saveSuccess, setSaveSuccess] = useState(false);

    // Saved pivot sheet explorations list
    const [savedExplorations, setSavedExplorations] = useState<SavedExploration[]>(() => {
        try {
            const saved = localStorage.getItem(SAVED_EXPLORATIONS_KEY);
            return saved ? JSON.parse(saved) : [];
        } catch (e) {
            console.error('Failed to parse saved explorations', e);
            return [];
        }
    });

    useEffect(() => {
        try {
            localStorage.setItem(SAVED_EXPLORATIONS_KEY, JSON.stringify(savedExplorations));
        } catch (e) {
            console.error('Failed to save explorations list', e);
        }
    }, [savedExplorations]);

    const { user } = useAuth();

    // Adapter selection
    const [adapters, setAdapters] = useState<DataAdapter[]>([]);
    const [selectedAdapterId, setSelectedAdapterId] = useState<number | ''>(
        savedState.selectedAdapterId !== undefined ? savedState.selectedAdapterId : ''
    );
    const [loadingAdapters, setLoadingAdapters] = useState(true);

    // Schema discovery
    const [tables, setTables] = useState<string[]>([]);
    const [selectedTable, setSelectedTable] = useState<string>(savedState.selectedTable || '');
    const [loadingTables, setLoadingTables] = useState(false);

    const [columns, setColumns] = useState<TableColumn[]>([]);
    const [loadingColumns, setLoadingColumns] = useState(false);

    // Joins & Multi-table schema discovery
    const [joins, setJoins] = useState<JoinConfig[]>(savedState.joins || []);
    const [joinedTablesColumns, setJoinedTablesColumns] = useState<Record<string, TableColumn[]>>({});
    const [showJoinBuilder, setShowJoinBuilder] = useState(false);

    // Pivot table builder configuration (multi-instance: same field can appear multiple times)
    const [fields, setFields] = useState<PivotFieldConfig[]>(savedState.fields || []);
    const [rowLimit, setRowLimit] = useState<number | null>(savedState.rowLimit !== undefined ? savedState.rowLimit : 1000);

    // Available fields search query
    const [searchQuery, setSearchQuery] = useState('');

    // Collapse state for both sidebars together to enable full screen view of pivot table
    const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
    // Hover state to highlight cards during drop-to-reorder
    const [dragOverFieldId, setDragOverFieldId] = useState<string | null>(null);

    // Active drag tracking: could be a column name (from available) or an instance id (from well)
    const [draggedField, setDraggedField] = useState<string | null>(null);
    const [dragSource, setDragSource] = useState<'available' | 'well'>('available');

    // Distinct values cache for filter checklists (fieldName -> string[])
    const [distinctValuesCache, setDistinctValuesCache] = useState<Record<string, string[]>>({});

    // Collapsed/expanded state for row groups (hierarchical groupKey -> boolean)
    const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());

    // Query execution & results
    const [executing, setExecuting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [queryResult, setQueryResult] = useState<ExplorationResult | null>(null);
    const [isQueryFromCache, setIsQueryFromCache] = useState(false);
    const [isClientRePivoted, setIsClientRePivoted] = useState(false);
    const [tempJoins, setTempJoins] = useState<JoinConfig[]>([]);
    const [showQueryInspector, setShowQueryInspector] = useState(false);
    const [lastExecutedQueryFingerprintStr, setLastExecutedQueryFingerprintStr] = useState<string>('');

    // Sort order for dimensions/columns
    const [sortColumn, setSortColumn] = useState<string | null>(savedState.sortColumn || null);
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>(savedState.sortDirection || 'asc');

    const [hasAutoRun, setHasAutoRun] = useState(false);
    const hasAutoRunRef = useRef(false);

    // Z-Score / conditional formatting
    const [showToolsMenu, setShowToolsMenu] = useState(false);
    const [showZScorePanel, setShowZScorePanel] = useState(false);
    const [conditionalFormatRule, setConditionalFormatRule] = useState<ConditionalFormatRule | null>(null);
    const toolsMenuRef = useRef<HTMLDivElement>(null);

    const [showSortMenu, setShowSortMenu] = useState(false);
    const sortMenuRef = useRef<HTMLDivElement>(null);
    const [showExportMenu, setShowExportMenu] = useState(false);
    const exportMenuRef = useRef<HTMLDivElement>(null);
    const fetchedFieldsRef = useRef<Set<string>>(new Set());

    // Close sort menu on outside click
    useEffect(() => {
        if (!showSortMenu) return;
        const handleClick = (e: MouseEvent) => {
            if (sortMenuRef.current && !sortMenuRef.current.contains(e.target as Node)) {
                setShowSortMenu(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [showSortMenu]);

    // Close export menu on outside click
    useEffect(() => {
        if (!showExportMenu) return;
        const handleClick = (e: MouseEvent) => {
            if (exportMenuRef.current && !exportMenuRef.current.contains(e.target as Node)) {
                setShowExportMenu(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [showExportMenu]);

    // Save state to localStorage whenever it changes
    useEffect(() => {
        try {
            const stateToSave: PersistentState = {
                activeView,
                activeExplorationId,
                explorationName,
                selectedAdapterId,
                selectedTable,
                joins,
                fields,
                rowLimit,
                sortColumn,
                sortDirection,
            };
            localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(stateToSave));
        } catch (e) {
            console.error('Failed to save state to localStorage', e);
        }
    }, [activeView, activeExplorationId, explorationName, selectedAdapterId, selectedTable, joins, fields, rowLimit, sortColumn, sortDirection]);

    // Close tools menu on outside click
    useEffect(() => {
        if (!showToolsMenu) return;
        const handleClick = (e: MouseEvent) => {
            if (toolsMenuRef.current && !toolsMenuRef.current.contains(e.target as Node)) {
                setShowToolsMenu(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [showToolsMenu]);

    // Fetch JDBC adapters
    useEffect(() => {
        dataAdaptersApi.list()
            .then(res => {
                const jdbcAdapters = res.filter(a => a.adapter_type === 'jdbc' || a.adapter_type === 'csv');
                setAdapters(jdbcAdapters);
                if (jdbcAdapters.length > 0 && !selectedAdapterId) {
                    setSelectedAdapterId(jdbcAdapters[0].id);
                }
            })
            .catch(err => {
                console.error(err);
                setError('Failed to fetch data adapters.');
            })
            .finally(() => setLoadingAdapters(false));
    }, []);

    // Fetch tables when adapter changes
    useEffect(() => {
        if (!selectedAdapterId) {
            setTables([]);
            setSelectedTable('');
            return;
        }
        setLoadingTables(true);
        setTables([]);

        if (!isInitialLoadRef.current) {
            setSelectedTable('');
            setColumns([]);
            setFields([]);
            setQueryResult(null);
            setError(null);
        }

        dataAdaptersApi.getTables(selectedAdapterId)
            .then(res => {
                setTables(res.tables);
                if (res.tables.length > 0) {
                    if (!isInitialLoadRef.current || !res.tables.includes(selectedTable)) {
                        setSelectedTable(res.tables[0]);
                    }
                }
            })
            .catch(err => {
                console.error(err);
                setError('Failed to load tables from adapter.');
            })
            .finally(() => setLoadingTables(false));
    }, [selectedAdapterId]);

    // Fetch columns when table changes
    useEffect(() => {
        if (!selectedAdapterId || !selectedTable) {
            setColumns([]);
            return;
        }
        setLoadingColumns(true);
        setColumns([]);
        setSearchQuery(''); // Reset search query when table changes

        if (!isInitialLoadRef.current) {
            setFields([]);
            setQueryResult(null);
            setError(null);
        }

        dataAdaptersApi.getColumns(selectedAdapterId, selectedTable)
            .then(res => {
                setColumns(res.columns);
                if (isInitialLoadRef.current) {
                    isInitialLoadRef.current = false;
                }
            })
            .catch(err => {
                console.error(err);
                setError('Failed to load columns for table.');
                if (isInitialLoadRef.current) {
                    isInitialLoadRef.current = false;
                }
            })
            .finally(() => setLoadingColumns(false));
    }, [selectedAdapterId, selectedTable]);

    // Fetch columns for joined tables
    useEffect(() => {
        if (!selectedAdapterId || joins.length === 0) {
            setJoinedTablesColumns({});
            return;
        }
        const tablesToFetch = Array.from(new Set(joins.map(j => j.table)));
        dataAdaptersApi.getMultiColumns(selectedAdapterId, tablesToFetch)
            .then(res => {
                setJoinedTablesColumns(res.tables);
            })
            .catch(err => {
                console.error(err);
                setError('Failed to load columns for joined tables.');
            });
    }, [selectedAdapterId, joins]);

    // Merged columns from primary table and joined tables
    const allColumns = useMemo(() => {
        const list: (TableColumn & { displayName: string; queryName: string; table: string })[] = [];
        columns.forEach(col => {
            list.push({
                ...col,
                displayName: joins.length > 0 ? `${selectedTable}.${col.name}` : col.name,
                queryName: joins.length > 0 ? `${selectedTable}.${col.name}` : col.name,
                table: selectedTable
            });
        });
        joins.forEach(j => {
            const cols = joinedTablesColumns[j.table] || [];
            cols.forEach(col => {
                const queryName = `${j.table}.${col.name}`;
                if (!list.some(item => item.queryName === queryName)) {
                    list.push({
                        ...col,
                        displayName: queryName,
                        queryName: queryName,
                        table: j.table
                    });
                }
            });
        });
        return list;
    }, [columns, joinedTablesColumns, joins, selectedTable]);

    // Available fields: filtered by search query
    const availableFields = useMemo(() => {
        if (!searchQuery) return allColumns;
        const q = searchQuery.toLowerCase();
        return allColumns.filter(col => col.displayName.toLowerCase().includes(q));
    }, [allColumns, searchQuery]);

    // Clear cache when table changes
    useEffect(() => {
        setDistinctValuesCache({});
        fetchedFieldsRef.current.clear();
        if (!isInitialLoadRef.current) {
            setJoins([]);
            setJoinedTablesColumns({});
        }
    }, [selectedAdapterId, selectedTable]);

    // Fetch distinct values for filter checklist
    const fetchDistinctValues = useCallback(async (fieldName: string) => {
        if (distinctValuesCache[fieldName] || !selectedAdapterId || !selectedTable) return;
        if (fetchedFieldsRef.current.has(fieldName)) return;
        fetchedFieldsRef.current.add(fieldName);
        try {
            const res = await dataAdaptersApi.getDistinctValues(
                selectedAdapterId as number, selectedTable, fieldName
            );
            setDistinctValuesCache(prev => ({ ...prev, [fieldName]: res.values }));
        } catch {
            setDistinctValuesCache(prev => ({ ...prev, [fieldName]: [] }));
        }
    }, [selectedAdapterId, selectedTable, distinctValuesCache]);

    // Fetch distinct values for any filters in 'values' mode automatically
    useEffect(() => {
        if (!selectedAdapterId || !selectedTable) return;
        fields.forEach(f => {
            if (f.area === 'filters' && f.filterMode === 'values') {
                const key = f.fieldName;
                if (!distinctValuesCache[key] && !fetchedFieldsRef.current.has(key)) {
                    fetchDistinctValues(key);
                }
            }
        });
    }, [fields, selectedAdapterId, selectedTable, distinctValuesCache, fetchDistinctValues]);

    // Drag-and-Drop Handlers
    const handleDragStartFromAvailable = (fieldName: string) => {
        setDraggedField(fieldName);
        setDragSource('available');
    };

    const handleDragStartFromWell = (instanceId: string) => {
        setDraggedField(instanceId);
        setDragSource('well');
    };

    const handleDragEnd = () => {
        setDraggedField(null);
        setDragOverFieldId(null);
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
    };

    const handleDrop = (area: 'rows' | 'columns' | 'values' | 'filters') => {
        if (!draggedField) return;

        if (dragSource === 'well') {
            const found = fields.find(f => f.id === draggedField);
            if (!found) return;
            const updatedField: PivotFieldConfig = { ...found, area };
            if (area === 'values' && !found.aggregate) {
                const colMeta = allColumns.find(c => c.queryName === found.fieldName);
                const isNumeric = colMeta ? (
                    colMeta.data_type.toLowerCase().includes('int') || 
                    colMeta.data_type.toLowerCase().includes('num') || 
                    colMeta.data_type.toLowerCase().includes('float') || 
                    colMeta.data_type.toLowerCase().includes('double') || 
                    colMeta.data_type.toLowerCase().includes('decimal') || 
                    colMeta.data_type.toLowerCase().includes('real')
                ) : true;
                updatedField.aggregate = isNumeric ? 'SUM' : 'COUNT';
                updatedField.alias = `${updatedField.aggregate.toLowerCase()}_${found.fieldName}`;
                updatedField.showAs = 'default';
            }
            if (area === 'filters' && !found.filterMode) {
                updatedField.filterMode = 'values';
                updatedField.selectedValues = [];
                fetchDistinctValues(found.fieldName);
            }
            setFields(prev => {
                // Remove from old position and append at the end
                return [...prev.filter(f => f.id !== draggedField), updatedField];
            });
        } else {
            // Create a new instance from available fields
            const newField: PivotFieldConfig = {
                id: genId(),
                fieldName: draggedField,
                area,
                sortOrder: 'asc',
            };
            if (area === 'values') {
                const colMeta = allColumns.find(c => c.queryName === draggedField);
                const isNumeric = colMeta ? (
                    colMeta.data_type.toLowerCase().includes('int') || 
                    colMeta.data_type.toLowerCase().includes('num') || 
                    colMeta.data_type.toLowerCase().includes('float') || 
                    colMeta.data_type.toLowerCase().includes('double') || 
                    colMeta.data_type.toLowerCase().includes('decimal') || 
                    colMeta.data_type.toLowerCase().includes('real')
                ) : true;
                newField.aggregate = isNumeric ? 'SUM' : 'COUNT';
                newField.alias = `${newField.aggregate.toLowerCase()}_${draggedField}`;
                newField.showAs = 'default';
            }
            if (area === 'filters') {
                newField.filterMode = 'values';
                newField.selectedValues = [];
                fetchDistinctValues(draggedField);
            }
            setFields(prev => [...prev, newField]);
        }
        setDraggedField(null);
    };

    const handleDropOnField = (e: React.DragEvent, targetFieldId: string, area: 'rows' | 'columns' | 'values' | 'filters') => {
        e.preventDefault();
        e.stopPropagation();
        setDragOverFieldId(null);
        if (!draggedField) return;

        // If dropping onto self, do nothing
        if (dragSource === 'well' && draggedField === targetFieldId) return;

        let fieldToInsert: PivotFieldConfig;
        let isNew = false;

        if (dragSource === 'well') {
            const found = fields.find(f => f.id === draggedField);
            if (!found) return;
            fieldToInsert = { ...found, area };
            if (area === 'values' && !fieldToInsert.aggregate) {
                const colMeta = allColumns.find(c => c.queryName === fieldToInsert.fieldName);
                const isNumeric = colMeta ? (
                    colMeta.data_type.toLowerCase().includes('int') || 
                    colMeta.data_type.toLowerCase().includes('num') || 
                    colMeta.data_type.toLowerCase().includes('float') || 
                    colMeta.data_type.toLowerCase().includes('double') || 
                    colMeta.data_type.toLowerCase().includes('decimal') || 
                    colMeta.data_type.toLowerCase().includes('real')
                ) : true;
                fieldToInsert.aggregate = isNumeric ? 'SUM' : 'COUNT';
                fieldToInsert.alias = `${fieldToInsert.aggregate.toLowerCase()}_${fieldToInsert.fieldName}`;
                fieldToInsert.showAs = 'default';
            }
            if (area === 'filters' && !fieldToInsert.filterMode) {
                fieldToInsert.filterMode = 'values';
                fieldToInsert.selectedValues = [];
                fetchDistinctValues(fieldToInsert.fieldName);
            }
        } else {
            isNew = true;
            fieldToInsert = {
                id: genId(),
                fieldName: draggedField,
                area,
                sortOrder: 'asc',
            };
            if (area === 'values') {
                const colMeta = allColumns.find(c => c.queryName === draggedField);
                const isNumeric = colMeta ? (
                    colMeta.data_type.toLowerCase().includes('int') || 
                    colMeta.data_type.toLowerCase().includes('num') || 
                    colMeta.data_type.toLowerCase().includes('float') || 
                    colMeta.data_type.toLowerCase().includes('double') || 
                    colMeta.data_type.toLowerCase().includes('decimal') || 
                    colMeta.data_type.toLowerCase().includes('real')
                ) : true;
                fieldToInsert.aggregate = isNumeric ? 'SUM' : 'COUNT';
                fieldToInsert.alias = `${fieldToInsert.aggregate.toLowerCase()}_${draggedField}`;
                fieldToInsert.showAs = 'default';
            }
            if (area === 'filters') {
                fieldToInsert.filterMode = 'values';
                fieldToInsert.selectedValues = [];
                fetchDistinctValues(draggedField);
            }
        }

        setFields(prev => {
            const cleanList = isNew ? prev : prev.filter(f => f.id !== draggedField);
            const targetIdx = cleanList.findIndex(f => f.id === targetFieldId);
            if (targetIdx === -1) {
                return [...cleanList, fieldToInsert];
            }
            const result = [...cleanList];
            result.splice(targetIdx, 0, fieldToInsert);
            return result;
        });

        setDraggedField(null);
    };

    const removeField = (instanceId: string) => {
        setFields(prev => prev.filter(f => f.id !== instanceId));
    };

    const updateField = (instanceId: string, updates: Partial<PivotFieldConfig>) => {
        setFields(prev => prev.map(f => {
            if (f.id !== instanceId) return f;
            const next = { ...f, ...updates };
            // Auto-update alias when aggregate changes
            if (updates.aggregate && !f.customLabel) {
                next.alias = `${updates.aggregate.toLowerCase()}_${f.fieldName}`;
            }
            return next;
        }));
    };

    const toggleFilterValue = (instanceId: string, value: string) => {
        setFields(prev => prev.map(f => {
            if (f.id !== instanceId) return f;
            const selected = f.selectedValues || [];
            const next = selected.includes(value)
                ? selected.filter(v => v !== value)
                : [...selected, value];
            return { ...f, selectedValues: next };
        }));
    };

    const toggleAllFilterValues = (instanceId: string, allValues: string[]) => {
        setFields(prev => prev.map(f => {
            if (f.id !== instanceId) return f;
            const allSelected = (f.selectedValues || []).length === allValues.length;
            return { ...f, selectedValues: allSelected ? [] : [...allValues] };
        }));
    };

    // Run safe exploration query
    const handleExecute = async () => {
        if (!selectedAdapterId || !selectedTable) return;
        setExecuting(true);
        setError(null);

        // Compile dimensions (deduplicate: same field in rows + columns = one dimension)
        const dimSet = new Set<string>();
        fields.filter(f => f.area === 'rows' || f.area === 'columns').forEach(f => dimSet.add(f.fieldName));
        const dimensions = Array.from(dimSet);

        const metrics = fields.filter(f => f.area === 'values').map(f => ({
            column: f.fieldName,
            aggregate: f.aggregate || 'SUM',
            alias: (f.alias || `${(f.aggregate || 'SUM').toLowerCase()}_${f.fieldName}`).replace(/\./g, '_'),
        }));

        // Compile filters from multi-instance fields
        const filters: FilterConfig[] = fields.filter(f => f.area === 'filters').flatMap(f => {
            if (f.filterMode === 'values' && f.selectedValues && f.selectedValues.length > 0) {
                return [{
                    column: f.fieldName,
                    operator: 'IN' as FilterConfig['operator'],
                    value: f.selectedValues.join(','),
                }];
            } else if (f.filterMode === 'condition' && f.filterCondition) {
                return [{
                    column: f.fieldName,
                    operator: f.filterCondition.operator as FilterConfig['operator'],
                    value: f.filterCondition.value,
                }];
            }
            return [];
        });

        // Compute fingerprint for client-side smart re-pivoting
        const queryFingerprint = {
            table_name: selectedTable,
            joins: joins.length > 0 ? joins : undefined,
            dimensions: [...dimensions].sort(),
            metrics: [...metrics].sort((a, b) => a.alias.localeCompare(b.alias)),
            filters: [...filters].sort((a, b) => a.column.localeCompare(b.column) || a.operator.localeCompare(b.operator) || a.value.localeCompare(b.value)),
            row_limit: rowLimit,
        };
        const queryFingerprintStr = JSON.stringify(queryFingerprint);

        // Client-side pivot cache hit check
        if (queryResult && lastExecutedQueryFingerprintStr === queryFingerprintStr) {
            console.log("Client-side smart re-pivot: reusing existing queryResult.data");
            setIsClientRePivoted(true);
            setIsQueryFromCache(false);
            setExecuting(false);
            return;
        }

        const query: ExplorationQuery = {
            table_name: selectedTable,
            joins: joins.length > 0 ? joins : undefined,
            dimensions,
            metrics,
            filters,
            row_limit: rowLimit,
        };

        try {
            const result = await dataAdaptersApi.executeQuery(selectedAdapterId, query);
            
            // Map keys in result data to handle dot-notation for joins
            const mappedData = result.data.map(row => {
                const newRow = { ...row };
                if (joins.length > 0) {
                    joins.forEach(j => {
                        const cols = joinedTablesColumns[j.table] || [];
                        cols.forEach(c => {
                            const dbKey = `${j.table}_${c.name}`;
                            const queryKey = `${j.table}.${c.name}`;
                            if (dbKey in row) {
                                newRow[queryKey] = row[dbKey];
                            }
                        });
                    });
                    columns.forEach(c => {
                        const dbKeyPrefix = `${selectedTable}_${c.name}`;
                        const queryKey = `${selectedTable}.${c.name}`;
                        if (dbKeyPrefix in row) {
                            newRow[queryKey] = row[dbKeyPrefix];
                        } else if (c.name in row) {
                            newRow[queryKey] = row[c.name];
                        }
                    });
                }
                return newRow;
            });

            const finalResult = {
                ...result,
                data: mappedData
            };

            setQueryResult(finalResult);
            setLastExecutedQueryFingerprintStr(queryFingerprintStr);
            setIsClientRePivoted(false);
            setIsQueryFromCache(!!result.cache_hit);
            // Auto expand all row groups by default
            const rowFields = fields.filter(f => f.area === 'rows');
            const allKeys = getAllSubtotalKeys(finalResult.data, rowFields);
            setExpandedGroups(allKeys);
        } catch (err: any) {
            console.error(err);
            setError(err.response?.data?.error || 'Failed to execute query. Check database settings.');
        } finally {
            setExecuting(false);
        }
    };

    // Auto-run query on initial load if we restored some fields
    useEffect(() => {
        if (!loadingAdapters && !loadingTables && !loadingColumns && columns.length > 0 && fields.length > 0 && !hasAutoRunRef.current && !hasAutoRun) {
            hasAutoRunRef.current = true;
            setHasAutoRun(true);
            handleExecute();
        }
    }, [loadingAdapters, loadingTables, loadingColumns, columns, fields, hasAutoRun]);

    // Sync field names with join state (prefix with table name if joins are present)
    useEffect(() => {
        setFields(prev => prev.map(f => {
            if (joins.length > 0) {
                if (!f.fieldName.includes('.')) {
                    return {
                        ...f,
                        fieldName: `${selectedTable}.${f.fieldName}`,
                        alias: f.alias ? (f.alias.includes('.') ? f.alias : `${selectedTable}_${f.fieldName}`) : undefined
                    };
                }
            } else {
                if (f.fieldName.includes('.')) {
                    const parts = f.fieldName.split('.');
                    if (parts[0] === selectedTable) {
                        return {
                            ...f,
                            fieldName: parts[1],
                            alias: f.alias ? f.alias.replace(`${selectedTable}_`, '') : undefined
                        };
                    }
                }
            }
            return f;
        }));
    }, [joins.length, selectedTable]);

    // Populate tempJoins state when Join Builder opens
    useEffect(() => {
        if (showJoinBuilder) {
            setTempJoins(JSON.parse(JSON.stringify(joins)));
        }
    }, [showJoinBuilder, joins]);

    // Fetch joined table columns dynamically
    const fetchJoinedTableColumns = useCallback((tableName: string) => {
        if (!selectedAdapterId || !tableName || joinedTablesColumns[tableName]) return;
        dataAdaptersApi.getColumns(Number(selectedAdapterId), tableName)
            .then(res => {
                setJoinedTablesColumns(prev => ({
                    ...prev,
                    [tableName]: res.columns
                }));
            })
            .catch(err => {
                console.error(`Failed to fetch columns for table ${tableName}:`, err);
            });
    }, [selectedAdapterId, joinedTablesColumns]);

    // Pre-fetch columns for any already joined tables when modal opens
    useEffect(() => {
        if (showJoinBuilder && tempJoins.length > 0) {
            tempJoins.forEach(j => {
                if (j.table) {
                    fetchJoinedTableColumns(j.table);
                }
            });
        }
    }, [showJoinBuilder, tempJoins, fetchJoinedTableColumns]);

    const getRightTablesForJoin = (index: number) => {
        const selectedOthers = tempJoins
            .filter((_, idx) => idx !== index)
            .map(j => j.table)
            .filter(Boolean);
        return tables.filter(t => t !== selectedTable && !selectedOthers.includes(t));
    };

    const getLeftTablesForJoin = (index: number) => {
        const list = [selectedTable];
        for (let i = 0; i < index; i++) {
            const t = tempJoins[i].table;
            if (t && !list.includes(t)) {
                list.push(t);
            }
        }
        return list;
    };

    const handleRightTableChange = (index: number, newTable: string) => {
        setTempJoins(prev => prev.map((j, i) => {
            if (i !== index) return j;
            return {
                ...j,
                table: newTable,
                on: {
                    ...j.on,
                    right_column: ''
                }
            };
        }));
        if (newTable) {
            fetchJoinedTableColumns(newTable);
        }
    };

    const removeJoin = (index: number) => {
        const tableToRemove = tempJoins[index].table;
        let updated = tempJoins.filter((_, i) => i !== index);
        // Clean up subsequent joins depending on the deleted table
        updated = updated.map(j => {
            if (j.on.left_table === tableToRemove) {
                return {
                    ...j,
                    on: {
                        ...j.on,
                        left_table: selectedTable,
                        left_column: '',
                        right_column: ''
                    }
                };
            }
            return j;
        });
        setTempJoins(updated);
    };

    const handleSaveJoins = () => {
        // Validate
        for (const j of tempJoins) {
            if (!j.table || !j.on.left_column || !j.on.right_column) {
                setError("All joins must have a table and column mapping specified.");
                return;
            }
        }

        const activeTables = new Set(tempJoins.map(j => j.table));
        activeTables.add(selectedTable);

        // Clean up fields / filters referencing deleted tables
        setFields(prev => prev.filter(f => {
            if (f.fieldName.includes('.')) {
                const tbl = f.fieldName.split('.')[0];
                return activeTables.has(tbl);
            }
            return true;
        }));

        setJoins(tempJoins);
        setShowJoinBuilder(false);
    };

    // Dashboard navigation & sheet persistence actions
    const handleSaveExploration = (name?: string) => {
        const finalName = name || explorationName || 'Untitled Sheet';
        if (activeExplorationId) {
            setSavedExplorations(prev => prev.map(exp => {
                if (exp.id === activeExplorationId) {
                    return {
                        ...exp,
                        name: finalName,
                        updatedAt: new Date().toISOString(),
                        selectedAdapterId,
                        selectedTable,
                        joins,
                        fields,
                        rowLimit,
                        sortColumn,
                        sortDirection,
                    };
                }
                return exp;
            }));
        } else {
            const newId = genId();
            const newExp: SavedExploration = {
                id: newId,
                name: finalName,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString(),
                selectedAdapterId,
                selectedTable,
                joins,
                fields,
                rowLimit,
                sortColumn,
                sortDirection,
            };
            setActiveExplorationId(newId);
            setSavedExplorations(prev => [newExp, ...prev]);
        }
        setSaveSuccess(true);
        setTimeout(() => setSaveSuccess(false), 2000);
    };

    const handleDeleteExploration = (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (window.confirm('Are you sure you want to delete this sheet exploration?')) {
            setSavedExplorations(prev => prev.filter(exp => exp.id !== id));
            if (activeExplorationId === id) {
                setActiveExplorationId(null);
                setExplorationName('New Sheet');
            }
        }
    };

    const handleCreateNewSheet = () => {
        setActiveExplorationId(null);
        setExplorationName('New Sheet');
        setFields([]);
        setQueryResult(null);
        setError(null);
        setSortColumn(null);
        setSortDirection('asc');
        if (adapters.length > 0) {
            setSelectedAdapterId(adapters[0].id);
        }
        setActiveView('playground');
    };

    const handleLoadExploration = (exp: SavedExploration) => {
        isInitialLoadRef.current = true;
        setActiveExplorationId(exp.id);
        setExplorationName(exp.name);
        setSelectedAdapterId(exp.selectedAdapterId);
        setSelectedTable(exp.selectedTable);
        setJoins(exp.joins || []);
        setFields(exp.fields);
        setRowLimit(exp.rowLimit);
        setSortColumn(exp.sortColumn);
        setSortDirection(exp.sortDirection);
        setQueryResult(null);
        setError(null);
        setHasAutoRun(false);
        setActiveView('playground');
    };

    // Check if configuration has changed since last query run
    const isDirty = useMemo(() => {
        if (!queryResult) return false;
        
        const dimSet = new Set<string>();
        fields.filter(f => f.area === 'rows' || f.area === 'columns').forEach(f => dimSet.add(f.fieldName));
        const dimensions = Array.from(dimSet);

        const metrics = fields.filter(f => f.area === 'values').map(f => ({
            column: f.fieldName,
            aggregate: f.aggregate || 'SUM',
            alias: (f.alias || `${(f.aggregate || 'SUM').toLowerCase()}_${f.fieldName}`).replace(/\./g, '_'),
        }));

        const filters = fields.filter(f => f.area === 'filters').flatMap(f => {
            if (f.filterMode === 'values' && f.selectedValues && f.selectedValues.length > 0) {
                return [{
                    column: f.fieldName,
                    operator: 'IN' as FilterConfig['operator'],
                    value: f.selectedValues.join(','),
                }];
            } else if (f.filterMode === 'condition' && f.filterCondition) {
                return [{
                    column: f.fieldName,
                    operator: f.filterCondition.operator as FilterConfig['operator'],
                    value: f.filterCondition.value,
                }];
            }
            return [];
        });

        const currentQueryFingerprint = {
            table_name: selectedTable,
            joins: joins.length > 0 ? joins : undefined,
            dimensions: [...dimensions].sort(),
            metrics: [...metrics].sort((a, b) => a.alias.localeCompare(b.alias)),
            filters: [...filters].sort((a, b) => a.column.localeCompare(b.column) || a.operator.localeCompare(b.operator) || a.value.localeCompare(b.value)),
            row_limit: rowLimit,
        };

        return JSON.stringify(currentQueryFingerprint) !== lastExecutedQueryFingerprintStr;
    }, [queryResult, fields, selectedTable, rowLimit, joins, lastExecutedQueryFingerprintStr]);

    // Reshape results using pivot engine
    const pivotModel = useMemo(() => {
        if (!queryResult) return null;
        return buildPivotModel(queryResult.data, fields, expandedGroups);
    }, [queryResult, fields, expandedGroups]);

    // Compute z-scores when a conditional format rule is active
    const zScoreResult = useMemo(() => {
        if (!pivotModel || !conditionalFormatRule) return null;
        return computeZScores(pivotModel, conditionalFormatRule.targetHeader);
    }, [pivotModel, conditionalFormatRule]);

    // Handle interactive table sort
    const handleSort = (column: string) => {
        const rowFields = fields.filter(f => f.area === 'rows');
        const isRowField = rowFields.some(rf => rf.fieldName === column);

        if (isRowField) {
            setFields(prev => prev.map(f => {
                if (f.fieldName === column && f.area === 'rows') {
                    const newSort = f.sortOrder === 'desc' ? 'asc' : 'desc';
                    setSortColumn(column);
                    setSortDirection(newSort);
                    return { ...f, sortOrder: newSort };
                }
                return f;
            }));
        } else {
            if (sortColumn === column) {
                setSortDirection(prev => (prev === 'asc' ? 'desc' : 'asc'));
            } else {
                setSortColumn(column);
                setSortDirection('asc');
            }
        }
    };

    // Apply sorting to the pivot rows while maintaining parent-child grouping hierarchy
    const sortedPivotRows = useMemo(() => {
        if (!pivotModel) return [];
        if (!sortColumn) return pivotModel.rows;

        const toNumber = (val: unknown): number => {
            if (typeof val === 'number') return val;
            if (typeof val === 'string') {
                const parsed = parseFloat(val);
                return isNaN(parsed) ? 0 : parsed;
            }
            return 0;
        };

        // Build a lookup map of groupKey -> row for quick access
        const rowsMap = new Map<string, typeof pivotModel.rows[0]>();
        for (const row of pivotModel.rows) {
            rowsMap.set(row.groupKey, row);
        }

        const isRowFieldsSort = fields.some(f => f.fieldName === sortColumn && f.area === 'rows');

        return [...pivotModel.rows].sort((a, b) => {
            if (a.groupKey === b.groupKey) return 0;

            const pathA = a.groupKey.split('||');
            const pathB = b.groupKey.split('||');

            const minLen = Math.min(pathA.length, pathB.length);
            let diffIdx = -1;
            for (let i = 0; i < minLen; i++) {
                if (pathA[i] !== pathB[i]) {
                    diffIdx = i;
                    break;
                }
            }

            if (diffIdx === -1) {
                // One is a prefix of the other (e.g. North vs North||Office)
                // The parent (shorter path) always goes first (at the top as group header)
                return pathA.length - pathB.length;
            }

            // They differ at diffIdx. Find the row objects at that level
            const keyA = pathA.slice(0, diffIdx + 1).join('||');
            const keyB = pathB.slice(0, diffIdx + 1).join('||');

            const rA = rowsMap.get(keyA);
            const rB = rowsMap.get(keyB);

            if (!rA || !rB) {
                // Fallback to alphabetical if row not found (should not happen)
                return pathA[diffIdx].localeCompare(pathB[diffIdx], undefined, { numeric: true });
            }

            const rowFields = fields.filter(f => f.area === 'rows');
            const fieldAtDiff = rowFields[diffIdx];

            // Compare by sortColumn
            if (isRowFieldsSort) {
                // Sorting by a row dimension
                const sortDir = (fieldAtDiff?.fieldName === sortColumn) ? sortDirection : (fieldAtDiff?.sortOrder || 'asc');
                const valA = pathA[diffIdx];
                const valB = pathB[diffIdx];
                return sortDir === 'asc'
                    ? valA.localeCompare(valB, undefined, { numeric: true })
                    : valB.localeCompare(valA, undefined, { numeric: true });
            } else {
                // Sorting by a value metric column
                const valA = toNumber(rA.values[sortColumn]);
                const valB = toNumber(rB.values[sortColumn]);

                if (valA !== valB) {
                    return sortDirection === 'asc' ? valA - valB : valB - valA;
                }

                // Tie-breaker: sort direction configured for this level
                const sortDir = fieldAtDiff?.sortOrder || 'asc';
                const labelA = pathA[diffIdx];
                const labelB = pathB[diffIdx];
                return sortDir === 'asc'
                    ? labelA.localeCompare(labelB, undefined, { numeric: true })
                    : labelB.localeCompare(labelA, undefined, { numeric: true });
            }
        });
    }, [pivotModel, sortColumn, sortDirection, fields]);

    const handleExport = (format: 'csv' | 'xlsx' | 'xls' | 'pdf') => {
        if (!pivotModel) return;

        const rowFields = fields.filter(f => f.area === 'rows');
        
        if (format === 'csv') {
            const csvContent = pivotToCSV(pivotModel, rowFields);
            const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.setAttribute('href', url);
            link.setAttribute('download', `klex_pivot_${selectedTable || 'export'}.csv`);
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } else if (format === 'xlsx' || format === 'xls') {
            const data2D = pivotTo2DArray(pivotModel, rowFields);
            const ws = XLSX.utils.aoa_to_sheet(data2D);

            // Styling column widths roughly
            const colWidths = data2D[0].map((_, colIndex) => {
                let maxLen = 10;
                for (let rowIndex = 0; rowIndex < data2D.length; rowIndex++) {
                    const cellVal = data2D[rowIndex][colIndex];
                    if (cellVal != null) {
                        const len = String(cellVal).length;
                        if (len > maxLen) maxLen = len;
                    }
                }
                return { wch: maxLen + 2 };
            });
            ws['!cols'] = colWidths;

            const wb = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(wb, ws, 'Pivot Table');
            
            const fileExtension = format === 'xlsx' ? 'xlsx' : 'xls';
            XLSX.writeFile(wb, `klex_pivot_${selectedTable || 'export'}.${fileExtension}`);
        } else if (format === 'pdf') {
            let headersToExport = [...pivotModel.headers];
            let isTruncated = false;
            if (headersToExport.length > 100) {
                headersToExport = headersToExport.slice(0, 100);
                isTruncated = true;
                window.alert(`The table has ${pivotModel.headers.length} value columns. PDF export is limited to the first 100 columns for readability. For the full dataset, please use CSV or Excel export.`);
            }

            const COLS_PER_PAGE = 6;
            const chunks: string[][] = [];
            for (let i = 0; i < headersToExport.length; i += COLS_PER_PAGE) {
                chunks.push(headersToExport.slice(i, i + COLS_PER_PAGE));
            }

            const doc = new jsPDF({ orientation: 'landscape' });
            const rowHeaders = rowFields.length > 0 ? rowFields.map(rf => rf.fieldName) : ['Label'];

            chunks.forEach((chunk, chunkIdx) => {
                if (chunkIdx > 0) {
                    doc.addPage();
                }

                // Add Title
                doc.setFontSize(14);
                doc.text(`Pivot Table: ${selectedTable || 'Export'} (Part ${chunkIdx + 1} of ${chunks.length})`, 14, 10);
                doc.setFontSize(9);
                doc.text(`Generated on: ${new Date().toLocaleString()}`, 14, 15);
                if (isTruncated) {
                    doc.setTextColor(220, 38, 38); // red-600
                    doc.text("Warning: Truncated to the first 100 value columns. Use CSV or Excel for full export.", 14, 19);
                    doc.setTextColor(0, 0, 0); // reset
                }

                // Build table for this chunk
                const tableHeaders = [...rowHeaders, ...chunk];

                // Build body rows: rowLabels + values for current chunk
                const tableBody = pivotModel.rows
                    .filter(row => row.visible)
                    .map(row => {
                        let rowLabels: string[] = [];
                        if (rowFields.length > 0) {
                            const parts = row.groupKey.split('||');
                            for (let i = 0; i < rowFields.length; i++) {
                                if (i < row.depth) {
                                    rowLabels.push(rowFields[i].repeatLabels ? (parts[i] || '') : '');
                                } else if (i === row.depth) {
                                    rowLabels.push(parts[i] || '');
                                } else {
                                    rowLabels.push(row.type === 'subtotal' ? 'Total' : '');
                                }
                            }
                        } else {
                            rowLabels = [row.label];
                        }

                        const cells = [...rowLabels];
                        for (const header of chunk) {
                            const val = row.values[header];
                            const showAs = pivotModel.headerShowAs[header];
                            if (val != null) {
                                if (showAs && showAs !== 'default' && typeof val === 'number') {
                                    cells.push(`${val.toFixed(1)}%`);
                                } else {
                                    const numVal = Number(val);
                                    cells.push(isNaN(numVal) ? String(val) : String(numVal));
                                }
                            } else {
                                cells.push('');
                            }
                        }
                        return cells;
                    });

                // Add Grand Total row if present
                if (Object.keys(pivotModel.grandTotal).length > 0) {
                    const rowLabels = rowFields.length > 0 
                        ? ['Grand Total', ...Array(rowFields.length - 1).fill('')]
                        : ['Grand Total'];
                    const cells = [...rowLabels];
                    for (const header of chunk) {
                        const val = pivotModel.grandTotal[header];
                        const showAs = pivotModel.headerShowAs[header];
                        if (val != null) {
                            if (showAs && showAs !== 'default') {
                                cells.push(`${val.toFixed(1)}%`);
                            } else {
                                const numVal = Number(val);
                                cells.push(isNaN(numVal) ? String(val) : String(numVal));
                            }
                        } else {
                            cells.push('');
                        }
                    }
                    tableBody.push(cells);
                }

                autoTable(doc, {
                    head: [tableHeaders],
                    body: tableBody,
                    startY: isTruncated ? 22 : 20,
                    theme: 'striped',
                    headStyles: { fillColor: [16, 185, 129], textColor: 255 }, // emerald-600 color
                    styles: { fontSize: 8, cellPadding: 2 },
                });
            });

            doc.save(`klex_pivot_${selectedTable || 'export'}.pdf`);
        }
    };

    const toggleGroup = (groupKey: string) => {
        setExpandedGroups(prev => {
            const next = new Set(prev);
            if (next.has(groupKey)) {
                next.delete(groupKey);
            } else {
                next.add(groupKey);
            }
            return next;
        });
    };

    const handleToggleExpandAll = () => {
        if (!pivotModel) return;
        const allKeys = pivotModel.rows.filter(r => r.type === 'subtotal').map(r => r.groupKey);
        setExpandedGroups(prev => {
            if (prev.size === allKeys.length) {
                return new Set();
            } else {
                return new Set(allKeys);
            }
        });
    };

    if (activeView === 'dashboard') {
        return (
            <div className="flex flex-col h-[calc(100vh-64px)] space-y-6 overflow-y-auto p-6 bg-gray-50/50 dark:bg-surface-900/20">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-extrabold text-gray-900 dark:text-gray-100 tracking-tight">
                            Pivot Table Sheets
                        </h1>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                            Build custom pivot table reports, aggregate metrics, and explore database connections.
                        </p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
                    {/* Create New Sheet Card */}
                    <div
                        onClick={handleCreateNewSheet}
                        className="bg-white dark:bg-surface-800 p-5 rounded-2xl border-2 border-dashed border-gray-200 dark:border-gray-700 hover:border-primary-500 dark:hover:border-primary-500 cursor-pointer transition-all flex flex-col items-center justify-center h-[180px] text-center gap-2 group shadow-sm hover:shadow-md"
                    >
                        <div className="w-12 h-12 rounded-full bg-primary-50 dark:bg-primary-950/40 text-primary-600 dark:text-primary-400 flex items-center justify-center group-hover:bg-primary-600 group-hover:text-white transition-all shadow-sm">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-6 h-6">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                            </svg>
                        </div>
                        <div>
                            <span className="block font-bold text-sm text-gray-800 dark:text-gray-200 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                                Create New Sheet
                            </span>
                            <span className="block text-xs text-gray-400 dark:text-gray-500 mt-0.5">
                                Start a fresh layout configuration
                            </span>
                        </div>
                    </div>

                    {/* Saved Explorations */}
                    {savedExplorations.map(exp => {
                        const adapterName = adapters.find(a => a.id === exp.selectedAdapterId)?.name || 'Unknown Adapter';
                        const dateStr = new Date(exp.updatedAt).toLocaleDateString(undefined, {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                        });

                        return (
                            <div
                                key={exp.id}
                                onClick={() => handleLoadExploration(exp)}
                                className="bg-white dark:bg-surface-800 p-5 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm hover:shadow-md hover:border-primary-300 dark:hover:border-primary-700/60 cursor-pointer transition-all flex flex-col justify-between h-[180px] relative group"
                            >
                                <button
                                    onClick={(e) => handleDeleteExploration(exp.id, e)}
                                    className="absolute top-4 right-4 text-gray-400 hover:text-red-500 transition-colors p-1 rounded-lg hover:bg-gray-50 dark:hover:bg-surface-700"
                                    title="Delete Sheet"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                                    </svg>
                                </button>

                                <div className="space-y-1">
                                    <div className="flex items-center gap-1.5 text-primary-500 dark:text-primary-400">
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 5.25h16.5m-16.5 4.5h16.5m-16.5 4.5h16.5m-16.5 4.5h16.5" />
                                        </svg>
                                        <span className="text-[10px] uppercase font-bold tracking-wider">
                                            Pivot Table Sheet
                                        </span>
                                    </div>
                                    <h3 className="font-bold text-gray-800 dark:text-gray-200 truncate pr-6 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                                        {exp.name}
                                    </h3>
                                    <div className="flex flex-wrap gap-1 mt-1">
                                        <span className="text-[10px] bg-gray-50 dark:bg-surface-900 border border-gray-100 dark:border-gray-700/60 text-gray-600 dark:text-gray-400 font-mono py-0.5 px-1.5 rounded w-fit">
                                            {adapterName}
                                        </span>
                                        <span className="text-[10px] bg-primary-50 dark:bg-primary-950/20 text-primary-600 dark:text-primary-400 font-mono py-0.5 px-1.5 rounded w-fit">
                                            {exp.selectedTable}
                                        </span>
                                    </div>
                                </div>

                                <div className="text-[10px] text-gray-400 dark:text-gray-500 font-medium">
                                    Saved {dateStr}
                                </div>
                            </div>
                        );
                    })}
                </div>

                {savedExplorations.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-20 text-center bg-white dark:bg-surface-800 border border-gray-100 dark:border-gray-800 rounded-3xl p-6 shadow-sm">
                        <div className="w-16 h-16 rounded-full bg-gray-50 dark:bg-surface-900 flex items-center justify-center text-gray-400 mb-4">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-8 h-8">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                            </svg>
                        </div>
                        <h3 className="text-base font-bold text-gray-800 dark:text-gray-200">No sheets found</h3>
                        <p className="text-xs text-gray-400 dark:text-gray-500 max-w-sm mt-1">
                            You haven't saved any pivot table layouts yet. Create a new sheet above to get started.
                        </p>
                    </div>
                )}
            </div>
        );
    }

    return (
        <div className="flex flex-col h-[calc(100vh-64px)] space-y-4">
            {/* Header Toolbar */}
            <div className="flex items-center justify-between bg-white dark:bg-surface-800 p-4 border border-gray-100 dark:border-gray-800 shadow-sm rounded-2xl flex-shrink-0">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => {
                            setActiveView('dashboard');
                            setActiveExplorationId(null);
                            setExplorationName('New Sheet');
                        }}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-surface-900 text-xs font-semibold hover:bg-gray-100 dark:hover:bg-surface-800 transition-all text-gray-600 dark:text-gray-300"
                    >
                        ← Back to Sheets
                    </button>
                    <div className="h-6 w-px bg-gray-200 dark:bg-gray-700" />
                    <div>
                        <div className="flex items-center gap-2">
                            <input
                                type="text"
                                value={explorationName}
                                onChange={(e) => setExplorationName(e.target.value)}
                                className="bg-transparent border-b border-transparent hover:border-gray-300 dark:hover:border-gray-700 focus:border-primary-500 focus:outline-none text-base font-bold text-gray-900 dark:text-gray-100 px-1 py-0.5 w-48 transition-all"
                                placeholder="Sheet Name..."
                            />
                            <button
                                onClick={() => handleSaveExploration()}
                                className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition-all ${
                                    saveSuccess 
                                        ? 'bg-green-600 text-white shadow-green-500/20' 
                                        : 'bg-primary-600 hover:bg-primary-700 text-white shadow-primary-500/20'
                                } shadow-sm`}
                            >
                                {saveSuccess ? '✓ Saved' : 'Save'}
                            </button>
                        </div>
                        <p className="text-[10px] text-gray-400 dark:text-gray-500 pl-1">
                            Interactive Pivot Table sheet
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-gray-400 dark:text-gray-500 uppercase tracking-wider">Connection</span>
                        <select
                            className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-surface-900 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300"
                            value={selectedAdapterId}
                            onChange={(e) => setSelectedAdapterId(Number(e.target.value))}
                            disabled={loadingAdapters}
                        >
                            {loadingAdapters && <option>Loading adapters...</option>}
                            {!loadingAdapters && adapters.length === 0 && <option>No active Postgres or CSV adapters</option>}
                            {adapters.map(a => (
                                <option key={a.id} value={a.id}>{a.name}</option>
                            ))}
                        </select>
                    </div>

                    <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-gray-400 dark:text-gray-500 uppercase tracking-wider">Table</span>
                        <select
                            className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-surface-900 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300"
                            value={selectedTable}
                            onChange={(e) => setSelectedTable(e.target.value)}
                            disabled={loadingTables || tables.length === 0}
                        >
                            {loadingTables && <option>Loading tables...</option>}
                            {!loadingTables && tables.length === 0 && <option>No tables found</option>}
                            {tables.map(t => (
                                <option key={t} value={t}>{t}</option>
                            ))}
                        </select>
                    </div>

                    {adapters.find(a => a.id === selectedAdapterId)?.adapter_type === 'jdbc' && selectedTable && (
                        <button
                            onClick={() => setShowJoinBuilder(true)}
                            className="flex items-center gap-1.5 rounded-xl border border-primary-200 dark:border-primary-800 bg-primary-50/50 dark:bg-primary-950/20 hover:bg-primary-50 dark:hover:bg-primary-950/30 px-3 py-1.5 text-sm font-semibold text-primary-600 dark:text-primary-400 transition-colors"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                            </svg>
                            <span>Join ({joins.length})</span>
                        </button>
                    )}
                </div>
            </div>

            {/* Split Panel Layout */}
            <div className={`flex flex-1 min-h-0 overflow-hidden transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'gap-0' : 'gap-4'}`}>
                {/* Panel 1: Available Fields (Leftmost) */}
                <div 
                    className={`bg-white dark:bg-surface-800 shadow-sm rounded-2xl flex flex-col min-h-0 transition-all duration-300 ease-in-out flex-shrink-0 ${
                        isSidebarCollapsed 
                            ? 'w-0 opacity-0 p-0 border-0 overflow-hidden pointer-events-none' 
                            : 'w-[280px] opacity-100 p-4 border border-gray-100 dark:border-gray-800'
                    }`}
                >
                        <div className="flex flex-col h-full min-h-0">
                            <div className="flex items-center justify-between mb-2 flex-shrink-0">
                                <span className="text-xs font-bold uppercase tracking-wider text-gray-400 dark:text-gray-500">
                                    Available Fields ({availableFields.length})
                                </span>
                            </div>
                            
                            {/* Search bar */}
                            <div className="relative mb-3 flex-shrink-0">
                                <input
                                    type="text"
                                    placeholder="Search fields..."
                                    className="w-full pl-8 pr-8 py-1.5 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-surface-900 text-xs text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                />
                                <svg className="absolute left-2.5 top-2.5 w-3.5 h-3.5 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                </svg>
                                {searchQuery && (
                                    <button
                                        onClick={() => setSearchQuery('')}
                                        className="absolute right-2.5 top-2 text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 text-sm font-bold"
                                    >
                                        ×
                                    </button>
                                )}
                            </div>

                            {/* List container - fills remaining height */}
                            <div className="flex-1 overflow-y-auto border border-gray-100 dark:border-gray-700/60 rounded-xl p-2 bg-gray-50 dark:bg-surface-900/40 space-y-1.5 min-h-0">
                                {loadingColumns && (
                                    <div className="text-center py-6 text-sm text-gray-400 dark:text-gray-500">
                                        Loading columns...
                                    </div>
                                )}
                                {!loadingColumns && availableFields.length === 0 && (
                                    <div className="text-center py-6 text-xs text-gray-400 dark:text-gray-500">
                                        {columns.length === 0 ? 'Select a table to see fields' : 'No matching fields'}
                                    </div>
                                )}
                                {!loadingColumns && availableFields.map(col => (
                                    <div
                                        key={col.name}
                                        draggable
                                        onDragStart={() => handleDragStartFromAvailable(col.name)}
                                        onDragEnd={handleDragEnd}
                                        className="flex items-center justify-between p-2 rounded-lg bg-white dark:bg-surface-800 border border-gray-100 dark:border-gray-700 shadow-sm cursor-grab active:cursor-grabbing hover:border-primary-400 dark:hover:border-primary-500 transition-colors"
                                    >
                                        <div className="flex items-center gap-2 min-w-0">
                                            <svg className="w-4 h-4 text-gray-400 dark:text-gray-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M4 6h16M4 12h16M4 18h16" />
                                            </svg>
                                            <span className="text-xs text-gray-800 dark:text-gray-200 font-medium truncate">{col.name}</span>
                                        </div>
                                        <span className="text-[9px] uppercase font-bold tracking-wider text-gray-400 dark:text-gray-500 bg-gray-100 dark:bg-surface-700 px-1.5 py-0.5 rounded flex-shrink-0">
                                            {col.data_type.split(' ')[0]}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                {/* Panel 2: Field Wells & Controls (Middle) */}
                <div 
                    className={`bg-white dark:bg-surface-800 shadow-sm rounded-2xl flex flex-col min-h-0 transition-all duration-300 ease-in-out flex-shrink-0 ${
                        isSidebarCollapsed 
                            ? 'w-0 opacity-0 p-0 border-0 overflow-hidden pointer-events-none' 
                            : 'w-[320px] opacity-100 p-4 border border-gray-100 dark:border-gray-800'
                    }`}
                >
                    <div className="flex flex-col h-full min-h-0">
                        <span className="text-xs font-bold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-3 flex-shrink-0">
                            Pivot Table Layout
                        </span>
                        
                        {/* Scrollable Wells Container */}
                        <div className="flex-1 overflow-y-auto space-y-3 pr-1 min-h-0 mb-4">
                            {/* Well: Rows */}
                            <div
                                onDragOver={handleDragOver}
                                onDrop={() => handleDrop('rows')}
                                className="border-2 border-dashed border-gray-200 dark:border-gray-700/80 rounded-xl p-3 bg-gray-50/50 dark:bg-surface-900/20 hover:border-primary-500 hover:bg-primary-50/10 dark:hover:border-primary-500 transition-colors"
                            >
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-xs font-bold text-gray-700 dark:text-gray-300">↕ Row Groups</span>
                                    <span className="text-[10px] text-gray-400">Rows axis</span>
                                </div>
                                <div className="space-y-1.5 min-h-[38px]">
                                    {fields.filter(f => f.area === 'rows').map(f => (
                                        <div
                                            key={f.id}
                                            draggable
                                            onDragStart={() => handleDragStartFromWell(f.id)}
                                            onDragEnd={handleDragEnd}
                                            onDragOver={(e) => {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                setDragOverFieldId(f.id);
                                            }}
                                            onDragLeave={() => setDragOverFieldId(null)}
                                            onDrop={(e) => handleDropOnField(e, f.id, 'rows')}
                                            className={`flex flex-col gap-1.5 p-2 rounded-lg bg-primary-50 dark:bg-primary-900/20 text-primary-900 dark:text-primary-300 border text-xs cursor-grab transition-all ${
                                                dragOverFieldId === f.id 
                                                    ? 'border-primary-500 ring-2 ring-primary-500/25 scale-[0.98]' 
                                                    : 'border-primary-200/50 dark:border-primary-800'
                                            }`}
                                        >
                                            <div className="flex items-center justify-between font-semibold">
                                                <span className="truncate mr-2">{f.fieldName}</span>
                                                <button onClick={() => removeField(f.id)} className="hover:text-red-500 text-sm">×</button>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <select
                                                    className="bg-white dark:bg-surface-800 border border-primary-200 dark:border-primary-700 text-[10px] py-0.5 px-1 rounded cursor-pointer"
                                                    value={f.sortOrder || 'asc'}
                                                    onChange={(e) => updateField(f.id, { sortOrder: e.target.value as 'asc' | 'desc' })}
                                                >
                                                    <option value="asc">A → Z</option>
                                                    <option value="desc">Z → A</option>
                                                </select>
                                                <label className="flex items-center gap-1 text-[10px] text-gray-500 dark:text-gray-400 cursor-pointer">
                                                    <input
                                                        type="checkbox"
                                                        checked={f.repeatLabels || false}
                                                        onChange={(e) => updateField(f.id, { repeatLabels: e.target.checked })}
                                                        className="w-3 h-3 rounded"
                                                    />
                                                    Repeat labels
                                                </label>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Well: Columns */}
                            <div
                                onDragOver={handleDragOver}
                                onDrop={() => handleDrop('columns')}
                                className="border-2 border-dashed border-gray-200 dark:border-gray-700/80 rounded-xl p-3 bg-gray-50/50 dark:bg-surface-900/20 hover:border-primary-500 hover:bg-primary-50/10 dark:hover:border-primary-500 transition-colors"
                            >
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-xs font-bold text-gray-700 dark:text-gray-300">↔ Column Labels</span>
                                    <span className="text-[10px] text-gray-400">Cols axis</span>
                                </div>
                                <div className="flex flex-wrap gap-1.5 min-h-[38px]">
                                    {fields.filter(f => f.area === 'columns').map(f => (
                                        <div
                                            key={f.id}
                                            draggable
                                            onDragStart={() => handleDragStartFromWell(f.id)}
                                            onDragEnd={handleDragEnd}
                                            onDragOver={(e) => {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                setDragOverFieldId(f.id);
                                            }}
                                            onDragLeave={() => setDragOverFieldId(null)}
                                            onDrop={(e) => handleDropOnField(e, f.id, 'columns')}
                                            className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold cursor-grab transition-all ${
                                                dragOverFieldId === f.id
                                                    ? 'bg-teal-200 dark:bg-teal-800 border-teal-500 ring-2 ring-teal-500/25 scale-[0.98]'
                                                    : 'bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-300 border border-teal-200/50'
                                            }`}
                                        >
                                            <span className="truncate max-w-[120px]">{f.fieldName}</span>
                                            <button onClick={() => removeField(f.id)} className="hover:text-red-500 ml-1">×</button>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Well: Values */}
                            <div
                                onDragOver={handleDragOver}
                                onDrop={() => handleDrop('values')}
                                className="border-2 border-dashed border-gray-200 dark:border-gray-700/80 rounded-xl p-3 bg-gray-50/50 dark:bg-surface-900/20 hover:border-primary-500 hover:bg-primary-50/10 dark:hover:border-primary-500 transition-colors"
                            >
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-xs font-bold text-gray-700 dark:text-gray-300">Σ Values</span>
                                    <span className="text-[10px] text-gray-400">Metrics</span>
                                </div>
                                <div className="space-y-1.5 min-h-[38px]">
                                    {fields.filter(f => f.area === 'values').map(f => (
                                        <div
                                            key={f.id}
                                            draggable
                                            onDragStart={() => handleDragStartFromWell(f.id)}
                                            onDragEnd={handleDragEnd}
                                            onDragOver={(e) => {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                setDragOverFieldId(f.id);
                                            }}
                                            onDragLeave={() => setDragOverFieldId(null)}
                                            onDrop={(e) => handleDropOnField(e, f.id, 'values')}
                                            className={`flex flex-col gap-1.5 p-2 rounded-lg bg-indigo-50 dark:bg-indigo-900/20 text-indigo-900 dark:text-indigo-300 border text-xs cursor-grab transition-all ${
                                                dragOverFieldId === f.id
                                                    ? 'border-indigo-500 ring-2 ring-indigo-500/25 scale-[0.98]'
                                                    : 'border-indigo-100 dark:border-indigo-800'
                                            }`}
                                        >
                                            <div className="flex items-center justify-between font-semibold">
                                                <span className="truncate mr-2">{f.customLabel || f.fieldName}</span>
                                                <button onClick={() => removeField(f.id)} className="hover:text-red-500 text-sm">×</button>
                                            </div>
                                            <div className="flex items-center gap-1.5 flex-wrap">
                                                <div className="flex items-center gap-1">
                                                    <span className="text-[9px] text-gray-500 dark:text-gray-400 uppercase">Summarize:</span>
                                                    <select
                                                        className="bg-white dark:bg-surface-800 border border-indigo-200 dark:border-indigo-700 text-[10px] py-0.5 px-1 rounded cursor-pointer"
                                                        value={f.aggregate || 'SUM'}
                                                        onChange={(e) => updateField(f.id, { aggregate: e.target.value as AggregateType })}
                                                    >
                                                        {AVAILABLE_AGGREGATES.map(op => (
                                                            <option key={op.value} value={op.value}>{op.label}</option>
                                                        ))}
                                                    </select>
                                                </div>
                                                <div className="flex items-center gap-1">
                                                    <span className="text-[9px] text-gray-500 dark:text-gray-400 uppercase">Show as:</span>
                                                    <select
                                                        className="bg-white dark:bg-surface-800 border border-indigo-200 dark:border-indigo-700 text-[10px] py-0.5 px-1 rounded cursor-pointer"
                                                        value={f.showAs || 'default'}
                                                        onChange={(e) => updateField(f.id, { showAs: e.target.value as ShowAsMode })}
                                                    >
                                                        {SHOW_AS_OPTIONS.map(opt => (
                                                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                        ))}
                                                    </select>
                                                </div>
                                                <div className="flex items-center gap-1">
                                                    <span className="text-[9px] text-gray-500 dark:text-gray-400 uppercase">Sort:</span>
                                                    <select
                                                        className="bg-white dark:bg-surface-800 border border-indigo-200 dark:border-indigo-700 text-[10px] py-0.5 px-1 rounded cursor-pointer"
                                                        value={
                                                            sortColumn && (sortColumn === getValueLabel(f) || sortColumn.endsWith(` — ${getValueLabel(f)}`))
                                                                ? sortDirection
                                                                : 'none'
                                                        }
                                                        onChange={(e) => {
                                                            const val = e.target.value;
                                                            if (val === 'none') {
                                                                setSortColumn(null);
                                                                setSortDirection('asc');
                                                            } else {
                                                                const metricLabel = getValueLabel(f);
                                                                let matchingHeader = metricLabel;
                                                                if (pivotModel?.headers) {
                                                                    const found = pivotModel.headers.find(h => h === metricLabel || h.endsWith(` — ${metricLabel}`));
                                                                    if (found) {
                                                                        matchingHeader = found;
                                                                    }
                                                                }
                                                                setSortColumn(matchingHeader);
                                                                setSortDirection(val as 'asc' | 'desc');
                                                            }
                                                        }}
                                                    >
                                                        <option value="none">None</option>
                                                        <option value="asc">Ascending (Smallest to Largest)</option>
                                                        <option value="desc">Descending (Largest to Smallest)</option>
                                                    </select>
                                                </div>
                                            </div>
                                            <input
                                                type="text"
                                                placeholder="Custom label..."
                                                className="bg-white dark:bg-surface-800 border border-indigo-200 dark:border-indigo-700 text-[10px] py-0.5 px-1.5 rounded text-gray-700 dark:text-gray-300 w-full"
                                                value={f.customLabel || ''}
                                                onChange={(e) => updateField(f.id, { customLabel: e.target.value })}
                                            />
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Well: Filters */}
                            <div
                                onDragOver={handleDragOver}
                                onDrop={() => handleDrop('filters')}
                                className="border-2 border-dashed border-gray-200 dark:border-gray-700/80 rounded-xl p-3 bg-gray-50/50 dark:bg-surface-900/20 hover:border-primary-500 hover:bg-primary-50/10 dark:hover:border-primary-500 transition-colors"
                            >
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-xs font-bold text-gray-700 dark:text-gray-300">🔲 Filters</span>
                                    <span className="text-[10px] text-gray-400">Data slice</span>
                                </div>
                                <div className="space-y-2 min-h-[38px]">
                                    {fields.filter(f => f.area === 'filters').map(f => {
                                        const vals = distinctValuesCache[f.fieldName] || [];
                                        const isConditionMode = f.filterMode === 'condition';
                                        const cond = f.filterCondition || { operator: '=', value: '' };
                                        const isNullOp = cond.operator === 'IS NULL' || cond.operator === 'IS NOT NULL';
                                        return (
                                            <div
                                                key={f.id}
                                                draggable
                                                onDragStart={() => handleDragStartFromWell(f.id)}
                                                onDragEnd={handleDragEnd}
                                                onDragOver={(e) => {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                    setDragOverFieldId(f.id);
                                                }}
                                                onDragLeave={() => setDragOverFieldId(null)}
                                                onDrop={(e) => handleDropOnField(e, f.id, 'filters')}
                                                className={`flex flex-col gap-1.5 p-2 rounded-lg bg-orange-50 dark:bg-orange-900/10 text-orange-950 dark:text-orange-300 border text-xs cursor-grab transition-all ${
                                                    dragOverFieldId === f.id
                                                        ? 'border-orange-500 ring-2 ring-orange-500/25 scale-[0.98]'
                                                        : 'border-orange-100 dark:border-orange-900/40'
                                                }`}
                                            >
                                                <div className="flex items-center justify-between font-bold">
                                                    <span className="truncate mr-2">{f.fieldName}</span>
                                                    <div className="flex items-center gap-1.5">
                                                        <button
                                                            onClick={() => updateField(f.id, { filterMode: isConditionMode ? 'values' : 'condition' })}
                                                            className="text-[9px] px-1.5 py-0.5 rounded bg-orange-200 dark:bg-orange-800/40 hover:bg-orange-300 dark:hover:bg-orange-700/50"
                                                        >
                                                            {isConditionMode ? '☑ Values' : '⚙ Condition'}
                                                        </button>
                                                        <button onClick={() => removeField(f.id)} className="hover:text-red-500 text-sm">×</button>
                                                    </div>
                                                </div>

                                                {isConditionMode ? (
                                                    <div className="flex gap-1">
                                                        <select
                                                            className="w-1/2 bg-white dark:bg-surface-800 border border-orange-200 dark:border-orange-800 text-[10px] py-0.5 px-1 rounded"
                                                            value={cond.operator}
                                                            onChange={(e) => updateField(f.id, { filterCondition: { ...cond, operator: e.target.value } })}
                                                        >
                                                            {AVAILABLE_OPERATORS.map(op => (
                                                                <option key={op.value} value={op.value}>{op.label}</option>
                                                            ))}
                                                        </select>
                                                        {!isNullOp && (
                                                            <input
                                                                type="text"
                                                                placeholder="Value..."
                                                                className="w-1/2 bg-white dark:bg-surface-800 border border-orange-200 dark:border-orange-800 text-[10px] py-0.5 px-1 rounded text-gray-700 dark:text-gray-300"
                                                                value={cond.value}
                                                                onChange={(e) => updateField(f.id, { filterCondition: { ...cond, value: e.target.value } })}
                                                            />
                                                        )}
                                                    </div>
                                                ) : (
                                                    <div>
                                                        <div className="flex items-center justify-between mb-1">
                                                            <span className="text-[9px] text-gray-500 dark:text-gray-400">
                                                                {(f.selectedValues || []).length}/{vals.length} selected
                                                            </span>
                                                            <button
                                                                onClick={() => toggleAllFilterValues(f.id, vals)}
                                                                className="text-[9px] text-orange-600 dark:text-orange-400 hover:underline"
                                                            >
                                                                {(f.selectedValues || []).length === vals.length ? 'Deselect All' : 'Select All'}
                                                            </button>
                                                        </div>
                                                        <div className="max-h-[120px] overflow-y-auto space-y-0.5 border border-orange-200 dark:border-orange-800/40 rounded p-1 bg-white dark:bg-surface-900/30">
                                                            {vals.length === 0 && (
                                                                <div className="text-[10px] text-gray-400 py-1 text-center">Loading values...</div>
                                                            )}
                                                            {vals.map(v => (
                                                                <label key={v} className="flex items-center gap-1.5 px-1 py-0.5 hover:bg-orange-50 dark:hover:bg-orange-900/20 rounded cursor-pointer">
                                                                    <input
                                                                        type="checkbox"
                                                                        checked={(f.selectedValues || []).includes(v)}
                                                                        onChange={() => toggleFilterValue(f.id, v)}
                                                                        className="w-3 h-3 rounded"
                                                                    />
                                                                    <span className="text-[10px] truncate">{v}</span>
                                                                </label>
                                                            ))}
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>

                        {/* Limit & Execute */}
                        <div className="mt-auto pt-4 border-t border-gray-100 dark:border-gray-800 flex flex-col gap-3 flex-shrink-0">
                            <div className="flex flex-col gap-1">
                                <div className="flex items-center justify-between">
                                    <label className="flex items-center gap-1.5 cursor-pointer select-none">
                                        <input
                                            type="checkbox"
                                            checked={rowLimit !== null}
                                            onChange={(e) => {
                                                if (e.target.checked) {
                                                    setRowLimit(1000);
                                                } else {
                                                    setRowLimit(null);
                                                }
                                            }}
                                            className="w-3.5 h-3.5 rounded text-primary-600 focus:ring-primary-500 border-gray-300 dark:border-gray-700"
                                        />
                                        <span className="text-xs font-semibold text-gray-600 dark:text-gray-400">Limit (Max Grouped Combinations):</span>
                                    </label>
                                    {rowLimit !== null && (
                                        <input
                                            type="number"
                                            className="w-20 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-surface-900 px-2 py-1 text-xs font-semibold text-gray-700 dark:text-gray-300"
                                            value={rowLimit}
                                            onChange={(e) => setRowLimit(Math.max(1, Number(e.target.value)))}
                                            max={10000}
                                            min={1}
                                        />
                                    )}
                                </div>
                                <span className="text-[10px] text-gray-400 dark:text-gray-500 leading-tight">
                                    Limits unique aggregated combinations fetched from the database. A lower limit may lead to incomplete totals.
                                </span>
                            </div>

                            <button
                                onClick={handleExecute}
                                disabled={executing || fields.length === 0}
                                className={`w-full inline-flex items-center justify-center gap-1.5 px-4 py-2 disabled:opacity-50 text-white rounded-xl text-sm font-semibold transition-all shadow-sm ${
                                    isDirty 
                                        ? 'bg-amber-600 hover:bg-amber-700 shadow-amber-500/20 ring-2 ring-amber-500/35 ring-offset-2 dark:ring-offset-surface-850' 
                                        : 'bg-primary-600 hover:bg-primary-700 shadow-primary-500/10'
                                }`}
                            >
                                {executing ? (
                                    <>
                                        <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                        Executing...
                                    </>
                                ) : (
                                    <>
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                                        </svg>
                                        Run Query
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Right Pivot Table Results Canvas */}
                <div className="flex-1 bg-white dark:bg-surface-800 border border-gray-100 dark:border-gray-800 shadow-sm rounded-2xl p-4 flex flex-col min-h-0 overflow-hidden">
                    {error && (
                        <div className="bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/60 rounded-xl p-3 text-sm text-red-600 dark:text-red-400 mb-4 flex-shrink-0 flex items-start gap-2">
                            <svg className="w-5 h-5 text-red-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <span className="font-medium">{error}</span>
                        </div>
                    )}

                    {/* Results Toolbar */}
                    {pivotModel && (
                        <div className="flex items-center justify-between pb-3 border-b border-gray-100 dark:border-gray-800 flex-shrink-0">
                            <div className="flex items-center gap-3 text-xs text-gray-500 flex-wrap">
                                <span className="flex items-center gap-1 font-semibold px-2 py-0.5 rounded-full bg-gray-100 dark:bg-surface-700 text-gray-700 dark:text-gray-300">
                                    ⏱️ {queryResult?.query_time_ms} ms
                                </span>
                                <span className="flex items-center gap-1 font-semibold px-2 py-0.5 rounded-full bg-gray-100 dark:bg-surface-700 text-gray-700 dark:text-gray-300">
                                    📊 {queryResult?.row_count} raw rows
                                </span>
                                {isQueryFromCache && (
                                    <span className="flex items-center gap-1 font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-950/20 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-900/40">
                                        Server Cache Hit ⚡
                                    </span>
                                )}
                                {isClientRePivoted && (
                                    <span className="flex items-center gap-1 font-semibold px-2.5 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950/20 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-900/40">
                                        Client Re-pivoted ⚡
                                    </span>
                                )}
                                {isDirty && (
                                    <span className="flex items-center gap-1 font-semibold px-2.5 py-0.5 rounded-full bg-amber-50 dark:bg-amber-950/20 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-900/40 animate-pulse">
                                        ⚠️ Configuration changed — click "Run Query" to refresh
                                    </span>
                                )}
                            </div>

                            <div className="flex items-center gap-2">
                                {user?.is_super_admin && queryResult?.debug && (
                                    <button
                                        onClick={() => setShowQueryInspector(prev => !prev)}
                                        className={`px-3 py-1.5 rounded-lg border text-xs font-semibold transition-colors flex items-center gap-1.5 ${
                                            showQueryInspector
                                                ? 'bg-primary-50 border-primary-300 text-primary-700 dark:bg-primary-950/40 dark:border-primary-800 dark:text-primary-300'
                                                : 'border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-gray-700 dark:text-gray-300'
                                        }`}
                                    >
                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                        </svg>
                                        SQL Inspector
                                    </button>
                                )}
                                {/* Tools Dropdown */}
                                <div className="relative" ref={toolsMenuRef}>
                                    <button
                                        onClick={() => setShowToolsMenu(prev => !prev)}
                                        className={`px-3 py-1.5 rounded-lg border text-xs font-semibold transition-colors flex items-center gap-1.5 ${
                                            conditionalFormatRule
                                                ? 'bg-violet-50 border-violet-300 text-violet-700 dark:bg-violet-950/40 dark:border-violet-800 dark:text-violet-300'
                                                : 'border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-gray-700 dark:text-gray-300'
                                        }`}
                                        id="pivot-tools-menu"
                                    >
                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                        </svg>
                                        Tools
                                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </button>
                                    {showToolsMenu && (
                                        <div className="absolute right-0 mt-1 w-52 bg-white dark:bg-surface-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-xl z-50 py-1 overflow-hidden">
                                            <button
                                                onClick={() => {
                                                    setShowToolsMenu(false);
                                                    setShowZScorePanel(true);
                                                }}
                                                className="w-full flex items-center gap-2.5 px-3.5 py-2.5 text-xs text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-surface-700 transition-colors text-left"
                                            >
                                                <div className="w-6 h-6 rounded-md bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center flex-shrink-0">
                                                    <svg className="w-3.5 h-3.5 text-violet-600 dark:text-violet-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                                    </svg>
                                                </div>
                                                <div>
                                                    <div className="font-semibold">Z-Score Analysis</div>
                                                    <div className="text-[10px] text-gray-400 dark:text-gray-500 mt-0.5">Anomaly detection & formatting</div>
                                                </div>
                                                {conditionalFormatRule && (
                                                    <span className="ml-auto w-2 h-2 rounded-full bg-violet-500 flex-shrink-0" />
                                                )}
                                            </button>
                                        </div>
                                    )}
                                </div>

                                <button
                                    onClick={() => setIsSidebarCollapsed(prev => !prev)}
                                    className={`px-3 py-1.5 rounded-lg border text-xs font-semibold transition-colors flex items-center gap-1.5 ${
                                        isSidebarCollapsed
                                            ? 'bg-primary-50 border-primary-300 text-primary-700 dark:bg-primary-950/40 dark:border-primary-800 dark:text-primary-300 hover:bg-primary-100/70'
                                            : 'border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-gray-700 dark:text-gray-300'
                                    }`}
                                    title={isSidebarCollapsed ? "Show panels" : "Hide panels"}
                                    id="pivot-toggle-fullscreen"
                                >
                                    {isSidebarCollapsed ? (
                                        <>
                                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 9h6m-6 3h6m-6 3h6M4 5a1 1 0 011-1h14a1 1 0 011 1v14a1 1 0 01-1 1H5a1 1 0 01-1-1V5z" />
                                            </svg>
                                        </>
                                    ) : (
                                        <>
                                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5v-4m0 4h-4m4 0l-5-5" />
                                            </svg>
                                        </>
                                    )}
                                </button>
                                <button
                                    onClick={handleToggleExpandAll}
                                    className="px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-xs font-semibold text-gray-700 dark:text-gray-300 transition-colors"
                                >
                                    Expand/Collapse All
                                </button>

                                {/* Sort Dropdown */}
                                <div className="relative" ref={sortMenuRef}>
                                    <button
                                        onClick={() => setShowSortMenu(prev => !prev)}
                                        className="px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-xs font-semibold text-gray-700 dark:text-gray-300 transition-colors flex items-center gap-1.5"
                                        id="pivot-sort-menu"
                                    >
                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0l4 4m-4-4v12" />
                                        </svg>
                                        Sort
                                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </button>
                                    {showSortMenu && (
                                        <div className="absolute right-0 mt-1 w-52 bg-white dark:bg-surface-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-xl z-50 py-1 overflow-hidden">
                                            {fields.filter(f => f.area === 'rows').map(f => (
                                                <div key={f.id} className="px-3 py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
                                                    <div className="font-semibold text-[10px] text-gray-500 dark:text-gray-400 mb-1">{f.fieldName}</div>
                                                    <div className="flex gap-1">
                                                        <button
                                                            onClick={() => {
                                                                updateField(f.id, { sortOrder: 'asc' });
                                                                if (sortColumn === f.fieldName) {
                                                                    setSortDirection('asc');
                                                                 }
                                                            }}
                                                            className={`flex-1 px-2 py-1 text-[10px] font-medium rounded border text-center transition-colors ${
                                                                f.sortOrder === 'asc'
                                                                    ? 'bg-primary-50 border-primary-300 text-primary-700 dark:bg-primary-950/40 dark:border-primary-800 dark:text-primary-300'
                                                                    : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-surface-700'
                                                            }`}
                                                        >
                                                            A → Z
                                                        </button>
                                                        <button
                                                            onClick={() => {
                                                                updateField(f.id, { sortOrder: 'desc' });
                                                                if (sortColumn === f.fieldName) {
                                                                    setSortDirection('desc');
                                                                }
                                                            }}
                                                            className={`flex-1 px-2 py-1 text-[10px] font-medium rounded border text-center transition-colors ${
                                                                f.sortOrder === 'desc'
                                                                    ? 'bg-primary-50 border-primary-300 text-primary-700 dark:bg-primary-950/40 dark:border-primary-800 dark:text-primary-300'
                                                                    : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-surface-700'
                                                            }`}
                                                        >
                                                            Z → A
                                                        </button>
                                                    </div>
                                                </div>
                                            ))}
                                            {/* Value fields sort options */}
                                            {fields.filter(f => f.area === 'values').length > 0 && (
                                                <>
                                                    {fields.filter(f => f.area === 'rows').length > 0 && (
                                                        <div className="px-3 py-1.5 border-b border-gray-100 dark:border-gray-700">
                                                            <div className="font-bold text-[9px] uppercase tracking-wider text-gray-400 dark:text-gray-500">Values</div>
                                                        </div>
                                                    )}
                                                    {fields.filter(f => f.area === 'values').map(f => {
                                                        const metricLabel = getValueLabel(f);
                                                        const isActive = sortColumn === metricLabel;
                                                        return (
                                                            <div key={f.id} className="px-3 py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
                                                                <div className="font-semibold text-[10px] text-gray-500 dark:text-gray-400 mb-1">{metricLabel}</div>
                                                                <div className="flex gap-1">
                                                                    <button
                                                                        onClick={() => {
                                                                            setSortColumn(metricLabel);
                                                                            setSortDirection('asc');
                                                                        }}
                                                                        className={`flex-1 px-2 py-1 text-[10px] font-medium rounded border text-center transition-colors ${
                                                                            isActive && sortDirection === 'asc'
                                                                                ? 'bg-primary-50 border-primary-300 text-primary-700 dark:bg-primary-950/40 dark:border-primary-800 dark:text-primary-300'
                                                                                : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-surface-700'
                                                                        }`}
                                                                    >
                                                                        1 → 9
                                                                    </button>
                                                                    <button
                                                                        onClick={() => {
                                                                            setSortColumn(metricLabel);
                                                                            setSortDirection('desc');
                                                                        }}
                                                                        className={`flex-1 px-2 py-1 text-[10px] font-medium rounded border text-center transition-colors ${
                                                                            isActive && sortDirection === 'desc'
                                                                                ? 'bg-primary-50 border-primary-300 text-primary-700 dark:bg-primary-950/40 dark:border-primary-800 dark:text-primary-300'
                                                                                : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-surface-700'
                                                                        }`}
                                                                    >
                                                                        9 → 1
                                                                    </button>
                                                                </div>
                                                            </div>
                                                        );
                                                    })}
                                                </>
                                            )}
                                            {sortColumn && (
                                                <button
                                                    onClick={() => {
                                                        setSortColumn(null);
                                                        setSortDirection('asc');
                                                        setShowSortMenu(false);
                                                    }}
                                                    className="w-full text-left px-3 py-2 text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/20 font-medium transition-colors flex items-center gap-1.5 border-t border-gray-100 dark:border-gray-700"
                                                >
                                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                    </svg>
                                                    Reset Interactive Sort
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </div>

                                {/* Export Dropdown */}
                                <div className="relative" ref={exportMenuRef}>
                                    <button
                                        onClick={() => setShowExportMenu(prev => !prev)}
                                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold transition-colors"
                                        id="pivot-export-menu"
                                    >
                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                        </svg>
                                        Export
                                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </button>
                                    {showExportMenu && (
                                        <div className="absolute right-0 mt-1 w-44 bg-white dark:bg-surface-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-xl z-50 py-1 overflow-hidden">
                                            <button
                                                onClick={() => { handleExport('csv'); setShowExportMenu(false); }}
                                                className="w-full text-left px-3.5 py-2 text-xs text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-surface-700 transition-colors flex items-center gap-2"
                                            >
                                                <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                                                CSV (.csv)
                                            </button>
                                            <button
                                                onClick={() => { handleExport('xlsx'); setShowExportMenu(false); }}
                                                className="w-full text-left px-3.5 py-2 text-xs text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-surface-700 transition-colors flex items-center gap-2"
                                            >
                                                <span className="w-2 h-2 rounded-full bg-blue-500"></span>
                                                Excel (.xlsx)
                                            </button>
                                            <button
                                                onClick={() => { handleExport('xls'); setShowExportMenu(false); }}
                                                className="w-full text-left px-3.5 py-2 text-xs text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-surface-700 transition-colors flex items-center gap-2"
                                            >
                                                <span className="w-2 h-2 rounded-full bg-teal-500"></span>
                                                Excel (.xls)
                                            </button>
                                            <button
                                                onClick={() => { handleExport('pdf'); setShowExportMenu(false); }}
                                                className="w-full text-left px-3.5 py-2 text-xs text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-surface-700 transition-colors flex items-center gap-2"
                                            >
                                                <span className="w-2 h-2 rounded-full bg-red-500"></span>
                                                PDF (.pdf)
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}

                    {/* SQL Query Inspector Panel */}
                    {showQueryInspector && queryResult?.debug && (
                        <div className="mt-3 p-4 rounded-xl border border-primary-200 dark:border-primary-800 bg-primary-50/30 dark:bg-primary-950/10 flex-shrink-0">
                            <div className="flex items-center justify-between mb-2">
                                <div className="flex items-center gap-2">
                                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-primary-100 dark:bg-primary-900/50 text-primary-700 dark:text-primary-300 uppercase">
                                        {queryResult.debug?.query_engine} Engine
                                    </span>
                                    <h4 className="text-xs font-bold text-gray-900 dark:text-gray-100">SQL Query</h4>
                                </div>
                                <div className="flex items-center gap-2">
                                    <button
                                        onClick={() => {
                                            if (queryResult?.debug?.generated_query) {
                                                navigator.clipboard.writeText(queryResult.debug.generated_query);
                                            }
                                        }}
                                        className="inline-flex items-center gap-1 px-2 py-1 rounded border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-[10px] font-semibold text-gray-600 dark:text-gray-400 transition-colors"
                                    >
                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                        </svg>
                                        Copy SQL
                                    </button>
                                    <button
                                        onClick={() => setShowQueryInspector(false)}
                                        className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                        </svg>
                                    </button>
                                </div>
                            </div>
                            <pre className="text-[11px] font-mono bg-gray-50 dark:bg-surface-900 border border-gray-200 dark:border-gray-800 rounded-lg p-3 overflow-x-auto text-gray-700 dark:text-gray-300 max-h-48 whitespace-pre-wrap">
                                {queryResult.debug?.generated_query}
                            </pre>
                        </div>
                    ) /* Note: closing of Results Toolbar wrapper */}
                    {/* Z-Score Legend Bar */}
                    {conditionalFormatRule && zScoreResult && (
                        <div className="flex items-center gap-3 mt-3 px-3 py-2 rounded-lg border border-violet-200 dark:border-violet-800/50 bg-violet-50/50 dark:bg-violet-950/20 flex-shrink-0">
                            <div className="flex items-center gap-1.5">
                                <div
                                    className="h-3 w-20 rounded-sm border border-gray-300 dark:border-gray-600"
                                    style={{
                                        background: `linear-gradient(to right, ${conditionalFormatRule.lowColor}, transparent 40%, transparent 60%, ${conditionalFormatRule.highColor})`,
                                    }}
                                />
                                <span className="text-[10px] text-gray-500 dark:text-gray-400">Low → High</span>
                            </div>
                            <div className="text-xs text-gray-600 dark:text-gray-300 font-medium">
                                Z-Score on: <span className="font-bold text-violet-700 dark:text-violet-400">{conditionalFormatRule.targetHeader}</span>
                                {' · '}
                                {conditionalFormatRule.gradient
                                    ? 'Heatmap gradient'
                                    : conditionalFormatRule.mode === 'percentile'
                                        ? `Top/Bottom ${Math.round(conditionalFormatRule.threshold * 100)}%`
                                        : `±${conditionalFormatRule.threshold}σ cutoff`
                                }
                            </div>
                            <div className="text-[10px] text-gray-500 dark:text-gray-400">
                                μ={zScoreResult.mean.toLocaleString(undefined, { maximumFractionDigits: 2 })} · σ={zScoreResult.stdDev.toLocaleString(undefined, { maximumFractionDigits: 2 })}
                            </div>
                            <button
                                onClick={() => setConditionalFormatRule(null)}
                                className="ml-auto text-xs text-gray-500 dark:text-gray-400 hover:text-red-500 dark:hover:text-red-400 transition-colors font-semibold flex items-center gap-1"
                            >
                                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                                Clear
                            </button>
                        </div>
                    )}

                    {/* Table Container */}
                    <div className="relative flex-1 overflow-auto mt-4 min-h-0 border border-gray-100 dark:border-gray-800 rounded-xl bg-gray-50 dark:bg-surface-900/30">
                        {executing && (
                            <div className="flex flex-col items-center justify-center h-full space-y-3 py-20">
                                <div className="w-10 h-10 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
                                <span className="text-sm font-medium text-gray-500 dark:text-gray-400">Executing database query...</span>
                            </div>
                        )}

                        {!executing && !pivotModel && (
                            <div className="flex flex-col items-center justify-center h-full text-center p-8 py-20 space-y-4">
                                <div className="w-16 h-16 rounded-full bg-primary-50 dark:bg-primary-900/10 flex items-center justify-center text-primary-500 dark:text-primary-400">
                                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                    </svg>
                                </div>
                                <div>
                                    <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Setup your Pivot Table</h3>
                                    <p className="text-xs text-gray-500 dark:text-gray-400 max-w-sm mt-1 mx-auto">
                                        Select an adapter and table at the top, drag columns into Rows/Columns/Values/Filterswells on the left, and click Run Query.
                                    </p>
                                </div>
                            </div>
                        )}

                        {!executing && pivotModel && pivotModel.rows.length === 0 && (
                            <div className="text-center py-20 text-sm text-gray-500 dark:text-gray-400 font-semibold">
                                No records matched the query criteria.
                            </div>
                        )}

                        {!executing && pivotModel && pivotModel.rows.length > 0 && (
                            <table className="min-w-full border-collapse text-left text-xs bg-white dark:bg-surface-800">
                                <thead className="sticky top-0 bg-gray-100 dark:bg-surface-900 z-10 border-b border-gray-200 dark:border-gray-800">
                                    <tr>
                                        {fields.filter(f => f.area === 'rows').length === 0 ? (
                                            <th
                                                onClick={() => handleSort(pivotModel.headers[0])}
                                                className="px-4 py-3 font-semibold text-gray-700 dark:text-gray-300 border-r border-gray-200 dark:border-gray-800 cursor-pointer hover:bg-gray-200 dark:hover:bg-surface-800 select-none"
                                            >
                                                <div className="flex items-center gap-1">
                                                    <span>Label</span>
                                                    {sortColumn === pivotModel.headers[0] && (
                                                        <span>{sortDirection === 'asc' ? '▲' : '▼'}</span>
                                                    )}
                                                </div>
                                            </th>
                                        ) : (
                                            fields.filter(f => f.area === 'rows').map((rf) => (
                                                <th
                                                    key={rf.id}
                                                    className="px-4 py-3 font-semibold text-gray-700 dark:text-gray-300 border-r border-gray-200 dark:border-gray-800 select-none bg-gray-50/50 dark:bg-surface-900/10"
                                                >
                                                    {rf.fieldName}
                                                </th>
                                            ))
                                        )}
                                        {pivotModel.headers.map((header) => (
                                            <th
                                                key={header}
                                                onClick={() => handleSort(header)}
                                                className="px-4 py-3 font-semibold text-gray-700 dark:text-gray-300 border-r border-gray-200 dark:border-gray-800 cursor-pointer hover:bg-gray-200 dark:hover:bg-surface-800 select-none text-right"
                                            >
                                                <div className="flex items-center justify-end gap-1">
                                                    <span>{header}</span>
                                                    {sortColumn === header && (
                                                        <span>{sortDirection === 'asc' ? '▲' : '▼'}</span>
                                                    )}
                                                </div>
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100 dark:divide-gray-800/80">
                                    {sortedPivotRows.map((row) => {
                                        if (!row.visible) return null;
                                        const isSubtotal = row.type === 'subtotal';
                                        return (
                                            <tr
                                                key={row.groupKey}
                                                className={`${
                                                    isSubtotal
                                                        ? 'bg-gray-50/70 dark:bg-surface-900/10 font-semibold'
                                                        : 'hover:bg-gray-50/50 dark:hover:bg-surface-900/5'
                                                } transition-colors`}
                                            >
                                                {/* Label Field */}
                                                {(() => {
                                                    const rowFields = fields.filter(f => f.area === 'rows');
                                                    if (rowFields.length === 0) {
                                                        return (
                                                            <td
                                                                className="px-4 py-2.5 border-r border-gray-100 dark:border-gray-800 text-gray-900 dark:text-gray-200 font-medium"
                                                            >
                                                                <span>{row.label}</span>
                                                            </td>
                                                        );
                                                    }
                                                    
                                                    const parts = row.groupKey.split('||');
                                                    return rowFields.map((rf, colIdx) => {
                                                        const cellValue = parts[colIdx] || '';
                                                        if (colIdx < row.depth) {
                                                            // Ancestor level: show label in plain text only if repeatLabels is enabled
                                                            return (
                                                                <td
                                                                    key={rf.id}
                                                                    className="px-4 py-2.5 border-r border-gray-100 dark:border-gray-800 text-gray-500 dark:text-gray-400 font-normal"
                                                                >
                                                                    {rf.repeatLabels ? cellValue : ''}
                                                                </td>
                                                            );
                                                        } else if (colIdx === row.depth) {
                                                            // Current level: show label with toggle if subtotal
                                                            return (
                                                                <td
                                                                    key={rf.id}
                                                                    className="px-4 py-2.5 border-r border-gray-100 dark:border-gray-800 text-gray-900 dark:text-gray-200 font-medium"
                                                                >
                                                                    <div className="flex items-center gap-1.5">
                                                                        {isSubtotal && (
                                                                            <button
                                                                                onClick={() => toggleGroup(row.groupKey)}
                                                                                className="w-4 h-4 flex items-center justify-center hover:bg-gray-200 dark:hover:bg-surface-700 rounded transition-colors text-gray-500 flex-shrink-0"
                                                                            >
                                                                                {row.expanded ? '▼' : '►'}
                                                                            </button>
                                                                        )}
                                                                        <span>{cellValue}</span>
                                                                    </div>
                                                                </td>
                                                            );
                                                        } else {
                                                            // Descendant level for subtotal: show empty space (or "Total" style)
                                                            return (
                                                                <td
                                                                    key={rf.id}
                                                                    className="px-4 py-2.5 border-r border-gray-100 dark:border-gray-800 text-gray-400 dark:text-gray-500/60 italic font-light"
                                                                >
                                                                    Total
                                                                </td>
                                                            );
                                                        }
                                                    });
                                                })()}

                                                {/* Value Cells */}
                                                {pivotModel.headers.map((header) => {
                                                    const cellVal = row.values[header];
                                                    const showAs = pivotModel.headerShowAs[header];
                                                    const cellBg = conditionalFormatRule && zScoreResult
                                                        ? getCellColor(conditionalFormatRule, zScoreResult, row.groupKey, header)
                                                        : null;
                                                    return (
                                                        <td
                                                            key={header}
                                                            className="px-4 py-2.5 text-right border-r border-gray-100 dark:border-gray-800 text-gray-700 dark:text-gray-300 font-mono transition-colors"
                                                            style={cellBg ? { backgroundColor: cellBg } : undefined}
                                                        >
                                                            {formatPivotValue(cellVal, showAs)}
                                                        </td>
                                                    );
                                                })}
                                            </tr>
                                        );
                                    })}

                                    {/* Grand Total Row */}
                                    {Object.keys(pivotModel.grandTotal).length > 0 && (
                                        <tr className="bg-gray-100 dark:bg-surface-900 font-bold border-t-2 border-gray-200 dark:border-gray-800 sticky bottom-0">
                                            <td 
                                                colSpan={fields.filter(f => f.area === 'rows').length || 1}
                                                className="px-4 py-3 border-r border-gray-100 dark:border-gray-800 text-primary-700 dark:text-primary-400"
                                            >
                                                Grand Total
                                            </td>
                                            {pivotModel.headers.map((header) => {
                                                const showAs = pivotModel.headerShowAs[header];
                                                return (
                                                    <td
                                                        key={header}
                                                        className="px-4 py-3 text-right border-r border-gray-100 dark:border-gray-800 text-primary-800 dark:text-primary-300 font-mono"
                                                    >
                                                        {formatPivotValue(pivotModel.grandTotal[header], showAs)}
                                                    </td>
                                                );
                                            })}
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        )}

                        {/* Z-Score Panel Overlay */}
                        {showZScorePanel && pivotModel && (
                            <ZScorePanel
                                pivotModel={pivotModel}
                                currentRule={conditionalFormatRule}
                                onApply={(rule) => {
                                    setConditionalFormatRule(rule);
                                    setShowZScorePanel(false);
                                }}
                                onClear={() => {
                                    setConditionalFormatRule(null);
                                    setShowZScorePanel(false);
                                }}
                                onClose={() => setShowZScorePanel(false)}
                            />
                        )}

                        {/* Join Builder Modal */}
                        <Modal isOpen={showJoinBuilder} onClose={() => setShowJoinBuilder(false)} maxWidth="max-w-3xl">
                            {/* Header */}
                            <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-100 dark:border-gray-800">
                                <div className="flex items-center gap-2">
                                    <div className="w-8 h-8 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0">
                                        <svg className="w-4 h-4 text-primary-600 dark:text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 00-5.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                                        </svg>
                                    </div>
                                    <div>
                                        <h2 className="text-base font-bold text-gray-900 dark:text-white">
                                            Query Joins Manager
                                        </h2>
                                        <p className="text-[11px] text-gray-500 dark:text-gray-400">
                                            Define relationships between tables to access fields across multiple tables.
                                        </p>
                                    </div>
                                </div>
                                <button
                                    onClick={() => setShowJoinBuilder(false)}
                                    className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-surface-700 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                                >
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            {/* List of Joins */}
                            <div className="space-y-4 max-h-[420px] overflow-y-auto pr-1">
                                {tempJoins.length === 0 ? (
                                    <div className="text-center py-10 border-2 border-dashed border-gray-200 dark:border-gray-700/60 rounded-xl bg-gray-50/50 dark:bg-surface-900/10">
                                        <svg className="mx-auto w-10 h-10 text-gray-300 dark:text-gray-600 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 00-5.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                                        </svg>
                                        <p className="text-xs font-semibold text-gray-600 dark:text-gray-400">No joins configured yet</p>
                                        <p className="text-[10px] text-gray-400 dark:text-gray-500 mt-1 max-w-sm mx-auto">
                                            Add a join to combine fields from another table in the database with the primary table <span className="font-semibold">{selectedTable}</span>.
                                        </p>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setTempJoins([...tempJoins, {
                                                    table: '',
                                                    type: 'INNER',
                                                    on: { left_table: selectedTable, left_column: '', right_column: '' }
                                                }]);
                                            }}
                                            className="mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary-600 hover:bg-primary-700 text-white text-xs font-semibold shadow-sm transition-colors"
                                        >
                                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
                                            </svg>
                                            Add Your First Join
                                        </button>
                                    </div>
                                ) : (
                                    tempJoins.map((join, idx) => {
                                        const rightTables = getRightTablesForJoin(idx);
                                        const leftTables = getLeftTablesForJoin(idx);
                                        const leftColumns = join.on.left_table === selectedTable
                                            ? columns
                                            : (joinedTablesColumns[join.on.left_table || ''] || []);
                                        const rightColumns = joinedTablesColumns[join.table] || [];

                                        return (
                                            <div key={idx} className="p-4 rounded-xl border border-gray-200 dark:border-gray-700/80 bg-gray-50/30 dark:bg-surface-900/10 flex flex-col gap-3 relative hover:border-gray-300 dark:hover:border-gray-600 transition-colors">
                                                {/* Remove button */}
                                                <button
                                                    type="button"
                                                    onClick={() => removeJoin(idx)}
                                                    className="absolute top-3 right-3 text-gray-400 hover:text-red-500 dark:hover:text-red-400 transition-colors p-1 rounded-md hover:bg-gray-100 dark:hover:bg-surface-700"
                                                >
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                    </svg>
                                                </button>

                                                {/* Title / Index badge */}
                                                <div className="flex items-center gap-2">
                                                    <span className="w-5 h-5 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-300 flex items-center justify-center text-[10px] font-bold">
                                                        {idx + 1}
                                                    </span>
                                                    <span className="text-xs font-bold text-gray-700 dark:text-gray-300">Join Definition</span>
                                                </div>

                                                {/* Form Layout */}
                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-1">
                                                    {/* Right Table selection */}
                                                    <div>
                                                        <label className="block text-[10px] font-semibold text-gray-500 dark:text-gray-400 uppercase mb-1">
                                                            Table to Join (Right)
                                                        </label>
                                                        <select
                                                            value={join.table}
                                                            onChange={(e) => handleRightTableChange(idx, e.target.value)}
                                                            className="w-full px-2.5 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-surface-800 text-xs font-semibold text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-1 focus:ring-primary-500"
                                                        >
                                                            <option value="">Select table...</option>
                                                            {join.table && !rightTables.includes(join.table) && (
                                                                <option value={join.table}>{join.table}</option>
                                                            )}
                                                            {rightTables.map(t => (
                                                                <option key={t} value={t}>{t}</option>
                                                            ))}
                                                        </select>
                                                    </div>

                                                    {/* Join Type */}
                                                    <div>
                                                        <label className="block text-[10px] font-semibold text-gray-500 dark:text-gray-400 uppercase mb-1">
                                                            Join Type
                                                        </label>
                                                        <select
                                                            value={join.type}
                                                            onChange={(e) => {
                                                                const val = e.target.value as JoinConfig['type'];
                                                                setTempJoins(prev => prev.map((j, i) => i === idx ? { ...j, type: val } : j));
                                                            }}
                                                            className="w-full px-2.5 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-surface-800 text-xs font-semibold text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-1 focus:ring-primary-500"
                                                        >
                                                            <option value="INNER">INNER JOIN</option>
                                                            <option value="LEFT">LEFT JOIN</option>
                                                            <option value="RIGHT">RIGHT JOIN</option>
                                                        </select>
                                                    </div>
                                                </div>

                                                {/* Condition (ON Clause) */}
                                                {join.table && (
                                                    <div className="bg-white dark:bg-surface-800 border border-gray-100 dark:border-gray-700/60 rounded-lg p-3 mt-1 flex flex-col gap-2">
                                                        <span className="text-[10px] font-semibold text-gray-500 dark:text-gray-400 uppercase">
                                                            Relationship Condition (ON)
                                                        </span>
                                                        <div className="flex flex-col md:flex-row items-center gap-2">
                                                            {/* Left Table select */}
                                                            <div className="flex-1 w-full">
                                                                <div className="text-[9px] text-gray-400 mb-0.5">Left Table</div>
                                                                <select
                                                                    value={join.on.left_table || selectedTable}
                                                                    onChange={(e) => {
                                                                        const val = e.target.value;
                                                                        setTempJoins(prev => prev.map((j, i) => {
                                                                            if (i !== idx) return j;
                                                                            return {
                                                                                ...j,
                                                                                on: {
                                                                                    ...j.on,
                                                                                    left_table: val,
                                                                                    left_column: ''
                                                                                }
                                                                            };
                                                                        }));
                                                                    }}
                                                                    className="w-full px-2 py-1 rounded border border-gray-200 dark:border-gray-700 bg-gray-50/50 dark:bg-surface-900 text-xs text-gray-700 dark:text-gray-300"
                                                                >
                                                                    {leftTables.map(t => (
                                                                        <option key={t} value={t}>{t}</option>
                                                                    ))}
                                                                </select>
                                                            </div>

                                                            {/* Left Column select */}
                                                            <div className="flex-1 w-full">
                                                                <div className="text-[9px] text-gray-400 mb-0.5">Left Column</div>
                                                                <select
                                                                    value={join.on.left_column}
                                                                    onChange={(e) => {
                                                                        const val = e.target.value;
                                                                        setTempJoins(prev => prev.map((j, i) => {
                                                                            if (i !== idx) return j;
                                                                            return { ...j, on: { ...j.on, left_column: val } };
                                                                        }));
                                                                    }}
                                                                    className="w-full px-2 py-1 rounded border border-gray-200 dark:border-gray-700 bg-white dark:bg-surface-800 text-xs text-gray-700 dark:text-gray-300"
                                                                >
                                                                    <option value="">Select column...</option>
                                                                    {leftColumns.map(c => (
                                                                        <option key={c.name} value={c.name}>{c.name}</option>
                                                                    ))}
                                                                </select>
                                                            </div>

                                                            <span className="text-xs font-bold text-gray-400 px-1 mt-3">=</span>

                                                            {/* Right Column select */}
                                                            <div className="flex-1 w-full">
                                                                <div className="text-[9px] text-gray-400 mb-0.5">Right Column ({join.table})</div>
                                                                <select
                                                                    value={join.on.right_column}
                                                                    onChange={(e) => {
                                                                        const val = e.target.value;
                                                                        setTempJoins(prev => prev.map((j, i) => {
                                                                            if (i !== idx) return j;
                                                                            return { ...j, on: { ...j.on, right_column: val } };
                                                                        }));
                                                                    }}
                                                                    className="w-full px-2 py-1 rounded border border-gray-200 dark:border-gray-700 bg-white dark:bg-surface-800 text-xs text-gray-700 dark:text-gray-300"
                                                                >
                                                                    <option value="">Select column...</option>
                                                                    {rightColumns.map(c => (
                                                                        <option key={c.name} value={c.name}>{c.name}</option>
                                                                    ))}
                                                                </select>
                                                            </div>
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        );
                                    })
                                )}
                            </div>

                            {/* Action buttons */}
                            <div className="flex items-center justify-between mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
                                {tempJoins.length > 0 && (
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setTempJoins([...tempJoins, {
                                                table: '',
                                                type: 'INNER',
                                                on: { left_table: selectedTable, left_column: '', right_column: '' }
                                            }]);
                                        }}
                                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-xs font-semibold text-gray-600 dark:text-gray-300 transition-colors"
                                    >
                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
                                        </svg>
                                        Add Join
                                    </button>
                                )}
                                <div className="flex items-center gap-2 ml-auto">
                                    <button
                                        type="button"
                                        onClick={() => setShowJoinBuilder(false)}
                                        className="px-4 py-2 rounded-xl border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-surface-700 text-xs font-semibold text-gray-600 dark:text-gray-300 transition-colors"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="button"
                                        onClick={handleSaveJoins}
                                        className="px-4 py-2 rounded-xl bg-primary-600 hover:bg-primary-700 text-white text-xs font-semibold shadow-glow-sm hover:shadow-glow transition-colors"
                                    >
                                        Apply & Save Joins
                                    </button>
                                </div>
                            </div>
                        </Modal>
                    </div>
                </div>
            </div>
        </div>
    );
}
