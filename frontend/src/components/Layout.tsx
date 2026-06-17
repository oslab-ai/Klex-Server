import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import { ChatbotWidget } from './chat/ChatbotWidget';

const SIDEBAR_KEY = 'klex_sidebar_collapsed';

export default function Layout() {
    const [collapsed, setCollapsed] = useState(() => {
        return localStorage.getItem(SIDEBAR_KEY) === 'true';
    });

    // Listen for sidebar collapse changes
    useEffect(() => {
        const handleStorage = () => {
            setCollapsed(localStorage.getItem(SIDEBAR_KEY) === 'true');
        };
        // Poll for changes (since sidebar updates localStorage)
        const interval = setInterval(handleStorage, 200);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="min-h-screen bg-surface-50 dark:bg-surface-900">
            <Sidebar />
            <main
                className="transition-all duration-300 ease-in-out min-h-screen"
                style={{ marginLeft: collapsed ? 72 : 260 }}
            >
                <div className="px-8 py-6">
                    <Outlet />
                </div>
            </main>
            <ChatbotWidget />
        </div>
    );
}
