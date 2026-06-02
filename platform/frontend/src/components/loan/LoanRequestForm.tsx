import React, { useState, useRef, useEffect } from 'react';
import {
    User, Building2, Upload, FileText, X, ChevronRight, ChevronDown,
    FileType, CheckCircle2, Loader2, AlertTriangle, Info, ShieldCheck,
    AlertCircle,
    Search,
} from 'lucide-react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import type { LoanData } from '../../pages/LoanApplication';
import { useReviewDocuments, useSubmitLoanEvaluation, useBankList, extractApiError } from '../../hooks/useLoan';
import type { ReviewDocument, EvaluationStatusResponse } from '../../api/loanApi';
import { useAuthStore } from '../../store/useAuthStore';
import { isValidAccountNumber } from '../../utils/validator';
import RrnInput from '../common/RrnInput';
import { Button } from '../common/Button';
import Card from '../common/Card';

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
    const iframeRef = useRef<HTMLIFrameElement>(null);
    const [rrnFront, setRrnFront] = useState('');
    const [rrnBack, setRrnBack] = useState('');
    const [formData, setFormData] = useState<LoanData>({
        userName: '',
        phone: '',
        bank: '',
        bankCode: '',
        accountNo: '',
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
        // srcDoc으로 생성된 iframe은 origin 검사가 까다로울 수 있으므로
        // 정확한 메시지 데이터(terms-scrolled-to-bottom)인지만 확인합니다.
        if (e.data === 'terms-scrolled-to-bottom') {
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
        setHasScrolledToBottom(false);
        setActiveDoc(doc);
        setIsModalOpen(true);
        setViewedDocs((prev) => new Set([...prev, doc.documentType]));
    };

    const buildTermsSrcDoc = (content: string | undefined): string => {
        const body = content ?? '<p style="padding:16px;font-family:sans-serif;color:#555">내용을 불러올 수 없습니다.</p>';
        const origin = window.location.origin;
        return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  html,body{margin:0;padding:24px;font-family:-apple-system,BlinkMacSystemFont,'Malgun Gothic','맑은 고딕',sans-serif;font-size:17px;line-height:1.8;color:#222;overflow-x:hidden;}
  p,li,span,div,td,th,label{font-size:17px !important;line-height:1.8 !important;}
  h1{font-size:1.4em !important;margin-top:0;} h2{font-size:1.25em !important;} h3{font-size:1.15em !important;}
  table{width:100%;border-collapse:collapse;font-size:17px !important;margin:16px 0;}
  td,th{padding:10px;border:1px solid #eee;}
</style>
</head>
<body>
<div id="content-wrapper">${body}</div>
<script>
(function(){
  var sent = false;
  function check(){
    if(sent) return;
    var wrapper = document.getElementById('content-wrapper');
    if(!wrapper) return;
    
    var scrollPos = Math.round(window.scrollY + window.innerHeight);
    var totalHeight = Math.max(
      document.documentElement.scrollHeight,
      document.body.scrollHeight,
      wrapper.offsetHeight
    );
    
    // 하단 도달 감지 (오차 범위 25px 허용하여 배율 대응)
    if(scrollPos >= totalHeight - 25){
      sent = true;
      window.parent.postMessage('terms-scrolled-to-bottom','${origin}');
    }
  }
  window.addEventListener('scroll', check);
  window.addEventListener('load', function(){
    setTimeout(check, 100); // 렌더링 완료 후 체크
  });
  // 창 크기 조절 대응
  window.addEventListener('resize', check);
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
        <div className="w-full">
            {fieldErrors.submit && (
                <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 max-w-4xl mx-auto animate-in fade-in slide-in-from-top-2">
                    <AlertCircle className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm font-bold">{fieldErrors.submit}</span>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* 1. 입력 영역 (2컬럼) */}
                <div className="lg:col-span-2 space-y-8">
                    
                    {/* 섹션 1: 고객 정보 및 계좌 */}
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <SectionHeader step={1} icon={<User className="w-5 h-5" />} title="고객 정보 및 입금 계좌" />

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-8">
                            {/* 성명 */}
                            <div className="col-span-1">
                                <label htmlFor="userName" className="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">
                                    성명
                                </label>
                                <input
                                    id="userName"
                                    type="text"
                                    className="w-full px-5 py-4 bg-slate-50/50 border border-slate-100 rounded-2xl text-base font-bold focus:ring-2 focus:ring-slate-200 outline-none transition-all"
                                    placeholder="예) 홍길동"
                                    value={formData.userName}
                                    onChange={(e) => setFormData({ ...formData, userName: e.target.value })}
                                />
                                {fieldErrors.userName && (
                                    <p className="mt-2 text-xs text-rose-500 font-bold">{fieldErrors.userName}</p>
                                )}
                            </div>

                            {/* 연락처 */}
                            <div className="col-span-1">
                                <label htmlFor="phone" className="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">
                                    연락처
                                </label>
                                <input
                                    id="phone"
                                    type="text"
                                    className="w-full px-5 py-4 bg-slate-50/50 border border-slate-100 rounded-2xl text-base font-bold focus:ring-2 focus:ring-slate-200 outline-none transition-all"
                                    placeholder="010-1234-5678"
                                    value={formData.phone}
                                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                />
                            </div>

                            {/* 주민등록번호 */}
                            <div className="col-span-2">
                                <RrnInput
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
                                    error={fieldErrors.rrn}
                                    label="주민등록번호"
                                />
                            </div>

                            {/* 구분선 */}
                            <div className="col-span-2 pt-4 border-t border-slate-50">
                                <div className="flex items-center gap-2 mb-6">
                                    <Building2 className="w-4 h-4 text-emerald-500" />
                                    <p className="text-sm font-black text-slate-700">대출금 입금 계좌</p>
                                </div>
                                
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                    <div className="col-span-1">
                                        <label htmlFor="bank" className="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">
                                            은행 선택
                                        </label>
                                        <div className="relative">
                                            <select
                                                id="bank"
                                                className="w-full px-5 py-4 pr-12 bg-slate-50/50 border border-slate-100 rounded-2xl text-base font-bold outline-none appearance-none cursor-pointer focus:ring-2 focus:ring-slate-200 transition-all"
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
                                                    {isBankListLoading ? '불러오는 중...' : '은행 선택'}
                                                </option>
                                                {bankList?.map((b) => (
                                                    <option key={b.bankCode} value={b.bankCode}>
                                                        {b.bankName}
                                                    </option>
                                                ))}
                                            </select>
                                            <ChevronDown className="absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 pointer-events-none" />
                                        </div>
                                    </div>

                                    <div className="col-span-2">
                                        <label htmlFor="accountNo" className="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">
                                            계좌번호
                                        </label>
                                        <input
                                            id="accountNo"
                                            type="text"
                                            className="w-full px-5 py-4 bg-slate-50/50 border border-slate-100 rounded-2xl text-base font-bold outline-none focus:ring-2 focus:ring-slate-200 transition-all font-mono"
                                            placeholder="숫자만 입력 (10~14자리)"
                                            value={formData.accountNo}
                                            onChange={(e) =>
                                                setFormData({ ...formData, accountNo: e.target.value.replace(/[^0-9]/g, '') })
                                            }
                                        />
                                        {fieldErrors.accountNo && (
                                            <p className="mt-2 text-xs text-rose-500 font-bold">{fieldErrors.accountNo}</p>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </Card>

                    {/* 섹션 2: 서류 업로드 */}
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <SectionHeader step={2} icon={<Upload className="w-5 h-5" />} title="서류 업로드" />

                        {/* 필수 서류 체크리스트 */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
                            {coveredDocs.map((doc) => (
                                <div
                                    key={doc.label}
                                    className={`flex items-center gap-4 px-5 py-4 rounded-2xl border transition-all ${
                                        doc.covered
                                            ? 'bg-emerald-50 border-emerald-100 text-emerald-700'
                                            : 'bg-slate-50/50 border-slate-100 text-slate-400'
                                    }`}
                                >
                                    <CheckCircle2
                                        className={`w-5 h-5 flex-shrink-0 ${
                                            doc.covered ? 'text-emerald-500' : 'text-slate-200'
                                        }`}
                                    />
                                    <div className="min-w-0">
                                        <p className="font-bold text-sm">{doc.label}</p>
                                        {!doc.covered && (
                                            <p className="text-[10px] opacity-60 mt-0.5">{doc.hint}</p>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>

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
                            className="border-2 border-dashed border-slate-200 rounded-3xl p-12 text-center hover:border-emerald-400 hover:bg-emerald-50/30 transition-all cursor-pointer group"
                        >
                            <div className="w-16 h-16 bg-slate-50 rounded-2xl flex items-center justify-center mx-auto mb-4 group-hover:bg-emerald-100 transition-colors">
                                <Upload className="w-8 h-8 text-slate-300 group-hover:text-emerald-500 transition-colors" />
                            </div>
                            <p className="text-base font-black text-slate-700 mb-1">클릭하거나 파일을 드래그하세요</p>
                            <p className="text-xs text-slate-400 font-medium">PDF 파일 전용 (최대 10MB)</p>
                        </div>

                        {/* 업로드된 파일 목록 */}
                        {files.length > 0 && (
                            <div className="mt-8 space-y-3">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-4">
                                    업로드된 파일 ({files.length}개)
                                </p>
                                {files.map((f) => (
                                    <div
                                        key={f.id}
                                        className="px-5 py-4 bg-white border border-slate-100 rounded-2xl flex items-center gap-4 animate-in fade-in slide-in-from-left-2"
                                    >
                                        <div className="w-10 h-10 bg-rose-50 rounded-xl flex items-center justify-center shrink-0">
                                            <FileType className="w-5 h-5 text-rose-500" />
                                        </div>
                                        <p className="flex-1 text-sm font-bold text-slate-700 truncate">{f.name}</p>
                                        <button
                                            type="button"
                                            onClick={() => handleFileDelete(f.id)}
                                            className="p-2 text-slate-300 hover:text-rose-500 transition-colors shrink-0"
                                        >
                                            <X className="w-5 h-5" />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </Card>

                    {/* 섹션 3: 약관 동의 */}
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <SectionHeader step={3} icon={<FileText className="w-5 h-5" />} title="약관 동의" />

                        <div className="bg-slate-50/50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4 mb-8">
                            <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                            <p className="text-xs text-slate-500 leading-relaxed font-medium">
                                각 항목의 <strong className="text-slate-900">내용 보기</strong>를 눌러 약관을 확인한 후 동의해주세요.
                                필수 항목에 모두 동의해야 심사 요청이 가능합니다.
                            </p>
                        </div>

                        {isDocsLoading ? (
                            <div className="flex items-center justify-center py-12">
                                <Loader2 className="w-8 h-8 text-emerald-500 animate-spin" />
                            </div>
                        ) : (
                            <div className="space-y-4">
                                <div className="mt-6 space-y-3">
                                    {agreedDocs.map((doc) => (
                                        <div
                                            key={doc.documentType}
                                            className="flex items-center gap-4 px-6 py-5 bg-white rounded-2xl border border-slate-100 hover:border-emerald-200 transition-all group"
                                        >
                                            <input
                                                type="checkbox"
                                                id={`doc-${doc.documentType}`}
                                                checked={doc.agreed}
                                                onChange={() => handleTermToggle(doc.documentType)}
                                                disabled={!viewedDocs.has(doc.documentType)}
                                                className="w-5 h-5 rounded-lg border-slate-300 text-emerald-600 focus:ring-emerald-500 disabled:opacity-40 transition-all"
                                            />
                                            <label
                                                htmlFor={`doc-${doc.documentType}`}
                                                className={`flex-1 text-sm font-bold text-slate-700 ${
                                                    viewedDocs.has(doc.documentType)
                                                        ? 'cursor-pointer'
                                                        : 'cursor-not-allowed opacity-60'
                                                }`}
                                            >
                                                {doc.documentName}
                                            </label>
                                            <span
                                                className={`px-3 py-1 rounded-full text-[10px] font-black shrink-0 ${
                                                    doc.isMandatory
                                                        ? 'bg-rose-50 text-rose-600'
                                                        : 'bg-blue-50 text-blue-600'
                                                }`}
                                            >
                                                {doc.isMandatory ? '필수' : '선택'}
                                            </span>
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                onClick={() => openModal(doc)}
                                                className="shrink-0 rounded-xl"
                                            >
                                                내용 보기
                                            </Button>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </Card>
                </div>

                {/* 2. 요약 및 실행 영역 (1컬럼) */}
                <div className="lg:col-span-1">
                    <div className="sticky top-10 space-y-6">
                        {/* 신청 현황 요약 카드 */}
                        <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[520px]">
                            <div className="space-y-8">
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">신청 현황 요약</span>
                                    <Search className="w-4 h-4 text-slate-300" />
                                </div>
                                
                                <div className="space-y-6">
                                    <div className="space-y-2">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">성명</p>
                                        <p className="text-sm font-black text-slate-900">{formData.userName || '정보 미입력'}</p>
                                    </div>

                                    <div className="w-full h-px bg-slate-50"></div>

                                    <div className="space-y-2">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">입금 계좌</p>
                                        {formData.bankCode && formData.accountNo ? (
                                            <div className="p-4 bg-emerald-50/50 rounded-2xl border border-emerald-100/50 animate-in slide-in-from-right-2">
                                                <p className="text-sm font-black text-slate-900">{formData.bank}</p>
                                                <p className="text-[11px] text-emerald-600 font-bold font-mono mt-0.5">{formData.accountNo}</p>
                                            </div>
                                        ) : (
                                            <p className="text-sm font-bold text-slate-300 italic">계좌 정보를 입력해 주세요</p>
                                        )}
                                    </div>

                                    <div className="w-full h-px bg-slate-50"></div>

                                    <div className="space-y-2">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">서류 준비도</p>
                                        <div className="flex items-center gap-2">
                                            <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                                                <div 
                                                    className="h-full bg-emerald-500 transition-all duration-500" 
                                                    style={{ width: `${(coveredDocs.filter(d => d.covered).length / coveredDocs.length) * 100}%` }}
                                                />
                                            </div>
                                            <span className="text-xs font-black text-slate-700">{coveredDocs.filter(d => d.covered).length}/{coveredDocs.length}</span>
                                        </div>
                                    </div>

                                    <div className="w-full h-px bg-slate-50"></div>

                                    <div className="bg-slate-50 rounded-2xl p-4 space-y-3">
                                        <div className="flex items-center gap-2 text-emerald-700 font-bold text-[11px]">
                                            <ShieldCheck className="w-4 h-4" />
                                            NICE 신용점수 조회
                                        </div>
                                        <p className="text-[10px] text-slate-500 font-medium leading-relaxed">
                                            심사 요청 시 NICE 신용정보 조회가 발생하며, 결과에 따라 한도가 산출됩니다.
                                        </p>
                                    </div>
                                </div>
                            </div>

                            <div className="mt-10 space-y-3">
                                <Button
                                    onClick={handleSubmit}
                                    disabled={isNextDisabled}
                                    variant={isNextDisabled ? 'secondary' : 'primary'}
                                    size="xl"
                                    fullWidth
                                    className={`h-20 rounded-2xl text-xl font-black shadow-lg transition-all group ${
                                        isNextDisabled ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-slate-900 text-white hover:bg-slate-800'
                                    }`}
                                >
                                    {submitMutation.isPending ? (
                                        <Loader2 className="w-6 h-6 animate-spin" />
                                    ) : (
                                        <>
                                            심사 요청하기
                                            <ChevronRight className={`w-6 h-6 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                                        </>
                                    )}
                                </Button>
                                <Button
                                    onClick={onBack}
                                    variant="outline"
                                    fullWidth
                                    className="h-12 border-none text-slate-400 font-bold hover:text-slate-600"
                                >
                                    이전 단계로
                                </Button>
                            </div>
                        </Card>
                    </div>
                </div>
            </div>

            {/* 심사 요청 전 최종 확인 모달 */}
            {isConfirmOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
                    <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                        {/* 경고 헤더 */}
                        <div className="bg-emerald-600 px-10 py-8 flex items-center gap-6">
                            <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center backdrop-blur-md">
                                <AlertTriangle className="w-8 h-8 text-white" />
                            </div>
                            <div>
                                <h3 className="text-2xl font-black text-white">최종 정보 확인</h3>
                                <p className="text-emerald-100 mt-1 font-medium">
                                    입력하신 내용이 정확한지 확인해 주세요.
                                </p>
                            </div>
                        </div>

                        <div className="p-10 space-y-8">
                            <div className="bg-slate-50 rounded-3xl p-8 space-y-5 border border-slate-100">
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">성명</span>
                                    <span className="text-lg font-black text-slate-900">{formData.userName}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">주민등록번호</span>
                                    <span className="text-lg font-black text-slate-900 font-mono tracking-widest">
                                        {rrnFront}-{rrnBack}●●●●●●
                                    </span>
                                </div>
                                <div className="flex justify-between items-center pt-5 border-t border-slate-200/50">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">입금 계좌</span>
                                    <div className="text-right">
                                        <p className="text-lg font-black text-slate-900">{formData.bank}</p>
                                        <p className="text-sm font-bold text-emerald-600 font-mono">{formData.accountNo}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="space-y-3">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">필수 서류 확인</p>
                                <div className="grid grid-cols-2 gap-3">
                                    {coveredDocs.map((doc) => (
                                        <div
                                            key={doc.label}
                                            className="flex items-center gap-3 px-4 py-3 bg-white border border-slate-100 rounded-2xl"
                                        >
                                            <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
                                            <span className="text-xs font-bold text-slate-700">{doc.label}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>

                        {/* 버튼 */}
                        <div className="px-10 pb-10 grid grid-cols-2 gap-4">
                            <Button
                                onClick={() => setIsConfirmOpen(false)}
                                variant="secondary"
                                size="xl"
                                className="h-16 rounded-2xl bg-slate-100 text-slate-600 hover:bg-slate-200"
                            >
                                수정하기
                            </Button>
                            <Button
                                onClick={handleActualSubmit}
                                variant="primary"
                                size="xl"
                                className="h-16 rounded-2xl bg-slate-900 text-white hover:bg-slate-800 shadow-xl shadow-slate-900/20"
                            >
                                확인 완료
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* 약관 모달 */}
            {isModalOpen && activeDoc && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
                    <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-4xl overflow-hidden flex flex-col h-[85vh] animate-in zoom-in-95 duration-300">
                        <div className="bg-slate-900 text-white px-10 py-8 flex items-center justify-between shrink-0">
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 bg-white/10 rounded-2xl flex items-center justify-center">
                                    <FileText className="w-6 h-6 text-slate-400" />
                                </div>
                                <h3 className="text-xl font-black">{activeDoc.documentName}</h3>
                            </div>
                            <button
                                type="button"
                                onClick={() => setIsModalOpen(false)}
                                className="w-10 h-10 flex items-center justify-center rounded-xl hover:bg-white/10 transition-colors"
                            >
                                <X className="w-6 h-6" />
                            </button>
                        </div>
                        
                        <div className="flex-1 min-h-0 bg-white">
                            <iframe
                                ref={iframeRef}
                                srcDoc={buildTermsSrcDoc(activeDoc.documentContent)}
                                className="w-full h-full border-0 block"
                                sandbox="allow-scripts"
                                title={activeDoc.documentName}
                            />
                        </div>

                        <div className="px-10 py-8 border-t border-slate-50 bg-slate-50/30 flex flex-col gap-6 shrink-0">
                            {!hasScrolledToBottom && (
                                <p className="text-sm text-rose-500 text-center font-black animate-bounce">
                                    * 약관을 끝까지 읽어주셔야 동의가 가능합니다.
                                </p>
                            )}
                            <div className="flex justify-end gap-4">
                                <Button
                                    onClick={() => setIsModalOpen(false)}
                                    variant="secondary"
                                    className="px-8 h-14 rounded-2xl bg-white border border-slate-200 text-slate-600 font-black"
                                >
                                    닫기
                                </Button>
                                <Button
                                    onClick={handleModalAgree}
                                    disabled={!hasScrolledToBottom}
                                    className={`px-10 h-14 rounded-2xl font-black transition-all ${
                                        hasScrolledToBottom
                                            ? 'bg-slate-900 text-white hover:bg-slate-800 shadow-lg'
                                            : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                                    }`}
                                >
                                    동의하고 계속하기
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default LoanRequestForm;
