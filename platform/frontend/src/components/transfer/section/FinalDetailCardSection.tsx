import React from 'react';

interface Props {
    fromAccount: string;
    amount: number;
    memo: string;
    setMemo: (val: string) => void;
}

const FinalDetailCardSection: React.FC<Props> = ({ fromAccount, amount, memo, setMemo }) => (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        <h3 className="text-lg font-bold text-gray-900 mb-6">이체 상세 정보</h3>
        <div className="space-y-6">
            <div className="flex justify-between items-center py-2 border-b border-gray-50">
                <span className="text-sm text-gray-500">출금 계좌</span>
                <span className="text-sm font-bold text-gray-900">{fromAccount}</span>
            </div>
            <div className="flex justify-between items-center py-4 border-b border-gray-50">
                <span className="text-sm text-gray-500">이체 금액</span>
                <span className="text-2xl font-bold text-emerald-700">₩ {amount.toLocaleString()}</span>
            </div>
            <div className="flex justify-between items-center py-4">
                <span className="text-sm text-gray-500">받는 분에게 표시</span>
                <input 
                    type="text"
                    value={memo}
                    onChange={(e) => setMemo(e.target.value)}
                    className="text-right text-sm font-medium text-gray-900 border-b border-transparent focus:border-emerald-500 focus:outline-none transition-all py-1 px-2"
                />
            </div>
        </div>
    </div>
);

export default FinalDetailCardSection;
