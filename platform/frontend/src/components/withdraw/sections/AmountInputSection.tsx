import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface AmountInputSectionProps {
    amount: string;
    onAmountChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onQuickAmountAdd: (value: number) => void;
    onAllIn: () => void;
}

const AmountInputSection: React.FC<AmountInputSectionProps> = ({
    amount,
    onAmountChange,
    onQuickAmountAdd,
    onAllIn,
}) => {
    return (
        <section className="space-y-4">
            <h3 className="text-sm font-medium text-gray-500">이체 금액</h3>
            <div className="relative group">
                <input
                    type="text"
                    value={amount === '0' || !amount ? '' : formatAmount(amount)}
                    onChange={onAmountChange}
                    className="w-full px-6 py-8 text-4xl font-extrabold text-right border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 pr-16 bg-white transition-all"
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
                        className="flex-1 min-w-[70px] py-3 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 hover:bg-gray-50 hover:border-emerald-200 hover:text-emerald-700 active:scale-95 transition-all"
                    >
                        + {val}만
                    </button>
                ))}
                <button
                    type="button"
                    onClick={onAllIn}
                    className="flex-1 min-w-[70px] py-3 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 hover:bg-gray-50 hover:border-emerald-200 hover:text-emerald-700 active:scale-95 transition-all"
                >
                    전액
                </button>
            </div>
        </section>
    );
};

export default AmountInputSection;
