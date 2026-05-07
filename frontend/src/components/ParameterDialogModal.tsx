import React, { useState, useMemo, useCallback } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
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
import Fade from '@mui/material/Fade';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { FieldRenderer } from './ReportParameterForm';
import { reportsApi } from '../api';
import { useTheme } from '../context/ThemeContext';
import type { ParameterMetadata, ParameterMetadataResponse, ValidationResponse } from '../types';

// ── Dark theme matching existing app ──
const dialogDarkTheme = createTheme({
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
        borderRadius: 12,
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
        MuiDialog: {
            styleOverrides: {
                paper: {
                    backgroundImage: 'none',
                },
            },
        },
    },
});

// ── Light theme ──
const dialogLightTheme = createTheme({
    palette: {
        mode: 'light',
        primary: {
            main: '#6366f1',
            light: '#818cf8',
            dark: '#4f46e5',
        },
        background: {
            default: '#ffffff',
            paper: '#f8fafc',
        },
        text: {
            primary: '#1e293b',
            secondary: '#64748b',
        },
        error: {
            main: '#ef4444',
        },
        success: {
            main: '#22c55e',
        },
    },
    shape: {
        borderRadius: 12,
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
        MuiDialog: {
            styleOverrides: {
                paper: {
                    backgroundImage: 'none',
                },
            },
        },
    },
});

interface ParameterDialogModalProps {
    open: boolean;
    reportId: number;
    reportName: string;
    metadata: ParameterMetadataResponse;
    onSubmit: (params: Record<string, string>) => void;
    onCancel: () => void;
    submitting?: boolean;
}

