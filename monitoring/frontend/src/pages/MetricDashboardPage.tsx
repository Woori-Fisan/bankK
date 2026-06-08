import React from 'react';

const GRAFANA_METRIC_URL =
    '/grafana/d/cfnwmp0m3r5kwf/metric-dashboard?orgId=1&from=now-1h&to=now&theme=light&kiosk=tv';

const MetricDashboardPage: React.FC = () => {
    return (
        <div className="flex flex-col h-full p-6 gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-gray-900">실시간 메트릭</h2>
                <span className="text-xs text-gray-400">Grafana 메트릭 대시보드</span>
            </div>

            <div className="flex-1 rounded-2xl overflow-hidden shadow-sm border border-gray-100 bg-white min-h-0">
                <iframe
                    src={GRAFANA_METRIC_URL}
                    title="Grafana 메트릭 대시보드"
                    className="w-full h-full"
                    style={{ minHeight: '600px' }}
                    allowFullScreen
                />
            </div>
        </div>
    );
};

export default MetricDashboardPage;
