import { create } from 'zustand';

export interface Message {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    isLoading?: boolean;
}

interface RagStore {
    messages: Message[];
    addMessage: (message: Message) => void;
    updateMessage: (id: string, updates: Partial<Message>) => void;
    removeMessage: (id: string) => void;
    clearMessages: () => void;
}

const INITIAL_MESSAGE: Message = {
    id: '1',
    role: 'assistant',
    content: '안녕하세요! 은행 업무 매뉴얼에 대해 궁금한 점을 물어보세요.'
};

export const useRagStore = create<RagStore>((set) => ({
    messages: [INITIAL_MESSAGE],
    addMessage: (message) => set((state) => ({ 
        messages: [...state.messages, message] 
    })),
    updateMessage: (id, updates) => set((state) => ({
        messages: state.messages.map((m) => m.id === id ? { ...m, ...updates } : m)
    })),
    removeMessage: (id) => set((state) => ({
        messages: state.messages.filter((m) => m.id !== id)
    })),
    clearMessages: () => set({ messages: [INITIAL_MESSAGE] })
}));
