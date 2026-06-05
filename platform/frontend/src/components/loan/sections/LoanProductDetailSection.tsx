import React from 'react';
import { Building2, Receipt, ChevronLeft, ChevronRight } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import AmountInput from '../../common/AmountInput';
import { formatAmount } from '../../../utils/formatter';
import type { LoanProduct } from '../../../pages/LoanApplication';

interface LoanProductDetailSectionProps {
    selectedProduct: LoanProduct | undefined;
    effectiveLimit: number;
    executeAmount: number;
    onAmountChange: (val: string) => void;
    onQuickAdd: (val: number) => void;
    onAllIn: () => void;
    amountError: string | null;
    period: number;
    onPeriodChange: (p: number) => void;
    onNext: () => void;
    onBack: () => void;
}

const LoanProductDetailSection: React.FC<LoanProductDetailSectionProps> = ({
    selectedProduct,
    effectiveLimit,
    executeAmount,
    onAmountChange,
    onQuickAdd,
    onAllIn,
    amountError,
    period,
    onPeriodChange,
    onNext,
    onBack,
}) => {
    const calculateMonthly = (limit: number, rate: number, p: number) => {
        const monthlyRate = (rate / 100) / 12;
        const numerator = limit * monthlyRate * Math.pow(1 + monthlyRate, p);
        const denominator = Math.pow(1 + monthlyRate, p) - 1;
        return Math.floor(numerator / denominator);
    };

    return (
        <Card padding="xl" className="bg-white border-slate-100 shadow-sm min-h-[620px]">
            <div className="flex items-center justify-between mb-10 pb-6 border-b border-slate-50">
                <div className="flex items-center gap-4">
                    <div className="w-16 h-16 bg-slate-50 rounded-2xl flex items-center justify-center text-slate-400 border border-slate-100 shadow-inner">
                        <Building2 className="w-8 h-8" />
                    </div>
                    <div>
                        <h3 className="text-2xl font-black text-slate-900 tracking-tight">{selectedProduct?.name}</h3>
                        <div className="flex gap-1.5 mt-2">
                            {selectedProduct?.tags.map((tag) => (
                                <span key={tag} className={`px-2.5 py-1 rounded-lg text-[10px] font-black ${tag === '최저금리' ? 'bg-rose-50 text-rose-600' : 'bg-blue-50 text-blue-600'}`}>
                                    {tag}
                                </span>
                            ))}
                        </div>
                    </div>
                </div>
                <div className="text-right">
                    <p className="text-4xl font-black text-emerald-600 tracking-tighter">{selectedProduct?.rate}% <span className="text-sm font-bold text-slate-400">/ 연</span></p>
                    <p className="text-xs text-slate-400 font-bold mt-1 uppercase tracking-widest">승인 총 한도 {formatAmount(effectiveLimit)}</p>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
                {/* 설정 슬롯 1: 금액 및 기간 */}
                <div className="space-y-8">
                    <AmountInput
                        label="대출 실행 금액 설정"
                        value={executeAmount}
                        onChange={onAmountChange}
                        onQuickAdd={onQuickAdd}
                        onAllIn={onAllIn}
                        disabled={!selectedProduct}
                        error={amountError || undefined}
                    />

                    <div className="space-y-4">
                        <label className="block text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">대출 기간(만기) 선택</label>
                        <div className="grid grid-cols-3 gap-3">
                            {[12, 24, 36].map(m => (
                                <button
                                    key={m}
                                    onClick={() => onPeriodChange(m)}
                                    className={`py-4 rounded-2xl text-sm font-black border-2 transition-all ${
                                        period === m
                                        ? 'bg-slate-900 border-slate-900 text-white shadow-lg'
                                        : 'bg-white border-slate-100 text-slate-400 hover:border-slate-200'
                                    }`}
                                >
                                    {m}개월
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {/* 설정 슬롯 2: 예상 상환금 리포트 */}
                <div className="space-y-6">
                    <div className="bg-emerald-900 text-white rounded-[32px] p-8 shadow-xl shadow-emerald-900/20 flex flex-col justify-between h-full">
                        <div className="space-y-6">
                            <div className="flex items-center justify-between">
                                <span className="text-[10px] font-black text-emerald-300 uppercase tracking-widest">월 예상 납입금</span>
                                <div className="w-8 h-8 bg-white/10 rounded-lg flex items-center justify-center">
                                    <Receipt className="w-4 h-4 text-emerald-300" />
                                </div>
                            </div>
                            <div>
                                <p className="text-4xl font-black tracking-tighter">
                                    <span className="text-xl font-bold mr-1 opacity-50">₩</span>
                                    {selectedProduct && !amountError && executeAmount > 0
                                        ? calculateMonthly(executeAmount, selectedProduct.rate, period).toLocaleString()
                                        : 0}
                                </p>
                                <p className="text-xs text-emerald-300/60 font-medium mt-2">원리금 균등 상환 방식 기준</p>
                            </div>
                        </div>

                        <div className="pt-8 border-t border-white/10 space-y-3">
                            <div className="flex justify-between text-xs font-bold">
                                <span className="text-emerald-300/50">최종 실행 금액</span>
                                <span>₩ {executeAmount.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between text-xs font-bold">
                                <span className="text-emerald-300/50">대출 만기일</span>
                                <span>{period}개월 후</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="mt-12 pt-10 border-t border-slate-50 flex justify-between items-center gap-8">
                <Button
                    onClick={onBack}
                    variant="secondary"
                    size="xl"
                    className="h-16 px-10 rounded-2xl bg-slate-100 text-slate-600 font-black hover:bg-slate-200"
                >
                    <ChevronLeft className="w-5 h-5 mr-2" />
                    이전 단계로
                </Button>
                <Button
                    onClick={onNext}
                    disabled={!!amountError || executeAmount < 1_000_000}
                    variant={!!amountError || executeAmount < 1_000_000 ? 'secondary' : 'primary'}
                    size="xl"
                    className={`flex-1 h-16 rounded-2xl text-lg font-black shadow-xl transition-all group ${
                        !!amountError || executeAmount < 1_000_000 ? 'bg-slate-200 text-slate-400' : 'bg-slate-900 text-white hover:bg-slate-800'
                    }`}
                >
                    이 상품으로 계약 서류 확인
                    <ChevronRight className="w-5 h-5 ml-2 transition-transform group-hover:translate-x-1" />
                </Button>
            </div>
        </Card>
    );
};

export default LoanProductDetailSection;
