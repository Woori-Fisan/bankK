import React from 'react';
import { CheckCircle2 } from 'lucide-react';

interface Props {
    badgeText?: string;
    label1: string;
    value1: string;
    label2: string;
    value2: string;
    label3?: string;
    value3?: string;
}

const InfoSummaryBoxSection: React.FC<Props> = ({
    badgeText = '실명확인 완료',
    label1, value1, label2, value2, label3, value3
}) => (
    <div className="w-full bg-gray-50 rounded-2xl p-10 border border-gray-100 flex flex-col items-center gap-6 mb-10">
        <div className="flex items-center gap-1.5 px-3 py-1 bg-emerald-100 rounded-full text-emerald-600 text-xs font-bold uppercase tracking-wider">
            <CheckCircle2 className="w-3.5 h-3.5" />
            {badgeText}
        </div>

        <div className="text-center">
            <span className="text-xs text-gray-400 font-medium block mb-1">{label1}</span>
            <span className="text-4xl font-bold text-gray-900">{value1}</span>
        </div>

        <div className="text-center">
            <span className="text-xs text-gray-400 font-medium block mb-1">{label2}</span>
            <span className="text-xl font-bold text-gray-900">{value2}</span>
        </div>

        {label3 && value3 && (
            <>
                <div className="h-px bg-gray-200 w-full"></div>
                <div className="text-center">
                    <span className="text-xs text-gray-400 font-medium block mb-1">{label3}</span>
                    <span className="text-2xl font-bold text-emerald-700">{value3}</span>
                </div>
            </>
        )}
    </div>
);

export default InfoSummaryBoxSection;
