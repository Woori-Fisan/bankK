import React, { useState } from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import RecipientInputFieldsSection from '../section/RecipientInputFieldsSection';
import StepActionSection from '../section/StepActionSection';
import { useTransferStore } from '../../../store/useTransferStore';

const RecipientInputForm: React.FC = () => {
    const { toBank, toAccountNumber, prevStep, nextStep } = useTransferStore();
    const [error, setError] = useState<string | null>(null);

    const handleInquiry = () => {
        if (toAccountNumber === '0000') {
            setError('수취인 계좌 정보가 올바르지 않습니다.');
        } else {
            setError(null);
            nextStep();
        }
    };

    return (
        <div className="w-full">
            <StepHeaderSection 
                title="수취인 확인" 
                description="수취인 정보를 정확하게 입력해주십시오. 오류 발생 시 이체가 지연될 수 있습니다."
            />
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                <RecipientInputFieldsSection setError={setError} />
                <div className="h-px bg-gray-100 w-full my-4"></div>
                <div className="space-y-4">
                    {error && (
                        <div className="flex items-center gap-2 px-4 py-3 bg-red-50 border border-red-100 rounded-xl text-red-600 text-sm font-bold">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            {error}
                        </div>
                    )}
                    <StepActionSection 
                        onPrev={prevStep}
                        onNext={handleInquiry}
                        nextLabel="수취인 조회"
                        nextDisabled={!toBank || !toAccountNumber}
                    />
                </div>
            </div>
        </div>
    );
};

export default RecipientInputForm;
