import React, { useState } from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import InquiryInputSection from '../section/InquiryInputSection';
import StepActionSection from '../section/StepActionSection';
import { useTransferStore } from '../../../store/useTransferStore';
import { getBalance } from '../../../api/transfer';

const AccountInquiryForm: React.FC = () => {
    const { fromBank, fromAccountNumber, customerRrnPrefix, nextStep, updateData } = useTransferStore();
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const handleInquiry = async () => {
        if (customerRrnPrefix.length !== 7) {
            setError('주민등록번호 앞 7자리를 정확히 입력해주세요.');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            // 실제 API 호출 (보안 필드는 현재 요구사항에 따라 더미 데이터로 전송)
            const response = await getBalance({
                bankCode: fromBank, // 실제로는 은행명 대신 코드가 필요할 수 있으나 현재는 입력된 값 그대로 사용
                accountNo: fromAccountNumber,
                customerRrnPrefix: customerRrnPrefix,
                encryptedKey: 'TEMP_ENCRYPTED_KEY',
                jwsSignature: 'TEMP_JWS_SIGNATURE'
            });

            if (response.success) {
                // 조회된 잔액 정보 등을 스토어에 업데이트
                updateData({ balance: response.data.balance });
                nextStep();
            } else {
                const errorDetail = response.error 
                    ? `[${response.error.code}] ${response.error.message}`
                    : '계좌 조회에 실패했습니다.';
                setError(errorDetail);
            }
        } catch (err: any) {
            console.error('계좌 조회 에러:', err);
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
                        nextLabel={isLoading ? "조회 중..." : "계좌 조회"}
                        nextDisabled={!fromBank || !fromAccountNumber || customerRrnPrefix.length !== 7 || isLoading}
                    />
                </div>
            </div>
        </div>
    );
};

export default AccountInquiryForm;
