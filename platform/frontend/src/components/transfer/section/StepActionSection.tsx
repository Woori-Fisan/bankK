import React from 'react';

interface Props {
    onPrev?: () => void;
    onNext?: () => void;
    prevLabel?: string;
    nextLabel?: string;
    nextDisabled?: boolean;
    fullWidth?: boolean;
}

const StepActionSection: React.FC<Props> = ({
    onPrev, onNext, prevLabel = '이전 단계', nextLabel = '다음 단계', nextDisabled, fullWidth = true
}) => (
    <div className={`pt-6 ${fullWidth ? 'w-full grid grid-cols-2 gap-4' : 'flex gap-4'}`}>
        {onPrev && (
            <button
                onClick={onPrev}
                className="py-4 border border-gray-200 text-gray-600 rounded-xl font-bold hover:bg-gray-50 transition-colors"
            >
                {prevLabel}
            </button>
        )}
        {onNext && (
            <button
                onClick={onNext}
                disabled={nextDisabled}
                className={`py-4 bg-[#065f46] text-white rounded-xl font-bold hover:bg-[#044e3a] transition-all shadow-lg shadow-emerald-900/10 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 ${!onPrev ? 'col-span-2' : ''}`}
            >
                {nextLabel}
            </button>
        )}
    </div>
);

export default StepActionSection;
