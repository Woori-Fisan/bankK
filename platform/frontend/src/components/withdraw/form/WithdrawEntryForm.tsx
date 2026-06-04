import React, { useState, useRef } from 'react';
import { AlertCircle } from 'lucide-react';
import WithdrawAccountSection from '../sections/WithdrawAccountSection';
import AmountInputSection from '../sections/AmountInputSection';
import WithdrawFeeSection from '../sections/WithdrawFeeSection';
import { formatAmount } from '../../../utils/formatter';
import type { WithdrawData } from '../../../types/withdraw';
import { fetchBalance } from '../../../api/inquiry';

export interface WithdrawEntryFormProps {
    initialData?: WithdrawData;
    onNext: (data: WithdrawData) => void;
}

const WithdrawEntryForm: React.FC<WithdrawEntryFormProps> = ({ initialData, onNext }) => {
    // 1. 상태 관리
    const [sourceAccount, setSourceAccount] = useState<{
        bankName: string;
        accountNumber: string;
        balance?: number;
    }>({
        bankName: initialData?.sourceAccount.bankName || '',
        accountNumber: initialData?.sourceAccount.accountNumber || '',
        balance: initialData?.sourceAccount.balance,
    });

    const [birthDate, setBirthDate] = useState(initialData?.birthDate || '');
    const [amount, setAmount] = useState(initialData?.amount || '0');
    const [fee] = useState(0);
    const [isCheckingBalance, setIsCheckingBalance] = useState(false);
    const [apiError, setApiError] = useState<string | null>(null);

    const rrnBackRef = useRef<HTMLInputElement>(null);

    // 2. 계좌 조회 로직 (onBlur 활용)
    const handleCheckBalance = async () => {
        if (!sourceAccount.bankName || !sourceAccount.accountNumber || birthDate.length !== 7) {
            return;
        }

        if (isCheckingBalance) return;

        setIsCheckingBalance(true);
        setApiError(null);
        setSourceAccount(prev => ({ ...prev, balance: undefined }));

        try {
            const bankCodeMap: Record<string, string> = {
                '국민은행': '004',
                'KB국민은행': '004',
                '우리은행': '020',
                '신한은행': '088',
                '하나은행': '081',
                '농협은행': '011',
                'NH농협은행': '011'
            };
            const bankCode = bankCodeMap[sourceAccount.bankName] || '020';

            const response = await fetchBalance({
                bankCode: bankCode,
                accountNo: sourceAccount.accountNumber,
                customerRrnPrefix: birthDate
            });

            if (response.success && response.data) {
                const fetchedBalance = parseInt(response.data.balance, 10);
                setSourceAccount(prev => ({
                    ...prev,
                    balance: fetchedBalance,
                }));
            } else {
                const errorMessage = response.error?.message || '사용자의 정보를 찾을 수 없습니다.';
                setApiError(errorMessage);
            }
        } catch (error: any) {
            const errorMessage = error.response?.data?.error?.message || '사용자의 정보를 찾을 수 없습니다.';
            setApiError(errorMessage);
        } finally {
            setIsCheckingBalance(false);
        }
    };

    // 3. 핸들러
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
            alert('주민등록번호 앞 7자리를 정확히 입력해주세요.');
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

    const isNextDisabled = 
        sourceAccount.balance === undefined || 
        isCheckingBalance || 
        !amount || 
        parseInt(amount, 10) === 0;

    return (
        <div className="w-full max-w-4xl mx-auto bg-white rounded-[2rem] shadow-xl shadow-gray-200/50 border border-gray-100 overflow-hidden">
            <div className="p-8 md:p-12 space-y-10">
                <header className="border-b border-gray-50 pb-6">
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">출금 신청</h2>
                </header>

                <div className="space-y-10">
                    {apiError && (
                        <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 animate-in fade-in slide-in-from-top-1 duration-200">
                            <AlertCircle className="w-5 h-5 flex-shrink-0" />
                            <span className="text-sm font-bold">{apiError}</span>
                        </div>
                    )}

                    <WithdrawAccountSection 
                        bankName={sourceAccount.bankName}
                        accountNumber={sourceAccount.accountNumber}
                        onBankChange={(val) => {
                            setSourceAccount(prev => ({ ...prev, bankName: val, balance: undefined }));
                            setApiError(null);
                        }}
                        onAccountChange={(val) => {
                            setSourceAccount(prev => ({ ...prev, accountNumber: val, balance: undefined }));
                            setApiError(null);
                        }}
                        onBlur={handleCheckBalance}
                    />

                    <section className="space-y-4">
                        <div className="flex justify-between items-center">
                            <h3 className="text-sm font-medium text-gray-500">본인 확인</h3>
                            {isCheckingBalance && (
                                <span className="text-xs text-emerald-600 font-medium animate-pulse flex items-center gap-1">
                                    <span className="w-1.5 h-1.5 bg-emerald-600 rounded-full animate-bounce" />
                                    계좌 정보 확인 중...
                                </span>
                            )}
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-semibold text-gray-400">주민등록번호</label>
                            <div className="flex items-center gap-3">
                                <div className="flex-1">
                                    <input
                                        type="text"
                                        inputMode="numeric"
                                        placeholder="앞 6자리"
                                        maxLength={6}
                                        value={birthDate.slice(0, 6)}
                                        onChange={(e) => {
                                            const val = e.target.value.replace(/[^0-9]/g, '');
                                            if (val.length <= 6) {
                                                setBirthDate(val + birthDate.slice(6, 7));
                                                setSourceAccount(prev => ({ ...prev, balance: undefined }));
                                                setApiError(null);
                                                if (val.length === 6) {
                                                    rrnBackRef.current?.focus();
                                                }
                                            }
                                        }}
                                        onBlur={handleCheckBalance}
                                        className="w-full px-4 py-3 border border-gray-200 rounded-lg text-center text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-all tracking-[0.2em] font-mono text-lg"
                                    />
                                </div>
                                <span className="text-gray-400 font-bold text-xl">-</span>
                                <div className="flex-[1.2] flex items-center gap-2">
                                    <input
                                        ref={rrnBackRef}
                                        type="text"
                                        inputMode="numeric"
                                        maxLength={1}
                                        value={birthDate.slice(6, 7)}
                                        onChange={(e) => {
                                            const val = e.target.value.replace(/[^0-9]/g, '');
                                            if (val.length <= 1) {
                                                setBirthDate(birthDate.slice(0, 6) + val);
                                                setSourceAccount(prev => ({ ...prev, balance: undefined }));
                                                setApiError(null);
                                            }
                                        }}
                                        onBlur={handleCheckBalance}
                                        className="w-14 px-0 py-3 border border-gray-200 rounded-lg text-center text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-all font-mono text-lg"
                                    />
                                    <div className="flex gap-1.5 ml-1">
                                        {[...Array(6)].map((_, i) => (
                                            <div key={i} className="w-3 h-3 rounded-full bg-gray-200"></div>
                                        ))}
                                    </div>
                                </div>
                            </div>
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
                        disabled={sourceAccount.balance === undefined || isCheckingBalance}
                    />

                    <WithdrawFeeSection fee={fee} isWaived={true} />
                </div>

                <footer className="pt-6">
                    <button
                        type="button"
                        onClick={handleSubmit}
                        disabled={isNextDisabled}
                        className={`w-full py-5 text-white text-xl font-black rounded-2xl transition-all shadow-lg ${
                            isNextDisabled
                                ? 'bg-gray-300 cursor-not-allowed shadow-none'
                                : 'bg-emerald-800 hover:bg-emerald-900 active:scale-[0.98] shadow-emerald-800/20'
                        }`}
                    >
                        다음
                    </button>
                </footer>
            </div>
        </div>
    );
};

export default WithdrawEntryForm;
