import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { formatAmount } from '../../utils/formatter';

export interface Transaction {
    id: string;
    date: string;
    description: string;
    withdrawal: string | number | null; // BigDecimal 대응을 위해 string 허용
    deposit: string | number | null;    // BigDecimal 대응을 위해 string 허용
    balance: string | number;           // BigDecimal 대응을 위해 string 허용
    status: '완료' | '대기';
}

interface TransactionTableProps {
    transactions: Transaction[];
    currentPage: number;
    totalEntries: number;
    pageSize: number;
    onPageChange: (page: number) => void;
}

const TransactionTable: React.FC<TransactionTableProps> = ({ 
    transactions, 
    currentPage, 
    totalEntries, 
    pageSize,
    onPageChange 
}) => {
    const totalPages = Math.ceil(totalEntries / pageSize);
    const startEntry = (currentPage - 1) * pageSize + 1;
    const endEntry = Math.min(currentPage * pageSize, totalEntries);

    // RULE_FE_STYLE 10.2: formatAmount 유틸리티 사용
    const formatCurrency = (val: string | number | null) => {
        if (val === null || val === undefined) return '-';
        return formatAmount(val);
    };

    return (
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden flex flex-col min-h-[500px]">
            <div className="flex-1 overflow-x-auto">
                <table className="w-full text-sm text-left border-collapse">
                    <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 font-medium sticky top-0 z-10">
                        <tr>
                            <th className="px-6 py-4">거래일시</th>
                            <th className="px-6 py-4">적요(내용)</th>
                            <th className="px-6 py-4 text-right">출금 (KRW)</th>
                            <th className="px-6 py-4 text-right">입금 (KRW)</th>
                            <th className="px-6 py-4 text-right">잔액 (KRW)</th>
                            <th className="px-6 py-4 text-center">상태</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50 text-gray-700">
                        {transactions.length > 0 ? (
                            transactions.map((tx) => (
                                <tr key={tx.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="px-6 py-4 text-gray-500 whitespace-nowrap">{tx.date}</td>
                                    <td className="px-6 py-4 font-medium">{tx.description}</td>
                                    <td className="px-6 py-4 text-right text-rose-600">
                                        {tx.withdrawal ? `-${formatCurrency(tx.withdrawal)}` : '-'}
                                    </td>
                                    <td className="px-6 py-4 text-right text-emerald-600">
                                        {tx.deposit ? `+${formatCurrency(tx.deposit)}` : '-'}
                                    </td>
                                    <td className="px-6 py-4 text-right font-medium">{formatCurrency(tx.balance)}</td>
                                    <td className="px-6 py-4 text-center">
                                        <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                                            tx.status === '완료' ? 'bg-emerald-50 text-emerald-600' : 'bg-orange-50 text-orange-600'
                                        }`}>
                                            {tx.status}
                                        </span>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={6} className="px-6 py-20 text-center text-gray-400">
                                    거래 내역이 없습니다.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            <div className="px-6 py-4 border-t border-gray-50 flex items-center justify-between bg-white">
                <span className="text-sm text-gray-500">
                    Showing <span className="font-semibold text-gray-900">{totalEntries > 0 ? startEntry : 0} to {endEntry}</span> of <span className="font-semibold text-gray-900">{totalEntries}</span> entries
                </span>
                <div className="flex items-center gap-1">
                    <button 
                        onClick={() => onPageChange(currentPage - 1)}
                        disabled={currentPage === 1}
                        className="p-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                    >
                        <ChevronLeft className="w-4 h-4" />
                    </button>
                    
                    {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                        <button 
                            key={page}
                            onClick={() => onPageChange(page)}
                            className={`min-w-[32px] h-8 rounded text-sm font-medium transition-all ${
                                page === currentPage 
                                    ? 'bg-emerald-800 text-white shadow-sm' 
                                    : 'hover:bg-gray-100 text-gray-600'
                            }`}
                        >
                            {page}
                        </button>
                    ))}

                    <button 
                        onClick={() => onPageChange(currentPage + 1)}
                        disabled={currentPage === totalPages || totalPages === 0}
                        className="p-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                    >
                        <ChevronRight className="w-4 h-4" />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default TransactionTable;
