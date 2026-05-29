import React from 'react';
import { Bot } from 'lucide-react';

interface AssistantMessageProps {
    content: string;
}

const AssistantMessage: React.FC<AssistantMessageProps> = ({ content }) => {
    return (
        <div className="flex gap-2">
            <div className="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center flex-shrink-0">
                <Bot className="w-4 h-4 text-emerald-700" />
            </div>
            <div className="bg-white p-3 rounded-2xl rounded-tl-none shadow-sm border border-gray-100 max-w-[80%] text-gray-800">
                <p className="text-sm whitespace-pre-wrap">{content}</p>
            </div>
        </div>
    );
};

export default AssistantMessage;
