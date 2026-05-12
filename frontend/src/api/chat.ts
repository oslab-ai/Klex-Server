import api from './client';

export interface ChatMessage {
    role: 'user' | 'assistant' | 'system';
    content: string;
}

export interface ChatResponse {
    role: 'assistant';
    content: string;
    navigation_target?: string | null;
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
