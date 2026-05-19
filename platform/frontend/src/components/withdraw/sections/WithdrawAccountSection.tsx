import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawAccountSectionProps {
    bankName: string;
    accountNumber: string;
    branchName: string;
    balance: number;
}

const WithdrawAccountSection: React.FC<WithdrawAccountSectionProps> = ({
    bankName,
    accountNumber,
    branchName,
    balance,
}) => {
    return (
        <section className="space-y-3">
            <h3 className="text-sm font-medium text-gray-500">출금 정보</h3>
            <div className="bg-gray-50 rounded-xl p-6 border border-gray-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1">
                    <span className="text-xs font-semibold text-gray-400 block">출금 계좌</span>
                    <div className="text-lg font-bold text-gray-900">
                        {bankName} {accountNumber} ({branchName})
                    </div>
                </div>
                <div className="text-right">
                    <span className="text-xs font-semibold text-gray-400 block">잔액</span>
                    <div className="text-xl font-bold text-gray-900">
                        <span className="text-sm mr-1">₩</span>
                        {formatAmount(balance)}
                    </div>
                </div>
            </div>
        </section>
    );
};

export default WithdrawAccountSection;
