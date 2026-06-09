import React from 'react';
import { Building, CheckCircle2 } from 'lucide-react';
import { formatAmount } from '../../../utils/formatter';
import type { LoanProduct } from '../../../pages/LoanApplication';

interface LoanProductListSectionProps {
    products: LoanProduct[];
    selectedId: number | undefined;
    onSelect: (id: number) => void;
    tab: 'RATE' | 'LIMIT';
    onTabChange: (tab: 'RATE' | 'LIMIT') => void;
}

const LoanProductListSection: React.FC<LoanProductListSectionProps> = ({
    products,
    selectedId,
    onSelect,
    tab,
    onTabChange,
}) => {
    return (
        <div className="sticky top-10 space-y-6">
            <div className="flex items-center justify-between px-1">
                <div className="inline-flex p-1 bg-slate-100 rounded-xl shadow-inner">
                    <button
                        onClick={() => onTabChange('RATE')}
                        className={`px-4 py-2 text-[10px] font-black rounded-lg transition-all ${tab === 'RATE' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`}
                    >
                        금리순
                    </button>
                    <button
                        onClick={() => onTabChange('LIMIT')}
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
                {products.map(product => {
                    const isSelected = selectedId === product.id;
                    
                    return (
                        <div
                            key={product.id}
                            onClick={() => onSelect(product.id)}
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
    );
};

export default LoanProductListSection;
