import { useState } from 'react';
import { AlertCircle, ChevronRight, Search, User } from 'lucide-react';
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
import Input from '../../common/Input';

export interface WithdrawEntryFormProps {
    initialData?: WithdrawData;
    onNext: (data: WithdrawData) => void;
    banks: BankOption[];
}

const WithdrawEntryForm: React.FC<WithdrawEntryFormProps> = ({ initialData, onNext, banks }) => {
    const [userName, setUserName] = useState(initialData?.userName || '');
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
        if (!userName) {
            alert('고객 성명을 입력해 주세요.');
            return;
        }
        if (birthDate.length !== 7) {
            alert('주민등록번호를 정확히 입력해 주세요.');
            return;
        }
        onNext({ userName, sourceAccount: { ...sourceAccount }, birthDate, amount, fee });
    };

    const isNextDisabled = !userName || sourceAccount.balance === undefined || isCheckingBalance || !amount || parseInt(amount, 10) === 0;

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
                            {/* 1단계: 고객 정보 입력 */}
                            <div className="space-y-6">
                                <div className="flex items-center gap-2 px-1">
                                    <User className="w-4 h-4 text-emerald-500" />
                                    <h3 className="text-sm font-bold text-slate-700">1. 고객 정보</h3>
                                </div>
                                <div className="max-w-md">
                                    <Input
                                        label="고객 성명"
                                        placeholder="예) 홍길동"
                                        value={userName}
                                        onChange={(e) => {
                                            setUserName(e.target.value);
                                            setApiError(null);
                                        }}
                                    />
                                </div>
                            </div>

                            {/* 2단계: 주민등록번호 입력 */}
                            <div className="pt-8 border-t border-slate-50">
                                <div className="flex items-center gap-2 px-1 mb-4">
                                    <User className="w-4 h-4 text-emerald-500" />
                                    <h3 className="text-sm font-bold text-slate-700">2. 주민등록번호</h3>
                                </div>
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

                            {/* 3단계: 출금 계좌 정보 입력 */}
                            <div className="pt-8 border-t border-slate-50">
                                <AccountInputSection 
                                    title="3. 출금 계좌 정보"
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
                <div className="lg:col-span-1">
                    <div className="sticky top-10 space-y-6">
                        {/* 출금 가능 잔액 카드 */}
                        <Card padding="lg" className="bg-white border-slate-100 shadow-sm min-h-[280px] flex flex-col justify-between transition-all duration-500">
                            <div className="space-y-6">
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-sm font-bold text-slate-700 uppercase tracking-widest">출금 요약</span>
                                </div>
                                
                                <div className="space-y-6">
                                    <div className="space-y-1">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">고객명</p>
                                        <p className="text-sm font-black text-slate-900">{userName || '미입력'}</p>
                                    </div>
                                    <div className="space-y-1">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">계좌번호</p>
                                        <p className="text-sm font-black text-slate-900 font-mono">{sourceAccount.bankName} {sourceAccount.accountNumber || '미입력'}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="pt-4 mt-4 border-t border-slate-50">
                                <div className="space-y-1.5">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase">출금 가능 잔액</p>
                                    <div className="mt-1">
                                        {isCheckingBalance ? (
                                            <div className="flex gap-1.5 items-baseline py-2">
                                                <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" />
                                                <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                                <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                                            </div>
                                        ) : sourceAccount.balance !== undefined ? (
                                            <div className="flex items-baseline gap-1">
                                                <span className="text-3xl font-black text-slate-900">{formatAmount(sourceAccount.balance)}</span>
                                                <span className="text-sm font-bold text-slate-500">원</span>
                                            </div>
                                        ) : (
                                            <p className="text-sm font-bold text-slate-300 py-2">계좌 확인 시 조회됩니다.</p>
                                        )}
                                    </div>
                                    <p className="text-[10px] font-bold text-emerald-600">수수료 전액 면제 (0원)</p>
                                </div>
                            </div>
                        </Card>

                        {/* 실행 버튼 */}
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
                            <ChevronRight className={`w-6 h-6 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default WithdrawEntryForm;
