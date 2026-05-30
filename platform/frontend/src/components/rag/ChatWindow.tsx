import React, { useState } from 'react';
import ChatHeader from './chat/ChatHeader';
import ChatMessageList from './chat/ChatMessageList';
import ChatInput from './chat/ChatInput';
import { ragApi } from '../../api/rag';
import { useRagStore } from '../../store/ragStore';
import type { Message } from '../../store/ragStore';

interface ChatWindowProps {
    onClose: () => void;
    isClosing: boolean;
    onAnimationEnd?: () => void;
}

const ChatWindow: React.FC<ChatWindowProps> = ({ onClose, isClosing, onAnimationEnd }) => {
    const { messages, addMessage, updateMessage, removeMessage, clearMessages } = useRagStore();
    const [isTyping, setIsTyping] = useState(false);

    const handleSend = async (content: string) => {
        if (!content.trim()) return;

        // 1. 사용자 메시지 추가
        const userMsg: Message = { id: Date.now().toString(), role: 'user', content };
        addMessage(userMsg);

        // 2. 로딩 메시지 추가
        setIsTyping(true);
        const loadingId = (Date.now() + 1).toString();
        addMessage({ id: loadingId, role: 'assistant', content: '', isLoading: true });

        try {
            // 3. API 호출
            const response = await ragApi.ask(content);
            
            // 4. 로딩 메시지 제거 및 실제 답변 추가 (타이핑 효과를 위해 빈 메시지로 시작)
            removeMessage(loadingId);
            
            const assistantMsgId = Date.now().toString();
            addMessage({ id: assistantMsgId, role: 'assistant', content: '' });
            
            // 5. 타이핑 효과 구현
            let currentText = '';
            const fullText = response.answer || '답변을 생성할 수 없습니다.';
            let index = 0;

            const timer = setInterval(() => {
                if (index < fullText.length) {
                    currentText += fullText[index];
                    updateMessage(assistantMsgId, { content: currentText });
                    index++;
                } else {
                    clearInterval(timer);
                    setIsTyping(false);
                }
            }, 30); // 타이핑 속도 조절

        } catch (error) {
            console.error('RAG API Error:', error);
            removeMessage(loadingId);
            addMessage({ 
                id: Date.now().toString(), 
                role: 'assistant', 
                content: '죄송합니다. 답변을 가져오는 중 오류가 발생했습니다.' 
            });
            setIsTyping(false);
        }
    };

    return (
        <div 
            className={`fixed bottom-8 right-8 w-96 h-[500px] bg-white rounded-2xl shadow-2xl flex flex-col border border-gray-200 z-50 overflow-hidden origin-bottom-right ${
                isClosing ? 'animate-scale-out' : 'animate-scale-in'
            }`}
            onAnimationEnd={() => {
                if (isClosing && onAnimationEnd) {
                    onAnimationEnd();
                }
            }}
        >
            <ChatHeader onClose={onClose} onReset={clearMessages} />
            
            <ChatMessageList messages={messages} />

            <ChatInput onSend={handleSend} disabled={isTyping} />
        </div>
    );
};

export default ChatWindow;
