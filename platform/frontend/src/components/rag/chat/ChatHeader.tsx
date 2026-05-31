import React from 'react';
import { X, Bot, RotateCcw } from 'lucide-react';

interface ChatHeaderProps {
    onClose: () => void;
    onReset?: () => void;
}

const ChatHeader: React.FC<ChatHeaderProps> = ({ onClose, onReset }) => {
    return (
        <div className="bg-emerald-700 p-4 flex items-center justify-between text-white shrink-0">
            <div className="flex items-center gap-2">
                <div className="bg-white/20 p-1.5 rounded-lg">
                    <Bot className="w-5 h-5" />
                </div>
                <div>
                    <h3 className="font-semibold text-sm">금융 업무 보조 AI</h3>
                    <p className="text-[10px] text-emerald-100">실시간 상담 및 업무 지원</p>
                </div>
            </div>
            <div className="flex items-center gap-1">
                {onReset && (
                    <button 
                        onClick={onReset}
                        title="대화 초기화"
                        className="hover:bg-white/10 p-1 rounded-md transition-colors"
                    >
                        <RotateCcw className="w-4 h-4 text-emerald-100" />
                    </button>
                )}
                <button 
                    onClick={onClose}
                    className="hover:bg-white/10 p-1 rounded-md transition-colors"
                >
                    <X className="w-5 h-5" />
                </button>
            </div>
        </div>
    );
};

export default ChatHeader;
