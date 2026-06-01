import React from 'react';
import { ArrowUpRight, ArrowDownRight } from 'lucide-react';

interface StatusCardProps {
    label: string;
    value: string;
    change: string;
    trend: 'up' | 'down';
}

const StatusCard: React.FC<StatusCardProps> = ({ label, value, change, trend }) => {
    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
            <p className="text-sm font-medium text-gray-500 mb-1">{label}</p>
            <div className="flex items-end justify-between">
                <h3 className="text-2xl font-bold text-gray-900">{value}</h3>
                <div className={`flex items-center text-xs font-bold px-2 py-1 rounded-full ${
                    trend === 'up' ? 'text-emerald-700 bg-emerald-50' : 'text-red-700 bg-red-50'
                }`}>
                    {trend === 'up' ? <ArrowUpRight size={14} className="mr-0.5" /> : <ArrowDownRight size={14} className="mr-0.5" />}
                    {change}
                </div>
            </div>
        </div>
    );
};

export default StatusCard;
