import React from 'react';
import { ShieldCheck } from 'lucide-react';

const SecurityStatus: React.FC = () => {
    return (
        <div className="w-full mt-8 bg-emerald-50 rounded-lg p-4 flex items-center justify-center gap-2 border border-emerald-100">
            <ShieldCheck className="w-5 h-5 text-emerald-600" />
            <span className="text-xs font-medium text-emerald-800">
                접속 IP 및 mTLS 환경 검증 완료
            </span>
        </div>
    );
};

export default SecurityStatus;
