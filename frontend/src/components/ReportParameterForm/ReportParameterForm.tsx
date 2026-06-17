import React, { useState, useEffect, useMemo, useCallback } from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';
import IconButton from '@mui/material/IconButton';
import Box from '@mui/material/Box';
import Skeleton from '@mui/material/Skeleton';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import FieldRenderer from './FieldRenderer';
import { reportsApi } from '../../api';
import { useTheme } from '../../context/ThemeContext';
import { useFormFill } from '../../context/FormFillContext';
import type { ParameterMetadata, ParameterMetadataResponse, ValidationResponse } from '../../types';

// ── Dark theme matching existing app ──
const darkTheme = createTheme({
    palette: {
        mode: 'dark',
        primary: {
            main: '#818cf8',
            light: '#a5b4fc',
            dark: '#6366f1',
        },
        background: {
            default: '#0f172a',
            paper: '#1e293b',
        },
        text: {
            primary: '#e2e8f0',
            secondary: '#94a3b8',
        },
        error: {
            main: '#f87171',
        },
        success: {
            main: '#34d399',
        },
    },
    shape: {
        borderRadius: 10,
    },
    typography: {
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
        fontSize: 13,
    },
    components: {
        MuiPaper: {
            styleOverrides: {
                root: {
                    backgroundImage: 'none',
                },
            },
        },
    },
});

const lightTheme = createTheme({
    palette: {
        mode: 'light',
        primary: { main: '#6366f1', light: '#818cf8', dark: '#4f46e5' },
        background: { default: '#ffffff', paper: '#f8fafc' },
        text: { primary: '#1e293b', secondary: '#64748b' },
        error: { main: '#ef4444' },
        success: { main: '#22c55e' },
    },
    shape: { borderRadius: 10 },
    typography: { fontFamily: 'Inter, system-ui, -apple-system, sans-serif', fontSize: 13 },
    components: {
        MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    },
});

// ── Component Props ──
interface ReportParameterFormProps {
    reportId: number;
    onRunReport: (params: Record<string, string>) => void;
    running?: boolean;
    initialValues?: Record<string, string>;
    preloadedMetadata?: ParameterMetadataResponse | null;
}

