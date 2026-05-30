import React, { useState } from 'react';
import { AlertTriangle, ShieldCheck, Loader2 } from 'lucide-react';
import PinpadModal from '../pinpad/PinpadModal';
import type { LoanData, LoanProduct } from '../../pages/LoanApplication';
import { useExecuteLoan, extractApiError } from '../../hooks/useLoan';
import { formatAmount } from '../../utils/formatter';

interface LoanExecutionConfirmProps {
    loanData: LoanData;
    product: LoanProduct;
    evaluationId: string;
    onNext: () => void;
    onBack: () => void;
}

const LoanExecutionConfirm: React.FC<LoanExecutionConfirmProps> = ({
    loanData,
    product,
    evaluationId,
    onNext,
    onBack,
}) => {
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const executeMutation = useExecuteLoan();

    const handleExecution = () => {
        setSubmitError(null);
        setIsPinpadOpen(true);
    };

    const handlePinComplete = async (pin: string) => {
        setIsPinpadOpen(false);
        setSubmitError(null);

        try {
            await executeMutation.mutateAsync({
                evaluationId,
                loanProductCode: product.loanProductCode,
                depositAccountNo: loanData.accountNo!,
                accountPassword: pin,
                executeAmount: product.executeAmount ?? product.limit,
                repaymentPeriod: product.period ?? 12,
            });

            onNext();
        } catch (err) {
            setSubmitError(extractApiError(err));
        }
    };

    const maturityDate = new Date();
    maturityDate.setMonth(maturityDate.getMonth() + (product.period ?? 0));
    const maturityDateString = maturityDate.toISOString().split('T')[0];

    const isLoading = executeMutation.isPending;

    return (
        <div className="flex flex-col items-center justify-center min-h-[600px]">
            <div className="bg-white border border-gray-200 rounded-3xl shadow-xl w-full max-w-xl overflow-hidden">
                <div className="p-10 border-b border-gray-100">
                    <h2 className="text-2xl font-bold text-gray-900 mb-1">대출 실행 최종 확인</h2>
                    <p className="text-base text-gray-500">아래 내용을 최종 확인 후 대출을 실행해 주세요.</p>
                </div>

                <div className="p-10 space-y-8">
                    <div className="bg-gray-50 rounded-2xl p-8 space-y-5 border border-gray-100">
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-gray-400 font-bold uppercase">신청인</span>
                            <span className="text-sm font-bold text-gray-900">{loanData.userName}</span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-gray-400 font-bold uppercase">상품명</span>
                            <span className="text-sm font-bold text-gray-900">{product.name}</span>
                        </div>
                        <div className="flex justify-between items-center pt-2 border-t border-gray-200">
                            <span className="text-xs text-gray-400 font-bold uppercase">대출 금액</span>
                            <span className="text-2xl font-bold text-emerald-600">
                                ₩ {formatAmount(product.executeAmount ?? product.limit)}
                            </span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-gray-400 font-bold uppercase">적용 금리</span>
                            <span className="text-sm font-bold text-gray-900">{product.rate}% (고정)</span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-gray-400 font-bold uppercase">대출 기간</span>
                            <span className="text-sm font-bold text-gray-900">{product.period}개월</span>
                        </div>
                        <div className="flex justify-between items-center pt-2 border-t border-gray-200">
                            <span className="text-xs text-gray-400 font-bold uppercase">입금 계좌</span>
                            <span className="text-sm font-bold text-gray-900">
                                {loanData.bank} {loanData.accountNo}
                            </span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-gray-400 font-bold uppercase">만기일</span>
                            <span className="text-sm font-bold text-gray-900">{maturityDateString}</span>
                        </div>
                    </div>

                    {submitError && (
                        <p className="text-sm text-red-500 text-center">{submitError}</p>
                    )}

                    <div className="bg-amber-50 border border-amber-100 rounded-xl p-5 flex gap-3">
                        <AlertTriangle className="w-6 h-6 text-amber-500 shrink-0" />
                        <p className="text-sm text-amber-700 leading-relaxed font-medium">
                            대출 실행 후에는 취소가 불가합니다. 위 내용을 다시 한번 확인해 주세요.
                        </p>
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onBack}
                            disabled={isLoading}
                            className="flex-1 py-4 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-base hover:bg-gray-50 transition-colors disabled:opacity-50"
                        >
                            이전으로
                        </button>
                        <button
                            type="button"
                            onClick={handleExecution}
                            disabled={isLoading}
                            className="flex-[2] py-5 bg-slate-900 text-white rounded-xl font-black text-base hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200 flex items-center justify-center gap-2 disabled:opacity-50"
                        >
                            {isLoading ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    대출 실행 중...
                                </>
                            ) : (
                                <>
                                    <ShieldCheck className="w-4 h-4" />
                                    대출 실행
                                </>
                            )}
                        </button>
                    </div>
                </div>
            </div>

            <PinpadModal
                isOpen={isPinpadOpen}
                onClose={() => setIsPinpadOpen(false)}
                onComplete={handlePinComplete}
                title="계좌 비밀번호 입력"
            />
        </div>
    );
};

export default LoanExecutionConfirm;
