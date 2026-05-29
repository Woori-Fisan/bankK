import React, { useRef, useState } from 'react';
import LogFilter from '../components/dashboard/LogFilter';
import MetricCard from '../components/dashboard/MetricCard';
import LogList from '../components/dashboard/LogList';
import { getLogList } from '../api/log';
import type { LogListRequest, SystemLog } from '../types/log';

const PAGE_SIZE = 20;

const DashboardPage: React.FC = () => {
    const [logs, setLogs] = useState<SystemLog[]>([]);
    const [totalPage, setTotalPage] = useState(0);
    const [totalCount, setTotalCount] = useState(0);
    const [currentPage, setCurrentPage] = useState(1);
    const [isLoading, setIsLoading] = useState(false);
    const lastRequestRef = useRef<LogListRequest | null>(null);

    const fetchLogs = async (request: LogListRequest, page: number) => {
        await Promise.resolve();
        setIsLoading(true);
        try {
            const res = await getLogList({ ...request, page: page - 1, size: PAGE_SIZE });
            if (res.success && res.data) {
                setLogs(res.data.logListDTO);
                setTotalPage(res.data.totalPage);
                setTotalCount(res.data.pageNum * res.data.pageSize); // 임시: API에 totalCount 없을 경우
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleSearch = (request: LogListRequest) => {
        lastRequestRef.current = request;
        setCurrentPage(1);
        fetchLogs(request, 1);
    };

    const handlePageChange = (page: number) => {
        if (!lastRequestRef.current) return;
        setCurrentPage(page);
        fetchLogs(lastRequestRef.current, page);
    };

    return (
        <div className="p-8 space-y-6 text-left">
            <div className="flex justify-between items-center">
                <div className="text-left">
                    <h2 className="text-2xl font-bold text-gray-900 text-left">모니터링 로그 조회</h2>
                </div>
            </div>

            {/* 조회 필터 */}
            <LogFilter onSearch={handleSearch} />

            {/* 주요 지표 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <MetricCard
                    label="총 로그 수"
                    value="1,284,092"
                    type="total"
                />
                <MetricCard
                    label="오류 건수"
                    value="42"
                    type="error"
                />
                <MetricCard
                    label="평균 응답시간"
                    value="42ms"
                    type="latency"
                />
                <MetricCard
                    label="처리 성공률"
                    value="99.98%"
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
