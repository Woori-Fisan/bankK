import React, { useEffect, useRef } from 'react';
import ChatMessage from './ChatMessage';

interface Message {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    isLoading?: boolean;
}

interface ChatMessageListProps {
    messages: Message[];
}

const ChatMessageList: React.FC<ChatMessageListProps> = ({ messages }) => {
    const scrollRef = useRef<HTMLDivElement>(null);

    // 메시지 목록이 변경될 때마다 하단으로 스크롤
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [messages]);

    return (
        <div 
            ref={scrollRef}
            className="flex-1 p-4 overflow-y-auto bg-gray-50 flex flex-col gap-4 
            scrollbar-thin scrollbar-thumb-emerald-200 scrollbar-track-transparent hover:scrollbar-thumb-emerald-300"
        >
            {messages.map((msg) => (
                <ChatMessage key={msg.id} role={msg.role} content={msg.content} isLoading={msg.isLoading} />
            ))}
        </div>
    );
};

export default ChatMessageList;
