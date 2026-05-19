import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawFeeSectionProps {
    fee: number;
    isWaived?: boolean;
}

const WithdrawFeeSection: React.FC<WithdrawFeeSectionProps> = ({ fee, isWaived = false }) => {
    return (
        <section className="flex justify-between items-center py-4 px-1">
            <span className="text-sm font-bold text-gray-500">이체 수수료</span>
            <div className="text-right">
                <span className="text-lg font-bold text-gray-900">₩ {formatAmount(fee)}</span>
                {isWaived && (
                    <span className="text-sm font-semibold text-emerald-600 ml-2">(우대 적용)</span>
                )}
            </div>
        </section>
    );
};

export default WithdrawFeeSection;
