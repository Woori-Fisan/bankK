import React from 'react';
import { useTransferStore } from '../../../store/useTransferStore';

const AmountInputSection: React.FC = () => {
    const { amount, updateData } = useTransferStore();
    const availableBalance = 1250000000;

    const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value.replace(/[^0-9]/g, '');
        const numValue = Number(value);
        if (numValue <= availableBalance) {
            updateData({ amount: numValue });
        }
    };

    const addAmount = (val: number) => {
        const nextAmount = amount + val;
        if (nextAmount <= availableBalance) {
            updateData({ amount: nextAmount });
        } else {
            updateData({ amount: availableBalance });
        }
    };

    const formatToKorean = (num: number) => {
        if (num === 0) return '';
        const units = ['', '만', '억', '조'];
        let result = '';
        let temp = num;
        let unitIdx = 0;
        while (temp > 0) {
            const part = temp % 10000;
            if (part > 0) result = part.toLocaleString() + units[unitIdx] + ' ' + result;
            temp = Math.floor(temp / 10000);
            unitIdx++;
        }
        return result.trim() + ' 원';
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-4">
                <label className="text-xs text-gray-400 font-bold uppercase tracking-wider ml-1">이체 금액 (KRW)</label>
                <div className="relative border-b-2 border-gray-200 focus-within:border-emerald-500 transition-all pb-2">
                    <span className="absolute left-0 bottom-4 text-3xl font-bold text-gray-900">₩</span>
                    <input
                        type="text"
                        value={amount === 0 ? '' : amount.toLocaleString()}
                        onChange={handleAmountChange}
                        className="w-full bg-transparent text-5xl font-bold text-gray-900 text-right pr-2 focus:outline-none"
                        placeholder="0"
                    />
                </div>
                <div className="text-right h-6">
                    <span className="text-emerald-700 font-medium text-sm">{formatToKorean(amount)}</span>
                </div>
            </div>

            <div className="grid grid-cols-4 gap-3">
                {['1만', '5만', '10만', '50만'].map((label, idx) => {
                    const vals = [10000, 50000, 100000, 500000];
                    return (
                        <button key={label} onClick={() => addAmount(vals[idx])} className="py-4 bg-gray-50 border border-gray-100 rounded-xl text-sm font-bold text-gray-700 hover:bg-gray-100 transition-colors">
                            +{label}
                        </button>
                    );
                })}
            </div>
            <button onClick={() => updateData({ amount: availableBalance })} className="w-full py-4 bg-gray-50 border border-gray-100 rounded-xl text-sm font-bold text-gray-700 hover:bg-gray-100 transition-colors">
                전액 입력
            </button>
        </div>
    );
};

export default AmountInputSection;
