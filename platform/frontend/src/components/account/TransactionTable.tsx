import React from 'react';
import { ChevronLeft, ChevronRight, Info } from 'lucide-react';
import TransactionRow, { type Transaction } from './TransactionRow';
import Card from '../common/Card';
import { Button } from '../common/Button';

export type { Transaction };

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

    return (
        <Card padding="none" className="overflow-hidden flex flex-col h-[750px]">
            <div className="flex-1 overflow-auto">
                <table className="w-full text-sm text-left border-collapse table-fixed">
                    <thead className="bg-slate-50/50 border-b border-slate-100 text-slate-400 font-bold uppercase tracking-wider sticky top-0 z-20 backdrop-blur-md">
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
                    <tbody className="divide-y divide-slate-50 text-slate-700">
                        {transactions.length > 0 ? (
                            transactions.map((tx) => (
                                <TransactionRow key={tx.id} tx={tx} />
                            ))
                        ) : (
                            <tr>
                                <td colSpan={7} className="px-6 py-32 text-center">
                                    <div className="flex flex-col items-center gap-3 text-slate-300">
                                        <Info className="w-12 h-12 stroke-[1]" />
                                        <p className="text-lg font-medium">조회된 거래 내역이 없습니다.</p>
                                    </div>
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            <div className="px-8 py-6 border-t border-slate-100 flex items-center justify-between bg-slate-50/50">
                <div className="flex items-center gap-3 text-sm text-slate-500">
                    <span className="font-medium">총 <span className="text-slate-900 font-black ml-1">{totalEntries.toLocaleString()}</span> 거래</span>
                    <div className="w-1 h-1 bg-slate-300 rounded-full" />
                    <span>페이지 {totalPages || 1} 중 {currentPage} 번째</span>
                </div>
                
                <div className="flex items-center gap-2">
                    <Button 
                        variant="outline"
                        size="icon"
                        onClick={() => onPageChange(currentPage - 1)}
                        disabled={currentPage === 1 || totalPages === 0}
                        className="bg-white border-slate-200"
                    >
                        <ChevronLeft className="w-5 h-5" />
                    </Button>
                    
                    <div className="flex items-center gap-1 mx-2">
                        {getPageNumbers().map((page) => (
                            <Button 
                                key={page}
                                variant={page === currentPage ? 'emerald' : 'outline'}
                                size="icon"
                                onClick={() => onPageChange(page)}
                                className={page === currentPage ? '' : 'bg-white border-slate-100 text-slate-500'}
                            >
                                {page}
                            </Button>
                        ))}
                    </div>

                    <Button 
                        variant="outline"
                        size="icon"
                        onClick={() => onPageChange(currentPage + 1)}
                        disabled={currentPage === totalPages || totalPages === 0}
                        className="bg-white border-slate-200"
                    >
                        <ChevronRight className="w-5 h-5" />
                    </Button>
                </div>
            </div>
        </Card>
    );
};

export default TransactionTable;
