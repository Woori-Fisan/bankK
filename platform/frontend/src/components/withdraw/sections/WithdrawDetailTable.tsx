import React from 'react';
import { CreditCard } from 'lucide-react';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawDetailTableProps {
    recipientName: string;
    bankName: string;
    accountNumber: string;
    balanceBefore: number;
    balanceAfter: number;
    transactionId: string;
    dateTime: string;
}

const WithdrawDetailTable: React.FC<WithdrawDetailTableProps> = ({
    recipientName,
    bankName,
    accountNumber,
    balanceBefore,
    balanceAfter,
    transactionId,
    dateTime,
}) => {
    return (
        <div className="space-y-6">
            <div className="flex items-center gap-2 text-emerald-700">
                <CreditCard className="w-5 h-5" />
                <h3 className="text-lg font-black tracking-tight">출금 계좌 정보</h3>
            </div>

            <div className="bg-gray-50 rounded-xl overflow-hidden border border-gray-100">
                <div className="grid grid-cols-2 p-6 border-b border-gray-100">
                    <div className="space-y-1">
                        <p className="text-sm font-bold text-gray-900">{recipientName}</p>
                    </div>
                    <div className="text-right space-y-1">
                        <p className="text-[10px] font-bold text-gray-400 uppercase">출금 계좌번호 (Account Number)</p>
                        <p className="text-sm font-bold text-gray-700">{bankName} {accountNumber}</p>
                    </div>
                </div>
                <div className="grid grid-cols-2 p-6 bg-white/50">
                    <div className="space-y-1">
                        <p className="text-[10px] font-bold text-gray-400 uppercase">출금 전 잔액 (Balance Before)</p>
                        <p className="text-sm font-bold text-gray-900">₩ {formatAmount(balanceBefore)}</p>
                    </div>
                    <div className="text-right space-y-1">
                        <p className="text-[10px] font-bold text-gray-400 uppercase">출금 후 잔액 (Balance After)</p>
                        <p className="text-sm font-bold text-gray-900">₩ {formatAmount(balanceAfter)}</p>
                    </div>
                </div>
            </div>

            <div className="flex justify-between items-center px-2 py-4 border-t border-gray-100 mt-4">
                <div className="space-y-1">
                    <p className="text-[10px] font-bold text-gray-400 uppercase">거래 일시 (Date/Time)</p>
                    <p className="text-xs font-bold text-gray-600">{dateTime}</p>
                </div>
                <div className="text-right space-y-1">
                    <p className="text-[10px] font-bold text-gray-400 uppercase">거래 번호 (Transaction ID)</p>
                    <p className="text-xs font-bold text-gray-600">{transactionId}</p>
                </div>
            </div>
        </div>
    );
};

export default WithdrawDetailTable;
