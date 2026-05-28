import React from 'react';
import LogFilter from '../components/dashboard/LogFilter';
import MetricCard from '../components/dashboard/MetricCard';
import LogList from '../components/dashboard/LogList';

const DashboardPage: React.FC = () => {
    return (
        <div className="p-8 space-y-6 text-left">
            <div className="flex justify-between items-center">
                <div className="text-left">
                    <h2 className="text-2xl font-bold text-gray-900 text-left">모니터링 로그 조회</h2>
                </div>
            </div>

            {/* 조회 필터 */}
            <LogFilter />

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
                <LogList />
            </div>
        </div>
    );
};

export default DashboardPage;
