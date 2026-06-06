/**
 * Pivot Table Engine
 *
 * Pure TypeScript module that transforms flat query result rows into a
 * hierarchical pivot table model — matching Excel and Google Sheets fidelity.
 *
 * Supports:
 * - Multi-level row grouping with collapsible subtotals
 * - Column cross-tabulation (unique values become dynamic column headers)
 * - Multiple value aggregations (SUM, AVG, MIN, MAX, COUNT, COUNT_DISTINCT, MEDIAN, PRODUCT)
 * - "Show As" modes: Default, % of Row, % of Column, % of Grand Total
 * - Multi-instance fields (same field in multiple wells via unique `id`)
 * - Grand totals
 * - Sorting
 * - Repeat row labels option
 */

import type { PivotFieldConfig, ShowAsMode } from '../types';

// ============================================================
// Types
// ============================================================

export interface PivotModel {
    /** Column headers — may be nested if cross-tabulating */
    headers: string[];
    /** Flattened list of renderable rows (including group / subtotal / data rows) */
    rows: PivotRow[];
    /** Grand total values keyed by header name */
    grandTotal: Record<string, number>;
    /** Total number of raw data rows before pivoting */
    rawRowCount: number;
    /** Show As mode per header for formatting */
    headerShowAs: Record<string, ShowAsMode>;
}

export type PivotRowType = 'data' | 'subtotal' | 'grand_total';

export interface PivotRow {
    type: PivotRowType;
    /** Depth level (0 = top-level, 1 = first nested group, etc.) */
    depth: number;
    /** Group key for this row (used to track expand/collapse) */
    groupKey: string;
    /** Display label for the first column (dimension value or group label) */
    label: string;
    /** Whether this group is expanded (only relevant for subtotal rows) */
    expanded: boolean;
    /** Whether this row is visible (hidden if a parent group is collapsed) */
    visible: boolean;
    /** Values keyed by header column name */
    values: Record<string, unknown>;
    /** Number of children under this group */
    childCount: number;
}

// ============================================================
// Aggregation helpers
// ============================================================

type AggregateType =
    | 'SUM' | 'COUNTA' | 'COUNT' | 'COUNTUNIQUE' | 'AVERAGE' | 'MAX' | 'MIN'
    | 'MEDIAN' | 'PRODUCT' | 'STDEV' | 'STDEVP' | 'VAR' | 'VARP'
    | 'AVG' | 'COUNT_DISTINCT';

function aggregateValues(values: number[], agg: AggregateType): number {
    if (values.length === 0) return 0;
    switch (agg) {
        case 'SUM':
            return values.reduce((a, b) => a + b, 0);
        case 'AVG':
        case 'AVERAGE':
            return values.reduce((a, b) => a + b, 0) / values.length;
        case 'MIN':
            return Math.min(...values);
        case 'MAX':
            return Math.max(...values);
        case 'COUNT':
        case 'COUNTA':
        case 'COUNT_DISTINCT':
        case 'COUNTUNIQUE':
            return values.reduce((a, b) => a + b, 0);
        case 'MEDIAN': {
            const sorted = [...values].sort((a, b) => a - b);
            const mid = Math.floor(sorted.length / 2);
            return sorted.length % 2 === 0
                ? (sorted[mid - 1] + sorted[mid]) / 2
                : sorted[mid];
        }
        case 'PRODUCT':
            return values.reduce((a, b) => a * b, 1);
        case 'STDEV': {
            if (values.length <= 1) return 0;
            const mean = values.reduce((a, b) => a + b, 0) / values.length;
            const variance = values.reduce((s, x) => s + (x - mean) ** 2, 0) / (values.length - 1);
            return variance ** 0.5;
        }
        case 'STDEVP': {
            if (values.length === 0) return 0;
            const mean = values.reduce((a, b) => a + b, 0) / values.length;
            const variance = values.reduce((s, x) => s + (x - mean) ** 2, 0) / values.length;
            return variance ** 0.5;
        }
        case 'VAR': {
            if (values.length <= 1) return 0;
            const mean = values.reduce((a, b) => a + b, 0) / values.length;
            return values.reduce((s, x) => s + (x - mean) ** 2, 0) / (values.length - 1);
        }
        case 'VARP': {
            if (values.length === 0) return 0;
            const mean = values.reduce((a, b) => a + b, 0) / values.length;
            return values.reduce((s, x) => s + (x - mean) ** 2, 0) / values.length;
        }
        default:
            return values.reduce((a, b) => a + b, 0);
    }
}

