import React, { useEffect } from 'react';
import { useTransferStore } from '../store/useTransferStore';
import TransferEntryForm from '../components/transfer/form/TransferEntryForm';
import FinalConfirmForm from '../components/transfer/form/FinalConfirmForm';
import ResultForm from '../components/transfer/form/ResultForm';

const TransferPage: React.FC = () => {
    const { step, reset } = useTransferStore();

    // 페이지 이탈 시 상태 리셋 (보안)
    useEffect(() => {
        return () => {
            reset();
        };
    }, [reset]);

    const renderStep = () => {
        // 1~5단계: 통합 입력 폼
        // 7단계: 이체 결과 확인
        switch (step) {
            case 7: return <ResultForm />;
            default: return <TransferEntryForm />;
        }
    };

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50 relative">
            <div className="px-10 py-12 max-w-7xl mx-auto w-full min-h-full flex flex-col">
                {renderStep()}
            </div>

            {/* 최종 정보 확인 모달 (Step 6) */}
            {step === 6 && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
                    <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                        <FinalConfirmForm />
                    </div>
                </div>
            )}
        </div>
    );
};

export default TransferPage;
