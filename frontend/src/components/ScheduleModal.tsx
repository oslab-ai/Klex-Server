import { useState, useEffect, useCallback } from 'react';
import Modal from './Modal';
import { schedulerApi, reportsApi, organizationsApi } from '../api';
import type { ScheduleRequest, Report, ParameterMetadata, Organization } from '../types';

type TriggerType = 'simple' | 'calendar';
type CalendarPreset = 'custom' | 'daily' | 'weekly' | 'monthly' | 'business';

interface ScheduleModalProps {
    isOpen: boolean;
    onClose: () => void;
    reportName: string;
    reportUri: string;
    reportId?: number;
}

const INTERVAL_UNITS = [
    { value: 'MINUTE', label: 'Minutes' },
    { value: 'HOUR', label: 'Hours' },
    { value: 'DAY', label: 'Days' },
    { value: 'WEEK', label: 'Weeks' },
];

const OUTPUT_FORMATS = ['PDF', 'CSV', 'XLS'];

const TIMEZONES = [
    'UTC',
    'America/New_York',
    'America/Chicago',
    'America/Denver',
    'America/Los_Angeles',
    'Europe/London',
    'Europe/Paris',
    'Europe/Berlin',
    'Asia/Tokyo',
    'Asia/Kolkata',
    'Asia/Shanghai',
    'Australia/Sydney',
];

const DAYS_OF_WEEK = [
    { value: 2, label: 'Mon' },
    { value: 3, label: 'Tue' },
    { value: 4, label: 'Wed' },
    { value: 5, label: 'Thu' },
    { value: 6, label: 'Fri' },
    { value: 7, label: 'Sat' },
    { value: 1, label: 'Sun' },
];

