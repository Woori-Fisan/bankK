import React, { useState, useRef, useEffect } from 'react';
import {
    User, Building2, Upload, FileText, X, ChevronLeft, ChevronRight, ChevronDown,
    FileType, CheckCircle2, Loader2, AlertTriangle, XCircle, Info, ShieldCheck,
} from 'lucide-react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import type { LoanData } from '../../pages/LoanApplication';
import { useReviewDocuments, useSubmitLoanEvaluation, useBankList, extractApiError } from '../../hooks/useLoan';
import type { ReviewDocument, EvaluationStatusResponse } from '../../api/loanApi';
import { fetchLoanResult } from '../../api/loanApi';
import { useAuthStore } from '../../store/useAuthStore';
import { isValidAccountNumber } from '../../utils/validator';
import { prepareSecureRequest, decryptBankResponse, encryptFileWithKey } from '../../utils/bankCrypto';

interface AgreedDoc extends ReviewDocument {
    agreed: boolean;
}

const REQUIRED_DOCS = [
    { label: '신분증 사본', hint: '신분증.pdf / 면허증.pdf', keywords: ['신분증', '면허증'] },
    { label: '재직증명서', hint: '재직증명서.pdf', keywords: ['재직증명서'] },
    { label: '근로소득 원천징수영수증', hint: '원천징수.pdf', keywords: ['원천징수'] },
    { label: '건강보험료 납부확인서', hint: '건강보험.pdf', keywords: ['건강보험'] },
] as const;

interface LoanRequestFormProps {
    onNext: (data: LoanData, loanNo: string) => void;
    onBack: () => void;
    onSseMessage: (data: EvaluationStatusResponse) => void;
    onSseError: (error: Error) => void;
}

const SectionHeader: React.FC<{ step: number; icon: React.ReactNode; title: string }> = ({ step, icon, title }) => (
    <div className="flex items-center gap-3 mb-8 pb-5 border-b border-gray-100">
        <span className="w-9 h-9 bg-slate-900 text-white rounded-full flex items-center justify-center text-sm font-bold shrink-0">
            {step}
        </span>
        <span className="text-gray-400">{icon}</span>
        <h3 className="text-lg font-bold text-gray-900">{title}</h3>
    </div>
);

