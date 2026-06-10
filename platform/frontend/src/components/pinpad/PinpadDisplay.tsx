import React from 'react';

interface PinpadDisplayProps {
    length: number;
    maxLength: number;
}

const PinpadDisplay: React.FC<PinpadDisplayProps> = ({ length, maxLength }) => {
    return (
        <div className="flex justify-center gap-4 mb-8">
            {Array.from({ length: maxLength }).map((_, i) => (
                <div
                    key={i}
                    className={`w-4 h-4 rounded-full border-2 transition-all duration-200 ${
                        i < length
                            ? 'bg-emerald-600 border-emerald-600 scale-110'
                            : 'bg-transparent border-slate-300'
                    }`}
                />
            ))}
        </div>
    );
};

export default PinpadDisplay;