function toNumber(val: unknown): number {
    if (typeof val === 'number') return val;
    if (typeof val === 'string') {
        const parsed = parseFloat(val);
        return isNaN(parsed) ? 0 : parsed;
    }
    return 0;
}

/** Generate a display label for a value field */
export function getValueLabel(f: PivotFieldConfig): string {
    if (f.customLabel) return f.customLabel;
    if (f.alias) return f.alias;
    const agg = f.aggregate || 'SUM';
    return `${agg}(${f.fieldName})`;
}

// ============================================================
// Core pivot logic
// ============================================================

export function buildPivotModel(
    data: Record<string, unknown>[],
    fields: PivotFieldConfig[],
    expandedGroups: Set<string>,
): PivotModel {
    const rowFields = fields.filter(f => f.area === 'rows');
    const colFields = fields.filter(f => f.area === 'columns');
    const valFields = fields.filter(f => f.area === 'values');

    // If no fields are configured, return a simple flat table
    if (rowFields.length === 0 && colFields.length === 0 && valFields.length === 0) {
        return buildFlatModel(data);
    }

    // Track Show As per header
    const headerShowAs: Record<string, ShowAsMode> = {};

    // Determine headers
    const headers: string[] = [];

    // If there are column fields, we need to build cross-tab headers
    let crossTabKeys: string[] = [];
    if (colFields.length > 0 && valFields.length > 0) {
        // Collect unique column dimension values
        const uniqueColVals = new Set<string>();
        for (const row of data) {
            const colKey = colFields.map(f => String(row[f.fieldName] ?? '(empty)')).join(' | ');
            uniqueColVals.add(colKey);
        }
        crossTabKeys = Array.from(uniqueColVals).sort();

        // Generate cross-tab headers: "ColVal — MetricLabel"
        for (const colKey of crossTabKeys) {
            for (const vf of valFields) {
                const metricLabel = getValueLabel(vf);
                const header = `${colKey} — ${metricLabel}`;
                headers.push(header);
                headerShowAs[header] = vf.showAs || 'default';
            }
        }
    } else if (valFields.length > 0) {
        // No column pivoting — just value headers
        for (const vf of valFields) {
            const header = getValueLabel(vf);
            headers.push(header);
            headerShowAs[header] = vf.showAs || 'default';
        }
    } else if (colFields.length === 0 && rowFields.length > 0) {
        // Row grouping only, show count
        headers.push('Count');
        headerShowAs['Count'] = 'default';
    }

    // Build the pivot rows
    const pivotRows: PivotRow[] = [];
    const grandTotalAcc: Record<string, number[]> = {};
    headers.forEach(h => { grandTotalAcc[h] = []; });

    if (rowFields.length > 0) {
        // Group data by row dimensions
        buildGroupedRows(
            data, rowFields, colFields, valFields, crossTabKeys, headers,
            0, '', expandedGroups, pivotRows, grandTotalAcc, true,
        );
    } else {
        // No row grouping — just compute values per cross-tab or flat
        const values: Record<string, unknown> = {};
        for (const header of headers) {
            if (colFields.length > 0) {
                // Cross-tab without row grouping
                const sepIdx = header.indexOf(' — ');
                if (sepIdx > -1) {
                    const colKey = header.substring(0, sepIdx);
                    const metricPart = header.substring(sepIdx + 3);
                    const vf = findValFieldByLabel(valFields, metricPart);
                    if (vf) {
                        const matchingRows = data.filter(row => {
                            const rowColKey = colFields.map(f => String(row[f.fieldName] ?? '(empty)')).join(' | ');
                            return rowColKey === colKey;
                        });
                        const nums = matchingRows.map(r => toNumber(r[vf.alias || vf.fieldName]));
                        const agg = vf.aggregate || 'SUM';
                        values[header] = aggregateValues(nums, agg);
                    }
                }
            } else {
                // Simple aggregation across all data
                const vf = findValFieldByLabel(valFields, header);
                if (vf) {
                    const nums = data.map(r => toNumber(r[vf.alias || vf.fieldName]));
                    const agg = vf.aggregate || 'SUM';
                    values[header] = aggregateValues(nums, agg);
                }
            }
        }

        pivotRows.push({
            type: 'data',
            depth: 0,
            groupKey: '__all__',
            label: 'Total',
            expanded: true,
            visible: true,
            values,
            childCount: 0,
        });
    }

    // Compute grand total
    const grandTotal: Record<string, number> = {};
    for (const [key, arr] of Object.entries(grandTotalAcc)) {
        grandTotal[key] = arr.reduce((a, b) => a + b, 0);
    }

    // ---- Apply "Show As" transformations ----
    const hasPercentage = Object.values(headerShowAs).some(m => m !== 'default');
    if (hasPercentage) {
        // Compute column totals (sum of all data/subtotal at depth 0 for each header)
        const columnTotals: Record<string, number> = { ...grandTotal };

        // Compute row totals per row
        for (const row of pivotRows) {
            let rowTotal = 0;
            for (const header of headers) {
                const val = row.values[header];
                if (typeof val === 'number') {
                    rowTotal += val;
                }
            }
            // Apply % transformations
            for (const header of headers) {
                const showAs = headerShowAs[header];
                if (showAs === 'default') continue;

                const rawVal = row.values[header];
                if (typeof rawVal !== 'number') continue;

                if (showAs === 'pct_of_grand_total') {
                    const gt = grandTotal[header] || 1;
                    row.values[header] = (rawVal / gt) * 100;
                } else if (showAs === 'pct_of_column') {
                    const ct = columnTotals[header] || 1;
                    row.values[header] = (rawVal / ct) * 100;
                } else if (showAs === 'pct_of_row') {
                    row.values[header] = rowTotal > 0 ? (rawVal / rowTotal) * 100 : 0;
                }
            }
        }

        // Also apply % to grand total row values
        for (const header of headers) {
            const showAs = headerShowAs[header];
            if (showAs === 'default') continue;
            // Grand total as % of itself = 100%
            if (showAs === 'pct_of_grand_total' || showAs === 'pct_of_column') {
                grandTotal[header] = 100;
            } else if (showAs === 'pct_of_row') {
                const rawGt = Object.values(grandTotal).reduce((s, v) => s + v, 0);
                grandTotal[header] = rawGt > 0 ? (grandTotal[header] / rawGt) * 100 : 0;
            }
        }
    }

    return {
        headers,
        rows: pivotRows,
        grandTotal,
        rawRowCount: data.length,
        headerShowAs,
    };
}

