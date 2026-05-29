import React, { useState } from 'react';
import ChatHeader from './chat/ChatHeader';
import ChatMessageList from './chat/ChatMessageList';
import ChatInput from './chat/ChatInput';

interface ChatWindowProps {
    onClose: () => void;
    isClosing: boolean;
}

const ChatWindow: React.FC<ChatWindowProps> = ({ onClose, isClosing }) => {
    // 테스트용 초기 메시지 상태
    const [messages] = useState([
        { id: '1', role: 'assistant' as const, content: '안녕하세요! 무엇을 도와드릴까요?' },
        { id: '2', role: 'user' as const, content: '오늘의 대출 금리 정보를 알고 싶어요.' }
    ]);

    return (
        <div className={`fixed bottom-8 right-8 w-96 h-125 bg-white rounded-2xl shadow-2xl flex flex-col border border-gray-200 z-50 overflow-hidden origin-bottom-right ${
            isClosing ? 'animate-scale-out' : 'animate-scale-in'
        }`}>
            <ChatHeader onClose={onClose} />
            
            <ChatMessageList messages={messages} />

            <ChatInput onSend={(msg) => console.log('Send message:', msg)} />
        </div>
    );
};

export default ChatWindow;
