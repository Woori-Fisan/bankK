import React, { useState } from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import FinalRecipientCardSection from '../section/FinalRecipientCardSection';
import FinalDetailCardSection from '../section/FinalDetailCardSection';
import StepActionSection from '../section/StepActionSection';
import PinpadModal from '../../pinpad/PinpadModal';
import { useTransferStore } from '../../../store/useTransferStore';

const FinalConfirmForm: React.FC = () => {
    const { fromAccountNumber, fromName, toName, toBank, toAccountNumber, amount, prevStep, nextStep } = useTransferStore();
    const [memo, setMemo] = useState(fromName);
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);

    const handlePinComplete = (pin: string) => {
        console.log('Final PIN Entered:', pin);
        setIsPinpadOpen(false);
        nextStep();
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
                    fromAccount={`기업 운영 계좌 (${fromAccountNumber})`}
                    amount={amount}
                    memo={memo}
                    setMemo={setMemo}
                />

                <StepActionSection 
                    onPrev={prevStep}
                    onNext={() => setIsPinpadOpen(true)}
                    prevLabel="이전"
                    nextLabel="인증 진행"
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
