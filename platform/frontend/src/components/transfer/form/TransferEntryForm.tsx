import { useState, useEffect } from 'react';
import { Landmark, AlertCircle, ArrowRight, Info, Wallet, User, CheckCircle2, Loader2 } from 'lucide-react';
import Card from '../../common/Card';
import RrnInput from '../../common/RrnInput';
import AccountInputSection from '../../common/AccountInputSection';
import { Button } from '../../common/Button';
import PageHeader from '../../common/PageHeader';
import AmountInputSection from '../section/AmountInputSection';
import BalanceCalculationSection from '../section/BalanceCalculationSection';
import { useTransferStore } from '../../../store/useTransferStore';
import { getBalance, getRecipient } from '../../../api/transfer';
import { fetchBankList, type BankOption } from '../../../api/loanApi';
import { formatAmount } from '../../../utils/formatter';
import Input from '../../common/Input';

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
            const response = await getBalance({
                bankCode: fromBank,
                accountNo: fromAccountNumber,
                customerRrnPrefix: customerRrnPrefix,
                encryptedKey: 'TEMP_ENCRYPTED_KEY',
                jwsSignature: 'TEMP_JWS_SIGNATURE'
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
            const response = await getRecipient({
                depositBankCode: toBank,
                depositAccountNo: toAccountNumber,
                encryptedKey: 'TEMP_ENCRYPTED_KEY',
                jwsSignature: 'TEMP_JWS_SIGNATURE'
            });
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
                {/* 1. 입력 영역 (2컬럼) */}
                <div className="lg:col-span-2 space-y-8">
                    
                    {/* 섹션 1: 입금 계좌 (받는 분) */}
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <div className="space-y-8">
                            <AccountInputSection 
                                title="1. 입금 계좌 정보 (받는 분)"
                                bankCode={toBank}
                                accountNumber={toAccountNumber}
                                banks={banks}
                                onBankChange={(name, code) => updateData({ toBank: code, toBankName: name, toName: '' })}
                                onAccountChange={(val) => updateData({ toAccountNumber: val, toName: '' })}
                                onBlur={handleRecipientInquiry}
                            />
                            
                            {/* 결과 영역 미리 할당 */}
                            <div className="pt-4 h-[100px] flex items-center">
                                {isRecipientInquired ? (
                                    <div className="w-full p-5 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-center justify-between animate-in fade-in zoom-in-95">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-emerald-600 shadow-sm border border-emerald-100">
                                                <User className="w-5 h-5" />
                                            </div>
                                            <div>
                                                <p className="text-[10px] font-black text-emerald-600 uppercase tracking-widest">수취인 확인 완료</p>
                                                <p className="text-xl font-black text-slate-900">{toName}</p>
                                            </div>
                                        </div>
                                        <CheckCircle2 className="w-6 h-6 text-emerald-500" />
                                    </div>
                                ) : (
                                    <div className="w-full p-6 bg-slate-50/50 rounded-2xl border border-dashed border-slate-200 flex items-center justify-between">
                                        <p className="text-sm text-slate-400 font-medium italic">
                                            {isRecipientInquiring ? '수취인 조회 중입니다...' : '수취인 정보를 입력하면 자동으로 조회됩니다.'}
                                        </p>
                                        {isRecipientInquiring && <Loader2 className="w-5 h-5 text-emerald-500 animate-spin" />}
                                    </div>
                                )}
                            </div>
                        </div>
                    </Card>

                    {/* 섹션 2: 출금 계좌 (내 계좌) */}
                    <Card padding="lg" className={`border-slate-100 shadow-sm transition-all duration-500 ${!isRecipientInquired ? 'opacity-50 pointer-events-none grayscale' : 'opacity-100'}`}>
                        <div className="space-y-10">
                            {/* 1. 보내는 분 성명 */}
                            <div className="space-y-6">
                                <div className="flex items-center gap-2 px-1">
                                    <User className="w-4 h-4 text-emerald-500" />
                                    <h3 className="text-sm font-bold text-slate-700">2. 출금 계좌 정보 (보내는 분)</h3>
                                </div>
                                <div className="max-w-md">
                                    <Input
                                        label="고객 성명"
                                        placeholder="예) 홍길동"
                                        value={fromName}
                                        onChange={(e) => updateData({ fromName: e.target.value, balance: '0' })}
                                        onBlur={handleSenderInquiry}
                                    />
                                </div>
                            </div>

                            {/* 2. 주민등록번호 */}
                            <div className="pt-8 border-t border-slate-50">
                                <RrnInput 
                                    rrnFront={customerRrnPrefix.slice(0, 6)}
                                    rrnBack={customerRrnPrefix.slice(6, 7)}
                                    onRrnFrontChange={(val) => updateData({ customerRrnPrefix: val + customerRrnPrefix.slice(6, 7), balance: '0' })}
                                    onRrnBackChange={(val) => updateData({ customerRrnPrefix: customerRrnPrefix.slice(0, 6) + val, balance: '0' })}
                                    onBlur={handleSenderInquiry}
                                    isChecking={isSenderInquiring}
                                />
                            </div>

                            {/* 3. 계좌 정보 */}
                            <div className="pt-8 border-t border-slate-50">
                                <AccountInputSection 
                                    title="보내는 분 계좌 정보"
                                    bankCode={fromBank}
                                    accountNumber={fromAccountNumber}
                                    banks={banks}
                                    onBankChange={(name, code) => updateData({ fromBank: code, fromBankName: name, balance: '0' })}
                                    onAccountChange={(val) => updateData({ fromAccountNumber: val, balance: '0' })}
                                    onBlur={handleSenderInquiry}
                                />
                            </div>

                            {/* 결과 영역 미리 할당 */}
                            <div className="pt-4 h-[100px] flex items-center">
                                {isSenderInquired ? (
                                    <div className="w-full p-5 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-center justify-between animate-in fade-in zoom-in-95">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-emerald-600 shadow-sm border border-emerald-100">
                                                <Wallet className="w-5 h-5" />
                                            </div>
                                            <div>
                                                <p className="text-[10px] font-black text-emerald-600 uppercase tracking-widest">내 계좌 확인 완료</p>
                                                <div className="flex items-baseline gap-1 mt-0.5">
                                                    <span className="text-xs font-bold text-emerald-600 mr-0.5">잔액</span>
                                                    <span className="text-lg font-black text-slate-900">₩ {formatAmount(balance)}</span>
                                                </div>
                                            </div>
                                        </div>
                                        <CheckCircle2 className="w-6 h-6 text-emerald-500" />
                                    </div>
                                ) : (
                                    <div className="w-full p-6 bg-slate-50/50 rounded-2xl border border-dashed border-slate-200 flex items-center justify-between">
                                        <p className="text-sm text-slate-400 font-medium italic">
                                            {isSenderInquiring ? '내 계좌 조회 중입니다...' : '출금 계좌 정보를 입력하면 자동으로 조회됩니다.'}
                                        </p>
                                        {isSenderInquiring && <Loader2 className="w-5 h-5 text-emerald-500 animate-spin" />}
                                    </div>
                                )}
                            </div>
                        </div>
                    </Card>

                    {/* 섹션 3: 이체 금액 입력 */}
                    <Card padding="lg" className={`border-slate-100 shadow-sm transition-all duration-500 ${!isSenderInquired ? 'opacity-50 pointer-events-none grayscale' : 'opacity-100'}`}>
                        <div className="space-y-10">
                            <div className="flex items-center gap-2 px-1">
                                <Landmark className="w-4 h-4 text-emerald-500" />
                                <h3 className="text-sm font-bold text-slate-700">3. 이체 금액 입력</h3>
                            </div>
                            <AmountInputSection />
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

                {/* 2. 요약 및 실행 영역 (1컬럼) */}
                <div className="lg:col-span-1">
                    <div className="sticky top-10 space-y-6">
                        <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[580px]">
                            <div className="space-y-8">
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">실시간 이체 현황</span>
                                    <ArrowRight className="w-4 h-4 text-slate-300" />
                                </div>
                                
                                <div className="space-y-6">
                                    <div className="space-y-2">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">받는 분</p>
                                        {isRecipientInquired ? (
                                            <div className="p-4 bg-emerald-50/50 rounded-2xl border border-emerald-100/50 animate-in slide-in-from-right-2">
                                                <p className="text-sm font-black text-slate-900">{toName}</p>
                                                <p className="text-[11px] text-emerald-600 font-bold mt-0.5">{toBankName} {toAccountNumber}</p>
                                            </div>
                                        ) : (
                                            <p className="text-sm font-bold text-slate-300 italic pl-1">정보를 입력해 주세요</p>
                                        )}
                                    </div>

                                    <div className="w-full h-px bg-slate-50"></div>

                                    <div className="space-y-2">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">보내는 분</p>
                                        {isSenderInquired ? (
                                            <div className="p-4 bg-emerald-50/50 rounded-2xl border border-emerald-100/50 animate-in slide-in-from-right-2">
                                                <p className="text-sm font-black text-slate-900">{fromName}</p>
                                                <p className="text-[11px] text-slate-500 font-mono mt-0.5">{fromBankName} {fromAccountNumber}</p>
                                                <p className="text-emerald-600 font-black mt-2 pt-2 border-t border-emerald-200/30 flex justify-between items-baseline">
                                                    <span className="text-[10px] uppercase">잔액</span>
                                                    <span className="text-lg">₩ {formatAmount(balance)}</span>
                                                </p>
                                            </div>
                                        ) : (
                                            <p className="text-sm font-bold text-slate-300 italic pl-1">정보 입력 및 계좌 확인이 필요합니다</p>
                                        )}
                                    </div>

                                    <div className="w-full h-px bg-slate-50"></div>

                                    <div className="space-y-1.5">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">최종 이체 금액</p>
                                        <div className="flex items-baseline gap-1">
                                            <span className="text-3xl font-black text-slate-900">{formatAmount(amount)}</span>
                                            <span className="text-sm font-bold text-slate-500">원</span>
                                        </div>
                                        <p className="text-[10px] font-bold text-emerald-600">수수료 전액 면제 (0원)</p>
                                    </div>
                                </div>
                            </div>

                            <div className="mt-10">
                                <Button
                                    onClick={handleNextClick}
                                    disabled={isNextDisabled}
                                    variant={isNextDisabled ? 'secondary' : 'primary'}
                                    size="xl"
                                    fullWidth
                                    className={`h-20 rounded-2xl text-xl font-black shadow-lg transition-all group ${
                                        isNextDisabled ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-slate-900 text-white hover:bg-slate-800'
                                    }`}
                                >
                                    다음 단계로
                                    <ArrowRight className={`w-6 h-6 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                                </Button>
                            </div>
                        </Card>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TransferEntryForm;
