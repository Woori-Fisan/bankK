import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawConfirmSectionProps {
    sourceAccount: {
        bankName: string;
        accountNumber: string;
    };
    birthDate: string;
    amount: string;
    fee: number;
}

const WithdrawConfirmSection: React.FC<WithdrawConfirmSectionProps> = ({
    sourceAccount,
    birthDate,
    amount,
    fee,
}) => {
    const totalAmount = parseInt(amount || '0', 10) + fee;

    return (
        <section className="w-full space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="bg-emerald-50/30 border border-emerald-100 rounded-2xl p-6 text-center">
                <p className="text-sm font-medium text-emerald-600 mb-1">총 출금 금액</p>
                <h3 className="text-4xl font-black text-emerald-900 tracking-tight">
                    <span className="text-2xl mr-1">₩</span>
                    {formatAmount(totalAmount)}
                </h3>
            </div>

            <div className="space-y-6">
                <div className="group">
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 px-1">출금 계좌</h4>
                    <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm group-hover:border-emerald-200 transition-colors">
                        <p className="text-xl font-bold text-gray-900">
                            {sourceAccount.bankName} {sourceAccount.accountNumber}
                        </p>
                    </div>
                </div>

                <div className="group">
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 px-1">본인 확인 (생년월일)</h4>
                    <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm group-hover:border-emerald-200 transition-colors">
                        <p className="text-xl font-bold text-gray-900 tracking-[0.5em]">{birthDate}</p>
                    </div>
                </div>
            </div>

            <div className="border-t border-dashed border-gray-200 pt-6 space-y-3 px-1">
                <div className="flex justify-between items-center text-sm">
                    <span className="text-gray-500 font-medium">신청 금액</span>
                    <span className="text-gray-900 font-bold">₩ {formatAmount(amount)}</span>
                </div>
                <div className="flex justify-between items-center text-sm">
                    <span className="text-gray-500 font-medium">출금 수수료</span>
                    <span className="text-gray-900 font-bold">₩ {formatAmount(fee)}</span>
                </div>
            </div>
        </section>
    );
};

export default WithdrawConfirmSection;
