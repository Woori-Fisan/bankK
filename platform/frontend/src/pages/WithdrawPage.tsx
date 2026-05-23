import React, { useState } from 'react';
import WithdrawEntryForm from '../components/withdraw/form/WithdrawEntryForm';
import WithdrawConfirmForm from '../components/withdraw/form/WithdrawConfirmForm';
import WithdrawResultView from '../components/withdraw/form/WithdrawResultView';
import WithdrawFailureView from '../components/withdraw/form/WithdrawFailureView';
import PinpadModal from '../components/pinpad/PinpadModal';
import type { WithdrawData, WithdrawResult } from '../types/withdraw';
import { executeWithdraw } from '../api/withdraw';

const WithdrawPage: React.FC = () => {
    const [step, setStep] = useState<'entry' | 'confirm' | 'success' | 'failure'>('entry');
    const [withdrawData, setWithdrawData] = useState<WithdrawData | null>(null);
    const [withdrawResult, setWithdrawResult] = useState<WithdrawResult | null>(null);
    const [errorType, setErrorType] = useState<'INVALID_PASSWORD' | 'SUSPENDED_ACCOUNT' | 'SYSTEM_ERROR'>('SYSTEM_ERROR');
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);

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

    const handlePinComplete = async (pin: string) => {
        if (!withdrawData) return;

        console.log('암호 입력 완료 (E2EE 암호화 대상):', pin);
        setIsPinpadOpen(false);
        setIsProcessing(true);

        try {
            // 은행명 -> 은행코드 매핑
            const bankCodeMap: Record<string, string> = {
                '국민은행': '004', 'KB국민은행': '004',
                '우리은행': '020',
                '신한은행': '088',
                '하나은행': '081',
                '농협은행': '011', 'NH농협은행': '011'
            };
            const bankCode = bankCodeMap[withdrawData.sourceAccount.bankName] || '020';

            // 출금 실행 API 호출
            const response = await executeWithdraw({
                encryptedKey: 'DUMMY_ENCRYPTED_KEY', // 추후 RSA 구현체와 연동
                jwsSignature: 'DUMMY_JWS_SIGNATURE', // 추후 JWS 구현체와 연동
                withdrawalBankCode: bankCode,
                withdrawalAccountNo: withdrawData.sourceAccount.accountNumber,
                withdrawalPassword: pin, // 실제 운영환경에서는 암호화된 값 전송
                customerRrnPrefix: withdrawData.birthDate,
                amount: Number(withdrawData.amount)
            });

            if (response.success && response.data) {
                const balanceBefore = withdrawData.sourceAccount.balance || 0;
                const balanceAfter = Number(response.data.balanceAfter);

                setWithdrawResult({
                    balanceBefore,
                    balanceAfter,
                    transactionId: response.data.transactionId,
                    dateTime: response.data.transactionDate
                });
                setStep('success');
            } else {
                // 실패 처리
                const code = response.error?.code;
                if (code === 'BANK_003') {
                    setErrorType('INVALID_PASSWORD');
                } else if (code === 'TRANSFER_005') {
                    setErrorType('SUSPENDED_ACCOUNT');
                } else {
                    setErrorType('SYSTEM_ERROR');
                }
                setStep('failure');
            }
        } catch (error: any) {
            console.error('출금 처리 중 오류 발생:', error);
            
            // Axios 에러인 경우 응답 바디의 에러 코드 확인
            const errorCode = error.response?.data?.error?.code;
            if (errorCode === 'BANK_003') {
                setErrorType('INVALID_PASSWORD');
            } else if (errorCode === 'TRANSFER_005') {
                setErrorType('SUSPENDED_ACCOUNT');
            } else {
                setErrorType('SYSTEM_ERROR');
            }
            setStep('failure');
        } finally {
            setIsProcessing(false);
        }
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
        <div className="p-8 min-h-full flex flex-col bg-gray-50 w-full max-w-[1600px] mx-auto relative">
            {isProcessing && (
                <div className="absolute inset-0 z-[100] bg-white/60 backdrop-blur-sm flex flex-col items-center justify-center">
                    <div className="w-16 h-16 border-4 border-emerald-100 border-t-emerald-800 rounded-full animate-spin mb-4" />
                    <p className="text-xl font-bold text-gray-900">출금 처리 중...</p>
                    <p className="text-gray-500 mt-2">잠시만 기다려주세요.</p>
                </div>
            )}
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
                        initialData={withdrawData || undefined}
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
