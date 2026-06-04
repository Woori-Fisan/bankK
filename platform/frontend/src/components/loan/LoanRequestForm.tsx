import React, { useState, useRef, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import type { LoanData } from '../../pages/LoanApplication';
import { useReviewDocuments, useSubmitLoanEvaluation, useBankList, extractApiError } from '../../hooks/useLoan';
import type { ReviewDocument, EvaluationStatusResponse } from '../../api/loanApi';
import { useAuthStore } from '../../store/useAuthStore';
import { isValidAccountNumber } from '../../utils/validator';

// Sub-components
import LoanCustomerSection from './sections/LoanCustomerSection';
import LoanUploadSection, { REQUIRED_DOCS } from './sections/LoanUploadSection';
import LoanTermsSection from './sections/LoanTermsSection';
import LoanSideSummary from './sections/LoanSideSummary';
import LoanTermsModal from './modals/LoanTermsModal';
import LoanConfirmModal from './modals/LoanConfirmModal';

interface AgreedDoc extends ReviewDocument {
    agreed: boolean;
}

interface LoanRequestFormProps {
    onNext: (data: LoanData, loanNo: string) => void;
    onBack: () => void;
    onSseMessage: (data: EvaluationStatusResponse) => void;
    onSseError: (error: Error) => void;
}

const LoanRequestForm: React.FC<LoanRequestFormProps> = ({ onNext, onBack, onSseMessage, onSseError }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const sseControllerRef = useRef<AbortController | null>(null);
    const [rrnFront, setRrnFront] = useState('');
    const [rrnBack, setRrnBack] = useState('');
    const [formData, setFormData] = useState<LoanData>({
        userName: '',
        phone: '',
        bank: '',
        bankCode: '',
        accountNo: '',
    });
    const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof LoanData | 'rrn' | 'submit', string>>>({});

    const [isUploading, setIsUploading] = useState(false);
    const [files, setFiles] = useState<{ id: number; name: string; file: File }[]>([]);
    const [agreedDocs, setAgreedDocs] = useState<AgreedDoc[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeDoc, setActiveDoc] = useState<AgreedDoc | null>(null);
    const [viewedDocs, setViewedDocs] = useState<Set<string>>(new Set());
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);

    const { data: docsData, isLoading: isDocsLoading } = useReviewDocuments();
    const { data: bankList, isLoading: isBankListLoading } = useBankList();
    const submitMutation = useSubmitLoanEvaluation();

    useEffect(() => {
        if (docsData?.documents) {
            setAgreedDocs(docsData.documents.map((d) => ({ ...d, agreed: false })));
        }
    }, [docsData]);

    const handleTermToggle = (documentType: string) => {
        setAgreedDocs((prev) =>
            prev.map((d) => (d.documentType === documentType ? { ...d, agreed: !d.agreed } : d)),
        );
    };

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selected = e.target.files;
        if (!selected) return;
        const fileArray = Array.from(selected);
        e.target.value = '';
        setFiles((prev) => [
            ...prev,
            ...fileArray.map((file, i) => ({ id: Date.now() + i, name: file.name, file })),
        ]);
    };

    const handleFileDelete = (id: number) => {
        setFiles((prev) => prev.filter((f) => f.id !== id));
    };

    const openModal = (doc: AgreedDoc) => {
        setActiveDoc(doc);
        setIsModalOpen(true);
        setViewedDocs((prev) => new Set([...prev, doc.documentType]));
    };

    const handleModalAgree = () => {
        if (activeDoc) {
            setAgreedDocs((prev) =>
                prev.map((d) => (d.documentType === activeDoc.documentType ? { ...d, agreed: true } : d)),
            );
        }
        setIsModalOpen(false);
    };

    const validate = (): boolean => {
        const errors: typeof fieldErrors = {};
        if (!formData.userName?.trim()) errors.userName = '성명을 입력해주세요.';
        if (rrnFront.length !== 6) {
            errors.rrn = '주민등록번호 앞 6자리를 입력해주세요.';
        } else if (!/^[1-4]$/.test(rrnBack)) {
            errors.rrn = '주민등록번호 뒤 1자리(1~4)를 입력해주세요.';
        }
        if (!formData.bankCode) errors.bank = '은행을 선택해주세요.';
        if (!formData.accountNo?.trim()) {
            errors.accountNo = '계좌번호를 입력해주세요.';
        } else if (!isValidAccountNumber(formData.accountNo)) {
            errors.accountNo = '올바른 계좌번호 형식을 입력해주세요. (10~14자리 숫자)';
        }
        if (!allDocsCovered) {
            const missingLabels = coveredDocs.filter((d) => !d.covered).map((d) => d.label);
            errors.submit = `누락된 서류: ${missingLabels.join(', ')}`;
        }
        const mandatoryNotAgreed = agreedDocs.filter((d) => d.isMandatory && !d.agreed);
        if (mandatoryNotAgreed.length > 0) {
            errors.submit = '필수 약관에 모두 동의해주세요.';
        }
        setFieldErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleActualSubmit = async () => {
        setIsConfirmOpen(false);
        setFieldErrors({});
        setIsUploading(true);
        const requestKey = crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`;
        const rrnPrefix = rrnFront + rrnBack;

        const { accessToken } = useAuthStore.getState();
        const controller = new AbortController();
        sseControllerRef.current = controller;

        fetchEventSource(`/api/v1/loan/subscribe?requestKey=${encodeURIComponent(requestKey)}`, {
            headers: { Authorization: `Bearer ${accessToken}` },
            signal: controller.signal,
            onmessage(event) {
                if (event.event === 'timeout') {
                    onSseError(new Error('심사 결과를 받지 못했습니다. 처음부터 다시 신청해주세요.'));
                    controller.abort();
                    return;
                }
                if (event.event !== 'result') return;
                try {
                    const parsed: EvaluationStatusResponse = JSON.parse(event.data);
                    onSseMessage(parsed);
                    controller.abort();
                } catch {
                    onSseError(new Error('응답 파싱 오류'));
                    controller.abort();
                }
            },
            onerror(err) {
                onSseError(new Error('심사 결과 조회 중 연결 오류가 발생했습니다.'));
                controller.abort();
                throw err;
            },
        });

        try {
            const agreedAt = new Date().toISOString();
            const documents = agreedDocs
                .filter((d) => d.agreed)
                .map((d) => ({ documentType: d.documentType, agreedAt }));

            const result = await submitMutation.mutateAsync({
                payload: {
                    requestKey,
                    bankCode: formData.bankCode!,
                    customerName: formData.userName!,
                    customerRrnPrefix: rrnPrefix,
                    customerPhone: formData.phone ?? '',
                    depositBankCode: formData.bankCode!,
                    depositAccountNo: formData.accountNo!,
                    documents,
                },
                files: files.map((f) => f.file),
            });

            onNext({ ...formData, rrn: `${rrnFront}-${rrnBack}` }, result.loanNo);
        } catch (err) {
            sseControllerRef.current?.abort();
            setIsUploading(false);
            setFieldErrors({ submit: extractApiError(err) });
        }
    };

    const fileNames = files.map((f) => f.name.toLowerCase());
    const coveredDocs = REQUIRED_DOCS.map((doc) => ({
        ...doc,
        covered: fileNames.some((name) => doc.keywords.some((kw) => name.includes(kw))),
    }));
    const allDocsCovered = coveredDocs.every((d) => d.covered);

    const isNextDisabled =
        submitMutation.isPending ||
        !allDocsCovered ||
        agreedDocs.filter((d) => d.isMandatory).some((d) => !d.agreed) ||
        !formData.userName ||
        !formData.accountNo;

    if (isUploading) {
        return (
            <div className="flex flex-col items-center justify-center py-20 bg-white border border-gray-200 rounded-3xl shadow-sm">
                <div className="relative mb-6">
                    <div className="w-16 h-16 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin" />
                </div>
                <h2 className="text-xl font-bold text-gray-900 mb-2">파일 전송 중...</h2>
                <p className="text-sm text-gray-500 text-center max-w-xs">
                    서류를 업로드하고 심사를 접수하고 있습니다.
                </p>
            </div>
        );
    }

    return (
        <div className="w-full">
            {fieldErrors.submit && (
                <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 max-w-4xl mx-auto animate-in fade-in slide-in-from-top-2">
                    <AlertCircle className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm font-bold">{fieldErrors.submit}</span>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">
                    <LoanCustomerSection
                        formData={formData}
                        onFormDataChange={setFormData}
                        rrnFront={rrnFront}
                        rrnBack={rrnBack}
                        onRrnFrontChange={(val) => {
                            setRrnFront(val);
                            setFieldErrors((p) => ({ ...p, rrn: undefined }));
                        }}
                        onRrnBackChange={(val) => {
                            setRrnBack(val);
                            setFieldErrors((p) => ({ ...p, rrn: undefined }));
                        }}
                        bankList={bankList}
                        fieldErrors={fieldErrors}
                    />

                    <LoanUploadSection
                        files={files}
                        onFileSelect={handleFileSelect}
                        onFileDelete={handleFileDelete}
                        fileInputRef={fileInputRef}
                    />

                    <LoanTermsSection
                        agreedDocs={agreedDocs}
                        onTermToggle={handleTermToggle}
                        onOpenModal={openModal}
                        viewedDocs={viewedDocs}
                        isLoading={isDocsLoading}
                    />
                </div>

                <div className="lg:col-span-1">
                    <LoanSideSummary
                        title="신청 현황 요약"
                        items={[
                            { label: '성명', value: formData.userName || '정보 미입력' },
                            { 
                                label: '입금 계좌', 
                                value: formData.bankCode && formData.accountNo ? (
                                    <div className="p-4 bg-emerald-50/50 rounded-2xl border border-emerald-100/50">
                                        <p className="font-black text-slate-900">{formData.bank}</p>
                                        <p className="text-[11px] text-emerald-600 font-bold font-mono mt-0.5">{formData.accountNo}</p>
                                    </div>
                                ) : '계좌 정보를 입력해 주세요'
                            },
                            {
                                label: '서류 준비도',
                                value: (
                                    <div className="flex items-center gap-2">
                                        <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                                            <div 
                                                className="h-full bg-emerald-500 transition-all duration-500" 
                                                style={{ width: `${(coveredDocs.filter(d => d.covered).length / coveredDocs.length) * 100}%` }}
                                            />
                                        </div>
                                        <span className="text-xs font-black text-slate-700">{coveredDocs.filter(d => d.covered).length}/{coveredDocs.length}</span>
                                    </div>
                                )
                            }
                        ]}
                        buttonText="심사 요청하기"
                        onButtonClick={() => { if (validate()) setIsConfirmOpen(true); }}
                        onBackClick={onBack}
                        isButtonDisabled={isNextDisabled}
                        isPending={submitMutation.isPending}
                        extraContent={
                            <div className="bg-slate-50 rounded-2xl p-4 space-y-3">
                                <p className="text-[10px] text-slate-500 font-medium leading-relaxed">
                                    심사 요청 시 NICE 신용정보 조회가 발생하며, 결과에 따라 한도가 산출됩니다.
                                </p>
                            </div>
                        }
                    />
                </div>
            </div>

            <LoanConfirmModal
                isOpen={isConfirmOpen}
                onClose={() => setIsConfirmOpen(false)}
                onConfirm={handleActualSubmit}
                title="최종 정보 확인"
                description="입력하신 내용이 정확한지 확인해 주세요."
                items={[
                    { label: '성명', value: formData.userName },
                    { label: '주민등록번호', value: <span className="font-mono tracking-widest">{rrnFront}-{rrnBack}●●●●●●</span> },
                    { 
                        label: '입금 계좌', 
                        value: (
                            <div className="text-right">
                                <p className="font-black text-slate-900">{formData.bank}</p>
                                <p className="text-sm font-bold text-emerald-600 font-mono">{formData.accountNo}</p>
                            </div>
                        )
                    }
                ]}
                extraChecklist={coveredDocs.map(d => ({ label: d.label, covered: d.covered }))}
            />

            <LoanTermsModal
                isOpen={isModalOpen}
                activeDoc={activeDoc}
                onClose={() => setIsModalOpen(false)}
                onAgree={handleModalAgree}
            />
        </div>
    );
};

export default LoanRequestForm;
