import { useState, useEffect } from 'react';
import { AlertCircle, Info, Landmark } from 'lucide-react';
import Card from '../../common/Card';
import PageHeader from '../../common/PageHeader';
import AmountInputSection from '../sections/AmountInputSection';
import BalanceCalculationSection from '../sections/BalanceCalculationSection';
import { useTransferStore } from '../../../store/useTransferStore';
import { fetchBalance } from '../../../api/inquiry';
import { getRecipient } from '../../../api/transfer';
import { fetchBankList, type BankOption } from '../../../api/loanApi';

// Sub-components
import TransferRecipientSection from '../sections/TransferRecipientSection';
import TransferSenderSection from '../sections/TransferSenderSection';
import TransferSideSummary from '../sections/TransferSideSummary';

const TransferEntryForm: React.FC = () => {
    const { 
        fromName, fromBank, fromBankName, fromAccountNumber, customerRrnPrefix, balance,
        toBank, toBankName, toAccountNumber, toName, amount,
        updateData
    } = useTransferStore();

    const [banks, setBanks] = useState<BankOption[]>([]);
    const [isSenderInquiring, setIsSenderInquiring] = useState(false);
    const [isRecipientInquiring, setIsRecipientInquiring] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const isSenderInquired = balance !== '0' && fromAccountNumber !== '' && fromName !== '';
    const isRecipientInquired = toName !== '' && toAccountNumber !== '';

    useEffect(() => {
        const getBanks = async () => {
            try {
                const bankList = await fetchBankList();
                setBanks(bankList);
            } catch (err) {
                console.error('은행 리스트 조회 실패:', err);
            }
        };
        getBanks();
    }, []);

    const handleSenderInquiry = async () => {
        if (!fromName || !fromBank || !fromAccountNumber || customerRrnPrefix.length !== 7) return;
        
        setIsSenderInquiring(true);
        setError(null);
        try {
            const response = await fetchBalance({
                bankCode: fromBank,
                accountNo: fromAccountNumber,
                customerRrnPrefix: customerRrnPrefix
            });
            if (response.success) {
                updateData({ balance: response.data.balance });
            } else {
                setError(response.error?.message || '계좌 조회에 실패했습니다.');
            }
        } catch (err) {
            setError('서버 통신 중 오류가 발생했습니다.');
        } finally {
            setIsSenderInquiring(false);
        }
    };

    const handleRecipientInquiry = async () => {
        if (!toBank || !toAccountNumber) return;
        setIsRecipientInquiring(true);
        setError(null);
        try {
            const response = await getRecipient(toBank, toAccountNumber);
            if (response.success) {
                updateData({ 
                    toName: response.data.depositorName,
                    toBankName: response.data.depositBankName,
                    toBankAccountNo: response.data.depositBankAccountNo
                });
            } else {
                setError(response.error?.message || '수취인 조회에 실패했습니다.');
            }
        } catch (err) {
            setError('서버 통신 중 오류가 발생했습니다.');
        } finally {
            setIsRecipientInquiring(false);
        }
    };

    const handleNextClick = () => {
        if (!isSenderInquired || !isRecipientInquired || !amount || amount <= 0) {
            setError('이체 정보를 모두 완성해 주세요.');
            return;
        }
        updateData({ step: 6 }); // 정보 확인 단계(FinalConfirmForm)로 이동
    };

    const isNextDisabled = !isSenderInquired || !isRecipientInquired || !amount || amount <= 0;

    return (
        <div className="w-full">
            <PageHeader 
                title="계좌 이체" 
                description="수취인 및 출금 계좌 정보를 입력하여 이체를 진행해 주세요."
            />

            {error && (
                <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 max-w-4xl mx-auto animate-in fade-in slide-in-from-top-2">
                    <AlertCircle className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm font-bold">{error}</span>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">
                    <TransferRecipientSection
                        toBank={toBank}
                        toAccountNumber={toAccountNumber}
                        banks={banks}
                        onBankChange={(name, code) => updateData({ toBank: code, toBankName: name, toName: '' })}
                        onAccountChange={(val) => updateData({ toAccountNumber: val, toName: '' })}
                        onBlur={handleRecipientInquiry}
                        isRecipientInquired={isRecipientInquired}
                        isRecipientInquiring={isRecipientInquiring}
                        toName={toName}
                    />

                    <TransferSenderSection
                        fromName={fromName}
                        fromBank={fromBank}
                        fromAccountNumber={fromAccountNumber}
                        customerRrnPrefix={customerRrnPrefix}
                        balance={balance}
                        banks={banks}
                        onNameChange={(val) => updateData({ fromName: val, balance: '0' })}
                        onRrnFrontChange={(val) => updateData({ customerRrnPrefix: val + customerRrnPrefix.slice(6, 7), balance: '0' })}
                        onRrnBackChange={(val) => updateData({ customerRrnPrefix: customerRrnPrefix.slice(0, 6) + val, balance: '0' })}
                        onBankChange={(name, code) => updateData({ fromBank: code, fromBankName: name, balance: '0' })}
                        onAccountChange={(val) => updateData({ fromAccountNumber: val, balance: '0' })}
                        onBlur={handleSenderInquiry}
                        isSenderInquired={isSenderInquired}
                        isSenderInquiring={isSenderInquiring}
                        isRecipientInquired={isRecipientInquired}
                    />

                    <Card padding="lg" className={`border-slate-100 shadow-sm transition-all duration-500 ${!isSenderInquired ? 'opacity-50 pointer-events-none grayscale' : 'opacity-100'}`}>
                        <div className="space-y-10">
                            <div className="flex items-center gap-2 px-1">
                                <Landmark className="w-4 h-4 text-emerald-500" />
                                <h3 className="text-sm font-bold text-slate-700">5. 이체 금액 입력</h3>
                            </div>
                            <AmountInputSection label="금액 입력" />
                            <div className="pt-8 border-t border-slate-50">
                                <BalanceCalculationSection />
                            </div>
                        </div>
                    </Card>

                    <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4">
                        <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                        <p className="text-xs text-slate-500 leading-relaxed font-medium">
                            이체 실행 시 출금 계좌의 비밀번호가 필요합니다. 본인 계좌임을 확인하기 위해 정확한 정보를 입력해 주세요.
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-1">
                    <TransferSideSummary
                        toName={toName}
                        toBankName={toBankName}
                        toAccountNumber={toAccountNumber}
                        isRecipientInquired={isRecipientInquired}
                        fromName={fromName}
                        fromBankName={fromBankName}
                        fromAccountNumber={fromAccountNumber}
                        balance={balance}
                        isSenderInquired={isSenderInquired}
                        amount={amount}
                        onNext={handleNextClick}
                        isNextDisabled={isNextDisabled}
                    />
                </div>
            </div>
        </div>
    );
};

export default TransferEntryForm;
