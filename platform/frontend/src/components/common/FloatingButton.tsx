import React from 'react';
import { Bot } from 'lucide-react';

const FloatingButton: React.FC = () => {
    return (
        <button className="fixed bottom-8 right-8 w-14 h-14 bg-emerald-700 hover:bg-emerald-800 rounded-full flex items-center justify-center shadow-lg transition-colors">
            <Bot className="w-7 h-7 text-white" />
        </button>
    );
};

export default FloatingButton;
