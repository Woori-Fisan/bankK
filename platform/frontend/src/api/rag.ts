import axios from 'axios';
import type { RagChatResponse } from '../types/rag';
import type { ApiResponse } from '../types/api';

const API_BASE_URL = '/api/rag';

export const ragApi = {
    ask: async (question: string): Promise<RagChatResponse> => {
        const response = await axios.get<ApiResponse<RagChatResponse>>(`${API_BASE_URL}/ask`, {
            params: { question }
        });
        
        if (response.data.success) {
            return response.data.data;
        } else {
            throw new Error(response.data.error?.message || 'RAG 질의 중 오류가 발생했습니다.');
        }
    }
};
