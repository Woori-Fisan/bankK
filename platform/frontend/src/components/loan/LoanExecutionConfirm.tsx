import React, { useState } from 'react';
import { AlertTriangle, ShieldCheck, Loader2, ChevronLeft } from 'lucide-react';
import PinpadModal from '../pinpad/PinpadModal';
import type { LoanData, LoanProduct } from '../../pages/LoanApplication';
import { useExecuteLoan, extractApiError } from '../../hooks/useLoan';
import { formatAmount } from '../../utils/formatter';
import { prepareSecureRequest, decryptBankResponse } from '../../utils/bankCrypto';
import type { ExecutionResponse } from '../../api/loanApi';

interface LoanExecutionConfirmProps {
    loanData: LoanData;
    product: LoanProduct;
    evaluationId: string;
    onNext: (result: ExecutionResponse) => void;
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

        // 1. 보안 요청 준비 (암호화 + 서명 + 키ID 통합 처리)
        const secureRequest = await prepareSecureRequest(
            {
                accountPassword: pin,
                depositAccountNo: loanData.accountNo!,
            },
            {
                loanNo: evaluationId,
                productId: product.id,
                executeAmount: product.executeAmount ?? product.limit,
                repaymentPeriod: product.period ?? 12,
                repaymentType: '원리금균등',
            },
            loanData.bankCode!,
        );

        if (!secureRequest) {
            setSubmitError('보안 요청 준비 중 오류가 발생했습니다.');
            return;
        }

        const { payload, headers, aesKey } = secureRequest;

        try {
            const result = await executeMutation.mutateAsync({
                payload,
                headers,
            });

            // 2. 응답 복호화 (메모리에 보관 중이던 aesKey 사용)
            if (result.resPayload) {
                const decrypted = await decryptBankResponse(result.resPayload, aesKey);
                result.borrowerName = decrypted.customerName;
            }

            onNext(result);
        } catch (err) {
            setSubmitError(extractApiError(err));
        }
    };

    const maturityDate = new Date();
    maturityDate.setMonth(maturityDate.getMonth() + (product.period ?? 0));
    const maturityDateString = maturityDate.toISOString().split('T')[0];

    const isLoading = executeMutation.isPending;

    return (
        <div>
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 w-full overflow-hidden">
                {/* 헤더 */}
                <div className="px-12 py-10 border-b border-gray-100">
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight mb-1">대출 실행 최종 확인</h2>
                    <p className="text-base text-gray-500 font-medium">아래 내용을 최종 확인 후 대출을 실행해 주세요.</p>
                </div>

                <div className="px-12 py-10 space-y-8">
                    {/* 대출 금액 강조 */}
                    <div className="bg-gray-50 rounded-2xl p-10 text-center border border-gray-100">
                        <p className="text-sm text-gray-500 mb-2 font-medium">대출 실행 금액</p>
                        <p className="text-5xl font-black text-gray-900 tracking-tight">
                            ₩ {formatAmount(product.executeAmount ?? product.limit)}
                        </p>
                    </div>

                    {/* 상세 정보 */}
                    <div className="grid grid-cols-2 gap-6">
                        <div className="space-y-5">
                            <div className="flex flex-col gap-1">
                                <span className="text-xs text-gray-400 font-bold uppercase tracking-wider">신청인</span>
                                <span className="text-base font-bold text-gray-900">{loanData.userName}</span>
                            </div>
                            <div className="flex flex-col gap-1">
                                <span className="text-xs text-gray-400 font-bold uppercase tracking-wider">상품명</span>
                                <span className="text-base font-bold text-gray-900">{product.name}</span>
                            </div>
                            <div className="flex flex-col gap-1">
                                <span className="text-xs text-gray-400 font-bold uppercase tracking-wider">적용 금리</span>
                                <span className="text-base font-bold text-gray-900">{product.rate}% (고정)</span>
                            </div>
                        </div>
                        <div className="space-y-5">
                            <div className="flex flex-col gap-1">
                                <span className="text-xs text-gray-400 font-bold uppercase tracking-wider">입금 계좌</span>
                                <span className="text-base font-bold text-gray-900">{loanData.bank} {loanData.accountNo}</span>
                            </div>
                            <div className="flex flex-col gap-1">
                                <span className="text-xs text-gray-400 font-bold uppercase tracking-wider">대출 기간</span>
                                <span className="text-base font-bold text-gray-900">{product.period}개월</span>
                            </div>
                            <div className="flex flex-col gap-1">
                                <span className="text-xs text-gray-400 font-bold uppercase tracking-wider">만기일</span>
                                <span className="text-base font-bold text-gray-900">{maturityDateString}</span>
                            </div>
                        </div>
                    </div>

                    {submitError && (
                        <p className="text-base text-red-500 text-center">{submitError}</p>
                    )}

                    <div className="bg-amber-50 border border-amber-100 rounded-xl p-5 flex gap-3">
                        <AlertTriangle className="w-6 h-6 text-amber-500 shrink-0 mt-0.5" />
                        <p className="text-base text-amber-700 leading-relaxed font-medium">
                            대출 실행 후에는 취소가 불가합니다. 위 내용을 다시 한번 확인해 주세요.
                        </p>
                    </div>
                </div>

                {/* 버튼 */}
                <div className="px-12 pb-10 grid grid-cols-2 gap-4">
                    <button
                        type="button"
                        onClick={onBack}
                        disabled={isLoading}
                        className="py-5 bg-gray-100 text-gray-600 rounded-2xl font-black text-lg hover:bg-gray-200 active:scale-[0.98] transition-all disabled:opacity-50 flex items-center justify-center gap-2"
                    >
                        <ChevronLeft className="w-5 h-5" />
                        이전으로
                    </button>
                    <button
                        type="button"
                        onClick={handleExecution}
                        disabled={isLoading}
                        className="py-5 bg-slate-900 text-white rounded-2xl font-black text-lg hover:bg-slate-800 active:scale-[0.98] transition-all shadow-lg shadow-slate-200 flex items-center justify-center gap-2 disabled:opacity-50"
                    >
                        {isLoading ? (
                            <>
                                <Loader2 className="w-5 h-5 animate-spin" />
                                대출 실행 중...
                            </>
                        ) : (
                            <>
                                <ShieldCheck className="w-5 h-5" />
                                대출 실행
                            </>
                        )}
                    </button>
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
