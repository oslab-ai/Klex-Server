import api from './client';

export interface ChatMessage {
    role: 'user' | 'assistant' | 'system';
    content: string;
    action_result?: ActionResult | null;
    action_pending?: ActionPending | null;
}

export interface ActionResult {
    type: 'success' | 'error';
    action: string;
    details: Record<string, unknown>;
}

export interface ActionPending {
    action: string;
    summary: string;
    params: Record<string, unknown>;
}

export interface ChatResponse {
    role: 'assistant';
    content: string;
    navigation_target?: string | null;
    action_result?: ActionResult | null;
    action_pending?: ActionPending | null;
}

export const chatService = {
    /**
     * Send a list of messages to the backend chatbot
     */
    sendMessage: async (messages: ChatMessage[]): Promise<ChatResponse> => {
        const response = await api.post<ChatResponse>('/api/chat/', { messages });
        return response.data;
    }
};
