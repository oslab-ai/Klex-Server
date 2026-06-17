import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bot, X, Send, Loader2, CheckCircle2, XCircle, AlertTriangle, Download } from 'lucide-react';
import { chatService, ChatMessage, ActionResult } from '../../api';
import { useFormFill } from '../../context/FormFillContext';

/* ── Helper: render an action-result card ───────────────────────── */
const ActionResultCard: React.FC<{ result: ActionResult }> = ({ result }) => {
    const isSuccess = result.type === 'success';
    const details = result.details || {};

    return (
        <div
            className={`mt-2 rounded-xl border px-3 py-2.5 text-xs ${
                isSuccess
                    ? 'border-emerald-300 dark:border-emerald-700 bg-emerald-50 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300'
                    : 'border-red-300 dark:border-red-700 bg-red-50 dark:bg-red-900/30 text-red-800 dark:text-red-300'
            }`}
        >
            <div className="flex items-center gap-1.5 font-semibold mb-1">
                {isSuccess ? (
                    <CheckCircle2 className="w-3.5 h-3.5" />
                ) : (
                    <XCircle className="w-3.5 h-3.5" />
                )}
                <span className="capitalize">
                    {result.action.replace(/_/g, ' ')} — {isSuccess ? 'Success' : 'Error'}
                </span>
            </div>

            {/* Render key details */}
            <div className="space-y-0.5 text-[11px] opacity-90">
                {!!details.report_name && <div>Report: <span className="font-medium">{String(details.report_name)}</span></div>}
                {!!details.schedule_name && <div>Schedule: <span className="font-medium">{String(details.schedule_name)}</span></div>}
                {!!details.format && <div>Format: <span className="font-medium uppercase">{String(details.format)}</span></div>}
                {!!details.cron && <div>Cron: <span className="font-mono font-medium">{String(details.cron)}</span></div>}
                {!!details.department && <div>Department: <span className="font-medium">{String(details.department)}</span></div>}
                {!!details.max_runs && <div>Max runs: <span className="font-medium">{String(details.max_runs)}</span></div>}
                {!!details.adapter_name && <div>Adapter: <span className="font-medium">{String(details.adapter_name)}</span></div>}
                {!!details.new_status && <div>Status: <span className="font-medium">{String(details.new_status)}</span></div>}
                {!!details.error && <div className="text-red-600 dark:text-red-400">{String(details.error)}</div>}
                {!!details.download_url && (
                    <a
                        href={details.download_url as string}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 mt-1 text-indigo-600 dark:text-indigo-400 hover:underline font-medium"
                    >
                        <Download className="w-3 h-3" /> Download Report
                    </a>
                )}
            </div>
        </div>
    );
};

/* ── Helper: confirmation card for destructive actions ──────────── */
const ConfirmationCard: React.FC<{
    summary: string;
    onConfirm: () => void;
    onCancel: () => void;
    disabled: boolean;
}> = ({ summary, onConfirm, onCancel, disabled }) => (
    <div className="mt-2 rounded-xl border border-amber-300 dark:border-amber-700 bg-amber-50 dark:bg-amber-900/30 px-3 py-2.5 text-xs text-amber-800 dark:text-amber-300">
        <div className="flex items-center gap-1.5 font-semibold mb-1">
            <AlertTriangle className="w-3.5 h-3.5" />
            <span>Confirm Action</span>
        </div>
        <p className="text-[11px] mb-2 opacity-90">{summary}</p>
        <div className="flex gap-2">
            <button
                onClick={onConfirm}
                disabled={disabled}
                className="px-3 py-1 rounded-lg bg-indigo-600 text-white text-[11px] font-medium hover:bg-indigo-700 disabled:opacity-50 transition-colors"
            >
                Confirm
            </button>
            <button
                onClick={onCancel}
                disabled={disabled}
                className="px-3 py-1 rounded-lg bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200 text-[11px] font-medium hover:bg-slate-300 dark:hover:bg-slate-600 disabled:opacity-50 transition-colors"
            >
                Cancel
            </button>
        </div>
    </div>
);