function buildGroupedRows(
    data: Record<string, unknown>[],
    rowFields: PivotFieldConfig[],
    colFields: PivotFieldConfig[],
    valFields: PivotFieldConfig[],
    crossTabKeys: string[],
    headers: string[],
    depth: number,
    parentKey: string,
    expandedGroups: Set<string>,
    result: PivotRow[],
    grandTotalAcc: Record<string, number[]>,
    parentVisible: boolean,
): void {
    const currentField = rowFields[depth];
    const isLeafLevel = depth === rowFields.length - 1;

    // Group data by current dimension
    const groups = new Map<string, Record<string, unknown>[]>();
    for (const row of data) {
        const val = String(row[currentField.fieldName] ?? '(empty)');
        if (!groups.has(val)) {
            groups.set(val, []);
        }
        groups.get(val)!.push(row);
    }

    // Sort groups
    const sortedKeys = Array.from(groups.keys()).sort((a, b) => {
        if (currentField.sortOrder === 'desc') return b.localeCompare(a, undefined, { numeric: true });
        return a.localeCompare(b, undefined, { numeric: true });
    });

    for (const groupVal of sortedKeys) {
        const groupData = groups.get(groupVal)!;
        const groupKey = parentKey ? `${parentKey}||${groupVal}` : groupVal;
        const isExpanded = expandedGroups.has(groupKey);
        const childVisible = parentVisible && isExpanded;

        // Compute subtotal values for this group
        const subtotalValues: Record<string, unknown> = {};
        computeAggregatedValues(groupData, colFields, valFields, headers, subtotalValues, depth === 0 ? grandTotalAcc : undefined);

        if (isLeafLevel) {
            // Leaf level: just add a single data row showing the aggregated values for this group
            result.push({
                type: 'data',
                depth,
                groupKey,
                label: groupVal,
                expanded: false,
                visible: parentVisible,
                values: subtotalValues,
                childCount: 0,
            });
        } else {
            // Non-leaf: add group subtotal header first, then recurse into children
            result.push({
                type: 'subtotal',
                depth,
                groupKey,
                label: groupVal,
                expanded: isExpanded,
                visible: parentVisible,
                values: subtotalValues,
                childCount: groupData.length,
            });

            buildGroupedRows(
                groupData, rowFields, colFields, valFields, crossTabKeys, headers,
                depth + 1, groupKey, expandedGroups, result, grandTotalAcc, childVisible,
            );
        }
    }
}

