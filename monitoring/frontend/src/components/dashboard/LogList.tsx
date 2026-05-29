import React, { useState } from 'react';
import type { SystemLog } from '../../types/log';
import LogDetailModal from './LogDetailModal';
import { getLog } from '../../api/log';

interface LogListProps {
    logs: SystemLog[];
    totalPage: number;
    currentPage: number;
    totalCount: number;
    isLoading: boolean;
    onPageChange: (page: number) => void;
}

const getTransactionType = (uri: string): string => {
    const lower = uri.toLowerCase();
    if (lower.includes('transfer')) return '이체';
    if (lower.includes('withdrawal')) return '출금';
    if (lower.includes('loan')) return '대출';
    return '';
};

const AGENCY_NAME: Record<string, string> = {
    'PO001': '우체국',
    'SB001': '저축은행',
};

const BANK_NAME: Record<string, string> = {
    '004': '국민은행',
    '020': '우리은행',
    '088': '신한은행',
    '081': '하나은행',
    '011': '농협은행',
};

const getStatusColor = (status: number) => {
    if (status >= 500) return 'text-red-600';
    if (status >= 400) return 'text-amber-600';
    return 'text-emerald-600';
};

const LogList: React.FC<LogListProps> = ({ logs, totalPage, currentPage, totalCount, isLoading, onPageChange }) => {
    const [selectedLog, setSelectedLog] = useState<SystemLog | null>(null);
    const [detailLoading, setDetailLoading] = useState(false);

    const handleRowClick = async (id: number) => {
        setDetailLoading(true);
        try {
            const res = await getLog(id);
            if (res.data) setSelectedLog(res.data);
        } finally {
            setDetailLoading(false);
        }
    };

    const pageNumbers = Array.from({ length: Math.min(totalPage, 5) }, (_, i) => {
        const half = 2;
        const start = Math.max(1, Math.min(currentPage - half, totalPage - 4));
        return start + i;
    });

    return (
        <>
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/30">
                    <h3 className="font-bold text-gray-900">로그 조회 결과</h3>
                    <span className="text-xs text-gray-500 font-medium">
                        총 {totalCount.toLocaleString()}건 검색됨 (페이지 {currentPage} / {totalPage || 1})
                    </span>
                </div>

                <div className="overflow-x-auto">
                    <div className="overflow-y-auto max-h-[480px]">
                        <table className="w-full text-left border-collapse">
                            <thead className="sticky top-0 z-10">
                                <tr className="bg-gray-50">
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">시간</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">거래 유형</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">발생지</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">레벨</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">트랜잭션 ID</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">은행명</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">대행기관</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">직원 ID</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">상태</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-50">
                                {isLoading ? (
                                    <tr>
                                        <td colSpan={9} className="px-6 py-16 text-center text-sm text-gray-400">
                                            불러오는 중...
                                        </td>
                                    </tr>
                                ) : logs.length === 0 ? (
                                    <tr>
                                        <td colSpan={9} className="px-6 py-16 text-center text-sm text-gray-400">
                                            조회된 로그가 없습니다.
                                        </td>
                                    </tr>
                                ) : (
                                    logs.map((log) => (
                                        <tr
                                            key={log.id}
                                            onClick={() => handleRowClick(log.id)}
                                            className={`hover:bg-emerald-50/50 transition-colors cursor-pointer ${detailLoading ? 'pointer-events-none opacity-60' : ''}`}
                                        >
                                            <td className="px-6 py-3 text-xs text-gray-600 whitespace-nowrap">
                                                {log.createdAt.replace('T', ' ').slice(0, 19)}
                                            </td>
                                            <td className="px-6 py-3 text-xs text-gray-700">{getTransactionType(log.httpUri)}</td>
                                            <td className="px-6 py-3 text-xs font-bold text-gray-700">{log.logType}</td>
                                            <td className="px-6 py-3">
                                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                                    log.level === 'ERROR' ? 'bg-red-100 text-red-600' :
                                                    log.level === 'WARN'  ? 'bg-amber-100 text-amber-600' :
                                                                            'bg-blue-100 text-blue-600'
                                                }`}>
                                                    {log.level}
                                                </span>
                                            </td>
                                            <td className="px-6 py-3 text-xs font-mono text-gray-500">{log.traceId}</td>
                                            <td className="px-6 py-3 text-xs text-gray-600">{BANK_NAME[log.bankCode] ?? log.bankCode}</td>
                                            <td className="px-6 py-3 text-xs text-gray-600">{AGENCY_NAME[log.agencyCode] ?? log.agencyCode}</td>
                                            <td className="px-6 py-3 text-xs text-gray-600">{log.staffId}</td>
                                            <td className={`px-6 py-3 text-xs font-bold ${getStatusColor(log.httpStatus)}`}>
                                                {log.httpStatus}
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                {totalPage > 1 && (
                    <div className="px-6 py-4 bg-gray-50/30 border-t border-gray-100 flex justify-center">
                        <nav className="flex gap-1">
                            {pageNumbers.map((n) => (
                                <button
                                    key={n}
                                    onClick={() => onPageChange(n)}
                                    className={`w-8 h-8 rounded-lg text-xs font-bold transition-colors ${
                                        n === currentPage
                                            ? 'bg-emerald-700 text-white'
                                            : 'bg-white border border-gray-200 text-gray-500 hover:bg-gray-50'
                                    }`}
                                >
                                    {n}
                                </button>
                            ))}
                        </nav>
                    </div>
                )}
            </div>

            {selectedLog && (
                <LogDetailModal log={selectedLog} onClose={() => setSelectedLog(null)} />
            )}
        </>
    );
};

export default LogList;
