import React, { useState } from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import FinalRecipientCardSection from '../section/FinalRecipientCardSection';
import FinalDetailCardSection from '../section/FinalDetailCardSection';
import StepActionSection from '../section/StepActionSection';
import PinpadModal from '../../pinpad/PinpadModal';
import { useTransferStore } from '../../../store/useTransferStore';
import { executeTransfer } from '../../../api/transfer';

const FinalConfirmForm: React.FC = () => {
    const { 
        fromBank, fromAccountNumber, customerRrnPrefix,
        toBank, toAccountNumber, toName, amount,
        prevStep, nextStep, updateData 
    } = useTransferStore();
    const [memo, setMemo] = useState('');
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handlePinComplete = async (pin: string) => {
        setIsPinpadOpen(false);
        setIsLoading(true);
        setError(null);

        try {
            const response = await executeTransfer({
                withdrawalBankCode: fromBank,
                withdrawalAccountNo: fromAccountNumber,
                withdrawalPassword: pin, // 입력된 핀패드 비밀번호 사용
                customerRrnPrefix: customerRrnPrefix,
                depositBankCode: toBank,
                depositAccountNo: toAccountNumber,
                amount: amount,
                encryptedKey: 'TEMP_ENCRYPTED_KEY',
                jwsSignature: 'TEMP_JWS_SIGNATURE'
            });

            if (response.success) {
                updateData({
                    transactionId: response.data.transactionId,
                    transactionDate: response.data.transactionDate,
                    balanceAfter: response.data.balanceAfter
                });
                nextStep();
            } else {
                const errorDetail = response.error 
                    ? `[${response.error.code}] ${response.error.message}`
                    : '이체 실행에 실패했습니다.';
                setError(errorDetail);
            }
        } catch (err: any) {
            console.error('이체 실행 에러:', err);
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
                title="이체 정보 확인" 
                description="이체 실행 전 최종 정보를 확인해 주세요."
            />
            <div className="space-y-6">
                <FinalRecipientCardSection 
                    recipientName={toName}
                    recipientBank={toBank}
                    recipientAccount={toAccountNumber}
                />

                <FinalDetailCardSection 
                    fromAccount={`${fromBank} (${fromAccountNumber})`}
                    amount={amount}
                    memo={memo}
                    setMemo={setMemo}
                />

                {error && (
                    <div className="px-6 py-4 bg-red-50 border border-red-100 rounded-2xl text-red-600 text-sm font-bold flex items-center gap-2">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        {error}
                    </div>
                )}

                <StepActionSection 
                    onPrev={prevStep}
                    onNext={() => setIsPinpadOpen(true)}
                    prevLabel="이전"
                    nextLabel={isLoading ? "이체 진행 중..." : "인증 진행"}
                    nextDisabled={isLoading}
                />

                <PinpadModal 
                    isOpen={isPinpadOpen} 
                    onClose={() => setIsPinpadOpen(false)} 
                    onComplete={handlePinComplete}
                    title="계좌 비밀번호 (6자리)"
                />
            </div>
        </div>
    );
};

export default FinalConfirmForm;
