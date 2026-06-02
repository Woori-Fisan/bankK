import React, { useState, useEffect } from 'react';
import { Landmark, AlertCircle, ArrowRight, Search, Info, User } from 'lucide-react';
import Card from '../../common/Card';
import AccountInputSection from '../../common/AccountInputSection';
import { Button } from '../../common/Button';
import PageHeader from '../../common/PageHeader';
import { useTransferStore } from '../../../store/useTransferStore';
import { getRecipient } from '../../../api/transfer';
import { fetchBankList, type BankOption } from '../../../api/loanApi';

const RecipientInputForm: React.FC = () => {
    const { 
        toBank, toBankName, toAccountNumber, 
        fromBankName, fromAccountNumber,
        prevStep, nextStep, updateData 
    } = useTransferStore();
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
        setIsLoading(true);
        setError(null);

        try {
            const response = await getRecipient({
                depositBankCode: toBank,
                depositAccountNo: toAccountNumber,
                encryptedKey: 'TEMP_ENCRYPTED_KEY',
                jwsSignature: 'TEMP_JWS_SIGNATURE'
            });

            if (response.success) {
                const { depositorName, depositBankName, depositBankAccountNo } = response.data;
                updateData({ 
                    toName: depositorName,
                    toBankName: depositBankName,
                    toBankAccountNo: depositBankAccountNo
                });
                nextStep();
            } else {
                setError(response.error?.message || '수취인 조회에 실패했습니다.');
            }
        } catch (err: any) {
            setError('서버 통신 중 오류가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const isNextDisabled = !toBank || !toAccountNumber || isLoading;

    return (
        <div className="w-full">
            <PageHeader 
                title="수취인 정보 입력" 
                description="이금을 받으실 분의 계좌 정보를 정확히 입력해 주세요."
            />

            {error && (
                <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 max-w-4xl mx-auto">
                    <AlertCircle className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm font-bold">{error}</span>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* 1. 입력 영역 (2컬럼) */}
                <div className="lg:col-span-2 space-y-8">
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <AccountInputSection 
                            title="입금 계좌 정보"
                            bankCode={toBank}
                            accountNumber={toAccountNumber}
                            banks={banks}
                            onBankChange={(name, code) => updateData({ toBank: code, toBankName: name })}
                            onAccountChange={(val) => updateData({ toAccountNumber: val })}
                        />
                    </Card>

                    <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4">
                        <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                        <p className="text-xs text-slate-500 leading-relaxed font-medium">
                            수취인 성명은 계좌 조회 후 자동으로 확인됩니다. 정확한 은행과 계좌번호를 입력해 주세요.
                        </p>
                    </div>
                </div>

                {/* 2. 요약 및 실행 영역 (1컬럼) */}
                <div className="lg:col-span-1 space-y-6">
                    <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[320px]">
                        <div className="space-y-6">
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">이체 요약</span>
                                <User className="w-4 h-4 text-slate-300" />
                            </div>
                            
                            <div className="space-y-4">
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase">출금 계좌</p>
                                    <p className="text-sm font-black text-slate-900">{fromBankName}</p>
                                    <p className="text-xs font-medium text-slate-500 font-mono">{fromAccountNumber}</p>
                                </div>
                                <div className="w-full h-px bg-slate-50"></div>
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase">입금 은행</p>
                                    <p className="text-sm font-black text-slate-900">{toBankName || '은행 미선택'}</p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase">입금 계좌번호</p>
                                    <p className="text-sm font-black text-slate-900 font-mono">{toAccountNumber || '번호 미입력'}</p>
                                </div>
                            </div>
                        </div>

                        <div className="space-y-3">
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
                                {isLoading ? '조회 중...' : '수취인 확인하기'}
                                <ArrowRight className={`w-5 h-5 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                            </Button>
                            <Button
                                onClick={prevStep}
                                variant="outline"
                                size="lg"
                                fullWidth
                                className="h-12 rounded-xl text-sm font-bold border-slate-200 text-slate-500 hover:bg-slate-50"
                            >
                                이전 단계로
                            </Button>
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    );
};

export default RecipientInputForm;
