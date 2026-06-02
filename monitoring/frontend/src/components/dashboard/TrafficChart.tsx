import React from 'react';

const TrafficChart: React.FC = () => {
    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 h-80 flex flex-col">
            <div className="flex justify-between items-center mb-6">
                <h3 className="font-bold text-gray-900 text-lg">System Traffic (TPS)</h3>
                <div className="flex gap-2">
                    <span className="flex items-center text-xs text-gray-500">
                        <span className="w-2 h-2 rounded-full bg-emerald-500 mr-2"></span> Success
                    </span>
                    <span className="flex items-center text-xs text-gray-500">
                        <span className="w-2 h-2 rounded-full bg-red-400 mr-2"></span> Error
                    </span>
                </div>
            </div>
            <div className="flex-1 w-full bg-gray-50 rounded-lg flex items-center justify-center relative overflow-hidden">
                {/* Mock SVG Graph */}
                <svg className="absolute bottom-0 left-0 w-full h-40" preserveAspectRatio="none">
                    <path
                        d="M0 40 Q 50 10, 100 35 T 200 20 T 300 38 T 400 15 T 500 30 T 600 10 L 600 40 L 0 40 Z"
                        fill="rgba(16, 185, 129, 0.1)"
                        className="w-full"
                    />
                    <path
                        d="M0 40 Q 50 10, 100 35 T 200 20 T 300 38 T 400 15 T 500 30 T 600 10"
                        fill="none"
                        stroke="#10b981"
                        strokeWidth="2"
                        className="w-full"
                    />
                </svg>
                <span className="text-gray-400 text-sm font-medium relative z-10">Real-time Metrics Visualization</span>
            </div>
        </div>
    );
};

export default TrafficChart;
