import React, { useState } from 'react';
import WithdrawEntryForm from '../components/withdraw/form/WithdrawEntryForm';
import WithdrawConfirmForm from '../components/withdraw/form/WithdrawConfirmForm';
import WithdrawResultView from '../components/withdraw/form/WithdrawResultView';
import WithdrawFailureView from '../components/withdraw/form/WithdrawFailureView';
import PinpadModal from '../components/pinpad/PinpadModal';
import type { WithdrawData, WithdrawResult } from '../types/withdraw';

const WithdrawPage: React.FC = () => {
    const [step, setStep] = useState<'entry' | 'confirm' | 'success' | 'failure'>('entry');
    const [withdrawData, setWithdrawData] = useState<WithdrawData | null>(null);
    const [withdrawResult, setWithdrawResult] = useState<WithdrawResult | null>(null);
    const [errorType, setErrorType] = useState<'INVALID_PASSWORD' | 'SYSTEM_ERROR'>('INVALID_PASSWORD');
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

        // 시뮬레이션: 무조건 성공 응답
        setTimeout(() => {
            const amount = parseInt(withdrawData?.amount || '0', 10);
            const balanceBefore = 45300000;
            const balanceAfter = balanceBefore - amount;

            setWithdrawResult({
                balanceBefore,
                balanceAfter,
                transactionId: `TX-${new Date().getTime()}-88902A`,
                dateTime: new Date().toLocaleString('ko-KR') + ' KST'
            });
            setStep('success');
        }, 500);
    };

    const handleRetry = () => {
        setStep('confirm'); // 확인 페이지로 돌아가서 다시 비밀번호 입력 시도
    };

    const handleHome = () => {
        setStep('entry');
        setWithdrawData(null);
        setWithdrawResult(null);
    };

    const handleCloseResult = () => {
        handleHome();
    };

    return (
        <div className="p-8 min-h-full flex flex-col bg-gray-50 w-full max-w-[1600px] mx-auto">
            <header className={`mb-10 w-full mx-auto transition-all duration-300 ${step === 'entry' ? 'max-w-4xl' : 'max-w-5xl'} ${step === 'success' || step === 'failure' ? 'opacity-0 h-0 overflow-hidden mb-0' : ''}`}>
                <h1 className="text-4xl font-black text-gray-900 tracking-tight">
                    {step === 'entry' ? '출금 정보 입력' : '출금 정보 확인'}
                </h1>
                <p className="text-gray-500 mt-2 font-medium">
                    {step === 'entry' 
                        ? '안전한 출금 거래를 위해 정보를 정확히 입력해주세요.' 
                        : '입력하신 출금 정보를 다시 한번 확인해주세요.'}
                </p>
            </header>

            <main className="w-full flex-1 flex justify-center items-start pt-4">
                {step === 'entry' ? (
                    <WithdrawEntryForm 
                        initialData={withdrawData ? {
                            depositBank: withdrawData.depositInfo.bankName,
                            depositAccount: withdrawData.depositInfo.accountNumber,
                            amount: withdrawData.amount
                        } : undefined}
                        onNext={handleNext} 
                    />
                ) : step === 'confirm' && withdrawData ? (
                    <WithdrawConfirmForm 
                        data={withdrawData} 
                        onConfirm={handleConfirm}
                        onBack={handleBack}
                    />
                ) : step === 'success' && withdrawData && withdrawResult ? (
                    <WithdrawResultView 
                        data={withdrawData}
                        result={withdrawResult}
                        onClose={handleCloseResult}
                    />
                ) : step === 'failure' ? (
                    <WithdrawFailureView 
                        errorType={errorType}
                        onRetry={handleRetry}
                        onHome={handleHome}
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
