import React, { useState, useMemo } from 'react';
import type { LoanProduct } from '../../pages/LoanApplication';

// Sub-components
import LoanProductListSection from './sections/LoanProductListSection';
import LoanProductDetailSection from './sections/LoanProductDetailSection';

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

    const handleAmountChange = (val: string) => {
        const raw = val.replace(/,/g, '');
        if (raw === '') { setExecuteAmount(0); setAmountError(null); return; }
        const num = Number(raw);
        if (isNaN(num)) return;

        if (num > effectiveLimit) {
            setExecuteAmount(num);
            setAmountError('한도 금액 이상의 입력은 불가능합니다.');
        } else if (num < 1_000_000) {
            setExecuteAmount(num);
            setAmountError('최소 100만원 이상 입력해주세요.');
        } else {
            setExecuteAmount(num);
            setAmountError(null);
        }
    };

    const handleQuickAdd = (val: number) => {
        const nextAmt = executeAmount + val;
        if (nextAmt > effectiveLimit) {
            setExecuteAmount(nextAmt);
            setAmountError('한도 금액 이상의 입력은 불가능합니다.');
        } else if (nextAmt < 1_000_000) {
            setExecuteAmount(nextAmt);
            setAmountError('최소 100만원 이상 입력해주세요.');
        } else {
            setExecuteAmount(nextAmt);
            setAmountError(null);
        }
    };

    return (
        <div className="w-full space-y-6 animate-in fade-in duration-500">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                <div className="lg:col-span-4">
                    <LoanProductListSection
                        products={sortedProducts}
                        selectedId={selectedId}
                        onSelect={setSelectedId}
                        tab={tab}
                        onTabChange={setTab}
                    />
                </div>

                <div className="lg:col-span-8 space-y-6">
                    <LoanProductDetailSection
                        selectedProduct={selectedProduct}
                        effectiveLimit={effectiveLimit}
                        executeAmount={executeAmount}
                        onAmountChange={handleAmountChange}
                        onQuickAdd={handleQuickAdd}
                        onAllIn={() => {
                            setExecuteAmount(effectiveLimit);
                            setAmountError(null);
                        }}
                        amountError={amountError}
                        period={period}
                        onPeriodChange={setPeriod}
                        onNext={() => onNext({ ...selectedProduct!, period, executeAmount })}
                        onBack={onBack}
                    />
                </div>
            </div>
        </div>
    );
};

export default LoanProductSelection;
