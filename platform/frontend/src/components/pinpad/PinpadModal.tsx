import React, { useState } from 'react';
import { X } from 'lucide-react';
import PinpadDisplay from './PinpadDisplay';
import PinpadKeypad from './PinpadKeypad';

interface PinpadModalProps {
    isOpen: boolean;
    onClose: () => void;
    onComplete: (pin: string) => void;
    title?: string;
}

const PinpadModal: React.FC<PinpadModalProps> = ({
    isOpen,
    onClose,
    onComplete,
    title = "비밀번호 입력"
}) => {
    const [pin, setPin] = useState<string>('');
    const MAX_LENGTH = 4;

    if (!isOpen) return null;

    const handleKeyClick = (key: string) => {
        if (pin.length < MAX_LENGTH) {
            const newPin = pin + key;
            setPin(newPin);
            if (newPin.length === MAX_LENGTH) {
                setTimeout(() => {
                    onComplete(newPin);
                    setPin('');
                }, 200);
            }
        }
    };

    const handleDelete = () => {
        setPin(prev => prev.slice(0, -1));
    };

    const handleReset = () => {
        setPin('');
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
            <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm overflow-hidden animate-in fade-in zoom-in duration-200">
                {/* Header */}
                <div className="p-6 border-b border-slate-100 flex items-center justify-between">
                    <h3 className="text-lg font-bold text-slate-900">{title}</h3>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
                        <X className="w-6 h-6" />
                    </button>
                </div>

                {/* Content */}
                <div className="p-8">
                    <PinpadDisplay length={pin.length} maxLength={MAX_LENGTH} />
                    
                    <PinpadKeypad 
                        onKeyClick={handleKeyClick} 
                        onDelete={handleDelete} 
                        onReset={handleReset} 
                    />
                </div>

                {/* Footer Tips */}
                <div className="bg-slate-50 p-4 text-center">
                    <p className="text-[11px] text-slate-400">
                        비밀번호 노출에 유의하시기 바랍니다.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default PinpadModal;
