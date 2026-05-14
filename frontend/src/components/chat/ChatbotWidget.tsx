import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bot, X, Send, Loader2 } from 'lucide-react';
import { chatService, ChatMessage } from '../../api';

export const ChatbotWidget: React.FC = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();

    // Scroll to bottom when messages change
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleSendMessage = async () => {
        if (!input.trim()) return;

        const userMessage: ChatMessage = { role: 'user', content: input.trim() };
        const updatedMessages = [...messages, userMessage];
        
        setMessages(updatedMessages);
        setInput('');
        setIsLoading(true);

        try {
            const response = await chatService.sendMessage(updatedMessages);
            
            // Add assistant response to chat
            setMessages(prev => [...prev, {
                role: 'assistant',
                content: response.content
            }]);

            // Handle navigation if requested by the LLM
            if (response.navigation_target) {
                setTimeout(() => {
                    navigate(response.navigation_target!);
                }, 1000); // Small delay to let user read the "Navigating..." message
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
                                    Ask me to open any report or page! <br/>
                                    e.g., "Open Monthly Sales report"
                                </p>
                            </div>
                        ) : (
                            messages.map((msg, idx) => (
                                <div 
                                    key={idx} 
                                    className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                                >
                                    <div 
                                        className={`max-w-[80%] rounded-2xl px-4 py-2 text-sm shadow-sm ${
                                            msg.role === 'user' 
                                                ? 'bg-indigo-600 text-white rounded-br-none' 
                                                : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 rounded-bl-none border border-slate-200 dark:border-slate-700'
                                        }`}
                                    >
                                        {msg.content}
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
                                placeholder="Type a message..."
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
