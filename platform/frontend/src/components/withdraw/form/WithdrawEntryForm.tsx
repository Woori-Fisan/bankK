import React, { useState, useEffect } from 'react';
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

    // 2. 계좌 조회 Debounce 로직 (은행, 계좌번호, 생년월일 모두 입력 시)
    useEffect(() => {
        // 모든 정보가 입력되지 않았거나, 생년월일이 6자리가 아니면 중단
        if (!sourceAccount.bankName || !sourceAccount.accountNumber || birthDate.length !== 6) {
            return;
        }

        const timer = setTimeout(() => {
            console.log(`출금 계좌 조회 시작 (전체 입력 완료): ${sourceAccount.bankName} ${sourceAccount.accountNumber} / ${birthDate}`);
            
            // Mock API 호출 시뮬레이션
            const mockFetchBalance = () => {
                const randomBalance = Math.floor(Math.random() * 100000000);
                setSourceAccount(prev => ({
                    ...prev,
                    balance: randomBalance,
                }));
                console.log(`출금 계좌 조회 완료: 잔액 ${randomBalance}`);
            };

            mockFetchBalance();
        }, 2000);

        return () => clearTimeout(timer);
    }, [sourceAccount.bankName, sourceAccount.accountNumber, birthDate]);
    
    // 3. 핸들러
    const handleSourceBankChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSourceAccount(prev => ({ ...prev, bankName: e.target.value, balance: undefined }));
    };

    const handleSourceAccountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSourceAccount(prev => ({ ...prev, accountNumber: e.target.value, balance: undefined }));
    };

    const handleBirthDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value.replace(/[^0-9]/g, '').slice(0, 6);
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
        if (birthDate.length !== 6) {
            alert('생년월일 6자리를 정확히 입력해주세요.');
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
                    />

                    <section className="space-y-4">
                        <h3 className="text-sm font-medium text-gray-500">본인 확인</h3>
                        <div className="space-y-2">
                            <label className="text-xs font-semibold text-gray-400">생년월일 (6자리)</label>
                            <input
                                type="text"
                                value={birthDate}
                                onChange={handleBirthDateChange}
                                className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900 transition-shadow text-lg tracking-widest"
                                placeholder="YYMMDD"
                                maxLength={6}
                            />
                        </div>
                    </section>

                    {sourceAccount.balance !== undefined && (
                        <div className="bg-gray-50 rounded-xl p-6 border border-gray-100 flex flex-col md:flex-row md:items-center justify-end gap-4 animate-in fade-in slide-in-from-top-2 duration-300">
                            <div className="text-right">
                                <span className="text-xs font-semibold text-gray-400 block">현재 잔액</span>
                                <div className="text-xl font-bold text-gray-900">
                                    <span className="text-sm mr-1">₩</span>
                                    {formatAmount(sourceAccount.balance)}
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
