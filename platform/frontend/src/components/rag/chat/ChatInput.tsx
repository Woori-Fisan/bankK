import React from 'react';
import { Send } from 'lucide-react';

interface ChatInputProps {
    onSend?: (message: string) => void;
}

const ChatInput: React.FC<ChatInputProps> = ({ onSend }) => {
    const [inputValue, setInputValue] = React.useState('');

    const handleSend = () => {
        if (inputValue.trim() && onSend) {
            onSend(inputValue);
            setInputValue('');
        }
    };

    return (
        <div className="p-4 border-t border-gray-100 bg-white shrink-0">
            <div className="flex gap-2">
                <input 
                    type="text" 
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    placeholder="메시지를 입력하세요..."
                    className="flex-1 bg-gray-100 border-none rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-emerald-500 transition-all outline-none"
                    onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                            handleSend();
                        }
                    }}
                />
                <button 
                    onClick={handleSend}
                    className="bg-emerald-700 hover:bg-emerald-800 text-white p-2 rounded-xl transition-colors shadow-md"
                >
                    <Send className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
};

export default ChatInput;