function computeAggregatedValues(
    groupData: Record<string, unknown>[],
    colFields: PivotFieldConfig[],
    valFields: PivotFieldConfig[],
    headers: string[],
    target: Record<string, unknown>,
    grandTotalAcc?: Record<string, number[]>,
): void {
    for (const header of headers) {
        if (colFields.length > 0) {
            const sepIdx = header.indexOf(' — ');
            if (sepIdx > -1) {
                const colKey = header.substring(0, sepIdx);
                const metricPart = header.substring(sepIdx + 3);
                const vf = findValFieldByLabel(valFields, metricPart);
                if (vf) {
                    const matchingRows = groupData.filter(row => {
                        const rowColKey = colFields.map(f => String(row[f.fieldName] ?? '(empty)')).join(' | ');
                        return rowColKey === colKey;
                    });
                    const nums = matchingRows.map(r => toNumber(r[vf.alias || vf.fieldName]));
                    const val = aggregateValues(nums, vf.aggregate || 'SUM');
                    target[header] = val;
                    grandTotalAcc?.[header]?.push(val);
                }
            }
        } else {
            const vf = findValFieldByLabel(valFields, header);
            if (vf) {
                const nums = groupData.map(r => toNumber(r[vf.alias || vf.fieldName]));
                const val = aggregateValues(nums, vf.aggregate || 'SUM');
                target[header] = val;
                grandTotalAcc?.[header]?.push(val);
            } else if (header === 'Count') {
                target[header] = groupData.length;
                grandTotalAcc?.[header]?.push(groupData.length);
            }
        }
    }
}

function findValFieldByLabel(valFields: PivotFieldConfig[], headerOrMetric: string): PivotFieldConfig | undefined {
    return valFields.find(v => getValueLabel(v) === headerOrMetric);
}

function buildFlatModel(data: Record<string, unknown>[]): PivotModel {
    if (data.length === 0) {
        return { headers: [], rows: [], grandTotal: {}, rawRowCount: 0, headerShowAs: {} };
    }

    const headers = Object.keys(data[0]);
    const rows: PivotRow[] = data.map((row, i) => ({
        type: 'data' as const,
        depth: 0,
        groupKey: `flat_${i}`,
        label: String(row[headers[0]] ?? ''),
        expanded: true,
        visible: true,
        values: { ...row },
        childCount: 0,
    }));

    return { headers, rows, grandTotal: {}, rawRowCount: data.length, headerShowAs: {} };
}

// ============================================================
// CSV Export
// ============================================================

