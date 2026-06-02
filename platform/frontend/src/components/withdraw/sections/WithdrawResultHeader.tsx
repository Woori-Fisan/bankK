import React from 'react';
import { Check } from 'lucide-react';

const WithdrawResultHeader: React.FC = () => {
    return (
        <div className="flex flex-col items-center text-center space-y-4">
            <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center">
                <Check className="w-8 h-8 text-emerald-600" strokeWidth={3} />
            </div>
            <div className="space-y-1">
                <h2 className="text-2xl font-black text-slate-900 tracking-tight">
                    출금이 성공적으로 완료되었습니다
                </h2>
                <p className="text-sm font-bold text-slate-400">
                    요청하신 금액이 안전하게 출금되었습니다.
                </p>
            </div>
        </div>
    );
};

export default WithdrawResultHeader;
