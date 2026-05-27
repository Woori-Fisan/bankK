import React from 'react';
import TransactionTypeBadge from './TransactionTypeBadge';
import { formatAmount } from '../../utils/formatter';

export interface Transaction {
    id: string;
    date: string;
    description: string;
    target: string | null;
    type: string;        // tx_type (DEPOSIT, WITHDRAW, TRANSFER, LOAN 등)
    amount: string | number; // 출금은 음수, 입금은 양수
    balance: string | number;
    status: '완료' | '대기';
}

interface TransactionRowProps {
    tx: Transaction;
}

const TransactionRow: React.FC<TransactionRowProps> = ({ tx }) => {
    const amountNum = Number(tx.amount);
    const isWithdrawal = amountNum < 0;
    const isDeposit = amountNum > 0;

    const formatCurrency = (val: string | number | null) => {
        if (val === null || val === undefined) return '-';
        return formatAmount(val);
    };

    return (
        <tr className="hover:bg-emerald-50/30 transition-all duration-200 group">
            <td className="px-6 py-5 whitespace-nowrap">
                <div className="flex flex-col">
                    <span className="text-gray-900 font-semibold">{tx.date.split(' ')[0]}</span>
                    <span className="text-gray-400 text-xs">{tx.date.split(' ')[1]}</span>
                </div>
            </td>
            <td className="px-6 py-5">
                <TransactionTypeBadge type={tx.type} />
            </td>
            <td className="px-6 py-5">
                <div className="flex flex-col">
                    <span className="text-gray-900 font-black text-base group-hover:text-emerald-900 transition-colors">
                        {tx.target || tx.description || '-'}
                    </span>
                    {tx.target && tx.description && (
                        <span className="text-gray-400 text-xs mt-0.5">
                            {tx.description}
                        </span>
                    )}
                </div>
            </td>
            <td className="px-6 py-5 text-right">
                {isWithdrawal ? (
                    <span className="text-rose-500 font-black text-lg">
                        {formatCurrency(tx.amount)}
                    </span>
                ) : (
                    <span className="text-gray-300">-</span>
                )}
            </td>
            <td className="px-6 py-5 text-right">
                {isDeposit ? (
                    <span className="text-emerald-600 font-black text-lg">
                        {formatCurrency(tx.amount)}
                    </span>
                ) : (
                    <span className="text-gray-300">-</span>
                )}
            </td>
            <td className="px-6 py-5 text-right font-bold text-gray-900 bg-gray-50/30">
                {formatCurrency(tx.balance)}
            </td>
            <td className="px-6 py-5 text-center">
                <span className={`px-3 py-1.5 rounded-full text-xs font-black shadow-sm ${
                    tx.status === '완료' 
                        ? 'bg-emerald-100 text-emerald-700' 
                        : 'bg-amber-100 text-amber-700'
                }`}>
                    {tx.status}
                </span>
            </td>
        </tr>
    );
};

export default TransactionRow;