const ParameterDialogModal: React.FC<ParameterDialogModalProps> = ({
    open,
    reportId,
    reportName,
    metadata,
    onSubmit,
    onCancel,
    submitting = false,
}) => {
    const { theme: appTheme } = useTheme();
    const isDark = appTheme === 'dark';
    const muiTheme = isDark ? dialogDarkTheme : dialogLightTheme;

    // ── State ──
    const [formValues, setFormValues] = useState<Record<string, string>>(() => {
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
        return defaults;
    });
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [validating, setValidating] = useState(false);
    const [validationMessage, setValidationMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});

    // ── Visible parameters (not hidden) ──
    const visibleParams = useMemo(() => {
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
        setFieldErrors((prev) => {
            if (!prev[name]) return prev;
            const next = { ...prev };
            delete next[name];
            return next;
        });
        setValidationMessage(null);
    }, []);

    const toggleGroup = useCallback((group: string) => {
        setCollapsedGroups((prev) => ({ ...prev, [group]: !prev[group] }));
    }, []);

    const handleSubmit = useCallback(async () => {
        setValidating(true);
        setFieldErrors({});
        setValidationMessage(null);
        try {
            const params: Record<string, string> = {};
            Object.entries(formValues).forEach(([k, v]) => {
                if (v !== '') params[k] = v;
            });

            const result: ValidationResponse = await reportsApi.validateParameters(reportId, params);
            if (!result.valid) {
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
                    text: `${errorCount} validation error${errorCount !== 1 ? 's' : ''} — please fix before running`,
                });
                return;
            }

            onSubmit(params);
        } catch (err: unknown) {
            const e = err as { response?: { data?: { error?: string } }; message?: string };
            setValidationMessage({
                type: 'error',
                text: e.response?.data?.error || e.message || 'Validation failed',
            });
        } finally {
            setValidating(false);
        }
    }, [formValues, reportId, onSubmit]);

    const handleReset = useCallback(() => {
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

    const filledCount = Object.values(formValues).filter((v) => v !== '').length;

    return (
        <ThemeProvider theme={muiTheme}>
            <LocalizationProvider dateAdapter={AdapterDayjs}>
                <Dialog
                    open={open}
                    maxWidth="sm"
                    fullWidth
                    disableEscapeKeyDown
                    TransitionComponent={Fade}
                    transitionDuration={300}
                    slotProps={{
                        backdrop: {
                            sx: {
                                backgroundColor: isDark ? 'rgba(0, 0, 0, 0.75)' : 'rgba(0, 0, 0, 0.4)',
                                backdropFilter: 'blur(8px)',
                            },
                        },
                    }}
                    PaperProps={{
                        sx: {
                            backgroundColor: isDark ? '#0f172a' : '#ffffff',
                            border: isDark
                                ? '1px solid rgba(99, 102, 241, 0.2)'
                                : '1px solid rgba(99, 102, 241, 0.15)',
                            borderRadius: '16px',
                            boxShadow: isDark
                                ? '0 25px 60px rgba(0, 0, 0, 0.5), 0 0 40px rgba(99, 102, 241, 0.15)'
                                : '0 25px 60px rgba(0, 0, 0, 0.12), 0 0 40px rgba(99, 102, 241, 0.08)',
                            maxHeight: '85vh',
                            overflow: 'hidden',
                        },
                    }}
                >
                    {/* ── Header ── */}
                    <Box
                        sx={{
                            px: 3,
                            py: 2.5,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            borderBottom: isDark
                                ? '1px solid rgba(148, 163, 184, 0.1)'
                                : '1px solid rgba(226, 232, 240, 0.8)',
                            background: isDark
                                ? 'linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.05))'
                                : 'linear-gradient(135deg, rgba(99, 102, 241, 0.06), rgba(139, 92, 246, 0.03))',
                        }}
                    >
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                            <Box
                                sx={{
                                    width: 40,
                                    height: 40,
                                    borderRadius: '12px',
                                    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    fontSize: '18px',
                                    boxShadow: '0 4px 12px rgba(99, 102, 241, 0.3)',
                                }}
                            >
                                ⚙
                            </Box>
                            <Box>
                                <Typography
                                    variant="subtitle1"
                                    sx={{
                                        fontWeight: 700,
                                        color: isDark ? '#e2e8f0' : '#1e293b',
                                        letterSpacing: '0.01em',
                                        lineHeight: 1.3,
                                    }}
                                >
                                    Configure Report Parameters
                                </Typography>
                                <Typography
                                    variant="caption"
                                    sx={{
                                        color: isDark ? '#64748b' : '#94a3b8',
                                        fontSize: '0.75rem',
                                    }}
                                >
                                    {reportName} — {visibleParams.length} parameter{visibleParams.length !== 1 ? 's' : ''} required
                                </Typography>
                            </Box>
                        </Box>
                        {filledCount > 0 && (
                            <Box
                                sx={{
                                    px: 1.5,
                                    py: 0.5,
                                    borderRadius: '8px',
                                    backgroundColor: isDark
                                        ? 'rgba(99, 102, 241, 0.15)'
                                        : 'rgba(99, 102, 241, 0.1)',
                                    border: isDark
                                        ? '1px solid rgba(99, 102, 241, 0.25)'
                                        : '1px solid rgba(99, 102, 241, 0.2)',
                                }}
                            >
                                <Typography
                                    variant="caption"
                                    sx={{
                                        color: isDark ? '#a5b4fc' : '#6366f1',
                                        fontWeight: 600,
                                        fontSize: '0.7rem',
                                    }}
                                >
                                    {filledCount} set
                                </Typography>
                            </Box>
                        )}
                    </Box>

                    {/* ── Content ── */}
                    <DialogContent
                        sx={{
                            px: 3,
                            py: 2.5,
                            backgroundColor: isDark ? '#0f172a' : '#ffffff',
                            '&::-webkit-scrollbar': { width: '6px' },
                            '&::-webkit-scrollbar-track': { background: 'transparent' },
                            '&::-webkit-scrollbar-thumb': {
                                background: isDark ? 'rgba(148, 163, 184, 0.2)' : 'rgba(148, 163, 184, 0.3)',
                                borderRadius: '4px',
                            },
                        }}
                    >
                        {/* Info banner */}
                        <Box
                            sx={{
                                mb: 2.5,
                                px: 2,
                                py: 1.5,
                                borderRadius: '10px',
                                backgroundColor: isDark
                                    ? 'rgba(59, 130, 246, 0.08)'
                                    : 'rgba(59, 130, 246, 0.06)',
                                border: isDark
                                    ? '1px solid rgba(59, 130, 246, 0.15)'
                                    : '1px solid rgba(59, 130, 246, 0.12)',
                                display: 'flex',
                                alignItems: 'flex-start',
                                gap: 1.5,
                            }}
                        >
                            <Box sx={{ mt: 0.25, fontSize: '14px', flexShrink: 0 }}>ℹ️</Box>
                            <Typography
                                variant="caption"
                                sx={{
                                    color: isDark ? '#93c5fd' : '#3b82f6',
                                    lineHeight: 1.5,
                                    fontSize: '0.78rem',
                                }}
                            >
                                This report requires parameters before it can be compiled.
                                Please fill in the fields below and click <strong>Run Report</strong>.
                            </Typography>
                        </Box>

                        {/* Parameter groups */}
                        {Object.entries(grouped).map(([group, params]) => (
                            <Paper
                                key={group}
                                elevation={0}
                                sx={{
                                    mb: 2,
                                    borderRadius: '12px',
                                    backgroundColor: isDark
                                        ? 'rgba(30, 41, 59, 0.6)'
                                        : 'rgba(248, 250, 252, 0.8)',
                                    border: isDark
                                        ? '1px solid rgba(148, 163, 184, 0.08)'
                                        : '1px solid rgba(226, 232, 240, 0.8)',
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
                                        px: 2,
                                        py: 1.25,
                                        cursor: 'pointer',
                                        '&:hover': {
                                            backgroundColor: isDark
                                                ? 'rgba(99, 102, 241, 0.05)'
                                                : 'rgba(99, 102, 241, 0.04)',
                                        },
                                        transition: 'background-color 0.15s',
                                    }}
                                >
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <Box
                                            sx={{
                                                width: 7,
                                                height: 7,
                                                borderRadius: '50%',
                                                backgroundColor: '#818cf8',
                                            }}
                                        />
                                        <Typography
                                            variant="caption"
                                            sx={{
                                                fontWeight: 700,
                                                color: isDark ? '#cbd5e1' : '#475569',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.08em',
                                                fontSize: '0.7rem',
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
                                            width: 24,
                                            height: 24,
                                            transition: 'transform 0.2s',
                                            transform: collapsedGroups[group] ? 'rotate(-90deg)' : 'rotate(0deg)',
                                        }}
                                    >
                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                            <path d="M6 9l6 6 6-6" />
                                        </svg>
                                    </IconButton>
                                </Box>

                                <Divider sx={{ borderColor: isDark ? 'rgba(148, 163, 184, 0.06)' : 'rgba(226, 232, 240, 0.6)' }} />

                                {/* Group fields */}
                                <Collapse in={!collapsedGroups[group]} timeout="auto">
                                    <Box sx={{ px: 2, py: 2 }}>
                                        <Grid container spacing={2}>
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
                                    borderRadius: '10px',
                                    fontSize: '0.8rem',
                                    backgroundColor:
                                        validationMessage.type === 'success'
                                            ? isDark ? 'rgba(52, 211, 153, 0.08)' : 'rgba(34, 197, 94, 0.08)'
                                            : isDark ? 'rgba(248, 113, 113, 0.08)' : 'rgba(239, 68, 68, 0.08)',
                                    color:
                                        validationMessage.type === 'success'
                                            ? isDark ? '#34d399' : '#16a34a'
                                            : isDark ? '#f87171' : '#dc2626',
                                    border: `1px solid ${
                                        validationMessage.type === 'success'
                                            ? isDark ? 'rgba(52, 211, 153, 0.2)' : 'rgba(34, 197, 94, 0.2)'
                                            : isDark ? 'rgba(248, 113, 113, 0.2)' : 'rgba(239, 68, 68, 0.2)'
                                    }`,
                                    '& .MuiAlert-icon': {
                                        color:
                                            validationMessage.type === 'success'
                                                ? isDark ? '#34d399' : '#16a34a'
                                                : isDark ? '#f87171' : '#dc2626',
                                    },
                                }}
                            >
                                {validationMessage.text}
                            </Alert>
                        )}
                    </DialogContent>

                    {/* ── Footer Actions ── */}
                    <Box
                        sx={{
                            px: 3,
                            py: 2,
                            borderTop: isDark
                                ? '1px solid rgba(148, 163, 184, 0.1)'
                                : '1px solid rgba(226, 232, 240, 0.8)',
                            backgroundColor: isDark ? 'rgba(15, 23, 42, 0.95)' : 'rgba(248, 250, 252, 0.95)',
                            backdropFilter: 'blur(12px)',
                        }}
                    >
                        <Stack spacing={1.5}>
                            <Button
                                variant="contained"
                                fullWidth
                                disabled={submitting || validating}
                                onClick={handleSubmit}
                                sx={{
                                    textTransform: 'none',
                                    fontWeight: 700,
                                    fontSize: '0.85rem',
                                    borderRadius: '12px',
                                    py: 1.25,
                                    background: 'linear-gradient(135deg, #6366f1, #818cf8)',
                                    boxShadow: '0 4px 16px rgba(99, 102, 241, 0.35)',
                                    '&:hover': {
                                        background: 'linear-gradient(135deg, #4f46e5, #6366f1)',
                                        boxShadow: '0 6px 20px rgba(99, 102, 241, 0.45)',
                                    },
                                    '&.Mui-disabled': {
                                        background: 'rgba(99, 102, 241, 0.3)',
                                        color: 'rgba(255, 255, 255, 0.5)',
                                    },
                                }}
                                startIcon={
                                    submitting || validating ? (
                                        <CircularProgress size={18} sx={{ color: 'white' }} />
                                    ) : (
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                                            <path d="M8 5v14l11-7z" />
                                        </svg>
                                    )
                                }
                            >
                                {submitting ? 'Compiling Report…' : validating ? 'Validating…' : 'Run Report'}
                            </Button>

                            <Stack direction="row" spacing={1}>
                                <Button
                                    variant="outlined"
                                    fullWidth
                                    disabled={submitting}
                                    onClick={handleReset}
                                    sx={{
                                        textTransform: 'none',
                                        fontWeight: 600,
                                        fontSize: '0.8rem',
                                        borderRadius: '10px',
                                        borderColor: isDark ? 'rgba(148, 163, 184, 0.2)' : 'rgba(203, 213, 225, 0.6)',
                                        color: isDark ? '#94a3b8' : '#64748b',
                                        '&:hover': {
                                            borderColor: '#818cf8',
                                            color: '#818cf8',
                                            backgroundColor: isDark ? 'rgba(99, 102, 241, 0.05)' : 'rgba(99, 102, 241, 0.04)',
                                        },
                                    }}
                                    startIcon={
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                        </svg>
                                    }
                                >
                                    Reset
                                </Button>
                                <Button
                                    variant="outlined"
                                    fullWidth
                                    disabled={submitting}
                                    onClick={onCancel}
                                    sx={{
                                        textTransform: 'none',
                                        fontWeight: 600,
                                        fontSize: '0.8rem',
                                        borderRadius: '10px',
                                        borderColor: isDark ? 'rgba(148, 163, 184, 0.2)' : 'rgba(203, 213, 225, 0.6)',
                                        color: isDark ? '#94a3b8' : '#64748b',
                                        '&:hover': {
                                            borderColor: '#f87171',
                                            color: '#f87171',
                                            backgroundColor: isDark ? 'rgba(248, 113, 113, 0.05)' : 'rgba(248, 113, 113, 0.04)',
                                        },
                                    }}
                                    startIcon={
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                            <path d="M15 19l-7-7 7-7" />
                                        </svg>
                                    }
                                >
                                    Cancel
                                </Button>
                            </Stack>
                        </Stack>
                    </Box>
                </Dialog>
            </LocalizationProvider>
        </ThemeProvider>
    );
};

export default ParameterDialogModal;