const LoanRequestForm: React.FC<LoanRequestFormProps> = ({ onNext, onBack, onSseMessage, onSseError }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const sseControllerRef = useRef<AbortController | null>(null);
    const aesKeyRef = useRef<CryptoKey | null>(null);
    const rrnBackRef = useRef<HTMLInputElement>(null);
    const iframeRef = useRef<HTMLIFrameElement>(null);
    const [rrnFront, setRrnFront] = useState('');
    const [rrnBack, setRrnBack] = useState('');
    const [formData, setFormData] = useState<LoanData>({
        userName: '',
        phone: '',
        bank: '',
        bankCode: '',
        accountNo: '',
        accountHolder: '',
    });
    const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof LoanData | 'submit', string>>>({});

    const [isUploading, setIsUploading] = useState(false);
    const [files, setFiles] = useState<{ id: number; name: string; file: File }[]>([]);
    const [agreedDocs, setAgreedDocs] = useState<AgreedDoc[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeDoc, setActiveDoc] = useState<AgreedDoc | null>(null);
    const [viewedDocs, setViewedDocs] = useState<Set<string>>(new Set());
    const [hasScrolledToBottom, setHasScrolledToBottom] = useState(false);
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);

    const { data: docsData, isLoading: isDocsLoading } = useReviewDocuments();
    const { data: bankList, isLoading: isBankListLoading } = useBankList();
    const submitMutation = useSubmitLoanEvaluation();

    React.useEffect(() => {
        if (docsData?.documents) {
            setAgreedDocs(docsData.documents.map((d) => ({ ...d, agreed: false })));
        }
    }, [docsData]);

    useEffect(() => {
        if (!isModalOpen) return;
        const handler = (e: MessageEvent) => {
            if (
                e.data === 'terms-scrolled-to-bottom' &&
                e.source === iframeRef.current?.contentWindow
            ) {
                setHasScrolledToBottom(true);
            }
        };
        window.addEventListener('message', handler);
        return () => window.removeEventListener('message', handler);
    }, [isModalOpen]);

    const handleTermToggle = (documentType: string) => {
        setAgreedDocs((prev) =>
            prev.map((d) => (d.documentType === documentType ? { ...d, agreed: !d.agreed } : d)),
        );
    };

    const handleAllAgreed = () => {
        const allViewed = agreedDocs.every((d) => viewedDocs.has(d.documentType));
        if (!allViewed) return;
        const allAgreed = agreedDocs.every((d) => d.agreed);
        setAgreedDocs((prev) => prev.map((d) => ({ ...d, agreed: !allAgreed })));
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
        setViewedDocs((prev) => new Set([...prev, doc.documentType]));
        setActiveDoc(doc);
        setHasScrolledToBottom(false);
        setIsModalOpen(true);
    };

    const buildTermsSrcDoc = (content: string | undefined): string => {
        const body = content ?? '<p style="padding:16px;font-family:sans-serif;color:#555">내용을 불러올 수 없습니다.</p>';
        const origin = window.location.origin;
        return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  html,body{margin:0;padding:20px 24px;font-family:-apple-system,BlinkMacSystemFont,'Malgun Gothic','맑은 고딕',sans-serif;font-size:17px;line-height:1.8;color:#222;}
  p,li,span,div,td,th,label{font-size:17px !important;line-height:1.8 !important;}
  h1{font-size:1.4em !important;} h2{font-size:1.25em !important;} h3{font-size:1.15em !important;}
  table{width:100%;border-collapse:collapse;font-size:17px !important;}
  td,th{padding:8px 10px;}
</style>
</head>
<body>
${body}
<script>
(function(){
  function check(){
    var scrolled=window.scrollY+window.innerHeight;
    var total=document.documentElement.scrollHeight;
    if(scrolled>=total-5){window.parent.postMessage('terms-scrolled-to-bottom','${origin}');}
  }
  window.addEventListener('scroll',check);
  window.addEventListener('load',check);
})();
</` + `script>
</body>
</html>`;
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

    useEffect(() => {
        return () => {};
    }, []);

    const handleSubmit = () => {
        if (!validate()) return;
        setIsConfirmOpen(true);
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

        // 1. 보안 요청 준비 (암호화 + 서명 + 키ID 통합 처리)
        const agreedAt = new Date().toISOString();
        const documents = agreedDocs
            .filter((d) => d.agreed)
            .map((d) => ({ documentType: d.documentType, agreedAt }));

        const secureRequest = await prepareSecureRequest(
            {
                customerName: formData.userName!,
                customerRrnPrefix: rrnPrefix,
                depositAccountNo: formData.accountNo!,
            },
            {
                requestKey,
                bankCode: formData.bankCode!,
                customerPhone: formData.phone ?? '',
                depositBankCode: formData.bankCode!,
                documents,
            },
            formData.bankCode!
        );

        if (!secureRequest) {
            setIsUploading(false);
            setFieldErrors({ submit: '보안 요청 준비 중 오류가 발생했습니다.' });
            return;
        }

        const { payload, headers, aesKey } = secureRequest;
        aesKeyRef.current = aesKey;

        // SSE 실패 시 Redis polling으로 결과를 복구하는 fallback (3초 간격, 최대 5회)
        const pollForResult = async (key: string, cryptoKey: CryptoKey) => {
            for (let i = 0; i < 5; i++) {
                await new Promise((r) => setTimeout(r, 3000));
                try {
                    const result = await fetchLoanResult(key);
                    if (result === null) continue;
                    if (result.resPayload) {
                        const decrypted = await decryptBankResponse(result.resPayload, cryptoKey);
                        if (decrypted.availableProducts) {
                            result.availableProducts = decrypted.availableProducts.map((p: any) => ({
                                loanProductCode: String(p.productId),
                                loanProductName: p.productName,
                                minAmount: p.minLimit,
                                maxAmount: p.maxLimit,
                                interestRate: p.minRate,
                                loanPeriodMonths: 36,
                            }));
                        }
                        result.approvedLimit = decrypted.approvedLimit;
                        result.rejectionMessage = decrypted.rejectReason;
                    }
                    onSseMessage(result);
                    return;
                } catch (_) { /* 개별 polling 실패는 무시하고 재시도 */ }
            }
            onSseError(new Error('심사 결과를 받지 못했습니다. 잠시 후 다시 확인하거나 처음부터 다시 신청해주세요.'));
        };

        fetchEventSource(`/api/v1/loan/subscribe?requestKey=${encodeURIComponent(requestKey)}`, {
            headers: { Authorization: `Bearer ${accessToken}` },
            signal: controller.signal,
            async onmessage(event) {
                if (event.event === 'heartbeat') return;
                if (event.event === 'timeout') {
                    controller.abort();
                    if (aesKeyRef.current) pollForResult(requestKey, aesKeyRef.current);
                    else onSseError(new Error('심사 결과를 받지 못했습니다. 처음부터 다시 신청해주세요.'));
                    return;
                }
                if (event.event !== 'result') return;
                try {
                    const parsed: EvaluationStatusResponse = JSON.parse(event.data);

                    // 2. 응답 복호화 (메모리에 보관 중이던 aesKey 사용)
                    if (parsed.resPayload) {
                        const decrypted = await decryptBankResponse(parsed.resPayload, aesKey);
                        if (decrypted.availableProducts) {
                            parsed.availableProducts = decrypted.availableProducts.map((p: any) => ({
                                loanProductCode: String(p.productId),
                                loanProductName: p.productName,
                                minAmount: p.minLimit,
                                maxAmount: p.maxLimit,
                                interestRate: p.minRate,
                                loanPeriodMonths: 36,
                            }));
                        }
                        parsed.approvedLimit = decrypted.approvedLimit;
                        parsed.rejectionMessage = decrypted.rejectReason;
                    }

                    onSseMessage(parsed);
                    controller.abort();
                } catch (e) {
                    console.error('SSE decryption error:', e);
                    onSseError(new Error('심사 결과 해독 중 오류가 발생했습니다.'));
                    controller.abort();
                }
            },
            onerror(err) {
                controller.abort();
                if (aesKeyRef.current) pollForResult(requestKey, aesKeyRef.current);
                else onSseError(new Error('심사 결과 조회 중 연결 오류가 발생했습니다.'));
                throw err;
            },
        });

        try {
            // 3. 파일 암호화 루프 (JSON 암호화에 사용된 동일 AES 키 재사용)
            const encryptedFiles = await Promise.all(
                files.map(async (f) => {
                    const encryptedBlob = await encryptFileWithKey(f.file, aesKey);
                    // 원본 파일명 유지 (은행이 파일명으로 서류 종류를 식별함)
                    return new File([encryptedBlob], f.name, { type: 'application/octet-stream' });
                })
            );

            const result = await submitMutation.mutateAsync({
                payload,
                files: encryptedFiles,
                headers,
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
            <div className="flex flex-col items-center justify-center py-20 bg-white border border-gray-200 rounded-3xl">
                <div className="relative mb-6">
                    <Loader2 className="w-16 h-16 text-blue-500 animate-spin" />
                    <div className="absolute inset-0 flex items-center justify-center">
                        <div className="w-8 h-8 bg-blue-50 rounded-full" />
                    </div>
                </div>
                <h2 className="text-xl font-bold text-gray-900 mb-2">파일 전송 중...</h2>
                <p className="text-sm text-gray-500 text-center max-w-xs">
                    서류를 업로드하고 심사를 접수하고 있습니다.
                </p>
                <div className="flex gap-1.5 mt-6">
                    <div className="w-1.5 h-1.5 bg-blue-500 rounded-full animate-pulse" />
                    <div className="w-1.5 h-1.5 bg-blue-500 rounded-full animate-pulse delay-75" />
                    <div className="w-1.5 h-1.5 bg-blue-500 rounded-full animate-pulse delay-150" />
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">

            {/* ── Section 1: 고객 정보 + 입금 계좌 ── */}
            <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-sm">
                <SectionHeader step={1} icon={<User className="w-5 h-5" />} title="고객 정보 및 입금 계좌" />

                <div className="grid grid-cols-2 gap-x-8 gap-y-6">
                    {/* 성명 */}
                    <div className="col-span-1">
                        <label htmlFor="userName" className="block text-sm font-bold text-gray-500 mb-2">
                            성명
                        </label>
                        <input
                            id="userName"
                            type="text"
                            className="w-full px-4 py-4 bg-gray-50 border border-gray-200 rounded-xl text-base focus:ring-2 focus:ring-blue-500 outline-none"
                            placeholder="예) 홍길동"
                            value={formData.userName}
                            onChange={(e) => setFormData({ ...formData, userName: e.target.value })}
                            onBlur={() => {
                                if (!formData.userName?.trim())
                                    setFieldErrors((p) => ({ ...p, userName: '성명을 입력해주세요.' }));
                                else setFieldErrors((p) => ({ ...p, userName: undefined }));
                            }}
                        />
                        {fieldErrors.userName && (
                            <p className="mt-1.5 text-sm text-red-500">{fieldErrors.userName}</p>
                        )}
                    </div>

                    {/* 연락처 */}
                    <div className="col-span-1">
                        <label htmlFor="phone" className="block text-sm font-bold text-gray-500 mb-2">
                            연락처
                        </label>
                        <input
                            id="phone"
                            type="text"
                            className="w-full px-4 py-4 bg-gray-50 border border-gray-200 rounded-xl text-base focus:ring-2 focus:ring-blue-500 outline-none"
                            placeholder="010-1234-5678"
                            value={formData.phone}
                            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                        />
                    </div>

                    {/* 주민등록번호 — 전체 너비 */}
                    <div className="col-span-2">
                        <label className="block text-sm font-bold text-gray-500 mb-2">
                            주민등록번호
                        </label>
                        <div className="flex items-center gap-2">
                            <input
                                type="text"
                                inputMode="numeric"
                                maxLength={6}
                                placeholder="000000"
                                value={rrnFront}
                                onChange={(e) => {
                                    const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                                    setRrnFront(val);
                                    if (val.length === 6) rrnBackRef.current?.focus();
                                }}
                                onBlur={() => {
                                    if (rrnFront.length > 0 && rrnFront.length < 6)
                                        setFieldErrors((p) => ({ ...p, rrn: '앞 6자리를 모두 입력해주세요.' }));
                                    else setFieldErrors((p) => ({ ...p, rrn: undefined }));
                                }}
                                className="w-36 px-4 py-4 bg-gray-50 border border-gray-200 rounded-xl text-base focus:ring-2 focus:ring-blue-500 outline-none text-center tracking-widest"
                            />
                            <span className="text-gray-400 font-bold text-lg">-</span>
                            <input
                                ref={rrnBackRef}
                                type="text"
                                inputMode="numeric"
                                maxLength={1}
                                value={rrnBack}
                                onChange={(e) => {
                                    const val = e.target.value.replace(/[^1-4]/g, '').slice(0, 1);
                                    setRrnBack(val);
                                }}
                                onBlur={() => {
                                    if (rrnBack.length > 0 && !/^[1-4]$/.test(rrnBack))
                                        setFieldErrors((p) => ({ ...p, rrn: '뒷자리는 1~4 사이 숫자입니다.' }));
                                    else setFieldErrors((p) => ({ ...p, rrn: undefined }));
                                }}
                                className="w-10 px-2 py-4 bg-gray-50 border border-gray-200 rounded-xl text-base focus:ring-2 focus:ring-blue-500 outline-none text-center"
                            />
                            <span className="text-gray-300 text-xl tracking-widest select-none">●●●●●●</span>
                        </div>
                        {fieldErrors.rrn && (
                            <p className="mt-1.5 text-sm text-red-500">{fieldErrors.rrn}</p>
                        )}
                    </div>

                    {/* 구분선 */}
                    <div className="col-span-2 border-t border-dashed border-gray-100 pt-2">
                        <div className="flex items-center gap-2 mb-1">
                            <Building2 className="w-4 h-4 text-gray-400" />
                            <p className="text-sm font-bold text-gray-500">대출금 입금 계좌</p>
                        </div>
                    </div>

                    {/* 은행 선택 */}
                    <div className="col-span-1">
                        <label htmlFor="bank" className="block text-sm font-bold text-gray-500 mb-2">
                            은행 선택
                        </label>
                        <div className="relative">
                            <select
                                id="bank"
                                className="w-full px-4 py-4 pr-10 bg-gray-50 border border-gray-200 rounded-xl text-base outline-none appearance-none cursor-pointer"
                                value={formData.bankCode}
                                disabled={isBankListLoading}
                                onChange={(e) => {
                                    const selected = bankList?.find((b) => b.bankCode === e.target.value);
                                    setFormData({
                                        ...formData,
                                        bank: selected?.bankName ?? '',
                                        bankCode: e.target.value,
                                    });
                                }}
                            >
                                <option value="">
                                    {isBankListLoading ? '불러오는 중...' : '은행을 선택하세요'}
                                </option>
                                {bankList?.map((b) => (
                                    <option key={b.bankCode} value={b.bankCode}>
                                        {b.bankName}
                                    </option>
                                ))}
                            </select>
                            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
                        </div>
                        {fieldErrors.bank && (
                            <p className="mt-1.5 text-sm text-red-500">{fieldErrors.bank}</p>
                        )}
                    </div>

                    {/* 예금주 */}
                    <div className="col-span-1">
                        <label htmlFor="accountHolder" className="block text-sm font-bold text-gray-500 mb-2">
                            예금주
                        </label>
                        <input
                            id="accountHolder"
                            type="text"
                            className="w-full px-4 py-4 bg-gray-50 border border-gray-200 rounded-xl text-base outline-none"
                            value={formData.accountHolder}
                            onChange={(e) =>
                                setFormData({ ...formData, accountHolder: e.target.value })
                            }
                        />
                    </div>

                    {/* 계좌번호 — 전체 너비 */}
                    <div className="col-span-2">
                        <label htmlFor="accountNo" className="block text-sm font-bold text-gray-500 mb-2">
                            계좌번호
                        </label>
                        <input
                            id="accountNo"
                            type="text"
                            className="w-full px-4 py-4 bg-gray-50 border border-gray-200 rounded-xl text-base outline-none"
                            placeholder="숫자만 입력 (10~14자리)"
                            value={formData.accountNo}
                            onChange={(e) =>
                                setFormData({ ...formData, accountNo: e.target.value.replace(/[^0-9]/g, '') })
                            }
                            onBlur={() => {
                                if (!formData.accountNo?.trim())
                                    setFieldErrors((p) => ({ ...p, accountNo: '계좌번호를 입력해주세요.' }));
                                else if (!isValidAccountNumber(formData.accountNo))
                                    setFieldErrors((p) => ({
                                        ...p,
                                        accountNo: '올바른 계좌번호 형식을 입력해주세요. (10~14자리 숫자)',
                                    }));
                                else setFieldErrors((p) => ({ ...p, accountNo: undefined }));
                            }}
                        />
                        <p className="mt-1.5 text-sm text-gray-400">
                            * '-' 없이 숫자만 입력해 주세요.
                        </p>
                        {fieldErrors.accountNo && (
                            <p className="mt-1 text-xs text-red-500">{fieldErrors.accountNo}</p>
                        )}
                    </div>
                </div>
            </div>

            {/* ── Section 2: 서류 업로드 ── */}
            <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-sm">
                <SectionHeader step={2} icon={<Upload className="w-5 h-5" />} title="서류 업로드" />

                {/* 필수 서류 체크리스트 */}
                <div className="grid grid-cols-2 gap-3 mb-6">
                    {coveredDocs.map((doc) => (
                        <div
                            key={doc.label}
                            className={`flex items-center gap-3 px-4 py-3 rounded-xl border text-sm ${
                                doc.covered
                                    ? 'bg-emerald-50 border-emerald-200 text-emerald-700'
                                    : 'bg-gray-50 border-gray-100 text-gray-500'
                            }`}
                        >
                            <CheckCircle2
                                className={`w-4 h-4 flex-shrink-0 ${
                                    doc.covered ? 'text-emerald-500' : 'text-gray-300'
                                }`}
                            />
                            <div className="min-w-0">
                                <p className="font-medium text-sm">{doc.label}</p>
                                {!doc.covered && (
                                    <p className="text-[10px] text-gray-400 mt-0.5">{doc.hint}</p>
                                )}
                            </div>
                        </div>
                    ))}
                </div>

                <p className="text-sm text-gray-400 mb-4">
                    파일명에 위 키워드가 포함되어야 인식됩니다. (PDF, 최대 10MB)
                </p>

                <input
                    type="file"
                    ref={fileInputRef}
                    className="hidden"
                    multiple
                    accept=".pdf"
                    onChange={handleFileSelect}
                />

                {/* 드래그 업로드 영역 */}
                <div
                    onClick={() => fileInputRef.current?.click()}
                    className="border-2 border-dashed border-gray-200 rounded-2xl p-12 text-center hover:border-blue-400 hover:bg-blue-50/30 transition-all cursor-pointer group"
                >
                    <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4 group-hover:bg-blue-100 transition-colors">
                        <Upload className="w-8 h-8 text-gray-400 group-hover:text-blue-500 transition-colors" />
                    </div>
                    <p className="text-base font-medium text-gray-600 mb-1">클릭하거나 파일을 드래그하세요</p>
                    <p className="text-sm text-gray-400">PDF 파일만 업로드 가능합니다</p>
                </div>

                {/* 업로드된 파일 목록 */}
                {files.length > 0 && (
                    <div className="mt-6 space-y-2">
                        <p className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-3">
                            업로드된 파일 ({files.length}개)
                        </p>
                        {files.map((f) => (
                            <div
                                key={f.id}
                                className="px-4 py-3 bg-gray-50 border border-gray-100 rounded-xl flex items-center gap-3"
                            >
                                <div className="w-9 h-9 bg-white border border-gray-200 rounded-lg flex items-center justify-center shrink-0">
                                    <FileType className="w-4 h-4 text-red-500" />
                                </div>
                                <p className="flex-1 text-base font-medium text-gray-900 truncate">{f.name}</p>
                                <button
                                    type="button"
                                    onClick={() => handleFileDelete(f.id)}
                                    className="text-gray-300 hover:text-red-500 transition-colors shrink-0"
                                >
                                    <X className="w-4 h-4" />
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* ── 심사 안내 ── */}
            <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-sm">
                <div className="flex items-center gap-2 mb-5">
                    <Info className="w-5 h-5 text-gray-400" />
                    <h3 className="text-lg font-bold text-gray-900">심사 안내</h3>
                </div>
                <ul className="space-y-3 text-base text-gray-600 leading-relaxed mb-5">
                    <li className="flex gap-2">
                        <span className="text-gray-400 shrink-0">•</span>
                        <span>심사는 서류 제출 후 <strong className="text-gray-900">수 초 ~ 수십 초</strong> 내에 완료됩니다.</span>
                    </li>
                    <li className="flex gap-2">
                        <span className="text-gray-400 shrink-0">•</span>
                        <span>신용점수 600점 미만 시 대출이 거절될 수 있습니다.</span>
                    </li>
                    <li className="flex gap-2">
                        <span className="text-gray-400 shrink-0">•</span>
                        <span>DSR 40% 초과 시 대출이 제한될 수 있습니다.</span>
                    </li>
                    <li className="flex gap-2">
                        <span className="text-gray-400 shrink-0">•</span>
                        <span>법정 최고금리 연 20% 초과 상품은 취급하지 않습니다.</span>
                    </li>
                    <li className="flex gap-2">
                        <span className="text-gray-400 shrink-0">•</span>
                        <span>본 심사는 고객의 신용점수에 영향을 줄 수 있습니다.</span>
                    </li>
                </ul>
                <div className="p-4 bg-emerald-50 rounded-xl">
                    <div className="flex items-center gap-2 text-emerald-700 font-bold text-sm mb-1.5">
                        <ShieldCheck className="w-5 h-5" />
                        NICE 신용점수 조회 동의 필요
                    </div>
                    <p className="text-sm text-emerald-600">다음 단계에서 신용정보조회 동의서에 서명이 필요합니다.</p>
                </div>
            </div>

            {/* ── Section 3: 약관 동의 ── */}
            <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-sm">
                <SectionHeader step={3} icon={<FileText className="w-5 h-5" />} title="약관 동의" />

                <p className="text-base text-gray-500 mb-6">
                    각 항목의 <strong>내용 보기</strong>를 눌러 약관을 확인한 후 동의해주세요.
                    필수 항목에 모두 동의해야 심사 요청이 가능합니다.
                </p>

                {isDocsLoading ? (
                    <div className="flex items-center justify-center py-12">
                        <Loader2 className="w-6 h-6 text-emerald-500 animate-spin" />
                    </div>
                ) : (
                    <>
                        {/* 전체 동의 버튼 */}
                        <button
                            type="button"
                            onClick={handleAllAgreed}
                            disabled={!agreedDocs.every((d) => viewedDocs.has(d.documentType))}
                            className={`w-full py-4 mb-2 rounded-xl text-base font-bold flex items-center justify-center gap-2 transition-colors ${
                                agreedDocs.every((d) => viewedDocs.has(d.documentType))
                                    ? 'bg-slate-900 text-white hover:bg-slate-800'
                                    : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                            }`}
                        >
                            <CheckCircle2 className="w-4 h-4" />
                            전체 약관에 동의합니다
                        </button>
                        {agreedDocs.some((d) => !viewedDocs.has(d.documentType)) && (
                            <p className="text-xs text-amber-600 text-center mb-4">
                                모든 약관의 내용 보기를 먼저 확인해주세요.
                            </p>
                        )}

                        <div className="mt-4 space-y-3">
                            {agreedDocs.map((doc) => (
                                <div
                                    key={doc.documentType}
                                    className="flex items-center gap-4 px-5 py-4 bg-gray-50 rounded-xl border border-gray-100 hover:border-blue-200 transition-colors"
                                >
                                    <input
                                        type="checkbox"
                                        id={`doc-${doc.documentType}`}
                                        checked={doc.agreed}
                                        onChange={() => handleTermToggle(doc.documentType)}
                                        disabled={!viewedDocs.has(doc.documentType)}
                                        className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 disabled:opacity-40 disabled:cursor-not-allowed"
                                    />
                                    <label
                                        htmlFor={`doc-${doc.documentType}`}
                                        className={`flex-1 text-base font-medium text-gray-900 ${
                                            viewedDocs.has(doc.documentType)
                                                ? 'cursor-pointer'
                                                : 'cursor-not-allowed opacity-60'
                                        }`}
                                    >
                                        {doc.documentName}
                                    </label>
                                    <span
                                        className={`px-2 py-0.5 rounded text-[10px] font-bold shrink-0 ${
                                            doc.isMandatory
                                                ? 'bg-red-50 text-red-600'
                                                : 'bg-blue-50 text-blue-600'
                                        }`}
                                    >
                                        {doc.isMandatory ? '필수' : '선택'}
                                    </span>
                                    <button
                                        type="button"
                                        onClick={() => openModal(doc)}
                                        className="shrink-0 px-4 py-2.5 bg-white border border-gray-200 text-blue-600 text-sm font-bold rounded-lg hover:bg-blue-50 transition-colors"
                                    >
                                        내용 보기
                                    </button>
                                </div>
                            ))}
                        </div>
                    </>
                )}
            </div>

            {/* 에러 메시지 */}
            {fieldErrors.submit && (
                <div className="px-5 py-4 bg-red-50 border border-red-200 rounded-xl">
                    <p className="text-sm text-red-600 text-center">{fieldErrors.submit}</p>
                </div>
            )}

            {/* 네비게이션 */}
            <div className="flex justify-between pt-4 pb-8">
                <button
                    type="button"
                    onClick={onBack}
                    className="flex items-center gap-2 px-6 py-4 bg-gray-100 text-gray-600 rounded-2xl font-black text-base hover:bg-gray-200 active:scale-[0.98] transition-all"
                >
                    <ChevronLeft className="w-4 h-4" />
                    이전으로
                </button>
                <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={isNextDisabled}
                    className={`flex items-center gap-2 px-10 py-4 rounded-2xl font-black text-base transition-all shadow-md active:scale-[0.98] ${
                        isNextDisabled
                            ? 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                            : 'bg-slate-900 text-white hover:bg-slate-800'
                    }`}
                >
                    {submitMutation.isPending ? (
                        <>
                            <Loader2 className="w-4 h-4 animate-spin" />
                            심사 접수 중...
                        </>
                    ) : (
                        <>
                            심사 요청하기
                            <ChevronRight className="w-4 h-4" />
                        </>
                    )}
                </button>
            </div>

            {/* 심사 요청 전 최종 확인 모달 */}
            {isConfirmOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden">
                        {/* 경고 헤더 */}
                        <div className="bg-amber-500 px-8 py-6 flex items-center gap-4">
                            <AlertTriangle className="w-8 h-8 text-white shrink-0" />
                            <div>
                                <h3 className="text-xl font-black text-white">심사 요청 전 최종 확인</h3>
                                <p className="text-sm text-amber-100 mt-1">
                                    아래 내용이 정확한지 확인해주세요. 심사 요청 후에는 수정이 불가합니다.
                                </p>
                            </div>
                        </div>

                        <div className="p-8 space-y-6">
                            {/* 고객 정보 */}
                            <div>
                                <p className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-3">고객 정보</p>
                                <div className="bg-gray-50 rounded-xl p-5 space-y-3">
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm text-gray-500">성명</span>
                                        <span className="text-base font-bold text-gray-900">{formData.userName}</span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm text-gray-500">주민등록번호</span>
                                        <span className="text-base font-bold text-gray-900 tracking-widest">
                                            {rrnFront}-{rrnBack}●●●●●●
                                        </span>
                                    </div>
                                    {formData.phone && (
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-500">연락처</span>
                                            <span className="text-base font-bold text-gray-900">{formData.phone}</span>
                                        </div>
                                    )}
                                    <div className="flex justify-between items-center pt-3 border-t border-gray-200">
                                        <span className="text-sm text-gray-500">입금 계좌</span>
                                        <span className="text-base font-bold text-gray-900">
                                            {formData.bank} {formData.accountNo}
                                        </span>
                                    </div>
                                </div>
                            </div>

                            {/* 첨부 서류 확인 */}
                            <div>
                                <p className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-3">
                                    필수 서류 확인 (4종)
                                </p>
                                <div className="space-y-2">
                                    {coveredDocs.map((doc) => {
                                        const matchedFile = files.find((f) =>
                                            doc.keywords.some((kw) => f.name.toLowerCase().includes(kw))
                                        );
                                        return (
                                            <div
                                                key={doc.label}
                                                className={`flex items-center gap-3 px-4 py-3.5 rounded-xl border ${
                                                    doc.covered
                                                        ? 'bg-emerald-50 border-emerald-200'
                                                        : 'bg-red-50 border-red-200'
                                                }`}
                                            >
                                                {doc.covered ? (
                                                    <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0" />
                                                ) : (
                                                    <XCircle className="w-5 h-5 text-red-400 shrink-0" />
                                                )}
                                                <div className="flex-1 min-w-0">
                                                    <p className={`text-sm font-bold ${doc.covered ? 'text-emerald-800' : 'text-red-700'}`}>
                                                        {doc.label}
                                                    </p>
                                                    {matchedFile && (
                                                        <p className="text-xs text-emerald-600 mt-0.5 truncate">
                                                            {matchedFile.name}
                                                        </p>
                                                    )}
                                                </div>
                                                <span className={`text-xs font-bold shrink-0 ${doc.covered ? 'text-emerald-600' : 'text-red-500'}`}>
                                                    {doc.covered ? '확인' : '미첨부'}
                                                </span>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>

                        {/* 버튼 */}
                        <div className="px-8 pb-8 grid grid-cols-2 gap-3">
                            <button
                                type="button"
                                onClick={() => setIsConfirmOpen(false)}
                                className="py-4 bg-gray-100 text-gray-600 rounded-2xl font-black text-base hover:bg-gray-200 active:scale-[0.98] transition-all"
                            >
                                취소 (수정하기)
                            </button>
                            <button
                                type="button"
                                onClick={handleActualSubmit}
                                className="py-4 bg-slate-900 text-white rounded-2xl font-black text-base hover:bg-slate-800 active:scale-[0.98] transition-all shadow-lg"
                            >
                                확인 후 심사 요청
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* 약관 모달 */}
            {isModalOpen && activeDoc && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden">
                        <div className="bg-slate-900 text-white px-8 py-6 flex items-center justify-between">
                            <div className="flex items-center gap-3">
                                <FileText className="w-6 h-6 text-slate-400" />
                                <h3 className="text-base font-bold">{activeDoc.documentName}</h3>
                            </div>
                            <button
                                type="button"
                                onClick={() => setIsModalOpen(false)}
                                className="text-slate-400 hover:text-white transition-colors"
                            >
                                <X className="w-7 h-7" />
                            </button>
                        </div>
                        <iframe
                            ref={iframeRef}
                            srcDoc={buildTermsSrcDoc(activeDoc.documentContent)}
                            className="w-full h-[600px] border-0 bg-white"
                            sandbox="allow-scripts"
                            title={activeDoc.documentName}
                        />
                        <div className="px-8 pt-3 pb-2 border-t border-gray-100">
                            {!hasScrolledToBottom && (
                                <p className="text-sm text-amber-600 text-center font-medium">
                                    약관을 끝까지 스크롤해야 동의할 수 있습니다.
                                </p>
                            )}
                        </div>
                        <div className="px-8 pb-6 flex justify-end gap-3">
                            <button
                                type="button"
                                onClick={() => setIsModalOpen(false)}
                                className="px-6 py-3 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-base"
                            >
                                닫기
                            </button>
                            <button
                                type="button"
                                onClick={handleModalAgree}
                                disabled={!hasScrolledToBottom}
                                className={`px-8 py-3 rounded-xl font-bold text-base transition-colors ${
                                    hasScrolledToBottom
                                        ? 'bg-slate-900 text-white hover:bg-slate-800'
                                        : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                                }`}
                            >
                                동의하고 닫기
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default LoanRequestForm;
