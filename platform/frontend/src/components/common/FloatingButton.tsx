import React from 'react';
import { Bot } from 'lucide-react';

interface FloatingButtonProps {
    onClick: () => void;
}

const FloatingButton: React.FC<FloatingButtonProps> = ({ onClick }) => {
    return (
        <button 
            onClick={onClick}
            className="fixed bottom-8 right-8 w-14 h-14 bg-emerald-700 hover:bg-emerald-800 rounded-full flex items-center justify-center shadow-lg transition-colors z-50"
        >
            <Bot className="w-7 h-7 text-white" />
        </button>
    );
};

export default FloatingButton;
