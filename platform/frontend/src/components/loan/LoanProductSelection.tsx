import React, { useState, useMemo } from 'react';
import { Building, ChevronRight, ChevronLeft, Building2, CheckCircle2, Receipt } from 'lucide-react';
import type { LoanProduct } from '../../pages/LoanApplication';
import { formatAmount } from '../../utils/formatter';
import Card from '../common/Card';
import { Button } from '../common/Button';

interface LoanProductSelectionProps {
    products: LoanProduct[];
    approvedLimit?: number;
    onNext: (product: LoanProduct) => void;
    onBack: () => void;
}

const LoanProductSelection: React.FC<LoanProductSelectionProps> = ({ products, approvedLimit, onNext, onBack }) => {
    const [selectedId, setSelectedId] = useState(products[0]?.id);
    const [tab, setTab] = useState<'RATE' | 'LIMIT'>('RATE');
    const [period, setPeriod] = useState(24);
    const [executeAmount, setExecuteAmount] = useState(() => {
        const first = products[0];
        if (!first) return 0;
        return Math.min(first.limit, approvedLimit ?? first.limit);
    });
    const [amountError, setAmountError] = useState<string | null>(null);

    // 정렬 로직 적용
    const sortedProducts = useMemo(() => {
        const sorted = [...products];
        if (tab === 'RATE') {
            return sorted.sort((a, b) => a.rate - b.rate);
        } else {
            return sorted.sort((a, b) => b.limit - a.limit);
        }
    }, [products, tab]);

    const selectedProduct = sortedProducts.find(p => p.id === selectedId) || sortedProducts[0];
    const effectiveLimit = selectedProduct
        ? Math.min(selectedProduct.limit, approvedLimit ?? selectedProduct.limit)
        : 0;

    React.useEffect(() => {
        if (selectedProduct) {
            setExecuteAmount(effectiveLimit);
            setAmountError(null);
        }
    }, [selectedProduct?.id, effectiveLimit]);

    const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const raw = e.target.value.replace(/,/g, '');
        if (raw === '') { setExecuteAmount(0); setAmountError(null); return; }
        const val = Number(raw);
        if (isNaN(val)) return;
        const capped = Math.min(val, effectiveLimit);
        setExecuteAmount(capped);
        setAmountError(capped < 1_000_000 ? '최소 100만원 이상 입력해주세요.' : null);
    };

    const calculateMonthly = (limit: number, rate: number, p: number) => {
        const monthlyRate = (rate / 100) / 12;
        const numerator = limit * monthlyRate * Math.pow(1 + monthlyRate, p);
        const denominator = Math.pow(1 + monthlyRate, p) - 1;
        return Math.floor(numerator / denominator);
    };

    return (
        <div className="w-full space-y-6 animate-in fade-in duration-500">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                {/* 1. 상품 목록 (4컬럼 - Sticky 사이드바 스타일) */}
                <div className="lg:col-span-4">
                    <div className="sticky top-10 space-y-6">
                        <div className="flex items-center justify-between px-1">
                            <div className="inline-flex p-1 bg-slate-100 rounded-xl shadow-inner">
                                <button
                                    onClick={() => setTab('RATE')}
                                    className={`px-4 py-2 text-[10px] font-black rounded-lg transition-all ${tab === 'RATE' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`}
                                >
                                    금리순
                                </button>
                                <button
                                    onClick={() => setTab('LIMIT')}
                                    className={`px-4 py-2 text-[10px] font-black rounded-lg transition-all ${tab === 'LIMIT' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`}
                                >
                                    한도순
                                </button>
                            </div>
                            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 text-emerald-600 rounded-full border border-emerald-100">
                                <Building className="w-3.5 h-3.5" />
                                <span className="text-[10px] font-black uppercase tracking-widest">{products.length}건</span>
                            </div>
                        </div>

                        <div className="space-y-3 max-h-[calc(100vh-300px)] overflow-y-auto pr-2 custom-scrollbar">
                            {sortedProducts.map(product => {
                                const isSelected = selectedId === product.id;
                                return (
                                    <div
                                        key={product.id}
                                        onClick={() => setSelectedId(product.id)}
                                        className={`p-5 rounded-2xl border-2 transition-all cursor-pointer relative overflow-hidden group ${
                                            isSelected
                                            ? 'border-emerald-500 bg-emerald-50/20 shadow-md'
                                            : 'border-slate-100 bg-white hover:border-emerald-200'
                                        }`}
                                    >
                                        <div className="flex justify-between items-start mb-4">
                                            <div className="min-w-0">
                                                <h4 className={`text-sm font-black truncate ${isSelected ? 'text-slate-900' : 'text-slate-600'}`}>{product.name}</h4>
                                                <p className="text-[10px] text-emerald-600 font-bold mt-0.5">{product.rate}%</p>
                                            </div>
                                            <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all ${
                                                isSelected ? 'bg-emerald-500 border-emerald-500 text-white' : 'border-slate-200'
                                            }`}>
                                                <CheckCircle2 className="w-3 h-3" />
                                            </div>
                                        </div>
                                        <p className="text-[10px] text-slate-400 font-medium">최대 {formatAmount(product.limit)}</p>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>

                {/* 2. 상품 상세 (8컬럼 - 메인 설정 영역) */}
                <div className="lg:col-span-8 space-y-6">
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
                                <div className="space-y-4">
                                    <label className="block text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">
                                        대출 실행 금액 설정
                                    </label>
                                    <div className="relative">
                                        <input
                                            type="text"
                                            inputMode="numeric"
                                            value={executeAmount.toLocaleString()}
                                            onChange={handleAmountChange}
                                            className={`w-full px-6 py-5 bg-slate-50 border-2 rounded-2xl text-2xl font-black text-slate-900 focus:ring-4 focus:ring-emerald-500/10 outline-none transition-all ${amountError ? 'border-rose-400' : 'border-slate-50'}`}
                                        />
                                        <span className="absolute right-6 top-1/2 -translate-y-1/2 text-base font-black text-slate-400">원</span>
                                    </div>
                                    {amountError && (
                                        <p className="mt-1 text-xs text-rose-500 font-bold px-1">{amountError}</p>
                                    )}
                                    <div className="flex gap-2">
                                        {[0.3, 0.5, 0.7, 1.0].map((ratio) => (
                                            <button
                                                key={ratio}
                                                type="button"
                                                onClick={() => {
                                                    const amt = Math.floor(effectiveLimit * ratio / 10000) * 10000;
                                                    setExecuteAmount(amt);
                                                    setAmountError(null);
                                                }}
                                                className="flex-1 py-2.5 bg-slate-50 hover:bg-slate-100 text-slate-600 text-[10px] font-black rounded-xl border border-slate-100 transition-all"
                                            >
                                                {ratio === 1.0 ? '전액' : `${ratio * 100}%`}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div className="space-y-4">
                                    <label className="block text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">대출 기간(만기) 선택</label>
                                    <div className="grid grid-cols-3 gap-3">
                                        {[12, 24, 36].map(m => (
                                            <button
                                                key={m}
                                                onClick={() => setPeriod(m)}
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
                                onClick={() => onNext({ ...selectedProduct, period, executeAmount })}
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
                </div>
            </div>
        </div>
    );
};

export default LoanProductSelection;
