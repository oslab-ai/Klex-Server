/**
 * Z-Score Analysis Panel
 *
 * Slide-over panel for configuring Z-Score anomaly detection
 * and conditional formatting on pivot table value columns.
 */

import { useState, useMemo } from 'react';
import type { ConditionalFormatRule } from '../types';
import type { PivotModel } from '../utils/pivotEngine';

interface ZScorePanelProps {
    pivotModel: PivotModel;
    currentRule: ConditionalFormatRule | null;
    onApply: (rule: ConditionalFormatRule) => void;
    onClear: () => void;
    onClose: () => void;
}

const COLOR_PRESETS: { label: string; high: string; low: string }[] = [
    { label: '🔴 Red / 🔵 Blue', high: '#ef4444', low: '#3b82f6' },
    { label: '🟢 Green / 🔴 Red', high: '#22c55e', low: '#ef4444' },
    { label: '🟠 Orange / 🟣 Purple', high: '#f97316', low: '#a855f7' },
    { label: '🔴 Red / 🟢 Green', high: '#ef4444', low: '#22c55e' },
];

export default function ZScorePanel({
    pivotModel,
    currentRule,
    onApply,
    onClear,
    onClose,
}: ZScorePanelProps) {
    // Form state — initialize from currentRule if present
    const [targetHeader, setTargetHeader] = useState(
        currentRule?.targetHeader || pivotModel.headers[0] || ''
    );
    const [mode, setMode] = useState<'percentile' | 'zscore'>(
        currentRule?.mode || 'percentile'
    );
    const [threshold, setThreshold] = useState(
        currentRule?.threshold ?? 0.25
    );
    const [zCutoff, setZCutoff] = useState(
        currentRule?.mode === 'zscore' ? currentRule.threshold : 2.0
    );
    const [highColor, setHighColor] = useState(currentRule?.highColor || '#ef4444');
    const [lowColor, setLowColor] = useState(currentRule?.lowColor || '#3b82f6');
    const [gradient, setGradient] = useState(currentRule?.gradient ?? false);

    // Only show numeric value headers (filter out dimension-like columns)
    const numericHeaders = useMemo(() => {
        if (!pivotModel || pivotModel.rows.length === 0) return pivotModel.headers;
        // Check first data row to find numeric columns
        const firstDataRow = pivotModel.rows.find(r => r.type === 'data');
        if (!firstDataRow) return pivotModel.headers;

        return pivotModel.headers.filter(h => {
            const val = firstDataRow.values[h];
            return typeof val === 'number';
        });
    }, [pivotModel]);

    const handleApply = () => {
        onApply({
            targetHeader,
            mode,
            threshold: mode === 'percentile' ? threshold : zCutoff,
            highColor,
            lowColor,
            gradient,
        });
    };

    const matchingPreset = COLOR_PRESETS.findIndex(
        p => p.high === highColor && p.low === lowColor
    );

    return (
        <div className="absolute top-0 right-0 bottom-0 w-80 bg-white dark:bg-surface-800 border-l border-gray-200 dark:border-gray-700 shadow-2xl z-30 flex flex-col overflow-hidden rounded-r-2xl">
            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-surface-900/50 flex-shrink-0">
                <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center">
                        <svg className="w-4 h-4 text-violet-600 dark:text-violet-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                        </svg>
                    </div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Z-Score Analysis</h3>
                </div>
                <button
                    onClick={onClose}
                    className="w-7 h-7 rounded-lg hover:bg-gray-200 dark:hover:bg-surface-700 flex items-center justify-center transition-colors text-gray-500 dark:text-gray-400"
                >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>
            </div>

            {/* Body */}
            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-5">
                {/* Column Selector */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                        Analyze Column
                    </label>
                    <select
                        value={targetHeader}
                        onChange={(e) => setTargetHeader(e.target.value)}
                        className="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-surface-700 text-xs text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-colors"
                    >
                        {numericHeaders.map((h) => (
                            <option key={h} value={h}>{h}</option>
                        ))}
                    </select>
                    {numericHeaders.length === 0 && (
                        <p className="text-xs text-amber-600 dark:text-amber-400 mt-1">No numeric columns available.</p>
                    )}
                </div>

                {/* Mode Toggle */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                        Detection Mode
                    </label>
                    <div className="flex rounded-lg border border-gray-200 dark:border-gray-600 overflow-hidden">
                        <button
                            onClick={() => setMode('percentile')}
                            className={`flex-1 px-3 py-2 text-xs font-semibold transition-colors ${
                                mode === 'percentile'
                                    ? 'bg-primary-500 text-white'
                                    : 'bg-white dark:bg-surface-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-surface-600'
                            }`}
                        >
                            Percentile
                        </button>
                        <button
                            onClick={() => setMode('zscore')}
                            className={`flex-1 px-3 py-2 text-xs font-semibold transition-colors border-l border-gray-200 dark:border-gray-600 ${
                                mode === 'zscore'
                                    ? 'bg-primary-500 text-white'
                                    : 'bg-white dark:bg-surface-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-surface-600'
                            }`}
                        >
                            Z-Score Cutoff
                        </button>
                    </div>
                </div>

                {/* Threshold Control */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                        {mode === 'percentile' ? 'Percentile Threshold' : 'Z-Score Cutoff (±)'}
                    </label>
                    {mode === 'percentile' ? (
                        <div className="space-y-2">
                            <input
                                type="range"
                                min={5}
                                max={50}
                                step={5}
                                value={threshold * 100}
                                onChange={(e) => setThreshold(parseInt(e.target.value) / 100)}
                                className="w-full h-2 bg-gray-200 dark:bg-surface-600 rounded-lg appearance-none cursor-pointer accent-primary-500"
                            />
                            <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400">
                                <span>5%</span>
                                <span className="font-bold text-primary-600 dark:text-primary-400">
                                    Top/Bottom {Math.round(threshold * 100)}%
                                </span>
                                <span>50%</span>
                            </div>
                        </div>
                    ) : (
                        <div className="flex items-center gap-2">
                            <span className="text-xs text-gray-500 dark:text-gray-400 font-mono">±</span>
                            <input
                                type="number"
                                min={0.5}
                                max={5}
                                step={0.1}
                                value={zCutoff}
                                onChange={(e) => setZCutoff(parseFloat(e.target.value) || 2.0)}
                                className="flex-1 px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-surface-700 text-xs text-gray-900 dark:text-gray-100 font-mono focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-colors"
                            />
                            <span className="text-xs text-gray-400 dark:text-gray-500">σ</span>
                        </div>
                    )}
                </div>

                {/* Color Presets */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                        Color Scheme
                    </label>
                    <div className="space-y-1.5">
                        {COLOR_PRESETS.map((preset, idx) => (
                            <button
                                key={idx}
                                onClick={() => {
                                    setHighColor(preset.high);
                                    setLowColor(preset.low);
                                }}
                                className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg border text-xs transition-colors ${
                                    matchingPreset === idx
                                        ? 'border-primary-400 dark:border-primary-600 bg-primary-50/50 dark:bg-primary-950/20 text-primary-700 dark:text-primary-300 font-semibold'
                                        : 'border-gray-200 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-surface-700 text-gray-700 dark:text-gray-300'
                                }`}
                            >
                                <div className="flex items-center gap-1">
                                    <span
                                        className="w-4 h-4 rounded-full border border-gray-300 dark:border-gray-500 flex-shrink-0"
                                        style={{ backgroundColor: preset.high }}
                                    />
                                    <span className="text-gray-400 dark:text-gray-500">/</span>
                                    <span
                                        className="w-4 h-4 rounded-full border border-gray-300 dark:border-gray-500 flex-shrink-0"
                                        style={{ backgroundColor: preset.low }}
                                    />
                                </div>
                                <span>{preset.label}</span>
                            </button>
                        ))}
                    </div>
                </div>

                {/* Gradient Toggle */}
                <div className="flex items-center justify-between p-3 rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50/50 dark:bg-surface-700/50">
                    <div>
                        <div className="text-xs font-semibold text-gray-700 dark:text-gray-300">Heatmap Gradient</div>
                        <div className="text-[10px] text-gray-500 dark:text-gray-400 mt-0.5">Color every cell on a continuous scale</div>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                        <input
                            type="checkbox"
                            checked={gradient}
                            onChange={(e) => setGradient(e.target.checked)}
                            className="sr-only peer"
                        />
                        <div className="w-9 h-5 bg-gray-200 dark:bg-surface-600 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-500 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary-500" />
                    </label>
                </div>

                {/* Preview Strip */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                        Preview
                    </label>
                    <div className="h-6 rounded-lg overflow-hidden border border-gray-200 dark:border-gray-600 flex">
                        <div
                            className="flex-1"
                            style={{
                                background: gradient
                                    ? `linear-gradient(to right, ${lowColor}, transparent 40%, transparent 60%, ${highColor})`
                                    : `linear-gradient(to right, ${lowColor} 0%, ${lowColor} ${Math.round(threshold * 100)}%, transparent ${Math.round(threshold * 100)}%, transparent ${Math.round((1 - threshold) * 100)}%, ${highColor} ${Math.round((1 - threshold) * 100)}%, ${highColor} 100%)`,
                            }}
                        />
                    </div>
                    <div className="flex justify-between text-[10px] text-gray-400 dark:text-gray-500 mt-0.5 px-0.5">
                        <span>Low</span>
                        <span>Normal</span>
                        <span>High</span>
                    </div>
                </div>
            </div>

            {/* Footer */}
            <div className="flex items-center gap-2 px-4 py-3 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-surface-900/50 flex-shrink-0">
                <button
                    onClick={handleApply}
                    disabled={numericHeaders.length === 0}
                    className="flex-1 px-3 py-2 rounded-lg bg-primary-600 hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-semibold transition-colors"
                >
                    Apply
                </button>
                {currentRule && (
                    <button
                        onClick={onClear}
                        className="px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-surface-700 text-xs font-semibold text-gray-700 dark:text-gray-300 transition-colors"
                    >
                        Clear
                    </button>
                )}
                <button
                    onClick={onClose}
                    className="px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-surface-700 text-xs font-semibold text-gray-700 dark:text-gray-300 transition-colors"
                >
                    Close
                </button>
            </div>
        </div>
    );
}
