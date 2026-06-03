import React from 'react';
import TransactionTypeBadge from './TransactionTypeBadge';
import { formatAmount, formatDate } from '../../utils/formatter';
import Badge from '../common/Badge';

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

    const formatTxType = (type: string) => {
        const types: Record<string, string> = {
            'DEPOSIT': '입금',
            'WITHDRAW': '출금',
            'TRANSFER': '이체',
            'LOAN': '대출',
            'FEE': '수수료'
        };
        return types[type] || type;
    };

    return (
        <tr className="hover:bg-emerald-50/30 transition-all duration-200 group">
            <td className="px-6 py-5 whitespace-nowrap">
                <div className="flex flex-col">
                    <span className="text-gray-900 font-semibold">{formatDate(tx.date, false, 'dot')}</span>
                    <span className="text-gray-400 text-xs">{tx.date.includes(' ') ? tx.date.split(' ')[1] : ''}</span>
                </div>
            </td>
            <td className="px-6 py-5">
                {/* 기술적 유형 대신 금액의 부호(Sign)를 기준으로 입금/출금 표시 */}
                <TransactionTypeBadge amount={tx.amount} />
            </td>
            <td className="px-6 py-5">
                <div className="flex flex-col">
                    <span className="text-gray-900 font-black text-base group-hover:text-emerald-900 transition-colors">
                        {tx.target || tx.description || '-'}
                    </span>
                    <div className="flex items-center gap-1.5 mt-0.5">
                        <span className="text-[10px] font-bold text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded">
                            {formatTxType(tx.type)}
                        </span>
                        {tx.target && tx.description && (
                            <span className="text-gray-400 text-xs">
                                {tx.description}
                            </span>
                        )}
                    </div>
                </div>
            </td>
            <td className="px-6 py-5 text-right">
                {isWithdrawal ? (
                    <span className="text-rose-500 font-black text-lg">
                        {formatCurrency(tx.amount)}
                    </span>
                ) : (
                    <span className="text-slate-200">-</span>
                )}
            </td>
            <td className="px-6 py-5 text-right">
                {isDeposit ? (
                    <span className="text-emerald-600 font-black text-lg">
                        {formatCurrency(tx.amount)}
                    </span>
                ) : (
                    <span className="text-slate-200">-</span>
                )}
            </td>
            <td className="px-6 py-5 text-right font-bold text-gray-900 bg-gray-50/30">
                {formatCurrency(tx.balance)}
            </td>
            <td className="px-6 py-5 text-center">
                <Badge 
                    color={tx.status === '완료' ? 'emerald' : 'amber'}
                    variant="subtle"
                >
                    {tx.status}
                </Badge>
            </td>
        </tr>
    );
};

export default TransactionRow;
