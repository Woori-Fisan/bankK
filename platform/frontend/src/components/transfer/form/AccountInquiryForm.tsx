import React, { useState } from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import InquiryInputSection from '../section/InquiryInputSection';
import StepActionSection from '../section/StepActionSection';
import { useTransferStore } from '../../../store/useTransferStore';

const AccountInquiryForm: React.FC = () => {
    const { fromBank, fromAccountNumber, fromName, nextStep } = useTransferStore();
    const [error, setError] = useState<string | null>(null);

    const handleInquiry = () => {
        if (fromAccountNumber === '0000') {
            setError('올바르지 않은 계좌 정보입니다.');
        } else {
            setError(null);
            nextStep();
        }
    };

    return (
        <div className="w-full">
            <StepHeaderSection 
                title="이체 출금 계좌 조회" 
                description="출금할 계좌의 정보를 조회하기 위한 정보를 입력해주세요."
            />
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                <InquiryInputSection setError={setError} />
                <div className="mt-6 space-y-4">
                    {error && (
                        <div className="flex items-center gap-2 px-4 py-3 bg-red-50 border border-red-100 rounded-xl text-red-600 text-sm font-bold animate-in fade-in slide-in-from-top-1 duration-200">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            {error}
                        </div>
                    )}
                    <StepActionSection 
                        onNext={handleInquiry}
                        nextLabel="계좌 조회"
                        nextDisabled={!fromBank || !fromAccountNumber || !fromName}
                    />
                </div>
            </div>
        </div>
    );
};

export default AccountInquiryForm;
