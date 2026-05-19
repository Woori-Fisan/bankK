import React from 'react';
import { Landmark, CheckCircle2 } from 'lucide-react';

interface Props {
    recipientName: string;
    recipientBank: string;
    recipientAccount: string;
}

const FinalRecipientCardSection: React.FC<Props> = ({ recipientName, recipientBank, recipientAccount }) => (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        <div className="flex justify-between items-center mb-6">
            <h3 className="text-lg font-bold text-gray-900">받는 사람 정보</h3>
            <div className="flex items-center gap-1 px-2 py-1 bg-emerald-100 rounded-lg text-emerald-600 text-[10px] font-bold">
                <CheckCircle2 className="w-3 h-3" />
                확인됨
            </div>
        </div>
        <div className="bg-gray-50 rounded-xl p-6 flex items-center gap-4 border border-gray-100">
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                <Landmark className="w-6 h-6 text-blue-600" />
            </div>
            <div>
                <div className="text-xl font-bold text-gray-900">{recipientName}</div>
                <div className="text-sm text-gray-500">{recipientBank} &bull; {recipientAccount}</div>
            </div>
        </div>
    </div>
);

export default FinalRecipientCardSection;
