import React, { useEffect, useRef, useState } from 'react';
import LogFilter from '../components/dashboard/LogFilter';
import MetricCard from '../components/dashboard/MetricCard';
import LogList from '../components/dashboard/LogList';
import { getLogList, getTransactionSummary } from '../api/log';
import { getAgencies, getBanks } from '../api/common';
import type { LogListRequest, SystemLog } from '../types/log';
import type { TransactionSummaryResponse } from '../types/transaction';
import type { Agency, Bank } from '../types/common';

const PAGE_SIZE = 20;

const DashboardPage: React.FC = () => {
    const [logs, setLogs] = useState<SystemLog[]>([]);
    const [totalPage, setTotalPage] = useState(0);
    const [totalCount, setTotalCount] = useState(0);
    const [currentPage, setCurrentPage] = useState(1);
    const [isLoading, setIsLoading] = useState(false);
    const [summary, setSummary] = useState<TransactionSummaryResponse | null>(null);
    const [agencies, setAgencies] = useState<Agency[]>([]);
    const [banks, setBanks] = useState<Bank[]>([]);
    const lastRequestRef = useRef<LogListRequest | null>(null);

    useEffect(() => {
        getAgencies().then(res => {
            if (res.success && res.data) setAgencies(res.data);
        });
        getBanks().then(res => {
            if (res.success && res.data) setBanks(res.data);
        });
    }, []);

    const fetchLogs = async (request: LogListRequest, page: number) => {
        const res = await getLogList({ ...request, page: page - 1, size: PAGE_SIZE });
        if (res.success && res.data) {
            setLogs(res.data.logListDTO);
            setTotalPage(res.data.totalPage);
            setTotalCount(res.data.pageNum * res.data.pageSize);
        }
    };

    const fetchSummary = async (request: LogListRequest) => {
        const res = await getTransactionSummary(request);
        if (res.success && res.data) {
            setSummary(res.data);
        }
    };

    const handleSearch = async (request: LogListRequest) => {
        lastRequestRef.current = request;
        setCurrentPage(1);
        setIsLoading(true);
        try {
            await Promise.all([fetchLogs(request, 1), fetchSummary(request)]);
        } finally {
            setIsLoading(false);
        }
    };

    const handlePageChange = (page: number) => {
        if (!lastRequestRef.current) return;
        setCurrentPage(page);
        setIsLoading(true);
        fetchLogs(lastRequestRef.current, page).finally(() => setIsLoading(false));
    };

    return (
        <div className="p-8 space-y-6 text-left">
            <div className="flex justify-between items-center">
                <div className="text-left">
                    <h2 className="text-2xl font-bold text-gray-900 text-left">모니터링 로그 조회</h2>
                </div>
            </div>

            {/* 조회 필터 */}
            <LogFilter onSearch={handleSearch} agencies={agencies} banks={banks} />

            {/* 주요 지표 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <MetricCard
                    label="총 로그 수"
                    value={summary ? summary.totalCount.toLocaleString() : '-'}
                    type="total"
                />
                <MetricCard
                    label="오류 건수"
                    value={summary ? summary.errorCount.toLocaleString() : '-'}
                    type="error"
                />
                <MetricCard
                    label="평균 응답시간"
                    value={summary ? `${summary.averageElapsedMs}ms` : '-'}
                    type="latency"
                />
                <MetricCard
                    label="처리 성공률"
                    value={summary ? `${summary.successRate.toFixed(2)}%` : '-'}
                    type="success"
                />
            </div>

            {/* 로그 목록 */}
            <div className="grid grid-cols-1 gap-6">
                <LogList
                    logs={logs}
                    totalPage={totalPage}
                    currentPage={currentPage}
                    totalCount={totalCount}
                    isLoading={isLoading}
                    onPageChange={handlePageChange}
                />
            </div>
        </div>
    );
};

export default DashboardPage;
