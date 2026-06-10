import React from 'react';
import { FileText, AlertCircle, Clock, CheckCircle2 } from 'lucide-react';

interface MetricCardProps {
    label: string;
    value: string;
    type: 'total' | 'error' | 'latency' | 'success';
}

const MetricCard: React.FC<MetricCardProps> = ({ label, value, type }) => {
    const getConfig = () => {
        switch (type) {
            case 'total': return { icon: FileText, color: 'text-blue-600', bg: 'bg-blue-50' };
            case 'error': return { icon: AlertCircle, color: 'text-red-600', bg: 'bg-red-50' };
            case 'latency': return { icon: Clock, color: 'text-amber-600', bg: 'bg-amber-50' };
            case 'success': return { icon: CheckCircle2, color: 'text-emerald-600', bg: 'bg-emerald-50' };
        }
    };

    const config = getConfig();

    return (
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4 overflow-hidden">
            <div className={`w-11 h-11 flex-shrink-0 ${config.bg} rounded-xl flex items-center justify-center`}>
                <config.icon className={config.color} size={22} />
            </div>
            <div className="min-w-0 flex-1">
                <p className="text-xs font-bold text-gray-500 mb-0.5 truncate">{label}</p>
                <h3 className="text-xl font-extrabold text-gray-900 leading-tight truncate">{value}</h3>
            </div>
        </div>
    );
};

export default MetricCard;
