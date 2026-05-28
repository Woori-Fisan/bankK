import React from 'react';
import { mockServiceStatus } from '../../api/mockData';

const ServiceStatus: React.FC = () => {
    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex-1">
            <h3 className="font-bold text-gray-900 text-lg mb-6">Service Health</h3>
            <div className="space-y-4">
                {mockServiceStatus.map((service, index) => (
                    <div key={index} className="flex items-center justify-between p-3 rounded-xl hover:bg-gray-50 transition-colors">
                        <div className="flex items-center gap-3">
                            <div className={`w-2 h-2 rounded-full ${
                                service.status === 'Healthy' ? 'bg-emerald-500' : 'bg-amber-400'
                            }`} />
                            <span className="text-sm font-bold text-gray-700">{service.name}</span>
                        </div>
                        <div className="flex items-center gap-6 text-xs font-medium">
                            <span className="text-gray-500">Latency: <span className="text-gray-900">{service.latency}</span></span>
                            <span className="text-gray-500">Uptime: <span className="text-gray-900">{service.uptime}</span></span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default ServiceStatus;
