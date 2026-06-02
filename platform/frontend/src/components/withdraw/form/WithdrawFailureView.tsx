import React from 'react';
import { AlertCircle, RefreshCcw, Home } from 'lucide-react';

interface WithdrawFailureViewProps {
    errorType: 'INVALID_PASSWORD' | 'SUSPENDED_ACCOUNT' | 'SYSTEM_ERROR';
    onRetry: () => void;
    onHome: () => void;
}

const WithdrawFailureView: React.FC<WithdrawFailureViewProps> = ({
    errorType,
    onRetry,
    onHome,
}) => {
    const errorConfig = {
        INVALID_PASSWORD: {
            title: '비밀번호가 일치하지 않습니다',
            description: '입력하신 계좌 비밀번호가 틀렸습니다. 다시 확인 후 시도해주세요.',
            iconColor: 'text-orange-500',
            bgColor: 'bg-orange-50',
        },
        SUSPENDED_ACCOUNT: {
            title: '정지된 계좌입니다',
            description: '해당 계좌는 현재 거래가 정지된 상태입니다. 해당 은행에 문의해주세요.',
            iconColor: 'text-orange-500',
            bgColor: 'bg-orange-50',
        },
        SYSTEM_ERROR: {
            title: '시스템 장애가 발생했습니다',
            description: '서버와의 통신이 원활하지 않습니다. 잠시 후 다시 시도해주세요.',
            iconColor: 'text-red-500',
            bgColor: 'bg-red-50',
        },
    };

    const config = errorConfig[errorType];

    return (
        <div className="w-full max-w-2xl mx-auto bg-white rounded-[2rem] shadow-xl shadow-gray-200/50 border border-gray-100 overflow-hidden animate-in fade-in zoom-in duration-500">
            <div className="p-8 md:p-12 flex flex-col items-center text-center space-y-8">
                {/* Error Icon */}
                <div className={`w-20 h-20 ${config.bgColor} rounded-full flex items-center justify-center`}>
                    <AlertCircle className={`w-12 h-12 ${config.iconColor}`} strokeWidth={2.5} />
                </div>

                {/* Message */}
                <div className="space-y-3">
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">
                        {config.title}
                    </h2>
                    <p className="text-gray-500 font-medium leading-relaxed">
                        {config.description}
                    </p>
                </div>

                {/* Illustration/Spacer (Optional) */}
                <div className="py-4 w-full max-w-xs border-b border-gray-50"></div>

                {/* Actions */}
                <div className="w-full grid grid-cols-2 gap-4 pt-4">
                    <button
                        type="button"
                        onClick={onHome}
                        className="flex items-center justify-center gap-2 py-5 bg-gray-100 text-gray-600 text-lg font-black rounded-2xl hover:bg-gray-200 active:scale-[0.98] transition-all"
                    >
                        <Home className="w-5 h-5" />
                        처음으로
                    </button>
                    <button
                        type="button"
                        onClick={onRetry}
                        className="flex items-center justify-center gap-2 py-5 bg-emerald-800 text-white text-lg font-black rounded-2xl hover:bg-emerald-900 active:scale-[0.98] transition-all shadow-lg shadow-emerald-800/20"
                    >
                        <RefreshCcw className="w-5 h-5" />
                        다시 시도
                    </button>
                </div>
            </div>
        </div>
    );
};

export default WithdrawFailureView;
