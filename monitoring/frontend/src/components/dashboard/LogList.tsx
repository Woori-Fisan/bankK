import React, { useState } from 'react';

const SERVICES = ['Transfer', 'Auth', 'Withdraw', 'Loan', 'Inquiry'];
const LEVELS = ['INFO', 'INFO', 'INFO', 'WARN', 'ERROR'] as const;
const STATUSES: Record<string, string> = {
    Transfer: '200', Auth: '401', Withdraw: '200', Loan: '200', Inquiry: '200',
};
const BANKS = ['국민은행', '신한은행', '우리은행', '하나은행', '농협은행'];
const AGENCIES = ['핀테크A', '핀테크B', '핀테크C', '핀테크D'];

const pad = (n: number) => String(n).padStart(2, '0');

const generateMockLogs = (page: number) =>
    Array.from({ length: 20 }, (_, i) => {
        const service = SERVICES[i % SERVICES.length];
        const level = LEVELS[i % LEVELS.length];
        const minutes = 20 + Math.floor(i / 2);
        const seconds = pad((i * 3) % 60);
        const status = level === 'ERROR' ? (i % 2 === 0 ? '500' : '401') : STATUSES[service];
        return {
            id: `tx_${page}${pad(i + 1)}`,
            time: `2026-05-28 14:${pad(minutes)}:${seconds}`,
            service,
            level,
            status,
            bank: BANKS[i % BANKS.length],
            agency: AGENCIES[i % AGENCIES.length],
        };
    });

const LogList: React.FC = () => {
    const [currentPage, setCurrentPage] = useState(1);
    const logs = generateMockLogs(currentPage);

    const getStatusColor = (status: string) => {
        const first = status.charAt(0);
        return first === '4' || first === '5' ? 'text-red-600' : 'text-emerald-600';
    };

    return (
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
                                <tr key={log.id} className="hover:bg-gray-50/50 transition-colors cursor-pointer">
                                    <td className="px-6 py-3 text-xs text-gray-600 whitespace-nowrap">{log.time}</td>
                                    <td className="px-6 py-3 text-xs font-bold text-gray-700">{log.service}</td>
                                    <td className="px-6 py-3">
                                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                            log.level === 'ERROR' ? 'bg-red-100 text-red-600' :
                                            log.level === 'WARN'  ? 'bg-amber-100 text-amber-600' :
                                                                    'bg-blue-100 text-blue-600'
                                        }`}>
                                            {log.level}
                                        </span>
                                    </td>
                                    <td className="px-6 py-3 text-xs font-mono text-gray-500">{log.id}</td>
                                    <td className="px-6 py-3 text-xs text-gray-600">{log.bank}</td>
                                    <td className="px-6 py-3 text-xs text-gray-600">{log.agency}</td>
                                    <td className={`px-6 py-3 text-xs font-bold ${getStatusColor(log.status)}`}>{log.status}</td>
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
    );
};

export default LogList;
