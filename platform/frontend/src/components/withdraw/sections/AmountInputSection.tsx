import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface AmountInputSectionProps {
    amount: string;
    onAmountChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onQuickAmountAdd: (value: number) => void;
    onAllIn: () => void;
    disabled?: boolean;
}

const AmountInputSection: React.FC<AmountInputSectionProps> = ({
    amount,
    onAmountChange,
    onQuickAmountAdd,
    onAllIn,
    disabled = false,
}) => {
    return (
        <section className={`space-y-4 transition-opacity duration-300 ${disabled ? 'opacity-50' : 'opacity-100'}`}>
            <h3 className="text-sm font-medium text-gray-500">이체 금액</h3>
            <div className="relative group">
                <input
                    type="text"
                    value={amount === '0' || !amount ? '' : formatAmount(amount)}
                    onChange={onAmountChange}
                    disabled={disabled}
                    className={`w-full px-6 py-8 text-4xl font-extrabold text-right border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 pr-16 transition-all ${
                        disabled ? 'bg-gray-50 cursor-not-allowed' : 'bg-white'
                    }`}
                    placeholder="0"
                />
                <span className="absolute right-6 top-1/2 -translate-y-1/2 text-2xl font-bold text-gray-300">₩</span>
            </div>
            <div className="flex flex-wrap gap-2">
                {[100, 500, 1000].map((val) => (
                    <button
                        key={val}
                        type="button"
                        onClick={() => onQuickAmountAdd(val * 10000)}
                        disabled={disabled}
                        className={`flex-1 min-w-[70px] py-3 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 transition-all ${
                            disabled ? 'bg-gray-50 cursor-not-allowed' : 'hover:bg-gray-50 hover:border-emerald-200 hover:text-emerald-700 active:scale-95'
                        }`}
                    >
                        + {val}만
                    </button>
                ))}
                <button
                    type="button"
                    onClick={onAllIn}
                    disabled={disabled}
                    className={`flex-1 min-w-[70px] py-3 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 transition-all ${
                        disabled ? 'bg-gray-50 cursor-not-allowed' : 'hover:bg-gray-50 hover:border-emerald-200 hover:text-emerald-700 active:scale-95'
                    }`}
                >
                    전액
                </button>
            </div>
            {disabled && (
                <p className="text-xs text-amber-600 font-medium ml-1 animate-pulse">
                    * 계좌 정보를 정확히 입력하여 조회가 완료되면 금액을 입력할 수 있습니다.
                </p>
            )}
        </section>
    );
};

export default AmountInputSection;
