import React from 'react';
import { CheckCircle2 } from 'lucide-react';

interface Props {
    amount: number;
}

const SuccessSummarySection: React.FC<Props> = ({ amount }) => (
    <div className="p-12 flex flex-col items-center border-b border-gray-50">
        <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center mb-6">
            <CheckCircle2 className="w-10 h-10 text-emerald-600" />
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-2">이체가 성공적으로 완료되었습니다</h2>
        <p className="text-gray-400 text-sm">Transfer successfully completed.</p>
        <div className="mt-10 bg-white border border-gray-100 rounded-2xl p-8 w-full max-w-md shadow-sm">
            <div className="text-center">
                <span className="text-xs text-gray-400 font-bold uppercase tracking-wider block mb-2">이체 금액</span>
                <span className="text-4xl font-bold text-gray-900">₩ {amount.toLocaleString()}</span>
            </div>
        </div>
    </div>
);

export default SuccessSummarySection;
