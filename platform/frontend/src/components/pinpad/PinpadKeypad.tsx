import React, { useEffect, useState } from 'react';
import { Delete } from 'lucide-react';

interface PinpadKeypadProps {
    onKeyClick: (key: string) => void;
    onDelete: () => void;
    onReset: () => void;
}

const PinpadKeypad: React.FC<PinpadKeypadProps> = ({ onKeyClick, onDelete, onReset }) => {
    const [keys, setKeys] = useState<string[]>([]);

    const shuffleKeys = () => {
        const numbers = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
        const shuffled = numbers.sort(() => Math.random() - 0.5);
        setKeys(shuffled);
    };

    useEffect(() => {
        shuffleKeys();
    }, []);

    return (
        <div className="grid grid-cols-3 gap-3">
            {keys.map((key) => (
                <button
                    key={key}
                    onClick={() => {
                        onKeyClick(key);
                        shuffleKeys();
                    }}
                    className="h-14 bg-slate-50 hover:bg-slate-100 text-slate-900 font-bold rounded-xl text-xl transition-colors active:scale-95 flex items-center justify-center border border-slate-100"
                >
                    {key}
                </button>
            ))}
            <button
                onClick={onReset}
                className="h-14 bg-slate-50 hover:bg-slate-100 text-slate-500 font-medium rounded-xl text-sm transition-colors active:scale-95 flex items-center justify-center border border-slate-100"
            >
                초기화
            </button>
            <button
                onClick={onDelete}
                className="h-14 bg-slate-50 hover:bg-slate-100 text-slate-500 rounded-xl transition-colors active:scale-95 flex items-center justify-center border border-slate-100"
            >
                <Delete className="w-6 h-6" />
            </button>
        </div>
    );
};

export default PinpadKeypad;
