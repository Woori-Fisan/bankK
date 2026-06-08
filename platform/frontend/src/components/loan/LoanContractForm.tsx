import React, { useState, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import type { LoanProduct, LoanData } from '../../pages/LoanApplication';
import { useContractDocuments, extractApiError, useExecuteLoan } from '../../hooks/useLoan';
import type { ContractDocument, ExecutionResponse } from '../../api/loanApi';
import { formatAmount } from '../../utils/formatter';

// Sub-components
import LoanTermsSection from './sections/LoanTermsSection';
import LoanSideSummary from './sections/LoanSideSummary';
import LoanTermsModal from './modals/LoanTermsModal';
import LoanConfirmModal from './modals/LoanConfirmModal';
import PinpadModal from '../pinpad/PinpadModal';
import { decryptBankResponse, prepareSecureRequest } from '../../utils/bankCrypto';

interface AgreedContractDoc extends ContractDocument {
    agreed: boolean;
}

interface LoanContractFormProps {
    product: LoanProduct;
    loanData: LoanData;
    evaluationId: string;
    onNext: (result: ExecutionResponse) => void;
    onBack: () => void;
}

const LoanContractForm: React.FC<LoanContractFormProps> = ({
    product,
    loanData,
    evaluationId,
    onNext,
    onBack,
}) => {
    const { data, isLoading, error } = useContractDocuments(product.loanProductCode, evaluationId);
    const [agreedDocs, setAgreedDocs] = useState<AgreedContractDoc[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeDoc, setActiveDoc] = useState<AgreedContractDoc | null>(null);
    const [viewedDocs, setViewedDocs] = useState<Set<string>>(new Set());

    const [isPinpadOpen, setIsPinpadOpen] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [isExecutionConfirmOpen, setIsExecutionConfirmOpen] = useState(false);
    const executeMutation = useExecuteLoan();

    useEffect(() => {
        // 이미 데이터가 초기화된 경우(agreedDocs.length > 0) 재설정 방지
        if (data?.documents && agreedDocs.length === 0) {
            setAgreedDocs(data.documents.map((d) => ({ ...d, agreed: false })));
        }
    }, [data, agreedDocs.length]);

    const handleTermToggle = (documentType: string) => {
        setAgreedDocs((prev) =>
            prev.map((d) => (d.documentType === documentType ? { ...d, agreed: !d.agreed } : d)),
        );
    };

    const openModal = (doc: AgreedContractDoc) => {
        setActiveDoc(doc);
        setIsModalOpen(true);
        // 모달을 열 때가 아니라, 실제 '동의하고 확인'을 눌렀을 때 viewedDocs에 추가하도록 변경
    };

    const handleModalAgree = () => {
        if (!activeDoc) {
            setIsModalOpen(false);
            return;
        }

        const targetType = activeDoc.documentType;

        // 1. 읽음 목록에 즉시 추가 (함수형 업데이트로 최신 상태 보장)
        setViewedDocs(prev => {
            const next = new Set(prev);
            next.add(targetType);
            return next;
        });

        // 2. 해당 약관을 즉시 '동의' 상태로 변경
        setAgreedDocs(prev =>
            prev.map(d => d.documentType === targetType ? { ...d, agreed: true } : d)
        );

        setIsModalOpen(false);
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
                headers: { ...headers, 'X-Idempotency-Key': crypto.randomUUID() },
            });

            // 3. 응답 복호화 (메모리에 보관 중이던 aesKey 사용)
            if (result.resPayload) {
                const decrypted = await decryptBankResponse(result.resPayload, aesKey);
                // 복호화된 데이터(예: 고객 성명 등)를 결과 객체에 병합
                Object.assign(result, decrypted);
            }

            if (result.executedAt) {
                result.executedAt = result.executedAt.replace('Z', '');
            }

            onNext(result);
        } catch (err) {
            setSubmitError(extractApiError(err));
        }
    };

    const mandatoryDocs = agreedDocs.filter((d) => d.isMandatory);
    const isNextDisabled = mandatoryDocs.some((d) => !d.agreed);
    const isExecuting = executeMutation.isPending;

    return (
        <div className="space-y-8">
            {submitError && (
                <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 max-w-4xl mx-auto animate-in fade-in slide-in-from-top-2">
                    <AlertCircle className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm font-bold">{submitError}</span>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                <div className="lg:col-span-8 space-y-6">
                    {error && (
                        <div className="p-4 bg-red-50 border border-red-200 rounded-xl">
                            <p className="text-sm text-red-600">{extractApiError(error)}</p>
                        </div>
                    )}

                    <LoanTermsSection
                        agreedDocs={agreedDocs as any}
                        onTermToggle={handleTermToggle}
                        onOpenModal={openModal as any}
                        viewedDocs={viewedDocs}
                        isLoading={isLoading}
                        title="계약 서류 확인"
                        stepNumber="1"
                    />
                </div>

                <div className="lg:col-span-4">
                    <LoanSideSummary
                        title="선택 상품 요약"
                        items={[
                            { label: '상품명', value: product.name },
                            { label: '승인 한도', value: <span className="text-slate-500 line-through decoration-slate-300">₩ {formatAmount(product.limit)}</span> },
                            { 
                                label: '대출 신청 금액', 
                                value: <span className="text-xl text-emerald-600 font-black">₩ {formatAmount(product.executeAmount || product.limit)}</span> 
                            },
                            { label: '적용 금리', value: <>{product.rate}% <span className="text-xs font-bold text-slate-400 ml-1">(고정금리)</span></> },
                            { label: '대출 기간', value: `${product.period}개월` }
                        ]}
                        buttonText="대출 진행"
                        onButtonClick={() => setIsExecutionConfirmOpen(true)}
                        onBackClick={onBack}
                        isButtonDisabled={isNextDisabled || isLoading}
                        isPending={isExecuting}
                    />
                </div>
            </div>

            <LoanConfirmModal
                isOpen={isExecutionConfirmOpen}
                onClose={() => setIsExecutionConfirmOpen(false)}
                onConfirm={() => {
                    setIsExecutionConfirmOpen(false);
                    setSubmitError(null);
                    setIsPinpadOpen(true);
                }}
                title="최종 대출 실행 확인"
                description="계약 서류 동의를 마치고 대출을 실행하시겠습니까?"
                items={[
                    { label: '고객 성명', value: loanData.userName },
                    { label: '선택 상품', value: product.name },
                    { 
                        label: '대출 실행 금액', 
                        value: <span className="text-2xl text-emerald-600 font-black">₩ {formatAmount(product.executeAmount || product.limit)}</span> 
                    },
                    { label: '적용 금리', value: `${product.rate}% (고정)` },
                    { 
                        label: '대출금 입금 계좌', 
                        value: (
                            <div className="text-right">
                                <p className="text-sm font-bold text-emerald-600">{loanData.bank}</p>
                                <p className="text-lg font-black text-slate-900">{loanData.accountNo}</p>
                            </div>
                        )
                    }
                ]}
                bottomInfo="'확인 완료' 버튼을 누르면 대출이 즉시 실행되며, 지정하신 계좌로 대출금이 입금됩니다. 실행 후에는 취소가 불가하오니 신중히 확인해 주세요."
            />

            <LoanTermsModal
                isOpen={isModalOpen}
                activeDoc={activeDoc}
                onClose={() => setIsModalOpen(false)}
                onAgree={handleModalAgree}
            />

            <PinpadModal
                isOpen={isPinpadOpen}
                onClose={() => setIsPinpadOpen(false)}
                onComplete={handlePinComplete}
                title="계좌 비밀번호 입력"
            />
        </div>
    );
};

export default LoanContractForm;