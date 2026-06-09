import React from 'react';
import { CreditCard } from 'lucide-react';
import { formatAmount } from '../../../utils/formatter';

interface Props {
    toName: string;
    toBank: string;
    toAccountNumber: string;
    fromBank: string;
    fromAccountNumber: string;
    transactionId: string;
    transactionDate: string;
    balanceAfter: string;
}

const ResultDetailSection: React.FC<Props> = ({
    toName, toBank, toAccountNumber, fromBank, fromAccountNumber, transactionDate, balanceAfter
}) => {
    return (
        <div className="space-y-8">
            <div className="flex items-center gap-2 text-slate-800">
                <CreditCard className="w-5 h-5 text-emerald-600" />
                <h3 className="text-lg font-bold">거래 상세 정보</h3>
            </div>

            <div className="bg-slate-50/50 rounded-2xl border border-slate-100 divide-y divide-slate-100">
                {/* 1. 계좌 정보 (출금 -> 입금) */}
                <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
                    <div className="p-6 space-y-1">
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">출금 계좌</p>
                        <p className="text-sm font-bold text-slate-900">{fromBank} {fromAccountNumber}</p>
                    </div>
                    <div className="p-6 space-y-1 bg-emerald-50/30">
                        <p className="text-[10px] font-black text-emerald-600 uppercase tracking-widest">입금 계좌 (수취인: {toName})</p>
                        <p className="text-sm font-bold text-emerald-700">{toBank} {toAccountNumber}</p>
                    </div>
                </div>

                {/* 2. 거래 일시 및 잔액 */}
                <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
                    <div className="p-6 space-y-1">
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">거래 일시</p>
                        <p className="text-sm font-bold text-slate-600">{transactionDate}</p>
                    </div>
                    <div className="p-6 space-y-1">
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">이체 후 잔액</p>
                        <p className="text-xl font-black text-slate-900">₩ {formatAmount(balanceAfter)}</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ResultDetailSection;
