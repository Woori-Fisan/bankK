import React from 'react';
import { AlertCircle, ArrowRight, Wallet, User } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import PageHeader from '../../common/PageHeader';
import AmountInputSection from '../section/AmountInputSection';
import BalanceCalculationSection from '../section/BalanceCalculationSection';
import { useTransferStore } from '../../../store/useTransferStore';
import { formatAmount } from '../../../utils/formatter';

const AmountInputForm: React.FC = () => {
    const { 
        toName, toBankName, toBankAccountNo, 
        fromBankName, fromAccountNumber, balance,
        amount, nextStep, prevStep, setStep 
    } = useTransferStore();

    const isNextDisabled = !amount || amount <= 0;

    return (
        <div className="w-full">
            <PageHeader 
                title="이체 금액 입력" 
                description="이체하실 금액을 정확히 입력해 주세요."
            />

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* 1. 입력 영역 (2컬럼) */}
                <div className="lg:col-span-2 space-y-8">
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <div className="space-y-10">
                            {/* 받는 분 정보 (상단 요약) */}
                            <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex justify-between items-center">
                                <div className="flex flex-col gap-1">
                                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">받는 분</span>
                                    <span className="text-lg font-bold text-slate-900">{toName}</span>
                                    <span className="text-sm text-slate-500 font-mono">{toBankName} {toBankAccountNo}</span>
                                </div>
                                <button 
                                    onClick={() => setStep(3)} 
                                    className="px-4 py-2 bg-white border border-slate-200 rounded-lg text-xs font-bold text-emerald-600 hover:bg-emerald-50 transition-colors"
                                >
                                    변경
                                </button>
                            </div>

                            <div className="pt-2">
                                <AmountInputSection />
                            </div>

                            <div className="pt-8 border-t border-slate-50">
                                <BalanceCalculationSection />
                            </div>
                        </div>
                    </Card>

                    <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4">
                        <AlertCircle className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                        <p className="text-xs text-slate-500 leading-relaxed font-medium">
                            이체 수수료는 다음 단계에서 확인 가능합니다. 1일 이체 한도를 확인해 주세요.
                        </p>
                    </div>
                </div>

                {/* 2. 요약 및 실행 영역 (1컬럼) */}
                <div className="lg:col-span-1 space-y-6">
                    {/* 잔액 정보 카드 */}
                    <Card padding="lg" className="bg-emerald-900 text-white border-none shadow-xl shadow-emerald-900/10 flex flex-col justify-between min-h-[200px]">
                        <div>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-black text-emerald-300 uppercase tracking-widest">잔액 정보</span>
                                <Wallet className="w-4 h-4 text-emerald-400" />
                            </div>
                            <h4 className="text-xs font-bold text-emerald-100">출금 가능 잔액</h4>
                        </div>
                        
                        <div className="py-2">
                            <p className="text-4xl font-black tracking-tighter">
                                <span className="text-lg font-bold mr-1 opacity-60">₩</span>
                                {formatAmount(balance || 0)}
                            </p>
                        </div>

                        <div className="pt-3 border-t border-white/10">
                            <span className="text-[9px] font-black text-emerald-200 uppercase tracking-widest">실시간 계좌 정보가 확인되었습니다.</span>
                        </div>
                    </Card>

                    <div className="space-y-3">
                        <Button
                            onClick={nextStep}
                            disabled={isNextDisabled}
                            variant={isNextDisabled ? 'secondary' : 'primary'}
                            size="xl"
                            fullWidth
                            className={`h-16 rounded-2xl text-lg font-black shadow-lg transition-all group ${
                                isNextDisabled ? 'bg-slate-200 text-slate-400' : 'bg-slate-900 text-white hover:bg-slate-800'
                            }`}
                        >
                            다음 단계로
                            <ArrowRight className={`w-6 h-6 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                        </Button>
                        <Button
                            onClick={() => setStep(3)}
                            variant="outline"
                            size="lg"
                            fullWidth
                            className="h-12 rounded-xl text-sm font-bold border-slate-200 text-slate-500 hover:bg-slate-50"
                        >
                            이전 단계로
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AmountInputForm;
