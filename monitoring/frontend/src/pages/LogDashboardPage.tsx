import React from 'react';

const GRAFANA_URL =
    'http://localhost:3000/d/dfnt2zaw0xmgwf/monitoring-dashboard?orgId=1&refresh=10s&from=now-1h&to=now&kiosk=tv&theme=light';

const MetricsPage: React.FC = () => {
    return (
        <div className="flex flex-col h-full p-6 gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-gray-900">실시간 로그</h2>
                <span className="text-xs text-gray-400">10초마다 자동 갱신</span>
            </div>

            <div className="flex-1 rounded-2xl overflow-hidden shadow-sm border border-gray-100 bg-white min-h-0">
                <iframe
                    src={GRAFANA_URL}
                    title="Grafana 모니터링 대시보드"
                    className="w-full h-full"
                    style={{ minHeight: '600px' }}
                    allowFullScreen
                />
            </div>
        </div>
    );
};

export default MetricsPage;
