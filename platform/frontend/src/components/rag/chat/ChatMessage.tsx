import React from 'react';
import { Bot } from 'lucide-react';

interface ChatMessageProps {
    role: 'user' | 'assistant';
    content: string;
}

const ChatMessage: React.FC<ChatMessageProps> = ({ role, content }) => {
    const isAssistant = role === 'assistant';

    return (
        <div className={`flex gap-2 ${isAssistant ? '' : 'flex-row-reverse'}`}>
            {isAssistant && (
                <div className="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center flex-shrink-0">
                    <Bot className="w-4 h-4 text-emerald-700" />
                </div>
            )}
            <div className={`p-3 rounded-2xl shadow-sm border max-w-[80%] ${
                isAssistant 
                    ? 'bg-white rounded-tl-none border-gray-100 text-gray-800' 
                    : 'bg-emerald-600 rounded-tr-none border-emerald-500 text-white'
            }`}>
                <p className="text-sm whitespace-pre-wrap break-words">{content}</p>
            </div>
        </div>
    );
};

export default ChatMessage;
