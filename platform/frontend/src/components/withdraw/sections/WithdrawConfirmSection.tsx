import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawConfirmSectionProps {
    sourceAccount: {
        bankName: string;
        accountNumber: string;
        branchName: string;
    };
    depositInfo: {
        bankName: string;
        accountNumber: string;
        recipientName: string;
    };
    amount: string;
    fee: number;
}

const WithdrawConfirmSection: React.FC<WithdrawConfirmSectionProps> = ({
    sourceAccount,
    depositInfo,
    amount,
    fee,
}) => {
    const totalAmount = parseInt(amount || '0', 10) + fee;

    return (
        <section className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="bg-emerald-50/30 border border-emerald-100 rounded-2xl p-6 text-center">
                <p className="text-sm font-medium text-emerald-600 mb-1">총 출금 금액</p>
                <h3 className="text-4xl font-black text-emerald-900 tracking-tight">
                    <span className="text-2xl mr-1">₩</span>
                    {formatAmount(totalAmount)}
                </h3>
            </div>

            <div className="space-y-4">
                <div className="group">
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 px-1">출금 계좌</h4>
                    <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-sm group-hover:border-emerald-200 transition-colors">
                        <p className="text-lg font-bold text-gray-900">
                            {sourceAccount.bankName} {sourceAccount.accountNumber}
                        </p>
                        <p className="text-sm text-gray-500 font-medium">{sourceAccount.branchName}</p>
                    </div>
                </div>

                <div className="flex justify-center">
                    <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center">
                        <svg className="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                        </svg>
                    </div>
                </div>

                <div className="group">
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 px-1">받는 분 (입금 계좌)</h4>
                    <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-sm group-hover:border-emerald-200 transition-colors">
                        <p className="text-lg font-bold text-gray-900">{depositInfo.recipientName}</p>
                        <p className="text-sm text-gray-500 font-medium">
                            {depositInfo.bankName} {depositInfo.accountNumber}
                        </p>
                    </div>
                </div>
            </div>

            <div className="border-t border-dashed border-gray-200 pt-6 space-y-3 px-1">
                <div className="flex justify-between items-center text-sm">
                    <span className="text-gray-500 font-medium">이체 금액</span>
                    <span className="text-gray-900 font-bold">₩ {formatAmount(amount)}</span>
                </div>
                <div className="flex justify-between items-center text-sm">
                    <span className="text-gray-500 font-medium">이체 수수료</span>
                    <span className="text-gray-900 font-bold">₩ {formatAmount(fee)}</span>
                </div>
            </div>
        </section>
    );
};

export default WithdrawConfirmSection;
