import React, { useState } from 'react';
import WithdrawAccountSection from '../sections/WithdrawAccountSection';
import AmountInputSection from '../sections/AmountInputSection';
import WithdrawFeeSection from '../sections/WithdrawFeeSection';
import { formatAmount } from '../../../utils/formatter';
import type { WithdrawData } from '../../../types/withdraw';

export interface WithdrawEntryFormProps {
    initialData?: {
        amount: string;
        birthDate?: string;
    };
    onNext: (data: WithdrawData) => void;
}

const WithdrawEntryForm: React.FC<WithdrawEntryFormProps> = ({ initialData, onNext }) => {
    // 1. 상태 관리
    const [sourceAccount, setSourceAccount] = useState<{
        bankName: string;
        accountNumber: string;
        balance?: number;
    }>({
        bankName: '국민',
        accountNumber: '',
        balance: undefined,
    });

    const [birthDate, setBirthDate] = useState(initialData?.birthDate || '');
    const [amount, setAmount] = useState(initialData?.amount || '0');
    const [fee] = useState(0);
    const [isCheckingBalance, setIsCheckingBalance] = useState(false);

    // 2. 계좌 조회 Debounce 로직 (onBlur 활용)
    // 포커스 탈출 시 Mock API 호출 시뮬레이션을 위해 별도 핸들러 구현
    const handleCheckBalance = () => {
        // 필수 정보 미입력 시 중단 (은행명, 계좌번호, 생년월일 7자리)
        if (!sourceAccount.bankName || !sourceAccount.accountNumber || birthDate.length !== 7) {
            return;
        }

        // 이미 조회 중이면 중단
        if (isCheckingBalance) return;

        setIsCheckingBalance(true);
        // 기존 잔액 초기화 (새로운 조회를 시각적으로 알림)
        setSourceAccount(prev => ({ ...prev, balance: undefined }));

        console.log(`[onBlur] 출금 계좌 조회 시작: ${sourceAccount.bankName} ${sourceAccount.accountNumber} / ${birthDate}`);
        
        // Mock API 호출 시뮬레이션 (Debounce 효과를 위해 1초 대기)
        setTimeout(() => {
            const randomBalance = Math.floor(Math.random() * 5000000) + 1000000; // 100만 ~ 600만 랜덤 잔액
            setSourceAccount(prev => ({
                ...prev,
                balance: randomBalance,
            }));
            setIsCheckingBalance(false);
            console.log(`[onBlur] 출금 계좌 조회 완료: 잔액 ${randomBalance}`);
        }, 1000);
    };
    
    // 3. 핸들러
    const handleSourceBankChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSourceAccount(prev => ({ ...prev, bankName: e.target.value, balance: undefined }));
    };

    const handleSourceAccountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSourceAccount(prev => ({ ...prev, accountNumber: e.target.value, balance: undefined }));
    };

    const handleBirthDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value.replace(/[^0-9]/g, '').slice(0, 7);
        setBirthDate(val);
        setSourceAccount(prev => ({ ...prev, balance: undefined }));
    };

    const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value.replace(/[^0-9]/g, '');
        setAmount(val);
    };

    const handleQuickAmountAdd = (val: number) => {
        const current = parseInt(amount || '0', 10);
        setAmount((current + val).toString());
    };

    const handleAllIn = () => {
        if (sourceAccount.balance !== undefined) {
            setAmount(sourceAccount.balance.toString());
        }
    };

    const handleSubmit = () => {
        if (birthDate.length !== 7) {
            alert('생년월일 및 주민번호 뒷자리 첫글자(총 7자리)를 정확히 입력해주세요.');
            return;
        }

        onNext({
            sourceAccount: {
                ...sourceAccount,
            },
            birthDate,
            amount,
            fee,
        });
    };

    return (
        <div className="w-full max-w-4xl mx-auto bg-white rounded-[2rem] shadow-xl shadow-gray-200/50 border border-gray-100 overflow-hidden">
            <div className="p-8 md:p-12 space-y-10">
                <header className="border-b border-gray-50 pb-6">
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">출금 신청</h2>
                </header>

                <div className="space-y-10">
                    <WithdrawAccountSection 
                        bankName={sourceAccount.bankName}
                        accountNumber={sourceAccount.accountNumber}
                        onBankChange={handleSourceBankChange}
                        onAccountChange={handleSourceAccountChange}
                        onBankBlur={handleCheckBalance}
                        onAccountBlur={handleCheckBalance}
                    />

                    <section className="space-y-4">
                        <div className="flex justify-between items-end">
                            <h3 className="text-sm font-medium text-gray-500">본인 확인</h3>
                            {isCheckingBalance && (
                                <span className="text-xs text-emerald-600 font-medium animate-pulse flex items-center gap-1">
                                    <span className="w-1.5 h-1.5 bg-emerald-600 rounded-full animate-bounce" />
                                    계좌 정보 확인 중...
                                </span>
                            )}
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-semibold text-gray-400">생년월일 + 뒷자리 첫글자 (7자리)</label>
                            <input
                                type="text"
                                value={birthDate}
                                onChange={handleBirthDateChange}
                                onBlur={handleCheckBalance}
                                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900 transition-all text-lg tracking-[0.5em] ${
                                    isCheckingBalance ? 'border-emerald-200 bg-emerald-50/30' : 'border-gray-200'
                                }`}
                                placeholder="YYMMDDG"
                                maxLength={7}
                            />
                        </div>
                    </section>

                    {(sourceAccount.balance !== undefined || isCheckingBalance) && (
                        <div className={`bg-gray-50 rounded-xl p-6 border border-gray-100 flex flex-col md:flex-row md:items-center justify-end gap-4 animate-in fade-in slide-in-from-top-2 duration-300 ${isCheckingBalance ? 'opacity-50' : ''}`}>
                            <div className="text-right">
                                <span className="text-xs font-semibold text-gray-400 block">현재 잔액</span>
                                <div className="text-xl font-bold text-gray-900">
                                    {isCheckingBalance ? (
                                        <span className="text-gray-300">조회 중...</span>
                                    ) : (
                                        <>
                                            <span className="text-sm mr-1">₩</span>
                                            {formatAmount(sourceAccount.balance || 0)}
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}

                    <AmountInputSection 
                        amount={amount}
                        onAmountChange={handleAmountChange}
                        onQuickAmountAdd={handleQuickAmountAdd}
                        onAllIn={handleAllIn}
                    />

                    <WithdrawFeeSection fee={fee} isWaived={true} />
                </div>

                <footer className="pt-6">
                    <button
                        type="button"
                        onClick={handleSubmit}
                        className="w-full py-5 bg-emerald-800 text-white text-xl font-black rounded-2xl hover:bg-emerald-900 active:scale-[0.98] transition-all shadow-lg shadow-emerald-800/20"
                    >
                        다음
                    </button>
                </footer>
            </div>
        </div>
    );
};

export default WithdrawEntryForm;
