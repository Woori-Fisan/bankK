import React from 'react';
import { Bot } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import rehypeSanitize from 'rehype-sanitize';
import type { Schema } from 'hast-util-sanitize';

const CHAT_SANITIZE_SCHEMA: Schema = {
    tagNames: ['p', 'strong', 'em', 'del', 'br', 'hr',
               'ul', 'ol', 'li',
               'h1', 'h2', 'h3',
               'code', 'pre',
               'blockquote'],
    attributes: {
        code: ['className'],
    },
};

interface ChatMessageProps {
    role: 'user' | 'assistant';
    content: string;
    isLoading?: boolean;
}

const ChatMessage: React.FC<ChatMessageProps> = ({ role, content, isLoading }) => {
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
                {isLoading ? (
                    <div className="flex gap-1 py-1 px-2">
                        <div className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                        <div className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                        <div className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                    </div>
                ) : (
                    <div className="text-sm whitespace-pre-wrap break-words markdown-content">
                        <ReactMarkdown
                            rehypePlugins={[[rehypeSanitize, CHAT_SANITIZE_SCHEMA]]}
                            components={{
                                p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
                                ul: ({ children }) => <ul className="list-disc ml-4 mb-2">{children}</ul>,
                                ol: ({ children }) => <ol className="list-decimal ml-4 mb-2">{children}</ol>,
                                li: ({ children }) => <li className="mb-1">{children}</li>,
                                strong: ({ children }) => <strong className="font-bold">{children}</strong>,
                                code: ({ children }) => <code className="bg-gray-100 px-1 rounded text-emerald-700 font-mono text-xs">{children}</code>
                            }}
                        >
                            {content}
                        </ReactMarkdown>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ChatMessage;
