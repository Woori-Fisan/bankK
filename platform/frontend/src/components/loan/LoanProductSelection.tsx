import React, { useState, useMemo } from 'react';
import { Building, ChevronRight, ChevronLeft, Building2 } from 'lucide-react';

interface LoanProductSelectionProps {
    products: any[];
    onNext: (product: any) => void;
    onBack: () => void;
}

const LoanProductSelection: React.FC<LoanProductSelectionProps> = ({ products, onNext, onBack }) => {
    const [selectedId, setSelectedId] = useState(products[0]?.id);
    const [tab, setTab] = useState<'RATE' | 'LIMIT'>('RATE');
    const [period, setPeriod] = useState(24);

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
                                className={`px-4 py-1.5 text-xs font-bold rounded-md transition-all ${tab === 'RATE' ? 'bg-slate-900 text-white shadow-md' : 'text-gray-500 hover:text-gray-700'}`}
                            >
                                금리 낮은순
                            </button>
                            <button 
                                onClick={() => setTab('LIMIT')}
                                className={`px-4 py-1.5 text-xs font-bold rounded-md transition-all ${tab === 'LIMIT' ? 'bg-slate-900 text-white shadow-md' : 'text-gray-500 hover:text-gray-700'}`}
                            >
                                한도 높은순
                            </button>
                        </div>
                        <span className="text-[11px] text-gray-400 font-medium">대출가능 <span className="text-emerald-500">{products.length}</span>건</span>
                    </div>

                    <div className="space-y-3 h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                        {sortedProducts.map(product => (
                            <div 
                                key={product.id}
                                onClick={() => setSelectedId(product.id)}
                                className={`p-4 border rounded-2xl cursor-pointer transition-all flex items-center gap-4 ${
                                    selectedId === product.id 
                                    ? 'border-emerald-500 bg-emerald-50/30 ring-1 ring-emerald-500' 
                                    : 'border-gray-200 bg-white hover:border-gray-300 shadow-sm'
                                }`}
                            >
                                <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${selectedId === product.id ? 'bg-emerald-100 text-emerald-600' : 'bg-gray-100 text-gray-400'}`}>
                                    <Building className="w-5 h-5" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-xs font-bold text-gray-900 truncate">{product.name}</p>
                                    <div className="flex gap-1.5 mt-1">
                                        {product.tags.map((tag: string) => (
                                            <span key={tag} className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${tag === '최저금리' ? 'bg-red-50 text-red-600' : 'bg-blue-50 text-blue-600'}`}>
                                                {tag}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-lg font-bold text-gray-900 leading-none">{product.rate}%</p>
                                    <p className="text-[10px] text-gray-400 mt-1">{formatAmount(product.limit)}</p>
                                </div>
                                <ChevronRight className={`w-4 h-4 shrink-0 ${selectedId === product.id ? 'text-emerald-500' : 'text-gray-300'}`} />
                            </div>
                        ))}
                    </div>
                </div>

                {/* 상품 상세 */}
                <div className="space-y-4">
                    <p className="text-[11px] text-gray-400 font-bold uppercase tracking-wider">선택된 상품 상세</p>
                    <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-md">
                        <div className="flex items-center gap-3 mb-6">
                            <div className="w-12 h-12 bg-gray-50 border border-gray-100 rounded-xl flex items-center justify-center text-gray-400">
                                <Building2 className="w-6 h-6" />
                            </div>
                            <div>
                                <h3 className="text-sm font-bold text-gray-900">{selectedProduct?.name}</h3>
                                <div className="flex gap-1.5 mt-1">
                                    {selectedProduct?.tags.map((tag: string) => (
                                        <span key={tag} className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${tag === '최저금리' ? 'bg-red-50 text-red-600' : 'bg-blue-50 text-blue-600'}`}>
                                            {tag}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        </div>

                        <div className="mb-8">
                            <p className="text-3xl font-bold text-gray-900">{selectedProduct?.rate}%</p>
                            <p className="text-xs text-gray-500 mt-1">한도 {formatAmount(selectedProduct?.limit)}</p>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-[11px] text-gray-500 font-bold mb-2">만기 선택</label>
                                <div className="grid grid-cols-3 gap-2">
                                    {[12, 24, 36].map(m => (
                                        <button 
                                            key={m}
                                            onClick={() => setPeriod(m)}
                                            className={`py-2.5 rounded-xl text-xs font-bold border transition-all ${
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

                            <div className="bg-gray-50 rounded-xl p-5 border border-gray-100">
                                <div className="flex justify-between items-center mb-4 pb-4 border-b border-gray-200">
                                    <span className="text-xs text-gray-500">첫 달 갚을 금액</span>
                                    <span className="text-lg font-bold text-emerald-600">
                                        {selectedProduct ? calculateMonthly(selectedProduct.limit, selectedProduct.rate, period).toLocaleString() : 0}원
                                    </span>
                                </div>
                                <div className="space-y-3">
                                    <div className="flex justify-between text-[11px]">
                                        <span className="text-gray-400 font-medium">대출 금액</span>
                                        <span className="text-gray-900 font-bold">{formatAmount(selectedProduct?.limit)}</span>
                                    </div>
                                    <div className="flex justify-between text-[11px]">
                                        <span className="text-gray-400 font-medium">연 금리</span>
                                        <span className="text-gray-900 font-bold">{selectedProduct?.rate}%</span>
                                    </div>
                                    <div className="flex justify-between text-[11px]">
                                        <span className="text-gray-400 font-medium">대출 기간</span>
                                        <span className="text-gray-900 font-bold">{period}개월</span>
                                    </div>
                                    <div className="flex justify-between text-[11px]">
                                        <span className="text-gray-400 font-medium">상환 방법</span>
                                        <span className="text-gray-900 font-bold">원리금균등상환</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <button 
                            onClick={() => onNext({ ...selectedProduct, period })}
                            className="w-full mt-6 py-4 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200"
                        >
                            이 상품으로 계약 서류 확인 →
                        </button>
                    </div>
                </div>
            </div>

            <div className="flex justify-between pt-6 border-t border-gray-200">
                <button 
                    onClick={onBack}
                    className="flex items-center gap-2 px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-sm hover:bg-gray-50 transition-colors"
                >
                    <ChevronLeft className="w-4 h-4" />
                    이전으로
                </button>
            </div>
        </div>
    );
};

export default LoanProductSelection;
