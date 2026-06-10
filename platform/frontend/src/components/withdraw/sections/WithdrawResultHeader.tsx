import React from 'react';
import { CheckCircle2 } from 'lucide-react';

const WithdrawResultHeader: React.FC = () => {
    return (
        <div className="flex flex-col items-center text-center space-y-6 mb-12">
            <div className="w-20 h-20 bg-emerald-50 rounded-full flex items-center justify-center">
                <CheckCircle2 className="w-10 h-10 text-emerald-500" />
            </div>
            <div className="space-y-2">
                <h2 className="text-3xl font-black text-slate-900 tracking-tight">
                    출금이 성공적으로 완료되었습니다
                </h2>
                <p className="text-slate-500 font-medium text-base">
                    요청하신 금액이 안전하게 출금되었습니다.
                </p>
            </div>
        </div>
    );
};

export default WithdrawResultHeader;