export default function ScheduleModal({ isOpen, onClose, reportName, reportUri, reportId }: ScheduleModalProps) {
    // Selection state
    const [selectedReportId, setSelectedReportId] = useState<number | ''>('');

    // Form state
    const [scheduleName, setScheduleName] = useState('');
    const [triggerType, setTriggerType] = useState<TriggerType>('simple');
    const [timezone, setTimezone] = useState('UTC');
    const [selectedFormats, setSelectedFormats] = useState<string[]>(['PDF']);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    // Report parameter state
    const [paramMetadata, setParamMetadata] = useState<ParameterMetadata[]>([]);
    const [paramValues, setParamValues] = useState<Record<string, string>>({});
    const [loadingParams, setLoadingParams] = useState(false);
    const [paramsExpanded, setParamsExpanded] = useState(true);

    // Options fetched from API
    const [reports, setReports] = useState<Report[]>([]);
    const [organizations, setOrganizations] = useState<Organization[]>([]);
    const [loadingData, setLoadingData] = useState(false);
    useEffect(() => {
        if (isOpen) {
            setLoadingData(true);
            Promise.all([
                reportsApi.list().catch(() => []),
                organizationsApi.list().catch(() => [])
            ]).then(([reportsData, orgsData]) => {
                setReports(reportsData);
                setOrganizations(orgsData);
                setLoadingData(false);
            });
        }
    }, [isOpen]);

    // Resolved report ID: either passed as prop or from dropdown selection
    const activeReportId = reportId || (selectedReportId ? Number(selectedReportId) : null);

    // Fetch parameter metadata when a report is selected
    const fetchParamMetadata = useCallback(async (rid: number) => {
        setLoadingParams(true);
        try {
            const meta = await reportsApi.getParameterMetadata(rid);
            const visible = (meta.parameters || []).filter(
                (p: ParameterMetadata) => p.forPrompting && !p.hidden
            );
            setParamMetadata(visible);
            // Pre-fill default values
            const defaults: Record<string, string> = {};
            visible.forEach((p: ParameterMetadata) => {
                if (p.defaultValue != null) defaults[p.name] = p.defaultValue;
            });
            setParamValues(defaults);
        } catch {
            setParamMetadata([]);
            setParamValues({});
        } finally {
            setLoadingParams(false);
        }
    }, []);

    useEffect(() => {
        if (activeReportId) {
            fetchParamMetadata(activeReportId);
        } else {
            setParamMetadata([]);
            setParamValues({});
        }
    }, [activeReportId, fetchParamMetadata]);

    const handleParamChange = (name: string, value: string) => {
        setParamValues(prev => ({ ...prev, [name]: value }));
    };

    // Simple trigger state
    const [interval, setInterval] = useState(5);
    const [intervalUnit, setIntervalUnit] = useState('MINUTE');
    const [occurrenceCount, setOccurrenceCount] = useState(3);
    const [repeatForever, setRepeatForever] = useState(false);

    // Calendar trigger state
    const [calendarPreset, setCalendarPreset] = useState<CalendarPreset>('daily');
    const [cronExpression, setCronExpression] = useState('');
    const [dailyTime, setDailyTime] = useState('09:00');
    const [weeklyDay, setWeeklyDay] = useState(2); // Monday
    const [monthlyDay, setMonthlyDay] = useState(1);
    const [selectedDays, setSelectedDays] = useState<number[]>([2, 3, 4, 5, 6]); // Weekdays

    // Email delivery state
    const [emailTo, setEmailTo] = useState('');
    const [emailSubject, setEmailSubject] = useState('');
    const [emailMessage, setEmailMessage] = useState('');
    const [sendToOrganizations, setSendToOrganizations] = useState<number[]>([]);

    // UI state
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const toggleFormat = (format: string) => {
        setSelectedFormats(prev =>
            prev.includes(format)
                ? prev.filter(f => f !== format)
                : [...prev, format]
        );
    };

    const toggleDay = (day: number) => {
        setSelectedDays(prev =>
            prev.includes(day)
                ? prev.filter(d => d !== day)
                : [...prev, day].sort()
        );
    };

    const buildTrigger = (): ScheduleRequest['trigger'] => {
        if (triggerType === 'simple') {
            return {
                simpleTrigger: {
                    timezone,
                    recurrenceInterval: interval,
                    recurrenceIntervalUnit: intervalUnit,
                    occurrenceCount: repeatForever ? -1 : occurrenceCount,
                    startDate: startDate || null,
                    endDate: endDate || null,
                },
            };
        }

        // Calendar trigger
        let cron = cronExpression;
        if (calendarPreset === 'daily') {
            const [h, m] = dailyTime.split(':');
            cron = `0 ${m} ${h} * * ?`;
        } else if (calendarPreset === 'weekly') {
            const [h, m] = dailyTime.split(':');
            cron = `0 ${m} ${h} ? * ${weeklyDay}`;
        } else if (calendarPreset === 'monthly') {
            const [h, m] = dailyTime.split(':');
            cron = `0 ${m} ${h} ${monthlyDay} * ?`;
        } else if (calendarPreset === 'business') {
            return {
                calendarTrigger: {
                    timezone,
                    startTimeOfDay: '09:00:00',
                    endTimeOfDay: '17:00:00',
                    repeatInterval: 1,
                    repeatIntervalUnit: 'HOUR',
                    daysOfWeek: selectedDays,
                    startDate: startDate || null,
                    endDate: endDate || null,
                },
            };
        }

        return {
            calendarTrigger: {
                timezone,
                cronExpression: cron,
                startDate: startDate || null,
                endDate: endDate || null,
            },
        };
    };

    const handleSubmit = async () => {
        // Validation for missing report selection
        if (!reportUri && !selectedReportId) {
            setError('Please select a report to schedule.');
            return;
        }

        if (!scheduleName.trim()) {
            setError('Schedule name is required.');
            return;
        }
        if (selectedFormats.length === 0) {
            setError('Select at least one output format.');
            return;
        }
        if (!emailTo.trim()) {
            setError('Email recipient is required.');
            return;
        }
        if (triggerType === 'calendar' && calendarPreset === 'custom' && !cronExpression.trim()) {
            setError('Cron expression is required for custom calendar trigger.');
            return;
        }

        setSubmitting(true);
        setError(null);
        setSuccess(null);

        try {
            const getFinalReportName = () => {
                if (reportName) return reportName;
                if (selectedReportId) {
                    const r = reports.find(r => r.id === selectedReportId);
                    return r?.report_name || 'Report';
                }
                return 'Report';
            };

            const payload: ScheduleRequest = {
                reportUnitUri: reportUri || 'dummy_uri_will_be_replaced_by_backend',
                ...((reportId || selectedReportId) ? { report_id: (reportId || selectedReportId) as number } : {}),
                scheduleName: scheduleName.trim(),
                outputFormats: { outputFormat: selectedFormats },
                outputTimeZone: timezone,
                trigger: buildTrigger(),
                deliveryMethod: 'EMAIL',
                mailNotification: {
                    messageText: emailMessage || `Scheduled report: ${getFinalReportName()}`,
                    subject: emailSubject || `Report: ${getFinalReportName()}`,
                    toAddresses: {
                        address: emailTo.split(/[,;]\s*/).filter(Boolean),
                    },
                    sendToOrganizations,
                },
                ...(Object.keys(paramValues).length > 0 ? { parameters: paramValues } : {}),
            };

            const result = await schedulerApi.create(payload);
            setSuccess(`✅ Scheduled successfully! DAG: ${result.dag_id}`);

            // Reset form after short delay
            setTimeout(() => {
                resetForm();
                onClose();
            }, 2000);
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string; message?: string } }; message?: string };
            setError(
                e.response?.data?.error ||
                e.response?.data?.message ||
                e.message ||
                'Failed to create schedule'
            );
        } finally {
            setSubmitting(false);
        }
    };

    const resetForm = () => {
        setSelectedReportId('');
        setScheduleName('');
        setTriggerType('simple');
        setTimezone('UTC');
        setSelectedFormats(['PDF']);
        setStartDate('');
        setEndDate('');
        setInterval(5);
        setIntervalUnit('MINUTE');
        setOccurrenceCount(3);
        setRepeatForever(false);
        setCalendarPreset('daily');
        setCronExpression('');
        setDailyTime('09:00');
        setWeeklyDay(2);
        setMonthlyDay(1);
        setSelectedDays([2, 3, 4, 5, 6]);
        setEmailTo('');
        setEmailSubject('');
        setEmailMessage('');
        setSendToOrganizations([]);
        setParamMetadata([]);
        setParamValues({});
        setParamsExpanded(true);
        setError(null);
        setSuccess(null);
    };

    const handleClose = () => {
        resetForm();
        onClose();
    };

    return (
        <Modal isOpen={isOpen} onClose={handleClose} maxWidth="max-w-4xl">
            {/* Header */}
            <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 bg-gradient-to-br from-violet-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-purple-500/30">
                    <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
                <div>
                    <h2 className="text-lg font-bold">Schedule Report</h2>
                    {reportName && <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-[280px]">{reportName}</p>}
                </div>
            </div>

            {/* Success message */}
            {success && (
                <div className="mb-4 p-3 rounded-lg bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-sm">
                    {success}
                </div>
            )}

            {/* Error message */}
            {error && (
                <div className="mb-4 p-3 rounded-lg bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
                    {error}
                </div>
            )}

            <div className="grid grid-cols-2 gap-6">
                <div className="space-y-5">
                {/* Global Configuration */}
                {(!reportUri || reportUri === '') && (
                    <div>
                        <label className="block text-sm font-medium mb-1.5">Select Report</label>
                        <select
                            value={selectedReportId}
                            onChange={(e) => setSelectedReportId(e.target.value === '' ? '' : Number(e.target.value))}
                            className="input w-full"
                        >
                            <option value="">-- Choose a Report --</option>
                            {reports.map((r) => (
                                <option key={r.id} value={r.id}>{r.report_name} ({r.repo_name})</option>
                            ))}
                        </select>
                    </div>
                )}

                {/* Report Parameters */}
                {activeReportId && (loadingParams || paramMetadata.length > 0) && (
                    <div className="rounded-lg border border-amber-200/50 dark:border-amber-800/30 overflow-hidden">
                        <button
                            type="button"
                            onClick={() => setParamsExpanded(!paramsExpanded)}
                            className="w-full flex items-center justify-between px-4 py-2.5 bg-amber-50/50 dark:bg-amber-900/10 hover:bg-amber-100/50 dark:hover:bg-amber-900/20 transition-colors"
                        >
                            <span className="inline-flex items-center gap-2">
                                <svg className="w-4 h-4 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
                                </svg>
                                <span className="text-sm font-medium text-amber-700 dark:text-amber-300">
                                    Report Parameters{paramMetadata.length > 0 && ` (${paramMetadata.length})`}
                                </span>
                            </span>
                            <svg className={`w-4 h-4 text-amber-500 transition-transform ${paramsExpanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                            </svg>
                        </button>
                        {paramsExpanded && (
                            <div className="px-4 py-3 space-y-3 bg-amber-50/20 dark:bg-amber-900/5">
                                {loadingParams ? (
                                    <div className="flex items-center justify-center py-4 gap-2 text-sm text-gray-400">
                                        <div className="w-4 h-4 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
                                        Loading parameters…
                                    </div>
                                ) : paramMetadata.map(param => (
                                    <div key={param.name}>
                                        <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">
                                            {param.label || param.name}
                                            {param.required && <span className="text-red-400 ml-0.5">*</span>}
                                        </label>
                                        {param.widget === 'Select' && param.allowedValues ? (
                                            <select
                                                value={paramValues[param.name] || ''}
                                                onChange={e => handleParamChange(param.name, e.target.value)}
                                                className="input text-sm"
                                            >
                                                <option value="">-- Select --</option>
                                                {param.allowedValues.map(opt => (
                                                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                ))}
                                            </select>
                                        ) : param.widget === 'MultiSelect' && param.allowedValues ? (
                                            <select
                                                multiple
                                                value={(paramValues[param.name] || '').split(',').filter(Boolean)}
                                                onChange={e => {
                                                    const selected = Array.from(e.target.selectedOptions, o => o.value);
                                                    handleParamChange(param.name, selected.join(','));
                                                }}
                                                className="input text-sm min-h-[72px]"
                                            >
                                                {param.allowedValues.map(opt => (
                                                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                ))}
                                            </select>
                                        ) : param.widget === 'Switch' ? (
                                            <label className="inline-flex items-center gap-2 cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={paramValues[param.name] === 'true'}
                                                    onChange={e => handleParamChange(param.name, String(e.target.checked))}
                                                    className="w-4 h-4 rounded border-gray-300 text-violet-600 focus:ring-violet-500"
                                                />
                                                <span className="text-xs text-gray-500">{paramValues[param.name] === 'true' ? 'Yes' : 'No'}</span>
                                            </label>
                                        ) : param.widget === 'DatePicker' ? (
                                            <input
                                                type="date"
                                                value={paramValues[param.name] || ''}
                                                onChange={e => handleParamChange(param.name, e.target.value)}
                                                className="input text-sm"
                                            />
                                        ) : param.widget === 'DateTimePicker' ? (
                                            <input
                                                type="datetime-local"
                                                value={paramValues[param.name] || ''}
                                                onChange={e => handleParamChange(param.name, e.target.value)}
                                                className="input text-sm"
                                            />
                                        ) : param.widget === 'NumericField' ? (
                                            param.allowedValues && param.allowedValues.length > 0 ? (
                                                <select
                                                    multiple
                                                    value={(paramValues[param.name] || '').split('|').filter(Boolean)}
                                                    onChange={e => {
                                                        const selected = Array.from(e.target.selectedOptions, o => o.value);
                                                        handleParamChange(param.name, selected.join('|'));
                                                    }}
                                                    className="input text-sm min-h-[72px]"
                                                >
                                                    {param.allowedValues.map(opt => (
                                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                    ))}
                                                </select>
                                            ) : (
                                                <select
                                                    multiple
                                                    className="input text-sm min-h-[48px]"
                                                    disabled
                                                >
                                                    <option disabled>No options available</option>
                                                </select>
                                            )
                                        ) : (
                                            param.allowedValues && param.allowedValues.length > 0 ? (
                                                <select
                                                    multiple
                                                    value={(paramValues[param.name] || '').split('|').filter(Boolean)}
                                                    onChange={e => {
                                                        const selected = Array.from(e.target.selectedOptions, o => o.value);
                                                        handleParamChange(param.name, selected.join('|'));
                                                    }}
                                                    className="input text-sm min-h-[72px]"
                                                >
                                                    {param.allowedValues.map(opt => (
                                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                    ))}
                                                </select>
                                            ) : (
                                                <select
                                                    multiple
                                                    className="input text-sm min-h-[48px]"
                                                    disabled
                                                >
                                                    <option disabled>No options available</option>
                                                </select>
                                            )
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* Schedule Name */}
                <div>
                    <label className="block text-sm font-medium mb-1.5">Schedule Name</label>
                    <input
                        type="text"
                        value={scheduleName}
                        onChange={e => setScheduleName(e.target.value)}
                        placeholder="e.g. Daily Sales Report"
                        className="input"
                    />
                </div>

                {/* Trigger Type Toggle */}
                <div>
                    <label className="block text-sm font-medium mb-2">Trigger Type</label>
                    <div className="flex rounded-lg bg-gray-100 dark:bg-gray-700 p-1 gap-1">
                        <button
                            type="button"
                            onClick={() => setTriggerType('simple')}
                            className={`flex-1 py-2 px-3 rounded-md text-sm font-medium transition-all ${
                                triggerType === 'simple'
                                    ? 'bg-white dark:bg-gray-600 shadow text-violet-700 dark:text-violet-300'
                                    : 'text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white'
                            }`}
                        >
                            <span className="inline-flex items-center gap-1.5">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                                Simple
                            </span>
                        </button>
                        <button
                            type="button"
                            onClick={() => setTriggerType('calendar')}
                            className={`flex-1 py-2 px-3 rounded-md text-sm font-medium transition-all ${
                                triggerType === 'calendar'
                                    ? 'bg-white dark:bg-gray-600 shadow text-violet-700 dark:text-violet-300'
                                    : 'text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white'
                            }`}
                        >
                            <span className="inline-flex items-center gap-1.5">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                </svg>
                                Calendar
                            </span>
                        </button>
                    </div>
                </div>

                {/* Simple Trigger Options */}
                {triggerType === 'simple' && (
                    <div className="space-y-3 p-4 rounded-lg bg-violet-50/50 dark:bg-violet-900/10 border border-violet-200/50 dark:border-violet-800/30">
                        <div className="flex gap-3">
                            <div className="flex-1">
                                <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Every</label>
                                <input
                                    type="text"
                                    inputMode="numeric"
                                    pattern="[0-9]*"
                                    value={interval}
                                    onChange={e => {
                                        const val = e.target.value.replace(/[^0-9]/g, '');
                                        setInterval(val === '' ? 0 : parseInt(val));
                                    }}
                                    className="input text-sm"
                                />
                            </div>
                            <div className="flex-1">
                                <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Unit</label>
                                <select
                                    value={intervalUnit}
                                    onChange={e => setIntervalUnit(e.target.value)}
                                    className="input text-sm"
                                >
                                    {INTERVAL_UNITS.map(u => (
                                        <option key={u.value} value={u.value}>{u.label}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                        <div>
                            <div className="flex items-center gap-2 mb-1.5">
                                <input
                                    type="checkbox"
                                    id="repeat-forever"
                                    checked={repeatForever}
                                    onChange={e => setRepeatForever(e.target.checked)}
                                    className="w-4 h-4 rounded border-gray-300 text-violet-600 focus:ring-violet-500"
                                />
                                <label htmlFor="repeat-forever" className="text-xs font-medium text-gray-600 dark:text-gray-400">Repeat forever</label>
                            </div>
                            {!repeatForever && (
                                <div>
                                    <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Occurrences</label>
                                    <input
                                        type="text"
                                        inputMode="numeric"
                                        pattern="[0-9]*"
                                        value={occurrenceCount}
                                        onChange={e => {
                                            const val = e.target.value.replace(/[^0-9]/g, '');
                                            setOccurrenceCount(val === '' ? 0 : parseInt(val));
                                        }}
                                        className="input text-sm"
                                    />
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Calendar Trigger Options */}
                {triggerType === 'calendar' && (
                    <div className="space-y-3 p-4 rounded-lg bg-violet-50/50 dark:bg-violet-900/10 border border-violet-200/50 dark:border-violet-800/30">
                        <div>
                            <label className="block text-xs font-medium mb-1.5 text-gray-600 dark:text-gray-400">Preset</label>
                            <div className="grid grid-cols-3 gap-1.5">
                                {(['daily', 'weekly', 'monthly', 'business', 'custom'] as CalendarPreset[]).map(p => (
                                    <button
                                        key={p}
                                        type="button"
                                        onClick={() => setCalendarPreset(p)}
                                        className={`text-xs py-1.5 px-2 rounded-md font-medium transition-all capitalize ${
                                            calendarPreset === p
                                                ? 'bg-violet-600 text-white shadow'
                                                : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-violet-100 dark:hover:bg-gray-600'
                                        }`}
                                    >
                                        {p}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {(calendarPreset === 'daily' || calendarPreset === 'weekly' || calendarPreset === 'monthly') && (
                            <div>
                                <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Time</label>
                                <input
                                    type="time"
                                    value={dailyTime}
                                    onChange={e => setDailyTime(e.target.value)}
                                    className="input text-sm"
                                />
                            </div>
                        )}

                        {calendarPreset === 'weekly' && (
                            <div>
                                <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Day of Week</label>
                                <select
                                    value={weeklyDay}
                                    onChange={e => setWeeklyDay(parseInt(e.target.value))}
                                    className="input text-sm"
                                >
                                    {DAYS_OF_WEEK.map(d => (
                                        <option key={d.value} value={d.value}>{d.label}</option>
                                    ))}
                                </select>
                            </div>
                        )}

                        {calendarPreset === 'monthly' && (
                            <div>
                                <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Day of Month</label>
                                <input
                                    type="text"
                                    inputMode="numeric"
                                    pattern="[0-9]*"
                                    value={monthlyDay}
                                    onChange={e => {
                                        const val = e.target.value.replace(/[^0-9]/g, '');
                                        const num = val === '' ? 0 : parseInt(val);
                                        setMonthlyDay(num > 31 ? 31 : num);
                                    }}
                                    className="input text-sm"
                                />
                            </div>
                        )}

                        {calendarPreset === 'business' && (
                            <div>
                                <label className="block text-xs font-medium mb-1.5 text-gray-600 dark:text-gray-400">Active Days</label>
                                <div className="flex gap-1">
                                    {DAYS_OF_WEEK.map(d => (
                                        <button
                                            key={d.value}
                                            type="button"
                                            onClick={() => toggleDay(d.value)}
                                            className={`w-9 h-9 rounded-lg text-xs font-medium transition-all ${
                                                selectedDays.includes(d.value)
                                                    ? 'bg-violet-600 text-white shadow'
                                                    : 'bg-white dark:bg-gray-700 text-gray-500 hover:bg-violet-100'
                                            }`}
                                        >
                                            {d.label}
                                        </button>
                                    ))}
                                </div>
                                <p className="text-xs text-gray-400 mt-1">9 AM – 5 PM, every hour</p>
                            </div>
                        )}

                        {calendarPreset === 'custom' && (
                            <div>
                                <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Cron Expression</label>
                                <input
                                    type="text"
                                    value={cronExpression}
                                    onChange={e => setCronExpression(e.target.value)}
                                    placeholder="0 0 9 * * ?"
                                    className="input text-sm font-mono"
                                />
                                <p className="text-xs text-gray-400 mt-1">Quartz cron format: sec min hour day month weekday</p>
                            </div>
                        )}
                    </div>
                )}
                </div> {/* Close first column space-y-5 */}

                {/* Second Column */}
                <div className="space-y-5">
                    {/* Output Formats */}
                    <div>
                        <label className="block text-sm font-medium mb-2">Output Formats</label>
                        <div className="flex gap-2">
                            {OUTPUT_FORMATS.map(fmt => (
                                <button
                                    key={fmt}
                                    type="button"
                                    onClick={() => toggleFormat(fmt)}
                                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                                        selectedFormats.includes(fmt)
                                            ? 'bg-gradient-to-r from-violet-500 to-purple-600 text-white shadow-md shadow-purple-500/20'
                                            : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                                    }`}
                                >
                                    {fmt}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Timezone */}
                    <div>
                        <label className="block text-sm font-medium mb-1.5">Timezone</label>
                        <select
                            value={timezone}
                            onChange={e => setTimezone(e.target.value)}
                            className="input text-sm"
                        >
                            {TIMEZONES.map(tz => (
                                <option key={tz} value={tz}>{tz}</option>
                            ))}
                        </select>
                    </div>

                    {/* Start / End Date */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Start Date <span className="text-gray-400">(optional)</span></label>
                            <input
                                type="datetime-local"
                                value={startDate}
                                onChange={e => setStartDate(e.target.value)}
                                className="input text-sm"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">End Date <span className="text-gray-400">(optional)</span></label>
                            <input
                                type="datetime-local"
                                value={endDate}
                                onChange={e => setEndDate(e.target.value)}
                                className="input text-sm"
                            />
                        </div>
                    </div>

                    {/* Email Delivery */}
                    <div className="space-y-3">
                        <label className="block text-sm font-medium">Email Delivery</label>
                        
                        <div className="bg-gray-50 dark:bg-gray-800/50 p-3 rounded-lg border border-gray-200 dark:border-gray-700/50">
                            <div className="mb-2">
                                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">Send to Organizations</span>
                                <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 mb-2">Select organizations to distribute this report automatically to all their active members.</p>
                            </div>
                            <select
                                multiple
                                value={sendToOrganizations.map(String)}
                                onChange={e => {
                                    const selected = Array.from(e.target.selectedOptions, o => Number(o.value));
                                    setSendToOrganizations(selected);
                                }}
                                className="input text-sm min-h-[96px] bg-white"
                            >
                                {organizations.map(org => (
                                    <option key={org.id} value={org.id}>{org.name}</option>
                                ))}
                            </select>
                        </div>
                    <div>
                        <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">To</label>
                        <input
                            type="text"
                            value={emailTo}
                            onChange={e => setEmailTo(e.target.value)}
                            placeholder="email@example.com, another@example.com"
                            className="input text-sm"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Subject</label>
                        <input
                            type="text"
                            value={emailSubject}
                            onChange={e => setEmailSubject(e.target.value)}
                            placeholder={`Report: ${reportName}`}
                            className="input text-sm"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium mb-1 text-gray-600 dark:text-gray-400">Message</label>
                        <textarea
                            value={emailMessage}
                            onChange={e => setEmailMessage(e.target.value)}
                            placeholder="Please find the attached report."
                            rows={2}
                            className="input text-sm resize-none"
                        />
                    </div>
                </div>
            </div>
            </div>

            {/* Actions */}
            <div className="flex gap-3 pt-6 w-full justify-end border-t border-gray-200 dark:border-gray-800 mt-6 md:col-span-2">
                <button
                    type="button"
                    onClick={handleClose}
                    className="btn-secondary"
                    disabled={submitting}
                >
                    Cancel
                </button>
                <button
                        type="button"
                        onClick={handleSubmit}
                        disabled={submitting || loadingData}
                        className="flex-1 btn bg-gradient-to-r from-violet-500 to-purple-600 text-white hover:from-violet-600 hover:to-purple-700 shadow-lg shadow-purple-500/20 disabled:opacity-50"
                    >
                        {submitting ? (
                            <span className="flex items-center justify-center gap-2">
                                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                Scheduling...
                            </span>
                        ) : loadingData ? (
                            <span className="flex items-center justify-center gap-2">
                                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                Loading...
                            </span>
                        ) : (
                            'Schedule Report'
                        )}
                    </button>
                </div>
        </Modal>
    );
}
