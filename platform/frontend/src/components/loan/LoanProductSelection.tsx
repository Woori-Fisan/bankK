import React, { useState, useMemo } from 'react';
import { Building, ChevronRight, ChevronLeft, Building2 } from 'lucide-react';
import type { LoanProduct } from '../../pages/LoanApplication';

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
    }, [selectedProduct?.id]);

    const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const raw = e.target.value.replace(/,/g, '');
        if (raw === '') { setExecuteAmount(0); setAmountError(null); return; }
        const val = Number(raw);
        if (isNaN(val)) return;
        const capped = Math.min(val, effectiveLimit);
        setExecuteAmount(capped);
        setAmountError(capped < 1_000_000 ? '최소 100만원 이상 입력해주세요.' : null);
    };

    const formatAmount = (amt: number) => {
        if (amt >= 100000000) return `${amt / 100000000}억원`;
        if (amt >= 10000) return `${amt / 10000}만원`;
        return `${amt}원`;
    };

    const calculateMonthly = (limit: number, rate: number, p: number) => {
        const monthlyRate = (rate / 100) / 12;
        const numerator = limit * monthlyRate * Math.pow(1 + monthlyRate, p);
        const denominator = Math.pow(1 + monthlyRate, p) - 1;
        return Math.floor(numerator / denominator);
    };

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 상품 목록 */}
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <div className="inline-flex p-1 bg-gray-100 rounded-lg shadow-inner">
                            <button
                                onClick={() => setTab('RATE')}
                                className={`px-5 py-2 text-sm font-bold rounded-md transition-all ${tab === 'RATE' ? 'bg-slate-900 text-white shadow-md' : 'text-gray-500 hover:text-gray-700'}`}
                            >
                                금리 낮은순
                            </button>
                            <button
                                onClick={() => setTab('LIMIT')}
                                className={`px-5 py-2 text-sm font-bold rounded-md transition-all ${tab === 'LIMIT' ? 'bg-slate-900 text-white shadow-md' : 'text-gray-500 hover:text-gray-700'}`}
                            >
                                한도 높은순
                            </button>
                        </div>
                        <span className="text-sm text-gray-400 font-medium">대출가능 <span className="text-emerald-500">{products.length}</span>건</span>
                    </div>

                    <div className="space-y-3 h-[620px] overflow-y-auto pr-2 custom-scrollbar">
                        {sortedProducts.map(product => (
                            <div
                                key={product.id}
                                onClick={() => setSelectedId(product.id)}
                                className={`p-6 border rounded-2xl cursor-pointer transition-all flex items-center gap-4 ${
                                    selectedId === product.id
                                    ? 'border-emerald-500 bg-emerald-50/30 ring-1 ring-emerald-500'
                                    : 'border-gray-200 bg-white hover:border-gray-300 shadow-sm'
                                }`}
                            >
                                <div className={`w-12 h-12 rounded-full flex items-center justify-center shrink-0 ${selectedId === product.id ? 'bg-emerald-100 text-emerald-600' : 'bg-gray-100 text-gray-400'}`}>
                                    <Building className="w-6 h-6" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-base font-bold text-gray-900 truncate">{product.name}</p>
                                    <div className="flex gap-1.5 mt-1">
                                        {product.tags.map((tag) => (
                                            <span key={tag} className={`px-2 py-1 rounded text-[10px] font-bold ${tag === '최저금리' ? 'bg-red-50 text-red-600' : 'bg-blue-50 text-blue-600'}`}>
                                                {tag}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-2xl font-bold text-gray-900 leading-none">{product.rate}%</p>
                                    <p className="text-sm text-gray-400 mt-1">{formatAmount(product.limit)}</p>
                                </div>
                                <ChevronRight className={`w-4 h-4 shrink-0 ${selectedId === product.id ? 'text-emerald-500' : 'text-gray-300'}`} />
                            </div>
                        ))}
                    </div>
                </div>

                {/* 상품 상세 */}
                <div className="space-y-4">
                    <p className="text-sm text-gray-400 font-bold uppercase tracking-wider">선택된 상품 상세</p>
                    <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-md">
                        <div className="flex items-center gap-3 mb-6">
                            <div className="w-14 h-14 bg-gray-50 border border-gray-100 rounded-xl flex items-center justify-center text-gray-400">
                                <Building2 className="w-7 h-7" />
                            </div>
                            <div>
                                <h3 className="text-lg font-bold text-gray-900">{selectedProduct?.name}</h3>
                                <div className="flex gap-1.5 mt-1">
                                    {selectedProduct?.tags.map((tag) => (
                                        <span key={tag} className={`px-2 py-1 rounded text-[10px] font-bold ${tag === '최저금리' ? 'bg-red-50 text-red-600' : 'bg-blue-50 text-blue-600'}`}>
                                            {tag}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        </div>

                        <div className="mb-8">
                            <p className="text-4xl font-bold text-gray-900">{selectedProduct?.rate}%</p>
                            <p className="text-sm text-gray-500 mt-1">승인 한도 {formatAmount(effectiveLimit)}</p>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-xs text-gray-500 font-bold mb-2">
                                    대출 신청 금액
                                </label>
                                <div className="flex items-center gap-2">
                                    <div className="relative flex-1">
                                        <input
                                            type="text"
                                            inputMode="numeric"
                                            value={executeAmount.toLocaleString()}
                                            onChange={handleAmountChange}
                                            className={`w-full px-3 py-3 pr-8 bg-gray-50 border rounded-xl text-base font-bold focus:ring-2 focus:ring-emerald-500 outline-none ${amountError ? 'border-red-400' : 'border-gray-200'}`}
                                        />
                                        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-400 font-medium">원</span>
                                    </div>
                                    <button
                                        type="button"
                                        onClick={() => { setExecuteAmount(effectiveLimit); setAmountError(null); }}
                                        className="shrink-0 px-3 py-3 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-bold rounded-xl border border-emerald-200 transition-colors whitespace-nowrap"
                                    >
                                        최대 한도 적용
                                    </button>
                                </div>
                                {amountError && (
                                    <p className="mt-1 text-xs text-red-500">{amountError}</p>
                                )}
                                <div className="flex gap-1.5 mt-2">
                                    {[0.3, 0.5, 0.7, 1.0].map((ratio) => (
                                        <button
                                            key={ratio}
                                            type="button"
                                            onClick={() => {
                                                const amt = Math.floor(effectiveLimit * ratio / 10000) * 10000;
                                                setExecuteAmount(amt);
                                                setAmountError(null);
                                            }}
                                            className="flex-1 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-600 text-xs font-bold rounded-lg transition-colors"
                                        >
                                            {ratio === 1.0 ? '전액' : `${ratio * 100}%`}
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <div>
                                <label className="block text-xs text-gray-500 font-bold mb-2">만기 선택</label>
                                <div className="grid grid-cols-3 gap-2">
                                    {[12, 24, 36].map(m => (
                                        <button
                                            key={m}
                                            onClick={() => setPeriod(m)}
                                            className={`py-3 rounded-xl text-sm font-bold border transition-all ${
                                                period === m
                                                ? 'bg-emerald-50 border-emerald-500 text-emerald-700 shadow-sm'
                                                : 'bg-white border-gray-200 text-gray-500 hover:border-gray-300'
                                            }`}
                                        >
                                            {m}개월
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="bg-gray-50 rounded-xl p-6 border border-gray-100">
                                <div className="flex justify-between items-center mb-4 pb-4 border-b border-gray-200">
                                    <span className="text-sm text-gray-500">첫 달 갚을 금액</span>
                                    <span className="text-xl font-bold text-emerald-600">
                                        {selectedProduct && !amountError && executeAmount > 0
                                            ? calculateMonthly(executeAmount, selectedProduct.rate, period).toLocaleString()
                                            : 0}원
                                    </span>
                                </div>
                                <div className="space-y-3">
                                    <div className="flex justify-between text-xs">
                                        <span className="text-gray-400 font-medium">대출 금액</span>
                                        <span className="text-gray-900 font-bold">{executeAmount.toLocaleString()}원</span>
                                    </div>
                                    <div className="flex justify-between text-xs">
                                        <span className="text-gray-400 font-medium">연 금리</span>
                                        <span className="text-gray-900 font-bold">{selectedProduct?.rate}%</span>
                                    </div>
                                    <div className="flex justify-between text-xs">
                                        <span className="text-gray-400 font-medium">대출 기간</span>
                                        <span className="text-gray-900 font-bold">{period}개월</span>
                                    </div>
                                    <div className="flex justify-between text-xs">
                                        <span className="text-gray-400 font-medium">상환 방법</span>
                                        <span className="text-gray-900 font-bold">원리금균등상환</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <button
                            onClick={() => onNext({ ...selectedProduct, period, executeAmount })}
                            disabled={!!amountError || executeAmount < 1_000_000}
                            className="w-full mt-8 py-5 bg-slate-900 text-white rounded-2xl font-black text-lg hover:bg-slate-800 active:scale-[0.98] transition-all shadow-lg shadow-slate-200 disabled:bg-gray-200 disabled:text-gray-400 disabled:cursor-not-allowed disabled:shadow-none disabled:active:scale-100"
                        >
                            이 상품으로 계약 서류 확인 →
                        </button>
                    </div>
                </div>
            </div>

            <div className="flex justify-between pt-6 border-t border-gray-200">
                <button
                    onClick={onBack}
                    className="flex items-center gap-2 px-6 py-4 bg-gray-100 text-gray-600 rounded-2xl font-black text-base hover:bg-gray-200 active:scale-[0.98] transition-all"
                >
                    <ChevronLeft className="w-4 h-4" />
                    이전으로
                </button>
            </div>
        </div>
    );
};

export default LoanProductSelection;
