import React, { useState, useEffect } from 'react';
import WithdrawEntryForm from '../components/withdraw/form/WithdrawEntryForm';
import WithdrawConfirmForm from '../components/withdraw/form/WithdrawConfirmForm';
import WithdrawResultView from '../components/withdraw/form/WithdrawResultView';
import WithdrawFailureView from '../components/withdraw/form/WithdrawFailureView';
import PinpadModal from '../components/pinpad/PinpadModal';
import type { WithdrawData, WithdrawResult } from '../types/withdraw';
import { executeWithdraw } from '../api/withdraw';
import { fetchBankList, type BankOption } from '../api/loanApi';
import PageHeader from '../components/common/PageHeader';

const WithdrawPage: React.FC = () => {
    const [step, setStep] = useState<'entry' | 'confirm' | 'success' | 'failure'>('entry');
    const [withdrawData, setWithdrawData] = useState<WithdrawData | null>(null);
    const [withdrawResult, setWithdrawResult] = useState<WithdrawResult | null>(null);
    const [errorType, setErrorType] = useState<'INVALID_PASSWORD' | 'SUSPENDED_ACCOUNT' | 'SYSTEM_ERROR'>('SYSTEM_ERROR');
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [banks, setBanks] = useState<BankOption[]>([]);

    useEffect(() => {
        const getBanks = async () => {
            try {
                const bankList = await fetchBankList();
                setBanks(bankList);
            } catch (error) {
                console.error('은행 목록을 불러오는데 실패했습니다.', error);
            }
        };
        getBanks();
    }, []);

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
        setIsPinpadOpen(false);
        setIsProcessing(true);

        try {
            const response = await executeWithdraw({
                encryptedKey: 'DUMMY_ENCRYPTED_KEY',
                jwsSignature: 'DUMMY_JWS_SIGNATURE',
                withdrawalBankCode: withdrawData.sourceAccount.bankCode,
                withdrawalAccountNo: withdrawData.sourceAccount.accountNumber,
                withdrawalPassword: pin,
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
                const code = response.error?.code;
                if (code === 'BANK_003') setErrorType('INVALID_PASSWORD');
                else if (code === 'TRANSFER_005') setErrorType('SUSPENDED_ACCOUNT');
                else setErrorType('SYSTEM_ERROR');
                setStep('failure');
            }
        } catch (error: any) {
            console.error('출금 처리 중 오류 발생:', error);
            const errorCode = error.response?.data?.error?.code;
            if (errorCode === 'BANK_003') setErrorType('INVALID_PASSWORD');
            else if (errorCode === 'TRANSFER_005') setErrorType('SUSPENDED_ACCOUNT');
            else setErrorType('SYSTEM_ERROR');
            setStep('failure');
        } finally {
            setIsProcessing(false);
        }
    };

    const handleRetry = () => setStep('confirm');
    const handleHome = () => {
        setStep('entry');
        setWithdrawData(null);
        setWithdrawResult(null);
    };
    const handleCloseResult = () => handleHome();

    return (
        <div className="p-10 max-w-7xl mx-auto min-h-full flex flex-col bg-gray-50/50 relative">
            {isProcessing && (
                <div className="absolute inset-0 z-[100] bg-white/60 backdrop-blur-sm flex flex-col items-center justify-center">
                    <div className="w-16 h-16 border-4 border-emerald-100 border-t-emerald-800 rounded-full animate-spin mb-4" />
                    <p className="text-xl font-bold text-slate-900">출금 처리 중...</p>
                    <p className="text-slate-500 mt-2 font-medium">잠시만 기다려주세요.</p>
                </div>
            )}
            
            <PageHeader 
                title={step === 'entry' ? '출금 정보 입력' : step === 'confirm' ? '출금 정보 확인' : '출금 결과'}
                description={step === 'entry' 
                    ? '안전한 출금 거래를 위해 정보를 정확히 입력해 주세요.' 
                    : step === 'confirm' 
                    ? '입력하신 출금 정보를 다시 한번 확인해 주세요.' 
                    : ''}
            />

            <main className="w-full flex-1 flex flex-col">
                {step === 'entry' ? (
                    <WithdrawEntryForm 
                        initialData={withdrawData || undefined}
                        onNext={handleNext}
                        banks={banks}
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
