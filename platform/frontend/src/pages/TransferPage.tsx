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
        // 6단계: 최종 정보 확인 및 비밀번호 입력
        // 7단계: 이체 결과 확인
        switch (step) {
            case 6: return <FinalConfirmForm />;
            case 7: return <ResultForm />;
            default: return <TransferEntryForm />;
        }
    };

    return (
        <div className="p-10 max-w-7xl mx-auto w-full min-h-full flex flex-col bg-gray-50/50 relative">
            {renderStep()}
        </div>
    );
};

export default TransferPage;
