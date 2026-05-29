import React, { useState } from 'react';
import type { SystemLog } from '../../types/log';
import LogDetailModal from './LogDetailModal';

const SERVICES = ['Transfer', 'Auth', 'Withdraw', 'Loan', 'Inquiry'];
const LEVELS = ['INFO', 'INFO', 'INFO', 'WARN', 'ERROR'] as const;
const STATUSES: Record<string, number> = {
    Transfer: 200, Auth: 401, Withdraw: 200, Loan: 200, Inquiry: 200,
};
const AGENCIES = ['우체국', '저축은행', '핀테크', '대행대행'];
const BANK_CODES = ['국민은행', '신한은행', '우리은행', '하나은행', '농협은행'];
const HTTP_METHODS = ['GET', 'POST', 'POST', 'GET', 'POST'];

const pad = (n: number) => String(n).padStart(2, '0');

const generateMockLogs = (page: number): SystemLog[] =>
    Array.from({ length: 20 }, (_, i) => {
        const service = SERVICES[i % SERVICES.length];
        const level = LEVELS[i % LEVELS.length];
        const minutes = 20 + Math.floor(i / 2);
        const seconds = pad((i * 3) % 60);
        const httpStatus = level === 'ERROR' ? (i % 2 === 0 ? 500 : 401) : STATUSES[service];
        const isError = httpStatus >= 400;
        return {
            id: page * 100 + i + 1,
            createdAt: `2026-05-28T14:${pad(minutes)}:${seconds}+09:00`,
            level,
            logType: `${service.toUpperCase()}_REQUEST`,
            traceId: `trace-${page}${pad(i + 1)}-abcd-efgh`,
            staffId: `staff_${pad((i % 5) + 1)}`,
            bankCode: BANK_CODES[i % BANK_CODES.length],
            targetCode: `TARGET_${pad((i % 3) + 1)}`,
            agencyCode: AGENCIES[i % AGENCIES.length],
            bankKeyId: `key_${BANK_CODES[i % BANK_CODES.length]}_${pad(i + 1)}`,
            httpMethod: HTTP_METHODS[i % HTTP_METHODS.length],
            httpUri: `/api/v1/${service.toLowerCase()}/execute`,
            httpStatus,
            elapsedMs: 10 + (i * 7) % 300,
            clientIp: `10.0.${i % 5}.${(i * 13) % 255}`,
            jwsSignature: `eyJhbGciOiJSUzI1NiJ9.eyJzdGFmZklkIjoic3RhZmZfMDEiLCJ0cmFjZUlkIjoidHJhY2UtJHtwYWdlfSR7cGFkKGkrMSl9In0.mock_signature_${page}_${i}`,
            bodyData: JSON.stringify({ service, action: 'execute', params: { amount: 10000 * (i + 1), currency: 'KRW' } }, null, 2),
            errorCode: isError ? `ERR_${httpStatus}` : null,
            errorMessage: isError ? (httpStatus === 500 ? '내부 서버 오류가 발생했습니다.' : '인증에 실패했습니다.') : null,
        };
    });

const LogList: React.FC = () => {
    const [currentPage, setCurrentPage] = useState(1);
    const [selectedLog, setSelectedLog] = useState<SystemLog | null>(null);
    const logs = generateMockLogs(currentPage);

    const getStatusColor = (status: number) => {
        if (status >= 500) return 'text-red-600';
        if (status >= 400) return 'text-amber-600';
        return 'text-emerald-600';
    };

    return (
        <>
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/30">
                    <h3 className="font-bold text-gray-900">로그 조회 결과</h3>
                    <span className="text-xs text-gray-500 font-medium">
                        총 1,284건 검색됨 (페이지 {currentPage} / 20개씩)
                    </span>
                </div>

                <div className="overflow-x-auto">
                    <div className="overflow-y-auto max-h-[480px]">
                        <table className="w-full text-left border-collapse">
                            <thead className="sticky top-0 z-10">
                                <tr className="bg-gray-50">
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">시간</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">서비스</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">레벨</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">트랜잭션 ID</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">은행명</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider whitespace-nowrap">대행기관</th>
                                    <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">상태</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-50">
                                {logs.map((log) => (
                                    <tr
                                        key={log.id}
                                        onClick={() => setSelectedLog(log)}
                                        className="hover:bg-emerald-50/50 transition-colors cursor-pointer"
                                    >
                                        <td className="px-6 py-3 text-xs text-gray-600 whitespace-nowrap">
                                            {log.createdAt.replace('T', ' ').slice(0, 19)}
                                        </td>
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
                                        <td className="px-6 py-3 text-xs text-gray-600">{log.bankCode}</td>
                                        <td className="px-6 py-3 text-xs text-gray-600">{log.agencyCode}</td>
                                        <td className={`px-6 py-3 text-xs font-bold ${getStatusColor(log.httpStatus)}`}>
                                            {log.httpStatus}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="px-6 py-4 bg-gray-50/30 border-t border-gray-100 flex justify-center">
                    <nav className="flex gap-1">
                        {[1, 2, 3, 4, 5].map((n) => (
                            <button
                                key={n}
                                onClick={() => setCurrentPage(n)}
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
            </div>

            {selectedLog && (
                <LogDetailModal log={selectedLog} onClose={() => setSelectedLog(null)} />
            )}
        </>
    );
};

export default LogList;
