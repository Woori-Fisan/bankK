import React from 'react';
import { CreditCard } from 'lucide-react';
import { formatAmount, formatDate } from '../../../utils/formatter';

interface WithdrawDetailTableProps {
    bankName: string;
    accountNumber: string;
    birthDate: string;
    balanceBefore: number;
    balanceAfter: number;
    transactionId: string;
    dateTime: string;
}

const WithdrawDetailTable: React.FC<WithdrawDetailTableProps> = ({
    bankName,
    accountNumber,
    balanceBefore,
    balanceAfter,
    dateTime,
}) => {
    return (
        <div className="space-y-6">
            <div className="flex items-center gap-2 text-slate-800">
                <CreditCard className="w-5 h-5 text-emerald-600" />
                <h3 className="text-lg font-bold">거래 상세 정보</h3>
            </div>

            <div className="bg-slate-50/50 rounded-2xl border border-slate-100 divide-y divide-slate-100">
                <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
                    <div className="p-6 space-y-1">
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">출금 계좌</p>
                        <p className="text-sm font-bold text-slate-900">{bankName} {accountNumber}</p>
                    </div>
                    <div className="p-6 space-y-1">
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">거래 일시</p>
                        <p className="text-sm font-bold text-slate-600">{formatDate(dateTime, true, 'dot')}</p>
                    </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
                    <div className="p-6 space-y-1">
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">출금 전 잔액</p>
                        <p className="text-sm font-bold text-slate-900">₩ {formatAmount(balanceBefore)}</p>
                    </div>
                    <div className="p-6 space-y-1 text-right md:text-left">
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">출금 후 잔액</p>
                        <p className="text-xl font-black text-emerald-600">₩ {formatAmount(balanceAfter)}</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default WithdrawDetailTable;
