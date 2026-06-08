import React from 'react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawConfirmSectionProps {
    sourceAccount: {
        bankName: string;
        accountNumber: string;
    };
    amount: string;
    fee: number;
}

const WithdrawConfirmSection: React.FC<WithdrawConfirmSectionProps> = ({
    sourceAccount,
    amount,
    fee,
}) => {
    const totalAmount = parseInt(amount || '0', 10) + fee;

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

            {/* 2. 통합 금액 정보 (중복 제거 및 한눈에 들어오게 합침) */}
            <div className="space-y-4 px-2">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest">총 출금 금액</h3>
                <div className="p-10 bg-white border-2 border-slate-100 rounded-[40px] text-center shadow-sm relative overflow-hidden">
                    {/* 수수료 정보 보조 표시 (강조색 제거) */}
                    <div className="mb-6 flex items-center justify-center gap-4 text-sm font-bold">
                        <div className="flex items-baseline gap-1 text-slate-400">
                            <span>신청 금액</span>
                            <span>{formatAmount(amount)}원</span>
                        </div>
                        <div className="w-px h-3 bg-slate-200"></div>
                        <div className="flex items-baseline gap-1 text-slate-400">
                            <span>수수료</span>
                            <span>{formatAmount(fee)}원 (면제)</span>
                        </div>
                    </div>

                    <div className="flex items-baseline justify-center gap-2">
                        <span className="text-xl font-bold text-slate-400">₩</span>
                        <span className="text-7xl font-black text-slate-900 tracking-tighter">
                            {formatAmount(totalAmount)}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default WithdrawConfirmSection;
