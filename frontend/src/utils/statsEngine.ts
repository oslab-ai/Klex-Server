/**
 * Statistical Analysis Engine
 *
 * Pure TypeScript module providing statistical computations on PivotModel data.
 * Currently supports Z-Score analysis with percentile-based and cutoff-based
 * conditional formatting.
 *
 * Design:
 * - Operates on already-built PivotModel — no backend calls
 * - Computes stats over leaf data rows only (excludes subtotals)
 * - Subtotal rows still receive z-scores relative to the leaf distribution
 */

import type { PivotModel } from './pivotEngine';
import type { ConditionalFormatRule } from '../types';

// ============================================================
// Types
// ============================================================

export interface ZScoreResult {
    mean: number;
    stdDev: number;
    /** Map of groupKey → z-score for each pivot row */
    scores: Map<string, number>;
    /** Map of groupKey → raw numeric value */
    rawValues: Map<string, number>;
    /** Map of groupKey → percentile rank (0–1) */
    percentiles: Map<string, number>;
    /** Min/max z-score for gradient normalization */
    minZ: number;
    maxZ: number;
}

// ============================================================
// Core Z-Score computation
// ============================================================

/**
 * Compute Z-scores for every row in the pivot model on a given header column.
 * Statistics (mean, stdDev) are computed from leaf-level data rows only.
 * Subtotal rows still get a z-score computed against the same distribution.
 */
export function computeZScores(model: PivotModel, header: string): ZScoreResult {
    const rawValues = new Map<string, number>();
    const leafValues: number[] = [];

    // First pass: collect raw numeric values; track leaf rows for distribution
    for (const row of model.rows) {
        const val = row.values[header];
        if (val == null || typeof val !== 'number') continue;

        rawValues.set(row.groupKey, val);

        if (row.type === 'data') {
            leafValues.push(val);
        }
    }

    // Compute mean
    const n = leafValues.length;
    if (n === 0) {
        return {
            mean: 0,
            stdDev: 0,
            scores: new Map(),
            rawValues,
            percentiles: new Map(),
            minZ: 0,
            maxZ: 0,
        };
    }

    const mean = leafValues.reduce((s, v) => s + v, 0) / n;

    // Compute population standard deviation
    const variance = leafValues.reduce((s, v) => s + (v - mean) ** 2, 0) / n;
    const stdDev = Math.sqrt(variance);

    // Second pass: compute z-scores for ALL rows (including subtotals)
    const scores = new Map<string, number>();
    let minZ = Infinity;
    let maxZ = -Infinity;

    for (const [groupKey, val] of rawValues) {
        const z = stdDev > 0 ? (val - mean) / stdDev : 0;
        scores.set(groupKey, z);
        if (z < minZ) minZ = z;
        if (z > maxZ) maxZ = z;
    }

    if (!isFinite(minZ)) minZ = 0;
    if (!isFinite(maxZ)) maxZ = 0;

    // Compute percentile ranks from leaf values (sorted ascending)
    const sorted = [...leafValues].sort((a, b) => a - b);
    const percentiles = new Map<string, number>();

    for (const [groupKey, val] of rawValues) {
        // Percentile = fraction of leaf values that are ≤ this value
        let count = 0;
        for (const sv of sorted) {
            if (sv <= val) count++;
            else break;
        }
        percentiles.set(groupKey, count / n);
    }

    return { mean, stdDev, scores, rawValues, percentiles, minZ, maxZ };
}

// ============================================================
// Color computation
// ============================================================

/**
 * Parse a hex color string (#RRGGBB) into [r, g, b].
 */
function hexToRgb(hex: string): [number, number, number] {
    const h = hex.replace('#', '');
    return [
        parseInt(h.substring(0, 2), 16),
        parseInt(h.substring(2, 4), 16),
        parseInt(h.substring(4, 6), 16),
    ];
}

/**
 * Linearly interpolate between two RGB colors.
 * t = 0 → colorA, t = 1 → colorB.
 */
function lerpColor(
    colorA: [number, number, number],
    colorB: [number, number, number],
    t: number,
): string {
    const clamped = Math.max(0, Math.min(1, t));
    const r = Math.round(colorA[0] + (colorB[0] - colorA[0]) * clamped);
    const g = Math.round(colorA[1] + (colorB[1] - colorA[1]) * clamped);
    const b = Math.round(colorA[2] + (colorB[2] - colorA[2]) * clamped);
    return `rgba(${r}, ${g}, ${b}, 0.25)`;
}

/**
 * Determine the background color for a cell given the active conditional
 * format rule and the pre-computed z-score result.
 *
 * Returns an `rgba(...)` string or `null` if no formatting applies.
 */
export function getCellColor(
    rule: ConditionalFormatRule,
    zResult: ZScoreResult,
    groupKey: string,
    header: string,
): string | null {
    // Only color the target column
    if (header !== rule.targetHeader) return null;

    const z = zResult.scores.get(groupKey);
    const pct = zResult.percentiles.get(groupKey);
    if (z == null || pct == null) return null;

    const highRgb = hexToRgb(rule.highColor);
    const lowRgb = hexToRgb(rule.lowColor);

    if (rule.gradient) {
        // Continuous gradient: map z-score linearly from minZ→maxZ onto lowColor→highColor
        const range = zResult.maxZ - zResult.minZ;
        if (range === 0) return null;
        const t = (z - zResult.minZ) / range; // 0 = lowest z, 1 = highest z
        return lerpColor(lowRgb, highRgb, t);
    }

    // Threshold mode
    if (rule.mode === 'percentile') {
        const topThreshold = 1 - rule.threshold; // e.g. 0.75 for top 25%
        const bottomThreshold = rule.threshold;   // e.g. 0.25 for bottom 25%

        if (pct >= topThreshold) {
            // High outlier — intensity based on how far above threshold
            const intensity = topThreshold < 1 ? (pct - topThreshold) / (1 - topThreshold) : 1;
            return `rgba(${highRgb[0]}, ${highRgb[1]}, ${highRgb[2]}, ${0.12 + intensity * 0.28})`;
        }
        if (pct <= bottomThreshold) {
            // Low outlier
            const intensity = bottomThreshold > 0 ? (bottomThreshold - pct) / bottomThreshold : 1;
            return `rgba(${lowRgb[0]}, ${lowRgb[1]}, ${lowRgb[2]}, ${0.12 + intensity * 0.28})`;
        }

        return null; // Within normal range
    }

    // Z-Score cutoff mode
    if (z >= rule.threshold) {
        const intensity = Math.min((z - rule.threshold) / 2, 1);
        return `rgba(${highRgb[0]}, ${highRgb[1]}, ${highRgb[2]}, ${0.12 + intensity * 0.28})`;
    }
    if (z <= -rule.threshold) {
        const intensity = Math.min((-z - rule.threshold) / 2, 1);
        return `rgba(${lowRgb[0]}, ${lowRgb[1]}, ${lowRgb[2]}, ${0.12 + intensity * 0.28})`;
    }

    return null;
}
