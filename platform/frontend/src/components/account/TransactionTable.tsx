import React from 'react';
import { ChevronLeft, ChevronRight, Info } from 'lucide-react';
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

interface TransactionTableProps {
    transactions: Transaction[];
    currentPage: number;
    totalEntries: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}

const TransactionTable: React.FC<TransactionTableProps> = ({ 
    transactions, 
    currentPage, 
    totalEntries, 
    totalPages,
    onPageChange 
}) => {
    const getPageNumbers = () => {
        const maxPagesToShow = 5;
        let startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
        const endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);

        if (endPage - startPage + 1 < maxPagesToShow) {
            startPage = Math.max(1, endPage - maxPagesToShow + 1);
        }

        const pages = [];
        for (let i = startPage; i <= endPage; i++) {
            pages.push(i);
        }
        return pages;
    };

    const formatCurrency = (val: string | number | null) => {
        if (val === null || val === undefined) return '-';
        return formatAmount(val);
    };

    return (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-xl shadow-gray-200/40 overflow-hidden flex flex-col h-[750px]">
            <div className="flex-1 overflow-auto">
                <table className="w-full text-sm text-left border-collapse table-fixed">
                    <thead className="bg-gray-50/50 border-b border-gray-100 text-gray-400 font-bold uppercase tracking-wider sticky top-0 z-20 backdrop-blur-md">
                        <tr>
                            <th className="px-6 py-5 text-[11px] w-[18%]">거래일시</th>
                            <th className="px-6 py-5 text-[11px] w-[12%]">구분</th>
                            <th className="px-6 py-5 text-[11px] w-[25%]">거래처 / 적요</th>
                            <th className="px-6 py-5 text-right text-[11px] w-[15%]">출금 (KRW)</th>
                            <th className="px-6 py-5 text-right text-[11px] w-[15%]">입금 (KRW)</th>
                            <th className="px-6 py-5 text-right text-[11px] w-[15%]">잔액 (KRW)</th>
                            <th className="px-6 py-5 text-center text-[11px] w-[100px]">상태</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50 text-gray-700">
                        {transactions.length > 0 ? (
                            transactions.map((tx) => {
                                const amountNum = Number(tx.amount);
                                const isWithdrawal = amountNum < 0;
                                const isDeposit = amountNum > 0;

                                return (
                                    <tr key={tx.id} className="hover:bg-emerald-50/30 transition-all duration-200 group">
                                        <td className="px-6 py-5 whitespace-nowrap">
                                            <div className="flex flex-col">
                                                <span className="text-gray-900 font-semibold">{tx.date.split(' ')[0]}</span>
                                                <span className="text-gray-400 text-xs">{tx.date.split(' ')[1]}</span>
                                            </div>
                                        </td>
                                        <td className="px-6 py-5">
                                            {(() => {
                                                const typeMap: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
                                                    'DEPOSIT': { label: '입금', color: 'text-emerald-600' },
                                                    'WITHDRAW': { label: '출금', color: 'text-rose-500' },
                                                    'TRANSFER': { label: '이체', color: 'text-blue-600' },
                                                    'LOAN': { label: '대출', color: 'text-indigo-600' },
                                                };
                                                const currentType = typeMap[tx.type] || { label: tx.type, color: 'text-gray-500', icon: null };
                                                
                                                return (
                                                    <div className={`flex items-center gap-2 font-bold ${currentType.color}`}>
                                                        {currentType.icon}
                                                        <span>{currentType.label}</span>
                                                    </div>
                                                );
                                            })()}
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
                            })
                        ) : (
                            <tr>
                                <td colSpan={7} className="px-6 py-32 text-center">
                                    <div className="flex flex-col items-center gap-3 text-gray-300">
                                        <Info className="w-12 h-12 stroke-[1]" />
                                        <p className="text-lg font-medium">조회된 거래 내역이 없습니다.</p>
                                    </div>
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            <div className="px-8 py-6 border-t border-gray-100 flex items-center justify-between bg-gray-50/50">
                <div className="flex items-center gap-3 text-sm text-gray-500">
                    <span className="font-medium">총 <span className="text-gray-900 font-black ml-1">{totalEntries.toLocaleString()}</span> 거래</span>
                    <div className="w-1 h-1 bg-gray-300 rounded-full" />
                    <span>페이지 {totalPages || 1} 중 {currentPage} 번째</span>
                </div>
                
                <div className="flex items-center gap-2">
                    <button 
                        onClick={() => onPageChange(currentPage - 1)}
                        disabled={currentPage === 1 || totalPages === 0}
                        className="w-10 h-10 flex items-center justify-center border border-gray-200 rounded-xl bg-white hover:bg-emerald-50 hover:border-emerald-200 hover:text-emerald-600 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm"
                    >
                        <ChevronLeft className="w-5 h-5" />
                    </button>
                    
                    <div className="flex items-center gap-1 mx-2">
                        {getPageNumbers().map((page) => (
                            <button 
                                key={page}
                                onClick={() => onPageChange(page)}
                                className={`w-10 h-10 rounded-xl text-sm font-black transition-all shadow-sm ${
                                    page === currentPage 
                                        ? 'bg-emerald-800 text-white shadow-emerald-800/20' 
                                        : 'bg-white border border-gray-100 text-gray-500 hover:bg-emerald-50 hover:border-emerald-100 hover:text-emerald-700'
                                }`}
                            >
                                {page}
                            </button>
                        ))}
                    </div>

                    <button 
                        onClick={() => onPageChange(currentPage + 1)}
                        disabled={currentPage === totalPages || totalPages === 0}
                        className="w-10 h-10 flex items-center justify-center border border-gray-200 rounded-xl bg-white hover:bg-emerald-50 hover:border-emerald-200 hover:text-emerald-600 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm"
                    >
                        <ChevronRight className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default TransactionTable;
