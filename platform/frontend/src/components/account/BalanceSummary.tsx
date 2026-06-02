import React from 'react';
import { formatAmount } from '../../utils/formatter';

interface SummaryCardProps {
    title: string;
    amount: number | string;
    isMain?: boolean;
}

const SummaryCard: React.FC<SummaryCardProps> = ({ title, amount, isMain }) => {
    // RULE_FE_STYLE 10.2: utils/formatter.js의 formatAmount() 사용
    const formattedAmount = formatAmount(amount);

    return (
        <div className={`p-6 h-32 rounded-2xl border border-gray-100 bg-white shadow-sm flex flex-col justify-between transition-all hover:shadow-md ${isMain ? 'col-span-full md:col-span-2' : 'col-span-full md:col-span-1'}`}>
            <div className="flex flex-col gap-1">
                <span className="text-[11px] font-black text-gray-400 uppercase tracking-widest">{title}</span>
                <div className="flex items-baseline gap-2 mt-1">
                    <span className="text-3xl font-black text-gray-900">
                        <span className="text-sm font-bold mr-1 opacity-40">₩</span>
                        {formattedAmount}
                    </span>
                </div>
            </div>
        </div>
    );
};

interface BalanceSummaryProps {
    customBalance?: number | string;
}

const BalanceSummary: React.FC<BalanceSummaryProps> = ({ customBalance }) => {
    // API에서 받은 값이 있으면 해당 값을 사용하고, 없으면 기본 더미 값을 표시합니다.
    const displayBalance = customBalance !== undefined ? customBalance : 24592840000;

    return (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <SummaryCard 
                title="총 계좌 잔액 (Total Balance)" 
                amount={displayBalance} 
                isMain 
            />
            <SummaryCard 
                title="출금 가능 잔액" 
                amount={displayBalance} 
            />
            <SummaryCard 
                title="지급 정지 / 보류액" 
                amount={0} 
            />
        </div>
    );
};

export default BalanceSummary;
