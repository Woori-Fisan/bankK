import React, { useState } from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import RecipientInputFieldsSection from '../section/RecipientInputFieldsSection';
import StepActionSection from '../section/StepActionSection';
import { useTransferStore } from '../../../store/useTransferStore';
import { getRecipient } from '../../../api/transfer';

const RecipientInputForm: React.FC = () => {
    const { toBank, toAccountNumber, prevStep, nextStep, updateData } = useTransferStore();
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const handleInquiry = async () => {
        setIsLoading(true);
        setError(null);

        try {
            const response = await getRecipient(toBank, toAccountNumber);

            if (response.success) {
                const { depositorName, depositBankName, depositBankAccountNo } = response.data;
                updateData({ 
                    toName: depositorName,
                    toBankName: depositBankName,
                    toBankAccountNo: depositBankAccountNo
                });
                nextStep();
            } else {
                const errorDetail = response.error 
                    ? `[${response.error.code}] ${response.error.message}`
                    : '수취인 조회에 실패했습니다.';
                setError(errorDetail);
            }
        } catch (err: any) {
            console.error('수취인 조회 에러:', err);
            const apiError = err.response?.data?.error;
            if (apiError) {
                setError(`[${apiError.code}] ${apiError.message}`);
            } else {
                setError('서버 통신 중 오류가 발생했습니다.');
            }
        } finally {
            setIsLoading(false);
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
                        nextLabel={isLoading ? "조회 중..." : "수취인 조회"}
                        nextDisabled={!toBank || !toAccountNumber || isLoading}
                    />
                </div>
            </div>
        </div>
    );
};

export default RecipientInputForm;
