import React from 'react';
import ChatMessage from './ChatMessage';

interface Message {
    id: string;
    role: 'user' | 'assistant';
    content: string;
}

interface ChatMessageListProps {
    messages: Message[];
}

const ChatMessageList: React.FC<ChatMessageListProps> = ({ messages }) => {
    return (
        <div className="flex-1 p-4 overflow-y-auto bg-gray-50 flex flex-col gap-4">
            {messages.map((msg) => (
                <ChatMessage key={msg.id} role={msg.role} content={msg.content} />
            ))}
        </div>
    );
};

export default ChatMessageList;