export function pivotTo2DArray(model: PivotModel, rowFields: PivotFieldConfig[] = []): (string | number)[][] {
    const data: (string | number)[][] = [];

    // Header row
    const rowHeaders = rowFields.length > 0 ? rowFields.map(rf => rf.fieldName) : ['Label'];
    const headerRow = [...rowHeaders, ...model.headers];
    data.push(headerRow);

    // Data rows (only visible ones)
    for (const row of model.rows) {
        if (!row.visible) continue;
        
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

        const cells: (string | number)[] = [...rowLabels];
        for (const header of model.headers) {
            const val = row.values[header];
            const showAs = model.headerShowAs[header];
            if (val != null) {
                if (showAs && showAs !== 'default' && typeof val === 'number') {
                    cells.push(`${val.toFixed(1)}%`);
                } else {
                    const numVal = Number(val);
                    cells.push(isNaN(numVal) ? String(val) : numVal);
                }
            } else {
                cells.push('');
            }
        }
        data.push(cells);
    }

    // Grand total row
    if (Object.keys(model.grandTotal).length > 0) {
        const rowLabels = rowFields.length > 0 
            ? ['Grand Total', ...Array(rowFields.length - 1).fill('')]
            : ['Grand Total'];
        const cells: (string | number)[] = [...rowLabels];
        for (const header of model.headers) {
            const val = model.grandTotal[header];
            const showAs = model.headerShowAs[header];
            if (val != null) {
                if (showAs && showAs !== 'default') {
                    cells.push(`${val.toFixed(1)}%`);
                } else {
                    const numVal = Number(val);
                    cells.push(isNaN(numVal) ? String(val) : numVal);
                }
            } else {
                cells.push('');
            }
        }
        data.push(cells);
    }

    return data;
}

export function pivotToCSV(model: PivotModel, rowFields: PivotFieldConfig[] = []): string {
    const lines: string[] = [];

    // Header row
    const rowHeaders = rowFields.length > 0 ? rowFields.map(rf => rf.fieldName) : ['Label'];
    const headerRow = [...rowHeaders, ...model.headers];
    lines.push(headerRow.map(escapeCSV).join(','));

    // Data rows (only visible ones)
    for (const row of model.rows) {
        if (!row.visible) continue;
        
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
        for (const header of model.headers) {
            const val = row.values[header];
            const showAs = model.headerShowAs[header];
            if (val != null) {
                if (showAs && showAs !== 'default' && typeof val === 'number') {
                    cells.push(`${val.toFixed(1)}%`);
                } else {
                    cells.push(String(val));
                }
            } else {
                cells.push('');
            }
        }
        lines.push(cells.map(escapeCSV).join(','));
    }

    // Grand total row
    if (Object.keys(model.grandTotal).length > 0) {
        const rowLabels = rowFields.length > 0 
            ? ['Grand Total', ...Array(rowFields.length - 1).fill('')]
            : ['Grand Total'];
        const cells = [...rowLabels];
        for (const header of model.headers) {
            const val = model.grandTotal[header];
            const showAs = model.headerShowAs[header];
            if (val != null) {
                if (showAs && showAs !== 'default') {
                    cells.push(`${val.toFixed(1)}%`);
                } else {
                    cells.push(String(val));
                }
            } else {
                cells.push('');
            }
        }
        lines.push(cells.map(escapeCSV).join(','));
    }

    return lines.join('\n');
}

function escapeCSV(value: string): string {
    if (value.includes(',') || value.includes('"') || value.includes('\n')) {
        return `"${value.replace(/"/g, '""')}"`;
    }
    return value;
}

// ============================================================
// Number formatting
// ============================================================

export function formatPivotValue(val: unknown, showAs?: ShowAsMode): string {
    if (val == null) return '—';
    if (typeof val === 'number') {
        if (showAs && showAs !== 'default') {
            return `${val.toFixed(1)}%`;
        }
        if (Number.isInteger(val)) {
            return val.toLocaleString();
        }
        return val.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
    return String(val);
}
