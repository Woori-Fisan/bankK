import React from 'react';
import { useTransferStore } from '../../../store/useTransferStore';

const BalanceCalculationSection: React.FC = () => {
    const { amount } = useTransferStore();
    const availableBalance = 1250000000;
    const remainingBalance = availableBalance - amount;

    return (
        <div className="bg-gray-50 rounded-2xl p-8 border border-gray-100 space-y-4">
            <div className="flex justify-between items-center">
                <span className="text-sm text-gray-500 font-medium">현재 잔액</span>
                <span className="text-base font-bold text-gray-900">₩ {availableBalance.toLocaleString()}</span>
            </div>
            <div className="flex justify-between items-center">
                <span className="text-sm text-gray-500 font-medium">이체 금액</span>
                <span className="text-base font-bold text-red-500">- ₩ {amount.toLocaleString()}</span>
            </div>
            <div className="h-px bg-gray-200 w-full my-2"></div>
            <div className="flex justify-between items-center">
                <span className="text-sm text-gray-900 font-bold">이체 후 예상 잔액</span>
                <span className="text-xl font-bold text-emerald-600">₩ {remainingBalance.toLocaleString()}</span>
            </div>
        </div>
    );
};

export default BalanceCalculationSection;
