import { useState } from 'react';
import type { WithdrawData } from '../../../types/withdraw';
import { fetchBalance } from '../../../api/inquiry';
import type { BankOption } from '../../../api/loanApi';
import ErrorAlert from '../../common/ErrorAlert';

// Sub-components
import WithdrawCustomerSection from '../sections/WithdrawCustomerSection';
import WithdrawSideSummary from '../sections/WithdrawSideSummary';
import AmountInputSection from '../sections/AmountInputSection';

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
    const [isCheckingBalance, setIsCheckingBalance] = useState(false);
    const [apiError, setApiError] = useState<string | null>(null);

    const handleCheckBalance = async () => {
        if (!sourceAccount.bankCode || !sourceAccount.accountNumber || birthDate.length !== 7) return;
        if (isCheckingBalance) return;

        setIsCheckingBalance(true);
        setApiError(null);
        setSourceAccount(prev => ({ ...prev, balance: undefined }));

        try {
            const response = await fetchBalance({
                bankCode: sourceAccount.bankCode,
                accountNo: sourceAccount.accountNumber,
                customerRrnPrefix: birthDate,
                customerName: userName
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
        if (!userName) return;
        onNext({ userName, sourceAccount: { ...sourceAccount }, birthDate, amount });
    };

    const isNextDisabled = !userName || sourceAccount.balance === undefined || isCheckingBalance || !amount || parseInt(amount, 10) === 0;

    return (
        <div className="w-full space-y-6 animate-in fade-in duration-500">
            <ErrorAlert message={apiError} />
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2 space-y-6">
                    <div>
                        <WithdrawCustomerSection
                            userName={userName}
                            onUserNameChange={(val) => { setUserName(val); setApiError(null); }}
                            birthDate={birthDate}
                            onRrnFrontChange={(val) => {
                                // 앞자리 수정 시: 새 값(최대 6자) + 기존 뒷자리(있을 경우)
                                const currentBack = birthDate.length >= 7 ? birthDate.charAt(6) : '';
                                setBirthDate(val.slice(0, 6) + (val.length === 6 ? currentBack : ''));
                                setSourceAccount(prev => ({ ...prev, balance: undefined }));
                                setApiError(null);
                            }}
                            onRrnBackChange={(val) => {
                                // 뒷자리 수정 시: 기존 앞자리(6자 유지) + 새 뒷자리(1자)
                                const currentFront = birthDate.slice(0, 6);
                                if (currentFront.length === 6) {
                                    setBirthDate(currentFront + val.slice(0, 1));
                                } else {
                                    // 앞자리가 아직 완성되지 않은 경우에도 뒷자리 위치를 보존하기 위해 
                                    // 내부적으로만 6자 공간을 확보 (패딩 대신 슬라이싱 활용)
                                    setBirthDate(currentFront.padEnd(6, ' ').slice(0, 6) + val.slice(0, 1));
                                }
                                setSourceAccount(prev => ({ ...prev, balance: undefined }));
                                setApiError(null);
                            }}
                            onBlur={handleCheckBalance}
                            isCheckingBalance={isCheckingBalance}
                            sourceAccount={sourceAccount}
                            banks={banks}
                            onBankChange={(name, code) => {
                                setSourceAccount(prev => ({ ...prev, bankName: name, bankCode: code, balance: undefined }));
                                setApiError(null);
                            }}
                            onAccountChange={(val) => {
                                setSourceAccount(prev => ({ ...prev, accountNumber: val, balance: undefined }));
                                setApiError(null);
                            }}
                        />
                    </div>

                    <div className="mt-10 pt-8 bg-white border border-slate-100 rounded-3xl p-8 shadow-sm">
                        <AmountInputSection 
                            amount={amount}
                            onAmountChange={(e) => setAmount(e.target.value.replace(/[^0-9]/g, ''))}
                            onQuickAmountAdd={(val) => setAmount((parseInt(amount || '0', 10) + val).toString())}
                            onAllIn={() => sourceAccount.balance && setAmount(sourceAccount.balance.toString())}
                            disabled={sourceAccount.balance === undefined || isCheckingBalance}
                        />
                    </div>
                </div>

                <div className="lg:col-span-1">
                    <WithdrawSideSummary
                        userName={userName}
                        sourceAccount={sourceAccount}
                        isCheckingBalance={isCheckingBalance}
                        onSubmit={handleSubmit}
                        isNextDisabled={isNextDisabled}
                    />
                </div>
            </div>
        </div>
    );
};

export default WithdrawEntryForm;
