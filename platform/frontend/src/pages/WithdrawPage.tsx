import { useState, useEffect } from 'react';
import WithdrawEntryForm from '../components/withdraw/form/WithdrawEntryForm';
import WithdrawResultView from '../components/withdraw/form/WithdrawResultView';
import WithdrawFailureView from '../components/withdraw/form/WithdrawFailureView';
import PinpadModal from '../components/pinpad/PinpadModal';
import type { WithdrawData, WithdrawResult } from '../types/withdraw';
import { executeWithdraw } from '../api/withdraw';
import { fetchBankList, type BankOption } from '../api/loanApi';
import PageHeader from '../components/common/PageHeader';
import { AlertTriangle } from 'lucide-react';
import { Button } from '../components/common/Button';
import { formatAmount } from '../utils/formatter';

const WithdrawPage: React.FC = () => {
    const [step, setStep] = useState<'entry' | 'success' | 'failure'>('entry');
    const [withdrawData, setWithdrawData] = useState<WithdrawData | null>(null);
    const [withdrawResult, setWithdrawResult] = useState<WithdrawResult | null>(null);
    const [errorType, setErrorType] = useState<'INVALID_PASSWORD' | 'SUSPENDED_ACCOUNT' | 'SYSTEM_ERROR'>('SYSTEM_ERROR');
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
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
        setIsConfirmModalOpen(true);
    };

    const handleActualConfirm = () => {
        setIsConfirmModalOpen(false);
        setIsPinpadOpen(true);
    };

    const handlePinComplete = async (pin: string) => {
        if (!withdrawData) return;
        setIsPinpadOpen(false);
        setIsProcessing(true);

        try {
            // API 레이어(api/withdraw.ts)에서 prepareSecureRequest를 수행하므로 raw 데이터를 전달합니다.
            const response = await executeWithdraw({
                withdrawalBankCode: withdrawData.sourceAccount.bankCode,
                withdrawalAccountNo: withdrawData.sourceAccount.accountNumber,
                withdrawalPassword: pin,
                customerRrnPrefix: withdrawData.birthDate,
                customerName: withdrawData.userName || '',
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

    const handleRetry = () => setIsConfirmModalOpen(true);
    const handleHome = () => {
        setStep('entry');
        setWithdrawData(null);
        setWithdrawResult(null);
    };
    const handleCloseResult = () => handleHome();

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50 relative">
            <div className="px-10 py-12 max-w-7xl mx-auto min-h-full flex flex-col">
                {isProcessing && (
                    <div className="absolute inset-0 z-[100] bg-white/60 backdrop-blur-sm flex flex-col items-center justify-center">
                        <div className="w-16 h-16 border-4 border-emerald-100 border-t-emerald-800 rounded-full animate-spin mb-4" />
                        <p className="text-xl font-bold text-slate-900">출금 처리 중...</p>
                        <p className="text-slate-500 mt-2 font-medium">잠시만 기다려주세요.</p>
                    </div>
                )}
                
                <PageHeader 
                    title={step === 'entry' ? '출금 정보 입력' : '출금 결과'}
                    description={step === 'entry' 
                        ? '안전한 출금 거래를 위해 정보를 정확히 입력해 주세요.' 
                        : ''}
                />

                <main className="w-full flex-1 flex flex-col">
                    {step === 'entry' ? (
                        <WithdrawEntryForm 
                            initialData={withdrawData || undefined}
                            onNext={handleNext}
                            banks={banks}
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
            </div>

            {/* 출금 정보 최종 확인 모달 */}
            {isConfirmModalOpen && withdrawData && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
                    <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                        {/* 헤더 */}
                        <div className="bg-emerald-600 px-10 py-8 flex items-center gap-6">
                            <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center backdrop-blur-md">
                                <AlertTriangle className="w-8 h-8 text-white" />
                            </div>
                            <div>
                                <h3 className="text-2xl font-black text-white">최종 정보 확인</h3>
                                <p className="text-emerald-100 mt-1 font-medium">
                                    입력하신 출금 정보가 정확한지 확인해 주세요.
                                </p>
                            </div>
                        </div>

                        <div className="p-10 space-y-8">
                            <div className="bg-slate-50 rounded-3xl p-8 space-y-5 border border-slate-100">
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">고객 성명</span>
                                    <span className="text-lg font-black text-slate-900">{withdrawData.userName}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">주민등록번호</span>
                                    <span className="text-lg font-black text-slate-900 font-mono tracking-widest">
                                        {withdrawData.birthDate.slice(0, 6)}-{withdrawData.birthDate.slice(6, 7)}●●●●●●
                                    </span>
                                </div>
                                <div className="flex justify-between items-center pt-5 border-t border-slate-200/50">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">출금 계좌</span>
                                    <div className="text-right">
                                        <p className="text-sm font-bold text-emerald-600 font-mono">{withdrawData.sourceAccount.bankName}</p>
                                        <p className="text-lg font-black text-slate-900">{withdrawData.sourceAccount.accountNumber}</p>
                                    </div>
                                </div>
                                
                                <div className="h-px bg-slate-200/30 mx-2" />

                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">출금 신청 금액</span>
                                    <span className="text-xl font-black text-slate-900">₩ {formatAmount(withdrawData.amount)}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">수수료</span>
                                    <span className="text-sm font-bold text-emerald-600">0원 (면제)</span>
                                </div>

                                <div className="flex justify-between items-center pt-5 border-t-2 border-slate-200 border-dashed">
                                    <span className="text-xs font-black text-slate-500 uppercase tracking-wider">출금 후 예상 잔액</span>
                                    <div className="text-right">
                                        <p className="text-2xl font-black text-emerald-600">
                                            ₩ {formatAmount((withdrawData.sourceAccount.balance || 0) - Number(withdrawData.amount))}
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* 버튼 */}
                        <div className="px-10 pb-10 grid grid-cols-2 gap-4">
                            <Button
                                onClick={() => setIsConfirmModalOpen(false)}
                                variant="secondary"
                                size="xl"
                                className="h-16 rounded-2xl bg-slate-100 text-slate-600 hover:bg-slate-200"
                            >
                                수정하기
                            </Button>
                            <Button
                                onClick={handleActualConfirm}
                                variant="primary"
                                size="xl"
                                className="h-16 rounded-2xl bg-slate-900 text-white hover:bg-slate-800 shadow-xl shadow-slate-900/20"
                            >
                                확인 완료
                            </Button>
                        </div>
                    </div>
                </div>
            )}

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
