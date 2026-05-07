import React from 'react';
// TextField is no longer used — all string/number params are now MultiSelect dropdowns
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import InputLabel from '@mui/material/InputLabel';
import FormControl from '@mui/material/FormControl';
import FormHelperText from '@mui/material/FormHelperText';
import OutlinedInput from '@mui/material/OutlinedInput';
import Chip from '@mui/material/Chip';
import Box from '@mui/material/Box';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import dayjs, { Dayjs } from 'dayjs';
import type { ParameterMetadata } from '../../types';

interface FieldRendererProps {
    param: ParameterMetadata;
    value: string;
    error?: string | null;
    onChange: (name: string, value: string) => void;
    isDark?: boolean;
}

const FieldRenderer: React.FC<FieldRendererProps> = ({ param, value, error, onChange, isDark = true }) => {
    if (param.hidden) return null;

    const handleChange = (val: string) => {
        onChange(param.name, val);
    };

    // Theme-aware color tokens
    const colors = {
        inputBg: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
        borderColor: isDark ? 'rgba(148,163,184,0.2)' : 'rgba(203,213,225,0.6)',
        labelColor: isDark ? '#94a3b8' : '#64748b',
        focusColor: '#818cf8',
        textColor: isDark ? '#e2e8f0' : '#1e293b',
        helperColor: isDark ? '#64748b' : '#94a3b8',
        menuBg: isDark ? '#1e293b' : '#ffffff',
        menuBorder: isDark ? 'rgba(148,163,184,0.15)' : 'rgba(226,232,240,0.8)',
        menuItemColor: isDark ? '#e2e8f0' : '#1e293b',
        menuItemHover: isDark ? 'rgba(99,102,241,0.15)' : 'rgba(99,102,241,0.08)',
        menuItemSelected: isDark ? 'rgba(99,102,241,0.25)' : 'rgba(99,102,241,0.12)',
        chipBg: isDark ? 'rgba(99,102,241,0.25)' : 'rgba(99,102,241,0.12)',
        chipColor: isDark ? '#e2e8f0' : '#4f46e5',
        chipDeleteColor: isDark ? 'rgba(226, 232, 240, 0.7)' : 'rgba(79, 70, 229, 0.6)',
        chipDeleteHover: isDark ? '#e2e8f0' : '#4f46e5',
        iconColor: isDark ? '#94a3b8' : '#94a3b8',
        placeholderColor: isDark ? '#64748b' : '#94a3b8',
        switchChecked: '#818cf8',
        switchTrack: '#6366f1',
    };

    const commonSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            backgroundColor: colors.inputBg,
            '&:hover .MuiOutlinedInput-notchedOutline': {
                borderColor: '#6366f1',
            },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                borderColor: colors.focusColor,
                borderWidth: '2px',
            },
        },
        '& .MuiInputLabel-root': {
            color: colors.labelColor,
            '&.Mui-focused': { color: colors.focusColor },
        },
        '& .MuiOutlinedInput-notchedOutline': {
            borderColor: colors.borderColor,
        },
        '& .MuiInputBase-input': {
            color: colors.textColor,
            fontSize: '0.875rem',
        },
        '& .MuiFormHelperText-root': {
            color: error ? '#f87171' : colors.helperColor,
            fontSize: '0.75rem',
        },
    };

    const menuPaperSx = {
        PaperProps: {
            sx: {
                backgroundColor: colors.menuBg,
                border: `1px solid ${colors.menuBorder}`,
                maxHeight: '300px',
                '& .MuiMenuItem-root': {
                    color: colors.menuItemColor,
                    fontSize: '0.875rem',
                    '&:hover': { backgroundColor: colors.menuItemHover },
                    '&.Mui-selected': { backgroundColor: colors.menuItemSelected },
                },
            },
        },
    };

    switch (param.widget) {
        case 'TextField':
        case 'NumericField': {
            // If allowedValues are available, render as MultiSelect dropdown
            if (param.allowedValues && param.allowedValues.length > 0) {
                const selected = value ? value.split('|').filter(Boolean) : [];
                return (
                    <FormControl fullWidth size="small" error={!!error} disabled={param.readOnly} sx={commonSx}>
                        <InputLabel sx={{ color: colors.labelColor, '&.Mui-focused': { color: colors.focusColor } }}>
                            {param.label}{param.required ? ' *' : ''}
                        </InputLabel>
                        <Select
                            multiple
                            value={selected}
                            onChange={(e) => {
                                const vals = typeof e.target.value === 'string'
                                    ? e.target.value.split(',')
                                    : (e.target.value as string[]);
                                handleChange(vals.join('|'));
                            }}
                            input={<OutlinedInput label={param.label + (param.required ? ' *' : '')} />}
                            renderValue={(sel) => (
                                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                    {(sel as string[]).map((v) => (
                                        <Chip
                                            key={v}
                                            label={param.allowedValues?.find(o => o.value === v)?.label ?? v}
                                            size="small"
                                            onMouseDown={(e) => e.stopPropagation()}
                                            sx={{
                                                backgroundColor: colors.chipBg,
                                                color: colors.chipColor,
                                                fontSize: '0.75rem',
                                            }}
                                        />
                                    ))}
                                </Box>
                            )}
                            MenuProps={menuPaperSx}
                            sx={{
                                color: colors.textColor,
                                '& .MuiSvgIcon-root': { color: colors.iconColor },
                            }}
                        >
                            {param.allowedValues?.map((opt) => (
                                <MenuItem key={opt.value} value={opt.value}>
                                    {opt.label}
                                </MenuItem>
                            ))}
                        </Select>
                        {error && <FormHelperText>{error}</FormHelperText>}
                    </FormControl>
                );
            }
            // Fallback: no allowedValues available — render empty multi-select
            return (
                <FormControl fullWidth size="small" error={!!error} disabled={param.readOnly} sx={commonSx}>
                    <InputLabel sx={{ color: colors.labelColor, '&.Mui-focused': { color: colors.focusColor } }}>
                        {param.label}{param.required ? ' *' : ''}
                    </InputLabel>
                    <Select
                        multiple
                        value={[]}
                        input={<OutlinedInput label={param.label + (param.required ? ' *' : '')} />}
                        renderValue={() => (
                            <Box sx={{ color: colors.placeholderColor, fontSize: '0.875rem' }}>No options available</Box>
                        )}
                        sx={{
                            color: colors.textColor,
                            '& .MuiSvgIcon-root': { color: colors.iconColor },
                        }}
                        MenuProps={{
                            PaperProps: {
                                sx: {
                                    backgroundColor: colors.menuBg,
                                    border: `1px solid ${colors.menuBorder}`,
                                },
                            },
                        }}
                    >
                        <MenuItem disabled value="">
                            <em style={{ color: colors.placeholderColor }}>No options loaded from data source</em>
                        </MenuItem>
                    </Select>
                    {error && <FormHelperText>{error}</FormHelperText>}
                </FormControl>
            );
        }

        case 'Switch':
            return (
                <FormControl fullWidth error={!!error} disabled={param.readOnly}>
                    <FormControlLabel
                        label={
                            <span style={{ color: colors.textColor, fontSize: '0.875rem' }}>
                                {param.label}{param.required ? ' *' : ''}
                            </span>
                        }
                        control={
                            <Switch
                                checked={value === 'true'}
                                onChange={(e) => handleChange(e.target.checked ? 'true' : 'false')}
                                sx={{
                                    '& .MuiSwitch-switchBase.Mui-checked': {
                                        color: colors.switchChecked,
                                    },
                                    '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                                        backgroundColor: colors.switchTrack,
                                    },
                                }}
                            />
                        }
                    />
                    {error && <FormHelperText sx={{ color: '#f87171' }}>{error}</FormHelperText>}
                </FormControl>
            );

        case 'DatePicker':
            return (
                <DatePicker
                    label={param.label + (param.required ? ' *' : '')}
                    value={value ? dayjs(value) : null}
                    onChange={(newVal: Dayjs | null) => handleChange(newVal ? newVal.format('YYYY-MM-DD') : '')}
                    disabled={param.readOnly}
                    slotProps={{
                        textField: {
                            fullWidth: true,
                            size: 'small',
                            error: !!error,
                            helperText: error || '',
                            sx: commonSx,
                        },
                    }}
                />
            );

        case 'DateTimePicker':
            return (
                <DateTimePicker
                    label={param.label + (param.required ? ' *' : '')}
                    value={value ? dayjs(value) : null}
                    onChange={(newVal: Dayjs | null) => handleChange(newVal ? newVal.format('YYYY-MM-DDTHH:mm:ss') : '')}
                    disabled={param.readOnly}
                    slotProps={{
                        textField: {
                            fullWidth: true,
                            size: 'small',
                            error: !!error,
                            helperText: error || '',
                            sx: commonSx,
                        },
                    }}
                />
            );

        case 'Select':
            return (
                <FormControl fullWidth size="small" error={!!error} disabled={param.readOnly} sx={commonSx}>
                    <InputLabel sx={{ color: colors.labelColor, '&.Mui-focused': { color: colors.focusColor } }}>
                        {param.label}{param.required ? ' *' : ''}
                    </InputLabel>
                    <Select
                        value={value}
                        label={param.label + (param.required ? ' *' : '')}
                        onChange={(e) => handleChange(e.target.value)}
                        sx={{
                            color: colors.textColor,
                            '& .MuiSvgIcon-root': { color: colors.iconColor },
                        }}
                        MenuProps={menuPaperSx}
                    >
                        <MenuItem value="">
                            <em style={{ color: colors.placeholderColor }}>None</em>
                        </MenuItem>
                        {param.allowedValues?.map((opt) => (
                            <MenuItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </MenuItem>
                        ))}
                    </Select>
                    {error && <FormHelperText>{error}</FormHelperText>}
                </FormControl>
            );

        case 'MultiSelect': {
            // Value is a pipe-separated string e.g. "val1|val2"
            const selected = value ? value.split('|').filter(Boolean) : [];
            return (
                <FormControl fullWidth size="small" error={!!error} disabled={param.readOnly} sx={commonSx}>
                    <InputLabel sx={{ color: colors.labelColor, '&.Mui-focused': { color: colors.focusColor } }}>
                        {param.label}{param.required ? ' *' : ''}
                    </InputLabel>
                    <Select
                        multiple
                        value={selected}
                        onChange={(e) => {
                            const vals = typeof e.target.value === 'string'
                                ? e.target.value.split(',')
                                : (e.target.value as string[]);
                            handleChange(vals.join('|'));
                        }}
                        input={<OutlinedInput label={param.label + (param.required ? ' *' : '')} />}
                        renderValue={(sel) => (
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                {(sel as string[]).map((v) => (
                                    <Chip
                                        key={v}
                                        label={param.allowedValues?.find(o => o.value === v)?.label ?? v}
                                        size="small"
                                        onMouseDown={(e) => e.stopPropagation()}
                                        sx={{
                                            backgroundColor: colors.chipBg,
                                            color: colors.chipColor,
                                            fontSize: '0.75rem',
                                        }}
                                    />
                                ))}
                            </Box>
                        )}
                        MenuProps={menuPaperSx}
                        sx={{
                            color: colors.textColor,
                            '& .MuiSvgIcon-root': { color: colors.iconColor },
                        }}
                    >
                        {param.allowedValues?.map((opt) => (
                            <MenuItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </MenuItem>
                        ))}
                    </Select>
                    {error && <FormHelperText>{error}</FormHelperText>}
                </FormControl>
            );
        }

        default: {
            // For any unknown widget with allowedValues, render MultiSelect
            if (param.allowedValues && param.allowedValues.length > 0) {
                const selected = value ? value.split('|').filter(Boolean) : [];
                return (
                    <FormControl fullWidth size="small" error={!!error} disabled={param.readOnly} sx={commonSx}>
                        <InputLabel sx={{ color: colors.labelColor, '&.Mui-focused': { color: colors.focusColor } }}>
                            {param.label}{param.required ? ' *' : ''}
                        </InputLabel>
                        <Select
                            multiple
                            value={selected}
                            onChange={(e) => {
                                const vals = typeof e.target.value === 'string'
                                    ? e.target.value.split(',')
                                    : (e.target.value as string[]);
                                handleChange(vals.join('|'));
                            }}
                            input={<OutlinedInput label={param.label + (param.required ? ' *' : '')} />}
                            renderValue={(sel) => (
                                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                    {(sel as string[]).map((v) => (
                                        <Chip
                                            key={v}
                                            label={param.allowedValues?.find(o => o.value === v)?.label ?? v}
                                            size="small"
                                            onDelete={() => {
                                                const newSelected = (sel as string[]).filter(item => item !== v);
                                                handleChange(newSelected.join('|'));
                                            }}
                                            onMouseDown={(e) => e.stopPropagation()}
                                            sx={{
                                                backgroundColor: colors.chipBg,
                                                color: colors.chipColor,
                                                fontSize: '0.75rem',
                                                '& .MuiChip-deleteIcon': {
                                                    color: colors.chipDeleteColor,
                                                    '&:hover': { color: colors.chipDeleteHover }
                                                }
                                            }}
                                        />
                                    ))}
                                </Box>
                            )}
                            MenuProps={menuPaperSx}
                            sx={{
                                color: colors.textColor,
                                '& .MuiSvgIcon-root': { color: colors.iconColor },
                            }}
                        >
                            {param.allowedValues?.map((opt) => (
                                <MenuItem key={opt.value} value={opt.value}>
                                    {opt.label}
                                </MenuItem>
                            ))}
                        </Select>
                        {error && <FormHelperText>{error}</FormHelperText>}
                    </FormControl>
                );
            }
            // True fallback: no allowed values
            return (
                <FormControl fullWidth size="small" error={!!error} disabled={param.readOnly} sx={commonSx}>
                    <InputLabel sx={{ color: colors.labelColor, '&.Mui-focused': { color: colors.focusColor } }}>
                        {param.label}{param.required ? ' *' : ''}
                    </InputLabel>
                    <Select
                        multiple
                        value={[]}
                        input={<OutlinedInput label={param.label + (param.required ? ' *' : '')} />}
                        renderValue={() => (
                            <Box sx={{ color: colors.placeholderColor, fontSize: '0.875rem' }}>No options available</Box>
                        )}
                        sx={{
                            color: colors.textColor,
                            '& .MuiSvgIcon-root': { color: colors.iconColor },
                        }}
                        MenuProps={{
                            PaperProps: {
                                sx: {
                                    backgroundColor: colors.menuBg,
                                    border: `1px solid ${colors.menuBorder}`,
                                },
                            },
                        }}
                    >
                        <MenuItem disabled value="">
                            <em style={{ color: colors.placeholderColor }}>No options loaded from data source</em>
                        </MenuItem>
                    </Select>
                    {error && <FormHelperText>{error}</FormHelperText>}
                </FormControl>
            );
        }
    }
};

export default FieldRenderer;