/* ── Main ChatbotWidget ─────────────────────────────────────────── */
export const ChatbotWidget: React.FC = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();
    const { setFormFillState } = useFormFill();

    // Scroll to bottom when messages change
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const sendMessageToBackend = async (outboundMessages: ChatMessage[]) => {
        setIsLoading(true);
        try {
            // Strip action_result/action_pending from messages sent to backend
            const cleanMessages = outboundMessages.map(m => ({
                role: m.role,
                content: m.content,
            }));

            const response = await chatService.sendMessage(cleanMessages);
            
            // Build assistant message with optional action data
            const assistantMessage: ChatMessage = {
                role: 'assistant',
                content: response.content,
            };
            if (response.action_result) {
                assistantMessage.action_result = response.action_result;
            }
            if (response.action_pending) {
                assistantMessage.action_pending = response.action_pending;
            }
            if (response.form_fill) {
                assistantMessage.form_fill = response.form_fill;
                setFormFillState({
                    reportId: response.form_fill.report_id,
                    parameters: response.form_fill.parameters,
                    timestamp: Date.now()
                });
            }

            setMessages(prev => [...prev, assistantMessage]);

            // Handle navigation if requested by the LLM
            if (response.navigation_target) {
                setTimeout(() => {
                    navigate(response.navigation_target!);
                }, 1000);
            }
            
        } catch (error) {
            console.error('Chat error:', error);
            setMessages(prev => [...prev, {
                role: 'assistant',
                content: 'Sorry, I encountered an error. Please try again later.'
            }]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSendMessage = async () => {
        if (!input.trim()) return;

        const userMessage: ChatMessage = { role: 'user', content: input.trim() };
        const updatedMessages = [...messages, userMessage];
        
        setMessages(updatedMessages);
        setInput('');

        await sendMessageToBackend(updatedMessages);
    };

    const handleConfirm = async () => {
        const confirmMessage: ChatMessage = { role: 'user', content: 'Yes, confirm and proceed.' };
        const updatedMessages = [...messages, confirmMessage];
        setMessages(updatedMessages);
        await sendMessageToBackend(updatedMessages);
    };

    const handleCancel = async () => {
        const cancelMessage: ChatMessage = { role: 'user', content: 'Cancel that, never mind.' };
        const updatedMessages = [...messages, cancelMessage];
        setMessages(updatedMessages);
        await sendMessageToBackend(updatedMessages);
    };

    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            handleSendMessage();
        }
    };

    return (
        <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end">
            {/* Chat Window */}
            {isOpen && (
                <div className="mb-4 w-80 sm:w-96 h-[500px] max-h-[70vh] bg-white/90 dark:bg-slate-900/90 backdrop-blur-xl border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden transition-all duration-300 ease-in-out transform origin-bottom-right">
                    {/* Header */}
                    <div className="flex items-center justify-between p-4 bg-indigo-600 dark:bg-indigo-700 text-white shadow-md">
                        <div className="flex items-center space-x-2">
                            <Bot className="w-5 h-5" />
                            <h3 className="font-semibold">Klex Assistant</h3>
                        </div>
                        <button 
                            onClick={() => setIsOpen(false)}
                            className="text-white/80 hover:text-white transition-colors p-1 rounded-full hover:bg-white/10"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Messages Area */}
                    <div className="flex-1 overflow-y-auto p-4 space-y-4">
                        {messages.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-full text-slate-500 dark:text-slate-400 space-y-3">
                                <Bot className="w-12 h-12 opacity-50" />
                                <p className="text-sm text-center">
                                    Hi! I'm your Klex assistant. <br/>
                                    I can open reports, compile them, <br/>
                                    schedule jobs, and manage adapters. <br/>
                                    <span className="text-xs opacity-70 mt-1 block">
                                        Try: "Run the monthly sales report as Excel"
                                    </span>
                                </p>
                            </div>
                        ) : (
                            messages.map((msg, idx) => (
                                <div 
                                    key={idx} 
                                    className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                                >
                                    <div 
                                        className={`max-w-[85%] rounded-2xl px-4 py-2 text-sm shadow-sm ${
                                            msg.role === 'user' 
                                                ? 'bg-indigo-600 text-white rounded-br-none' 
                                                : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 rounded-bl-none border border-slate-200 dark:border-slate-700'
                                        }`}
                                    >
                                        {msg.content}

                                        {/* Action result card */}
                                        {msg.action_result && (
                                            <ActionResultCard result={msg.action_result} />
                                        )}

                                        {/* Confirmation card */}
                                        {msg.action_pending && (
                                            <ConfirmationCard
                                                summary={msg.action_pending.summary}
                                                onConfirm={handleConfirm}
                                                onCancel={handleCancel}
                                                disabled={isLoading}
                                            />
                                        )}

                                        {/* Form Fill card */}
                                        {msg.form_fill && (
                                            <div className="mt-2 rounded-xl border border-blue-300 dark:border-blue-700 bg-blue-50 dark:bg-blue-900/30 px-3 py-2.5 text-xs text-blue-800 dark:text-blue-300">
                                                <div className="flex items-center gap-1.5 font-semibold mb-1">
                                                    <CheckCircle2 className="w-3.5 h-3.5" />
                                                    <span>Form Filled</span>
                                                </div>
                                                <div className="space-y-0.5 text-[11px] opacity-90">
                                                    {Object.entries(msg.form_fill.parameters).map(([key, val]) => (
                                                        <div key={key}>
                                                            <span className="font-medium">{key}:</span> {String(val)}
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))
                        )}
                        {isLoading && (
                            <div className="flex justify-start">
                                <div className="bg-slate-100 dark:bg-slate-800 rounded-2xl rounded-bl-none px-4 py-3 border border-slate-200 dark:border-slate-700">
                                    <Loader2 className="w-4 h-4 animate-spin text-indigo-500" />
                                </div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Input Area */}
                    <div className="p-3 border-t border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 backdrop-blur-sm">
                        <div className="relative flex items-center">
                            <input 
                                type="text"
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                onKeyDown={handleKeyPress}
                                placeholder="Ask me anything — reports, schedules, adapters..."
                                disabled={isLoading}
                                className="w-full pl-4 pr-12 py-3 rounded-full bg-slate-100 dark:bg-slate-800 border-none text-sm focus:ring-2 focus:ring-indigo-500 dark:text-slate-200 disabled:opacity-50"
                            />
                            <button
                                onClick={handleSendMessage}
                                disabled={!input.trim() || isLoading}
                                className="absolute right-2 p-1.5 bg-indigo-600 text-white rounded-full hover:bg-indigo-700 disabled:opacity-50 disabled:hover:bg-indigo-600 transition-colors"
                            >
                                <Send className="w-4 h-4" />
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Toggle Button */}
            {!isOpen && (
                <button
                    onClick={() => setIsOpen(true)}
                    className="w-14 h-14 bg-indigo-600 hover:bg-indigo-700 text-white rounded-full shadow-lg shadow-indigo-500/30 flex items-center justify-center transition-transform hover:scale-105 active:scale-95 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:focus:ring-offset-slate-900"
                >
                    <Bot className="w-7 h-7" />
                </button>
            )}
        </div>
    );
};