const ReportParameterForm: React.FC<ReportParameterFormProps> = ({
    reportId,
    onRunReport,
    running = false,
    initialValues,
    preloadedMetadata,
}) => {
    const { theme: appTheme } = useTheme();
    const isDark = appTheme === 'dark';

    // ── State ──
    const [metadata, setMetadata] = useState<ParameterMetadataResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [formValues, setFormValues] = useState<Record<string, string>>({});
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [validating, setValidating] = useState(false);
    const [validationMessage, setValidationMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});

    // ── Initialize metadata (use preloaded if available, otherwise fetch) ──
    useEffect(() => {
        const initFormDefaults = (data: ParameterMetadataResponse) => {
            const defaults: Record<string, string> = {};
            data.parameters.forEach((p) => {
                // Use initialValues if provided, otherwise fall back to defaults
                if (initialValues && initialValues[p.name] !== undefined) {
                    defaults[p.name] = initialValues[p.name];
                } else if (p.defaultValue !== null && p.defaultValue !== undefined) {
                    defaults[p.name] = p.defaultValue;
                } else if (p.type === 'boolean') {
                    defaults[p.name] = 'false';
                } else {
                    defaults[p.name] = '';
                }
            });
            setFormValues(defaults);
        };

        // If parent already fetched metadata, use it directly — no API call
        if (preloadedMetadata) {
            setMetadata(preloadedMetadata);
            initFormDefaults(preloadedMetadata);
            setLoading(false);
            return;
        }

        // Fallback: fetch from API
        let cancelled = false;
        const fetchMetadata = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await reportsApi.getParameterMetadata(reportId);
                if (cancelled) return;
                setMetadata(data);
                initFormDefaults(data);
            } catch (err: unknown) {
                if (cancelled) return;
                const e = err as { response?: { data?: { error?: string } }; message?: string };
                setError(e.response?.data?.error || e.message || 'Failed to load parameter metadata');
            } finally {
                if (!cancelled) setLoading(false);
            }
        };
        fetchMetadata();
        return () => { cancelled = true; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [reportId, preloadedMetadata]);

    // ── Listen for Chatbot Form Fill ──
    const { formFillState } = useFormFill();
    useEffect(() => {
        if (formFillState && formFillState.reportId === reportId) {
            // Merge the new parameters from the chat into the existing form values
            setFormValues(prev => ({
                ...prev,
                ...formFillState.parameters
            }));
            // Clear errors for fields that were updated
            setFieldErrors(prev => {
                const next = { ...prev };
                Object.keys(formFillState.parameters).forEach(k => delete next[k]);
                return next;
            });
            setValidationMessage(null);
        }
    }, [formFillState, reportId]);

    // ── Visible parameters (not hidden) ──
    const visibleParams = useMemo(() => {
        if (!metadata) return [];
        return metadata.parameters
            .filter((p) => !p.hidden)
            .sort((a, b) => a.order - b.order);
    }, [metadata]);

    // ── Group parameters ──
    const grouped = useMemo(() => {
        const groups: Record<string, ParameterMetadata[]> = {};
        visibleParams.forEach((p) => {
            const g = p.group || 'Parameters';
            if (!groups[g]) groups[g] = [];
            groups[g].push(p);
        });
        return groups;
    }, [visibleParams]);

    // ── Handlers ──
    const handleFieldChange = useCallback((name: string, value: string) => {
        setFormValues((prev) => ({ ...prev, [name]: value }));
        // Clear error for this field when user edits
        setFieldErrors((prev) => {
            if (!prev[name]) return prev;
            const next = { ...prev };
            delete next[name];
            return next;
        });
        setValidationMessage(null);
    }, []);

    const handleValidate = useCallback(async () => {
        setValidating(true);
        setFieldErrors({});
        setValidationMessage(null);
        try {
            // Build params: only send non-empty values
            const params: Record<string, string> = {};
            Object.entries(formValues).forEach(([k, v]) => {
                if (v !== '') params[k] = v;
            });

            const result: ValidationResponse = await reportsApi.validateParameters(reportId, params);
            if (result.valid) {
                setValidationMessage({ type: 'success', text: 'All parameters are valid ✓' });
            } else {
                const errs: Record<string, string> = {};
                result.parameterDetails?.forEach((d) => {
                    if (!d.valid && d.error) {
                        errs[d.name] = d.error;
                    }
                });
                setFieldErrors(errs);
                const errorCount = Object.keys(errs).length;
                setValidationMessage({
                    type: 'error',
                    text: `${errorCount} validation error${errorCount !== 1 ? 's' : ''} found`,
                });
            }
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string } }; message?: string };
            setValidationMessage({
                type: 'error',
                text: e.response?.data?.error || e.message || 'Validation failed',
            });
        } finally {
            setValidating(false);
        }
    }, [formValues, reportId]);

    const handleRunReport = useCallback(async () => {
        // Validate first, then run
        setValidating(true);
        setFieldErrors({});
        setValidationMessage(null);
        try {
            const params: Record<string, string> = {};
            Object.entries(formValues).forEach(([k, v]) => {
                if (v !== '') params[k] = v;
            });

            const result = await reportsApi.validateParameters(reportId, params);
            if (!result.valid) {
                const errs: Record<string, string> = {};
                result.parameterDetails?.forEach((d) => {
                    if (!d.valid && d.error) {
                        errs[d.name] = d.error;
                    }
                });
                setFieldErrors(errs);
                setValidationMessage({
                    type: 'error',
                    text: 'Fix validation errors before running the report',
                });
                return;
            }

            onRunReport(params);
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string } }; message?: string };
            setValidationMessage({
                type: 'error',
                text: e.response?.data?.error || e.message || 'Validation failed',
            });
        } finally {
            setValidating(false);
        }
    }, [formValues, reportId, onRunReport]);

    const handleReset = useCallback(() => {
        if (!metadata) return;
        const defaults: Record<string, string> = {};
        metadata.parameters.forEach((p) => {
            if (p.defaultValue !== null && p.defaultValue !== undefined) {
                defaults[p.name] = p.defaultValue;
            } else if (p.type === 'boolean') {
                defaults[p.name] = 'false';
            } else {
                defaults[p.name] = '';
            }
        });
        setFormValues(defaults);
        setFieldErrors({});
        setValidationMessage(null);
    }, [metadata]);

    const toggleGroup = useCallback((group: string) => {
        setCollapsedGroups((prev) => ({ ...prev, [group]: !prev[group] }));
    }, []);

    // ── Render ──
    return (
        <ThemeProvider theme={isDark ? darkTheme : lightTheme}>
            <LocalizationProvider dateAdapter={AdapterDayjs}>
                <Box
                    sx={{
                        width: 340,
                        flexShrink: 0,
                        height: '100%',
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                        backgroundColor: isDark ? '#0f172a' : '#ffffff',
                        borderRight: isDark ? '1px solid rgba(148,163,184,0.08)' : '1px solid rgba(226,232,240,0.8)',
                    }}
                >
                    {/* Header */}
                    <Box
                        sx={{
                            px: 2,
                            py: 1.5,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            borderBottom: isDark ? '1px solid rgba(148,163,184,0.1)' : '1px solid rgba(226,232,240,0.8)',
                            backgroundColor: isDark ? 'rgba(15,23,42,0.95)' : 'rgba(248,250,252,0.95)',
                            backdropFilter: 'blur(12px)',
                        }}
                    >
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Box
                                sx={{
                                    width: 28,
                                    height: 28,
                                    borderRadius: '8px',
                                    background: 'linear-gradient(135deg, #6366f1, #818cf8)',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    fontSize: '14px',
                                }}
                            >
                                ⚙
                            </Box>
                            <Typography
                                variant="subtitle2"
                                sx={{
                                    fontWeight: 700,
                                    color: isDark ? '#e2e8f0' : '#1e293b',
                                    letterSpacing: '0.02em',
                                }}
                            >
                                Report Parameters
                            </Typography>
                        </Box>
                        {visibleParams.length > 0 && (
                            <Typography
                                variant="caption"
                                sx={{
                                    color: isDark ? '#64748b' : '#6366f1',
                                    backgroundColor: 'rgba(99,102,241,0.1)',
                                    px: 1,
                                    py: 0.25,
                                    borderRadius: '6px',
                                    fontSize: '0.7rem',
                                    fontWeight: 600,
                                }}
                            >
                                {visibleParams.length} field{visibleParams.length !== 1 ? 's' : ''}
                            </Typography>
                        )}
                    </Box>

                    {/* Content */}
                    <Box
                        sx={{
                            flex: 1,
                            overflow: 'auto',
                            px: 1.5,
                            py: 1.5,
                            '&::-webkit-scrollbar': { width: '5px' },
                            '&::-webkit-scrollbar-track': { background: 'transparent' },
                            '&::-webkit-scrollbar-thumb': {
                                background: 'rgba(148,163,184,0.2)',
                                borderRadius: '4px',
                            },
                        }}
                    >
                        {/* Loading state */}
                        {loading && (
                            <Stack spacing={2} sx={{ mt: 1 }}>
                                {[1, 2, 3, 4].map((i) => (
                                    <Skeleton
                                        key={i}
                                        variant="rounded"
                                        height={56}
                                        sx={{
                                            bgcolor: 'rgba(148,163,184,0.08)',
                                            borderRadius: '10px',
                                        }}
                                    />
                                ))}
                            </Stack>
                        )}

                        {/* Error state */}
                        {error && !loading && (
                            <Alert
                                severity="warning"
                                sx={{
                                    mt: 1,
                                    borderRadius: '10px',
                                    backgroundColor: 'rgba(251,191,36,0.08)',
                                    color: '#fbbf24',
                                    border: '1px solid rgba(251,191,36,0.2)',
                                    '& .MuiAlert-icon': { color: '#fbbf24' },
                                }}
                            >
                                <Typography variant="caption">{error}</Typography>
                            </Alert>
                        )}

                        {/* No parameters */}
                        {!loading && !error && visibleParams.length === 0 && (
                            <Box
                                sx={{
                                    display: 'flex',
                                    flexDirection: 'column',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    py: 6,
                                    color: '#64748b',
                                }}
                            >
                                <Box
                                    sx={{
                                        width: 48,
                                        height: 48,
                                        borderRadius: '50%',
                                        backgroundColor: 'rgba(148,163,184,0.08)',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        mb: 2,
                                        fontSize: '20px',
                                    }}
                                >
                                    📋
                                </Box>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                    No Parameters
                                </Typography>
                                <Typography variant="caption" sx={{ mt: 0.5, textAlign: 'center' }}>
                                    This report has no configurable parameters
                                </Typography>
                            </Box>
                        )}

                        {/* Parameter groups */}
                        {!loading && !error && Object.entries(grouped).map(([group, params]) => (
                            <Paper
                                key={group}
                                elevation={0}
                                sx={{
                                    mb: 1.5,
                                    borderRadius: '12px',
                                    backgroundColor: isDark ? 'rgba(30,41,59,0.6)' : 'rgba(248,250,252,0.8)',
                                    border: isDark ? '1px solid rgba(148,163,184,0.08)' : '1px solid rgba(226,232,240,0.8)',
                                    overflow: 'hidden',
                                }}
                            >
                                {/* Group header */}
                                <Box
                                    onClick={() => toggleGroup(group)}
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        px: 1.5,
                                        py: 1,
                                        cursor: 'pointer',
                                        '&:hover': {
                                            backgroundColor: isDark ? 'rgba(99,102,241,0.05)' : 'rgba(99,102,241,0.04)',
                                        },
                                        transition: 'background-color 0.15s',
                                    }}
                                >
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <Box
                                            sx={{
                                                width: 6,
                                                height: 6,
                                                borderRadius: '50%',
                                                backgroundColor:
                                                    group === 'Basic Filters' ? '#34d399' : '#94a3b8',
                                            }}
                                        />
                                        <Typography
                                            variant="caption"
                                            sx={{
                                                fontWeight: 700,
                                                color: isDark ? '#cbd5e1' : '#475569',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.08em',
                                                fontSize: '0.65rem',
                                            }}
                                        >
                                            {group}
                                        </Typography>
                                        <Typography
                                            variant="caption"
                                            sx={{
                                                color: isDark ? '#475569' : '#94a3b8',
                                                fontSize: '0.65rem',
                                            }}
                                        >
                                            ({params.length})
                                        </Typography>
                                    </Box>
                                    <IconButton
                                        size="small"
                                        sx={{
                                            color: isDark ? '#64748b' : '#94a3b8',
                                            width: 22,
                                            height: 22,
                                            transition: 'transform 0.2s',
                                            transform: collapsedGroups[group] ? 'rotate(-90deg)' : 'rotate(0deg)',
                                        }}
                                    >
                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                            <path d="M6 9l6 6 6-6" />
                                        </svg>
                                    </IconButton>
                                </Box>

                                <Divider sx={{ borderColor: isDark ? 'rgba(148,163,184,0.06)' : 'rgba(226,232,240,0.6)' }} />

                                {/* Group fields */}
                                <Collapse in={!collapsedGroups[group]} timeout="auto">
                                    <Box sx={{ px: 1.5, py: 1.5 }}>
                                        <Grid container spacing={1.5}>
                                            {params.map((param) => (
                                                <Grid size={12} key={param.name}>
                                                    <FieldRenderer
                                                        param={param}
                                                        value={formValues[param.name] ?? ''}
                                                        error={fieldErrors[param.name]}
                                                        onChange={handleFieldChange}
                                                        isDark={isDark}
                                                    />
                                                </Grid>
                                            ))}
                                        </Grid>
                                    </Box>
                                </Collapse>
                            </Paper>
                        ))}

                        {/* Validation message */}
                        {validationMessage && (
                            <Alert
                                severity={validationMessage.type}
                                sx={{
                                    mt: 1,
                                    mb: 1,
                                    borderRadius: '10px',
                                    fontSize: '0.8rem',
                                    backgroundColor:
                                        validationMessage.type === 'success'
                                            ? 'rgba(52,211,153,0.08)'
                                            : 'rgba(248,113,113,0.08)',
                                    color:
                                        validationMessage.type === 'success' ? '#34d399' : '#f87171',
                                    border: `1px solid ${
                                        validationMessage.type === 'success'
                                            ? 'rgba(52,211,153,0.2)'
                                            : 'rgba(248,113,113,0.2)'
                                    }`,
                                    '& .MuiAlert-icon': {
                                        color:
                                            validationMessage.type === 'success'
                                                ? '#34d399'
                                                : '#f87171',
                                    },
                                }}
                            >
                                {validationMessage.text}
                            </Alert>
                        )}
                    </Box>

                    {/* Action buttons */}
                    {visibleParams.length > 0 && !loading && (
                        <Box
                            sx={{
                                px: 1.5,
                                py: 1.5,
                                borderTop: isDark ? '1px solid rgba(148,163,184,0.1)' : '1px solid rgba(226,232,240,0.8)',
                                backgroundColor: isDark ? 'rgba(15,23,42,0.95)' : 'rgba(248,250,252,0.95)',
                                backdropFilter: 'blur(12px)',
                            }}
                        >
                            <Stack spacing={1}>
                                <Button
                                    variant="contained"
                                    fullWidth
                                    disabled={running || validating}
                                    onClick={handleRunReport}
                                    sx={{
                                        textTransform: 'none',
                                        fontWeight: 700,
                                        fontSize: '0.8rem',
                                        borderRadius: '10px',
                                        py: 1,
                                        background: 'linear-gradient(135deg, #6366f1, #818cf8)',
                                        boxShadow: '0 4px 12px rgba(99,102,241,0.3)',
                                        '&:hover': {
                                            background: 'linear-gradient(135deg, #4f46e5, #6366f1)',
                                            boxShadow: '0 6px 16px rgba(99,102,241,0.4)',
                                        },
                                        '&.Mui-disabled': {
                                            background: 'rgba(99,102,241,0.3)',
                                            color: 'rgba(255,255,255,0.5)',
                                        },
                                    }}
                                    startIcon={
                                        running ? (
                                            <CircularProgress size={16} sx={{ color: 'white' }} />
                                        ) : (
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                                                <path d="M8 5v14l11-7z" />
                                            </svg>
                                        )
                                    }
                                >
                                    {running ? 'Running…' : 'Run Report'}
                                </Button>
                                <Stack direction="row" spacing={1}>
                                    <Button
                                        variant="outlined"
                                        fullWidth
                                        disabled={validating || running}
                                        onClick={handleValidate}
                                        sx={{
                                            textTransform: 'none',
                                            fontWeight: 600,
                                            fontSize: '0.75rem',
                                            borderRadius: '10px',
                                            borderColor: isDark ? 'rgba(148,163,184,0.2)' : 'rgba(203,213,225,0.6)',
                                            color: isDark ? '#94a3b8' : '#64748b',
                                            '&:hover': {
                                                borderColor: '#818cf8',
                                                color: '#818cf8',
                                                backgroundColor: isDark ? 'rgba(99,102,241,0.05)' : 'rgba(99,102,241,0.04)',
                                            },
                                        }}
                                        startIcon={
                                            validating ? (
                                                <CircularProgress size={14} sx={{ color: '#94a3b8' }} />
                                            ) : (
                                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                                    <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                </svg>
                                            )
                                        }
                                    >
                                        Validate
                                    </Button>
                                    <Button
                                        variant="outlined"
                                        fullWidth
                                        disabled={running}
                                        onClick={handleReset}
                                        sx={{
                                            textTransform: 'none',
                                            fontWeight: 600,
                                            fontSize: '0.75rem',
                                            borderRadius: '10px',
                                            borderColor: isDark ? 'rgba(148,163,184,0.2)' : 'rgba(203,213,225,0.6)',
                                            color: isDark ? '#94a3b8' : '#64748b',
                                            '&:hover': {
                                                borderColor: '#f87171',
                                                color: '#f87171',
                                                backgroundColor: isDark ? 'rgba(248,113,113,0.05)' : 'rgba(248,113,113,0.04)',
                                            },
                                        }}
                                        startIcon={
                                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                                <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                            </svg>
                                        }
                                    >
                                        Reset
                                    </Button>
                                </Stack>
                            </Stack>
                        </Box>
                    )}
                </Box>
            </LocalizationProvider>
        </ThemeProvider>
    );
};

export default ReportParameterForm;
