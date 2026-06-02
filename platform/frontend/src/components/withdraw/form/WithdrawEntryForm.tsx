import React, { useState, useRef } from 'react';
import { AlertCircle, ArrowRight, Wallet } from 'lucide-react';
import AccountInputSection from '../../common/AccountInputSection';
import AmountInputSection from '../sections/AmountInputSection';
import WithdrawFeeSection from '../sections/WithdrawFeeSection';
import { formatAmount } from '../../../utils/formatter';
import type { WithdrawData } from '../../../types/withdraw';
import { getBalance } from '../../../api/transfer';
import type { BankOption } from '../../../api/loanApi';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import RrnInput from '../../common/RrnInput';

export interface WithdrawEntryFormProps {
    initialData?: WithdrawData;
    onNext: (data: WithdrawData) => void;
    banks: BankOption[];
}

const WithdrawEntryForm: React.FC<WithdrawEntryFormProps> = ({ initialData, onNext, banks }) => {
    const [sourceAccount, setSourceAccount] = useState<{
        bankName: string;
        bankCode: string;
        accountNumber: string;
        balance?: number;
    }>({
        bankName: initialData?.sourceAccount.bankName || '',
        bankCode: initialData?.sourceAccount.bankCode || '',
        accountNumber: initialData?.sourceAccount.accountNumber || '',
        balance: initialData?.sourceAccount.balance,
    });

    const [birthDate, setBirthDate] = useState(initialData?.birthDate || '');
    const [amount, setAmount] = useState(initialData?.amount || '0');
    const [fee] = useState(0);
    const [isCheckingBalance, setIsCheckingBalance] = useState(false);
    const [apiError, setApiError] = useState<string | null>(null);

    const handleCheckBalance = async () => {
        if (!sourceAccount.bankCode || !sourceAccount.accountNumber || birthDate.length !== 7) return;
        if (isCheckingBalance) return;

        setIsCheckingBalance(true);
        setApiError(null);
        setSourceAccount(prev => ({ ...prev, balance: undefined }));

        try {
            const response = await getBalance({
                bankCode: sourceAccount.bankCode,
                accountNo: sourceAccount.accountNumber,
                customerRrnPrefix: birthDate,
                encryptedKey: 'DUMMY_ENCRYPTED_KEY',
                jwsSignature: 'DUMMY_JWS_SIGNATURE',
            });

            if (response.success && response.data) {
                setSourceAccount(prev => ({ ...prev, balance: parseInt(response.data.balance, 10) }));
            } else {
                setApiError(response.error?.message || '계좌 정보를 찾을 수 없습니다.');
            }
        } catch (error: any) {
            setApiError(error.response?.data?.error?.message || '조회 중 오류가 발생했습니다.');
        } finally {
            setIsCheckingBalance(false);
        }
    };

    const handleSubmit = () => {
        if (birthDate.length !== 7) {
            alert('주민등록번호를 정확히 입력해 주세요.');
            return;
        }
        onNext({ sourceAccount: { ...sourceAccount }, birthDate, amount, fee });
    };

    const isNextDisabled = sourceAccount.balance === undefined || isCheckingBalance || !amount || parseInt(amount, 10) === 0;

    return (
        <div className="w-full space-y-6 animate-in fade-in duration-500">
            {apiError && (
                <div className="flex items-center gap-3 text-rose-500 bg-rose-50 p-5 rounded-2xl border border-rose-100 max-w-full">
                    <AlertCircle className="w-6 h-6 flex-shrink-0" />
                    <span className="text-base font-bold">{apiError}</span>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 1. 입력 영역 (2컬럼 사용) */}
                <div className="lg:col-span-2 space-y-6">
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <div className="space-y-10">
                            {/* 1행: 계좌 정보 입력 */}
                            <AccountInputSection 
                                title="출금 계좌 정보"
                                bankCode={sourceAccount.bankCode}
                                accountNumber={sourceAccount.accountNumber}
                                banks={banks}
                                onBankChange={(name, code) => {
                                    setSourceAccount(prev => ({ ...prev, bankName: name, bankCode: code, balance: undefined }));
                                    setApiError(null);
                                }}
                                onAccountChange={(val) => {
                                    setSourceAccount(prev => ({ ...prev, accountNumber: val, balance: undefined }));
                                    setApiError(null);
                                }}
                                onBlur={handleCheckBalance}
                            />

                            {/* 2행: 본인 인증 입력 */}
                            <div className="pt-6 border-t border-slate-50">
                                <RrnInput 
                                    rrnFront={birthDate.slice(0, 6)}
                                    rrnBack={birthDate.slice(6, 7)}
                                    onRrnFrontChange={(val) => {
                                        setBirthDate(val + birthDate.slice(6, 7));
                                        setSourceAccount(prev => ({ ...prev, balance: undefined }));
                                        setApiError(null);
                                    }}
                                    onRrnBackChange={(val) => {
                                        setBirthDate(birthDate.slice(0, 6) + val);
                                        setSourceAccount(prev => ({ ...prev, balance: undefined }));
                                        setApiError(null);
                                    }}
                                    onBlur={handleCheckBalance}
                                    isChecking={isCheckingBalance}
                                />
                            </div>
                        </div>

                        {/* 하단: 금액 입력 */}
                        <div className="mt-10 pt-8 border-t border-slate-50">
                            <AmountInputSection 
                                amount={amount}
                                onAmountChange={(e) => setAmount(e.target.value.replace(/[^0-9]/g, ''))}
                                onQuickAmountAdd={(val) => setAmount((parseInt(amount || '0', 10) + val).toString())}
                                onAllIn={() => sourceAccount.balance && setAmount(sourceAccount.balance.toString())}
                                disabled={sourceAccount.balance === undefined || isCheckingBalance}
                            />
                        </div>
                    </Card>
                </div>

                {/* 2. 요약 및 실행 영역 (1컬럼) */}
                <div className="lg:col-span-1 space-y-6">
                    {/* 출금 가능 잔액 카드 */}
                    <Card padding="lg" className={`min-h-[180px] flex flex-col justify-between transition-all duration-500 ${sourceAccount.balance !== undefined ? 'bg-emerald-900 text-white border-none shadow-xl shadow-emerald-900/10' : 'bg-white border-slate-100 shadow-sm'}`}>
                        <div>
                            <div className="flex items-center justify-between mb-2">
                                <span className={`text-[10px] font-black uppercase tracking-widest ${sourceAccount.balance !== undefined ? 'text-emerald-300' : 'text-slate-400'}`}>잔액 정보</span>
                                <Wallet className={`w-4 h-4 ${sourceAccount.balance !== undefined ? 'text-emerald-400' : 'text-slate-300'}`} />
                            </div>
                            <h4 className={`text-xs font-bold ${sourceAccount.balance !== undefined ? 'text-emerald-100' : 'text-slate-500'}`}>출금 가능 잔액</h4>
                        </div>
                        
                        <div className="py-2">
                            {isCheckingBalance ? (
                                <div className="flex gap-1.5 items-baseline">
                                    <span className="w-2.5 h-2.5 bg-emerald-400 rounded-full animate-bounce" />
                                    <span className="w-2.5 h-2.5 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                    <span className="w-2.5 h-2.5 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                                </div>
                            ) : sourceAccount.balance !== undefined ? (
                                <p className="text-4xl font-black tracking-tighter">
                                    <span className="text-lg font-bold mr-1 opacity-60">₩</span>
                                    {formatAmount(sourceAccount.balance)}
                                </p>
                            ) : (
                                <p className="text-sm font-bold text-slate-300 leading-relaxed text-center py-4">계좌 정보 입력 시<br/>조회됩니다.</p>
                            )}
                        </div>

                        {sourceAccount.balance !== undefined && (
                            <div className="pt-3 border-t border-white/10" />
                        )}
                    </Card>

                    {/* 수수료 및 정책 카드 */}
                    <Card padding="md" className="border-slate-100 bg-white shadow-sm space-y-4">
                        <WithdrawFeeSection fee={fee} isWaived={true} />
                        <div className="pt-3 border-t border-slate-50 space-y-2">
                            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">안내 사항</p>
                            <ul className="space-y-1.5">
                                <li className="flex items-start gap-2 text-[11px] text-slate-500 font-medium">
                                    <div className="w-1 h-1 rounded-full bg-emerald-500 mt-1.5 flex-shrink-0" />
                                    출금 수수료 전액 면제
                                </li>
                                <li className="flex items-start gap-2 text-[11px] text-slate-500 font-medium">
                                    <div className="w-1 h-1 rounded-full bg-emerald-500 mt-1.5 flex-shrink-0" />
                                    1일 한도: 1,000,000원
                                </li>
                            </ul>
                        </div>
                    </Card>

                    {/* 실행 버튼 (대출 페이지 스타일 통일: 비활성 시 Gray, 활성 시 Slate) */}
                    <Button
                        onClick={handleSubmit}
                        disabled={isNextDisabled}
                        variant={isNextDisabled ? 'secondary' : 'primary'}
                        size="xl"
                        fullWidth
                        className={`h-16 rounded-2xl text-xl font-black shadow-lg transition-all group ${
                            isNextDisabled ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-slate-900 text-white hover:bg-slate-800'
                        }`}
                    >
                        다음 단계로
                        <ArrowRight className={`w-6 h-6 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default WithdrawEntryForm;
