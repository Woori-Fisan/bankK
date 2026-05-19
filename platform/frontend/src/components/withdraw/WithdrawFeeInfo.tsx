import React from 'react';
import { formatAmount } from '../../utils/formatter';

interface WithdrawFeeInfoProps {
    fee: number;
    isWaived?: boolean;
}

const WithdrawFeeInfo: React.FC<WithdrawFeeInfoProps> = ({ fee, isWaived = false }) => {
    return (
        <div className="flex justify-between items-center py-2">
            <span className="text-sm font-medium text-gray-500">이체 수수료</span>
            <div className="text-right">
                <span className="text-sm font-bold text-gray-900">₩ {formatAmount(fee)}</span>
                {isWaived && (
                    <span className="text-xs text-emerald-600 ml-1">(우대 적용)</span>
                )}
            </div>
        </div>
    );
};

export default WithdrawFeeInfo;
