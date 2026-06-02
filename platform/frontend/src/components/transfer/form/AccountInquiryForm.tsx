import { Landmark, AlertCircle, ArrowRight, Search, Info } from 'lucide-react';
import Card from '../../common/Card';
import RrnInput from '../../common/RrnInput';
import AccountInputSection from '../../common/AccountInputSection';
import { Button } from '../../common/Button';
import PageHeader from '../../common/PageHeader';
import { useEffect, useState } from 'react';
import { fetchBankList, type BankOption } from '../../../api/loanApi';
import { getBalance } from '../../../api/transfer';
import { useTransferStore } from '../../../store/useTransferStore';

const AccountInquiryForm: React.FC = () => {
    const { fromBank, fromBankName, fromAccountNumber, customerRrnPrefix, nextStep, updateData } = useTransferStore();
    const [banks, setBanks] = useState<BankOption[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

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

    const handleInquiry = async () => {
        if (customerRrnPrefix.length !== 7) {
            setError('주민등록번호를 정확히 입력해주세요.');
            return;
        }

        setIsLoading(true);
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
                nextStep();
            } else {
                setError(response.error?.message || '계좌 조회에 실패했습니다.');
            }
        } catch (err: any) {
            setError('서버 통신 중 오류가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const isNextDisabled = !fromBank || !fromAccountNumber || customerRrnPrefix.length !== 7 || isLoading;

    return (
        <div className="w-full">
            <PageHeader 
                title="이체 출금 계좌 조회" 
                description="이체를 진행할 계좌의 정보를 정확히 입력해 주세요."
            />

            {error && (
                <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 max-w-4xl mx-auto">
                    <AlertCircle className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm font-bold">{error}</span>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <div className="space-y-10">
                            <AccountInputSection 
                                title="출금 계좌 정보"
                                bankCode={fromBank}
                                accountNumber={fromAccountNumber}
                                banks={banks}
                                onBankChange={(name, code) => updateData({ fromBank: code, fromBankName: name })}
                                onAccountChange={(val) => updateData({ fromAccountNumber: val })}
                            />

                            <div className="pt-8 border-t border-slate-50">
                                <RrnInput 
                                    rrnFront={customerRrnPrefix.slice(0, 6)}
                                    rrnBack={customerRrnPrefix.slice(6, 7)}
                                    onRrnFrontChange={(val) => updateData({ customerRrnPrefix: val + customerRrnPrefix.slice(6, 7) })}
                                    onRrnBackChange={(val) => updateData({ customerRrnPrefix: customerRrnPrefix.slice(0, 6) + val })}
                                    onBlur={() => {}}
                                    isChecking={isLoading}
                                />
                            </div>
                        </div>
                    </Card>

                    <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4">
                        <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                        <p className="text-xs text-slate-500 leading-relaxed font-medium">
                            입력하신 정보는 이체 진행을 위한 본인 확인 및 계좌 조회를 위해 금융기관으로 안전하게 전송됩니다.
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-1 space-y-6">
                    <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[320px]">
                        <div className="space-y-6">
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">조회 요약</span>
                                <Search className="w-4 h-4 text-slate-300" />
                            </div>
                            
                            <div className="space-y-4">
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase">출금 은행</p>
                                    <p className="text-sm font-black text-slate-900">{fromBankName || '은행 미선택'}</p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase">계좌 번호</p>
                                    <p className="text-sm font-black text-slate-900 font-mono">{fromAccountNumber || '번호 미입력'}</p>
                                </div>
                            </div>
                        </div>

                        <Button
                            onClick={handleInquiry}
                            disabled={isNextDisabled}
                            variant={isNextDisabled ? 'secondary' : 'primary'}
                            size="xl"
                            fullWidth
                            className={`h-16 rounded-2xl text-lg font-black shadow-lg transition-all group ${
                                isNextDisabled ? 'bg-slate-200 text-slate-400' : 'bg-slate-900 text-white hover:bg-slate-800'
                            }`}
                        >
                            {isLoading ? '조회 중...' : '계좌 조회하기'}
                            <ArrowRight className={`w-5 h-5 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                        </Button>
                    </Card>
                </div>
            </div>
        </div>
    );
};

export default AccountInquiryForm;
