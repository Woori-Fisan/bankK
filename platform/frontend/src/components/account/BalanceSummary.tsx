import React from 'react';
import { ArrowUpRight } from 'lucide-react';

interface SummaryCardProps {
    title: string;
    amount: number;
    change?: string;
    isMain?: boolean;
}

const SummaryCard: React.FC<SummaryCardProps> = ({ title, amount, change, isMain }) => {
    const formattedAmount = new Intl.NumberFormat('ko-KR').format(amount);

    return (
        <div className={`p-6 rounded-xl border border-gray-100 bg-white shadow-sm flex flex-col justify-between ${isMain ? 'col-span-2' : 'col-span-1'}`}>
            <div className="flex flex-col gap-1">
                <span className="text-sm text-gray-500 font-medium">{title}</span>
                <div className="flex items-baseline gap-2">
                    <span className="text-2xl font-bold text-gray-900">₩ {formattedAmount}</span>
                    {change && (
                        <span className="text-sm font-semibold text-emerald-600 flex items-center">
                            <ArrowUpRight className="w-3 h-3 mr-0.5" />
                            {change}
                        </span>
                    )}
                </div>
            </div>
        </div>
    );
};

const BalanceSummary: React.FC = () => {
    return (
        <div className="grid grid-cols-4 gap-4 mb-6">
            <SummaryCard 
                title="총 계좌 잔액 (KRW)" 
                amount={24592840000} 
                change="1.2%" 
                isMain 
            />
            <SummaryCard 
                title="출금 가능 잔액" 
                amount={24500000000} 
            />
            <SummaryCard 
                title="지급 정지 / 보류액" 
                amount={92840000} 
            />
        </div>
    );
};

export default BalanceSummary;
