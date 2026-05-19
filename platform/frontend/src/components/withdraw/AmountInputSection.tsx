import React from 'react';
import { formatAmount } from '../../utils/formatter';

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
        <div className="space-y-4">
            <h3 className="text-sm font-medium text-gray-500">이체 금액</h3>
            <div className="relative">
                <input
                    type="text"
                    value={amount === '0' ? '' : formatAmount(amount)}
                    onChange={onAmountChange}
                    className="w-full px-4 py-6 text-3xl font-bold text-right border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 pr-12"
                    placeholder="0"
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xl font-bold text-gray-400">₩</span>
            </div>
            <div className="flex gap-2">
                {[100, 500, 1000].map((val) => (
                    <button
                        key={val}
                        type="button"
                        onClick={() => onQuickAmountAdd(val * 10000)}
                        className="flex-1 py-2 border border-gray-200 rounded text-sm font-medium text-gray-600 hover:bg-gray-50 active:bg-gray-100 transition-colors"
                    >
                        + {val}만
                    </button>
                ))}
                <button
                    type="button"
                    onClick={onAllIn}
                    className="flex-1 py-2 border border-gray-200 rounded text-sm font-medium text-gray-600 hover:bg-gray-50 active:bg-gray-100 transition-colors"
                >
                    전액
                </button>
            </div>
        </div>
    );
};

export default AmountInputSection;
