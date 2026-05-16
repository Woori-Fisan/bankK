import React from 'react';
import { LogOut } from 'lucide-react';

const FloatingButton: React.FC = () => {
    return (
        <button className="fixed bottom-8 right-8 w-14 h-14 bg-emerald-700 hover:bg-emerald-800 rounded-full flex items-center justify-center shadow-lg transition-colors">
            <LogOut className="w-6 h-6 text-white" />
        </button>
    );
};

export default FloatingButton;
