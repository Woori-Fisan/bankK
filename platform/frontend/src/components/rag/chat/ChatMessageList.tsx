import React from 'react';
import UserMessage from './UserMessage';
import AssistantMessage from './AssistantMessage';

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
                msg.role === 'user' ? (
                    <UserMessage key={msg.id} content={msg.content} />
                ) : (
                    <AssistantMessage key={msg.id} content={msg.content} />
                )
            ))}
        </div>
    );
};

export default ChatMessageList;
