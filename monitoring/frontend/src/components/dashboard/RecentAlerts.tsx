import React from 'react';
import { mockAlerts } from '../../api/mockData';
import { AlertCircle, AlertTriangle, Info } from 'lucide-react';

const RecentAlerts: React.FC = () => {
    const getIcon = (type: string) => {
        switch (type) {
            case 'Critical': return <AlertCircle size={16} className="text-red-500" />;
            case 'Warning': return <AlertTriangle size={16} className="text-amber-500" />;
            default: return <Info size={16} className="text-blue-500" />;
        }
    };

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 w-80">
            <h3 className="font-bold text-gray-900 text-lg mb-6">Recent Alerts</h3>
            <div className="space-y-4">
                {mockAlerts.map((alert) => (
                    <div key={alert.id} className="flex gap-3">
                        <div className="mt-0.5">{getIcon(alert.type)}</div>
                        <div className="flex-1">
                            <p className="text-xs font-bold text-gray-900 leading-tight mb-1">{alert.message}</p>
                            <span className="text-[10px] text-gray-400">{alert.time}</span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default RecentAlerts;
