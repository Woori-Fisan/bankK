import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawConfirmSectionProps {
    sourceAccount: {
        bankName: string;
        accountNumber: string;
    };
    amount: string;
}

const WithdrawConfirmSection: React.FC<WithdrawConfirmSectionProps> = ({
    sourceAccount,
    amount,
}) => {
    return (
        <div className="space-y-10">
            {/* 1. 출금 계좌 정보 */}
            <div className="space-y-4 px-2">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest">출금 계좌 정보</h3>
                <div className="p-8 bg-slate-50 rounded-3xl border border-slate-100">
                    <p className="text-xl font-bold text-emerald-600">{sourceAccount.bankName}</p>
                    <p className="text-3xl font-black text-slate-900 tracking-tight mt-1">{sourceAccount.accountNumber}</p>
                </div>
            </div>

            {/* 2. 출금 금액 */}
            <div className="space-y-4 px-2">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest">출금 금액</h3>
                <div className="p-10 bg-white border-2 border-slate-100 rounded-[40px] text-center shadow-sm relative overflow-hidden">
                    <div className="flex items-baseline justify-center gap-2">
                        <span className="text-xl font-bold text-slate-400">₩</span>
                        <span className="text-7xl font-black text-slate-900 tracking-tighter">
                            {formatAmount(amount)}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default WithdrawConfirmSection;
