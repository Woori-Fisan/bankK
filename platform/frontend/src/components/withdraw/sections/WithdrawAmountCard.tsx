import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawAmountCardProps {
    amount: string | number;
}

const WithdrawAmountCard: React.FC<WithdrawAmountCardProps> = ({ amount }) => {
    return (
        <div className="bg-white border border-gray-100 rounded-xl p-8 shadow-sm flex flex-col items-center justify-center space-y-2">
            <span className="text-sm font-bold text-gray-400">출금 금액</span>
            <div className="text-4xl font-black text-gray-900 tracking-tight">
                <span className="text-2xl mr-2">₩</span>
                {formatAmount(amount)}
            </div>
        </div>
    );
};

export default WithdrawAmountCard;
