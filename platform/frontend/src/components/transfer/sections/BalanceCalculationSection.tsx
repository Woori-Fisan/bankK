import React from 'react';
import { useTransferStore } from '../../../store/useTransferStore';

const BalanceCalculationSection: React.FC = () => {
    const { amount, balance } = useTransferStore();
    const availableBalance = Number(balance) || 0;
    const remainingBalance = availableBalance - amount;

    return (
        <div className="bg-slate-50 rounded-2xl p-8 border border-slate-100 space-y-5">
            <div className="flex justify-between items-center">
                <span className="text-sm text-slate-500 font-bold">현재 잔액</span>
                <span className="text-base font-black text-slate-900">₩ {availableBalance.toLocaleString()}</span>
            </div>
            <div className="flex justify-between items-center">
                <span className="text-sm text-slate-500 font-bold">이체 신청 금액</span>
                <span className="text-base font-black text-rose-500">- ₩ {amount.toLocaleString()}</span>
            </div>
            <div className="h-px bg-slate-200 w-full"></div>
            <div className="flex justify-between items-center pt-1">
                <span className="text-sm text-slate-900 font-black">이체 후 예상 잔액</span>
                <span className="text-2xl font-black text-emerald-600">₩ {remainingBalance.toLocaleString()}</span>
            </div>
        </div>
    );
};

export default BalanceCalculationSection;
