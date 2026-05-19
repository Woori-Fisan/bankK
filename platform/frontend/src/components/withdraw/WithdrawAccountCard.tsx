import React from 'react';
import { formatAmount } from '../../utils/formatter';

interface WithdrawAccountCardProps {
    bankName: string;
    accountNumber: string;
    branchName: string;
    balance: number;
}

const WithdrawAccountCard: React.FC<WithdrawAccountCardProps> = ({
    bankName,
    accountNumber,
    branchName,
    balance,
}) => {
    return (
        <div className="bg-gray-50 rounded-lg p-5 border border-gray-100">
            <div className="flex justify-between items-start mb-4">
                <span className="text-sm font-medium text-gray-500">출금 계좌</span>
                <div className="text-right">
                    <span className="text-sm font-medium text-gray-500">잔액: </span>
                    <span className="text-lg font-bold text-gray-900">₩ {formatAmount(balance)}</span>
                </div>
            </div>
            <div className="text-lg font-bold text-gray-900">
                {bankName} {accountNumber} ({branchName})
            </div>
        </div>
    );
};

export default WithdrawAccountCard;
