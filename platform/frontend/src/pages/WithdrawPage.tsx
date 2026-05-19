import React, { useState } from 'react';
import WithdrawEntryForm from '../components/withdraw/form/WithdrawEntryForm';
import WithdrawConfirmForm from '../components/withdraw/form/WithdrawConfirmForm';
import PinpadModal from '../components/pinpad/PinpadModal';
import type { WithdrawData } from '../types/withdraw';

const WithdrawPage: React.FC = () => {
    const [step, setStep] = useState<'entry' | 'confirm'>('entry');
    const [withdrawData, setWithdrawData] = useState<WithdrawData | null>(null);
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);

    const handleNext = (data: WithdrawData) => {
        setWithdrawData(data);
        setStep('confirm');
    };

    const handleBack = () => {
        setStep('entry');
    };

    const handleConfirm = () => {
        setIsPinpadOpen(true);
    };

    const handlePinComplete = (pin: string) => {
        console.log('암호 입력 완료 (E2EE 암호화 대상):', pin);
        setIsPinpadOpen(false);
        alert('출금이 성공적으로 요청되었습니다.');
        // TODO: 실제 API 연동
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-full flex flex-col bg-gray-50">
            <header className="mb-10">
                <h1 className="text-4xl font-black text-gray-900 tracking-tight">
                    {step === 'entry' ? '출금 정보 입력' : '출금 정보 확인'}
                </h1>
                <p className="text-gray-500 mt-2 font-medium">
                    {step === 'entry' 
                        ? '안전한 출금 거래를 위해 정보를 정확히 입력해주세요.' 
                        : '입력하신 출금 정보를 다시 한번 확인해주세요.'}
                </p>
            </header>

            <main className="flex-1 flex justify-center items-start pt-4">
                {step === 'entry' ? (
                    <WithdrawEntryForm 
                        initialData={withdrawData ? {
                            depositBank: withdrawData.depositInfo.bankName,
                            depositAccount: withdrawData.depositInfo.accountNumber,
                            amount: withdrawData.amount
                        } : undefined}
                        onNext={handleNext} 
                    />
                ) : withdrawData ? (
                    <WithdrawConfirmForm 
                        data={withdrawData} 
                        onConfirm={handleConfirm}
                        onBack={handleBack}
                    />
                ) : null}
            </main>

            <PinpadModal 
                isOpen={isPinpadOpen}
                onClose={() => setIsPinpadOpen(false)}
                onComplete={handlePinComplete}
                title="출금 비밀번호 입력"
            />
        </div>
    );
};

export default WithdrawPage;
