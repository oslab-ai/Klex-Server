import { useState, useEffect } from 'react';
import Modal from './Modal';
import type { ReportGroup } from '../types';

const GROUP_COLORS = [
    { key: 'violet', bg: 'bg-violet-500', ring: 'ring-violet-400', banner: 'from-violet-500/20 to-violet-600/10', border: 'border-violet-500/30', text: 'text-violet-400', badge: 'bg-violet-500/20 text-violet-300' },
    { key: 'blue', bg: 'bg-blue-500', ring: 'ring-blue-400', banner: 'from-blue-500/20 to-blue-600/10', border: 'border-blue-500/30', text: 'text-blue-400', badge: 'bg-blue-500/20 text-blue-300' },
    { key: 'emerald', bg: 'bg-emerald-500', ring: 'ring-emerald-400', banner: 'from-emerald-500/20 to-emerald-600/10', border: 'border-emerald-500/30', text: 'text-emerald-400', badge: 'bg-emerald-500/20 text-emerald-300' },
    { key: 'amber', bg: 'bg-amber-500', ring: 'ring-amber-400', banner: 'from-amber-500/20 to-amber-600/10', border: 'border-amber-500/30', text: 'text-amber-400', badge: 'bg-amber-500/20 text-amber-300' },
    { key: 'rose', bg: 'bg-rose-500', ring: 'ring-rose-400', banner: 'from-rose-500/20 to-rose-600/10', border: 'border-rose-500/30', text: 'text-rose-400', badge: 'bg-rose-500/20 text-rose-300' },
    { key: 'cyan', bg: 'bg-cyan-500', ring: 'ring-cyan-400', banner: 'from-cyan-500/20 to-cyan-600/10', border: 'border-cyan-500/30', text: 'text-cyan-400', badge: 'bg-cyan-500/20 text-cyan-300' },
    { key: 'pink', bg: 'bg-pink-500', ring: 'ring-pink-400', banner: 'from-pink-500/20 to-pink-600/10', border: 'border-pink-500/30', text: 'text-pink-400', badge: 'bg-pink-500/20 text-pink-300' },
    { key: 'orange', bg: 'bg-orange-500', ring: 'ring-orange-400', banner: 'from-orange-500/20 to-orange-600/10', border: 'border-orange-500/30', text: 'text-orange-400', badge: 'bg-orange-500/20 text-orange-300' },
];

export function getGroupColorConfig(colorKey: string) {
    return GROUP_COLORS.find(c => c.key === colorKey) || GROUP_COLORS[0];
}

export { GROUP_COLORS };

export interface GroupFormData {
    name: string;
    color: string;
    description: string;
}

interface ReportGroupModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (data: GroupFormData) => void;
    editingGroup?: ReportGroup | null;
    saving?: boolean;
}

export default function ReportGroupModal({ isOpen, onClose, onSave, editingGroup, saving }: ReportGroupModalProps) {
    const [name, setName] = useState('');
    const [color, setColor] = useState('violet');
    const [description, setDescription] = useState('');

    useEffect(() => {
        if (editingGroup) {
            setName(editingGroup.name);
            setColor(editingGroup.color);
            setDescription(editingGroup.description || '');
        } else {
            setName('');
            setColor('violet');
            setDescription('');
        }
    }, [editingGroup, isOpen]);

    const handleSave = () => {
        if (!name.trim() || saving) return;
        onSave({ name: name.trim(), color, description: description.trim() });
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose}>
            <div className="space-y-5">
                {/* Header */}
                <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-xl ${getGroupColorConfig(color).bg} flex items-center justify-center shadow-lg`}>
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                        </svg>
                    </div>
                    <div>
                        <h2 className="text-lg font-bold">{editingGroup ? 'Edit Group' : 'Create Group'}</h2>
                        <p className="text-xs text-gray-500 dark:text-gray-400">Organize your reports into folders</p>
                    </div>
                </div>

                {/* Group Name */}
                <div>
                    <label className="block text-sm font-semibold mb-1.5">Group Name</label>
                    <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="input"
                        placeholder="e.g. Finance Reports, HR, Monthly..."
                        autoFocus
                        onKeyDown={(e) => { if (e.key === 'Enter') handleSave(); }}
                    />
                </div>

                {/* Description */}
                <div>
                    <label className="block text-sm font-semibold mb-1.5">Description</label>
                    <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        className="input"
                        rows={2}
                        placeholder="Optional description for this group..."
                    />
                </div>

                {/* Color Picker */}
                <div>
                    <label className="block text-sm font-semibold mb-2">Color</label>
                    <div className="flex items-center gap-2.5 flex-wrap">
                        {GROUP_COLORS.map((c) => (
                            <button
                                key={c.key}
                                onClick={() => setColor(c.key)}
                                className={`w-8 h-8 rounded-full ${c.bg} transition-all duration-200 hover:scale-110 ${
                                    color === c.key
                                        ? `ring-2 ${c.ring} ring-offset-2 ring-offset-white dark:ring-offset-gray-800 scale-110`
                                        : 'opacity-60 hover:opacity-100'
                                }`}
                                title={c.key}
                            />
                        ))}
                    </div>
                </div>

                {/* Preview */}
                <div className={`rounded-xl border ${getGroupColorConfig(color).border} bg-gradient-to-r ${getGroupColorConfig(color).banner} p-3`}>
                    <div className="flex items-center gap-2">
                        <div className={`w-2.5 h-2.5 rounded-full ${getGroupColorConfig(color).bg}`} />
                        <span className={`text-sm font-semibold ${getGroupColorConfig(color).text}`}>
                            {name || 'Group Preview'}
                        </span>
                        <span className={`text-xs px-1.5 py-0.5 rounded-md ${getGroupColorConfig(color).badge}`}>
                            {editingGroup ? editingGroup.report_count : 0} reports
                        </span>
                    </div>
                </div>

                {/* Actions */}
                <div className="flex gap-3 pt-1">
                    <button onClick={onClose} className="btn-secondary flex-1">Cancel</button>
                    <button
                        onClick={handleSave}
                        disabled={!name.trim() || saving}
                        className="btn-primary flex-1 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {saving ? 'Saving...' : editingGroup ? 'Save Changes' : 'Create Group'}
                    </button>
                </div>
            </div>
        </Modal>
    );
}
