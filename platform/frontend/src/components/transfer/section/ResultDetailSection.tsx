import React from 'react';
import { ArrowRightLeft, Download } from 'lucide-react';

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
    toName, toBank, toAccountNumber, fromBank, fromAccountNumber, transactionId, transactionDate, balanceAfter
}) => {
    return (
        <div className="p-12 space-y-10">
            <div className="space-y-4">
                <div className="flex items-center gap-2 text-emerald-700 font-bold">
                    <ArrowRightLeft className="w-5 h-5 rotate-90" />
                    <span>입금 계좌 정보 (Deposit)</span>
                </div>
                <div className="bg-gray-50 rounded-2xl p-8 border border-gray-100 grid grid-cols-2 gap-y-6">
                    <div>
                        <span className="text-xs text-gray-400 block mb-1">수취인 (Recipient Name)</span>
                        <span className="text-base font-bold text-gray-900">{toName}</span>
                    </div>
                    <div>
                        <span className="text-xs text-gray-400 block mb-1">입금 계좌번호 (Account Number)</span>
                        <span className="text-base font-bold text-gray-900">{toBank} {toAccountNumber}</span>
                    </div>
                </div>
            </div>

            <div className="space-y-4">
                <div className="flex items-center gap-2 text-gray-700 font-bold">
                    <ArrowRightLeft className="w-5 h-5 -rotate-90" />
                    <span>출금 계좌 정보 (Withdrawal)</span>
                </div>
                <div className="bg-gray-50 rounded-2xl p-8 border border-gray-100 grid grid-cols-2 gap-y-6">
                    <div>
                        <span className="text-xs text-gray-400 block mb-1">출금 은행 (Withdrawal Bank)</span>
                        <span className="text-base font-bold text-gray-900">{fromBank}</span>
                    </div>
                    <div>
                        <span className="text-xs text-gray-400 block mb-1">출금 계좌번호 (Account Number)</span>
                        <span className="text-base font-bold text-gray-900">{fromAccountNumber}</span>
                    </div>
                    <div className="col-span-2 mt-2 border-t border-gray-200 pt-4">
                        <span className="text-xs text-gray-400 block mb-1">이체 후 잔액 (Balance After Transfer)</span>
                        <span className="text-lg font-bold text-emerald-700">₩ {formatAmount(balanceAfter)}</span>
                    </div>
                </div>
            </div>

            <div className="flex justify-between items-end border-t border-gray-100 pt-8">
                <div className="space-y-4">
                    <div>
                        <span className="text-[10px] text-gray-400 font-bold uppercase block">거래 일시 (Date/Time)</span>
                        <span className="text-xs text-gray-600 font-medium">{transactionDate}</span>
                    </div>
                    <div>
                        <span className="text-[10px] text-gray-400 font-bold uppercase block">거래 번호 (Transaction ID)</span>
                        <span className="text-xs text-gray-600 font-medium">{transactionId}</span>
                    </div>
                </div>
                <button className="flex items-center gap-2 px-4 py-2 text-gray-400 hover:text-emerald-600 transition-colors">
                    <Download className="w-4 h-4" />
                    <span className="text-xs font-bold">전자영수증 저장</span>
                </button>
            </div>
        </div>
    );
};

export default ResultDetailSection;
