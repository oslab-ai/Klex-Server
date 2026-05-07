import { useState, useEffect } from 'react';
import { X, MessageSquare, Send, User } from 'lucide-react';
import Modal from './Modal';
import { jobsApi } from '../api';
import type { ScheduledJob, JobComment } from '../types';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    job: ScheduledJob | null;
}

export default function JobCommentModal({ isOpen, onClose, job }: Props) {
    const [comments, setComments] = useState<JobComment[]>([]);
    const [newComment, setNewComment] = useState('');
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (isOpen && job) {
            fetchComments();
        }
    }, [isOpen, job?.id]);

    const fetchComments = async () => {
        if (!job) return;
        setLoading(true);
        try {
            const result = await jobsApi.getComments(job.id);
            setComments(result.comments);
        } catch (err) {
            console.error('Failed to fetch comments:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async () => {
        if (!job || !newComment.trim()) return;
        setSubmitting(true);
        try {
            const comment = await jobsApi.addComment(job.id, newComment.trim());
            setComments(prev => [comment, ...prev]);
            setNewComment('');
        } catch (err) {
            console.error('Failed to add comment:', err);
        } finally {
            setSubmitting(false);
        }
    };

    if (!job) return null;

    return (
        <Modal isOpen={isOpen} onClose={onClose} maxWidth="max-w-lg">
            {/* Header */}
            <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-gray-900 dark:text-white flex items-center gap-2">
                    <MessageSquare size={18} className="text-primary-600" />
                    Comments
                </h2>
                <button onClick={onClose} className="btn-ghost p-2 rounded-lg"><X size={16} /></button>
            </div>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">{job.schedule_name}</p>

            {/* New comment input */}
            <div className="flex gap-2 mb-4">
                <input
                    type="text"
                    value={newComment}
                    onChange={e => setNewComment(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleSubmit()}
                    placeholder="Add a comment..."
                    className="input flex-1"
                    disabled={submitting}
                />
                <button
                    onClick={handleSubmit}
                    disabled={!newComment.trim() || submitting}
                    className="btn-primary px-3"
                >
                    <Send size={14} />
                </button>
            </div>

            {/* Comments list */}
            <div className="space-y-3 max-h-[400px] overflow-y-auto">
                {loading ? (
                    <div className="text-center py-8 text-gray-500">Loading comments...</div>
                ) : comments.length === 0 ? (
                    <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                        <MessageSquare size={32} className="mx-auto mb-2 opacity-40" />
                        <p>No comments yet</p>
                    </div>
                ) : (
                    comments.map(comment => (
                        <div key={comment.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-700/30">
                            <div className="flex items-center gap-2 mb-1.5">
                                <div className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                                    <User size={12} className="text-primary-600 dark:text-primary-400" />
                                </div>
                                <span className="text-sm font-semibold text-gray-800 dark:text-gray-200">
                                    {comment.user_name || 'Unknown'}
                                </span>
                                <span className="text-xs text-gray-400">
                                    {new Date(comment.created_at).toLocaleString()}
                                </span>
                            </div>
                            <p className="text-sm text-gray-700 dark:text-gray-300 ml-8">{comment.text}</p>
                        </div>
                    ))
                )}
            </div>

            <div className="flex justify-end mt-4 pt-3 border-t border-gray-200 dark:border-gray-700">
                <button onClick={onClose} className="btn-secondary">Close</button>
            </div>
        </Modal>
    );
}
