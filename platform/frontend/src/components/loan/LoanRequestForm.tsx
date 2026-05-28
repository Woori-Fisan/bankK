import React, { useState, useRef, useEffect } from 'react';
import {
    User, Building2, Upload, FileText, X, ChevronLeft, ChevronRight,
    FileType, CheckCircle2, Loader2,
} from 'lucide-react';
import type { LoanData } from '../../pages/LoanApplication';
import { useReviewDocuments, useSubmitLoanEvaluation, useBankList, extractApiError } from '../../hooks/useLoan';
import type { ReviewDocument } from '../../api/loanApi';
import { uploadLoanDocument } from '../../api/loanApi';
import { isValidAccountNumber } from '../../utils/validator';

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
    onNext: (data: LoanData, applicationId: string) => void;
    onBack: () => void;
}

const LoanRequestForm: React.FC<LoanRequestFormProps> = ({ onNext, onBack }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);
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

    const [files, setFiles] = useState<{ id: number; name: string; progress: number; status: string; documentId?: string }[]>([]);
    const [agreedDocs, setAgreedDocs] = useState<AgreedDoc[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeDoc, setActiveDoc] = useState<AgreedDoc | null>(null);
    const [viewedDocs, setViewedDocs] = useState<Set<string>>(new Set());
    const [hasScrolledToBottom, setHasScrolledToBottom] = useState(false);

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
            if (e.data === 'terms-scrolled-to-bottom' && e.source === iframeRef.current?.contentWindow) {
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

    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const selected = e.target.files;
        if (!selected) return;
        const fileArray = Array.from(selected);
        e.target.value = '';

        for (let i = 0; i < fileArray.length; i++) {
            const file = fileArray[i];
            const tempId = Date.now() + i;

            setFiles((prev) => [
                ...prev,
                { id: tempId, name: file.name, progress: 0, status: '업로드 중...' },
            ]);

            try {
                const result = await uploadLoanDocument(file);
                setFiles((prev) =>
                    prev.map((f) =>
                        f.id === tempId
                            ? { ...f, progress: 100, status: '완료', documentId: result.documentId }
                            : f,
                    ),
                );
            } catch {
                setFiles((prev) =>
                    prev.map((f) =>
                        f.id === tempId ? { ...f, status: '업로드 실패', progress: 0 } : f,
                    ),
                );
            }
        }
    };

    const handleFileDelete = (id: number) => {
        setFiles((prev) => prev.filter((f) => f.id !== id));
    };

    const openModal = (doc: AgreedDoc) => {
        setViewedDocs(prev => new Set([...prev, doc.documentType]));
        setActiveDoc(doc);
        setHasScrolledToBottom(false);
        setIsModalOpen(true);
    };

    const buildTermsSrcDoc = (content: string | undefined): string => {
        const body = content ?? '<p style="padding:16px;font-family:sans-serif;color:#555">내용을 불러올 수 없습니다.</p>';
        return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>html,body{margin:0;padding:0;}</style>
</head>
<body>
${body}
<script>
(function(){
  function check(){
    var scrolled=window.scrollY+window.innerHeight;
    var total=document.documentElement.scrollHeight;
    if(scrolled>=total-5){window.parent.postMessage('terms-scrolled-to-bottom','*');}
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
            setAgreedDocs(prev =>
                prev.map(d => d.documentType === activeDoc.documentType ? { ...d, agreed: true } : d)
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

    const handleSubmit = async () => {
        if (!validate()) return;

        setFieldErrors({});
        const rrnPrefix = rrnFront + rrnBack;

        try {
            const agreedAt = new Date().toISOString();
            const documents = agreedDocs
                .filter((d) => d.agreed)
                .map((d) => ({
                    documentType: d.documentType,
                    agreedAt,
                }));

            const uploadedDocumentIds = files
                .filter((f) => f.documentId)
                .map((f) => f.documentId!);

            const result = await submitMutation.mutateAsync({
                bankCode: formData.bankCode!,
                customerName: formData.userName!,
                customerRrnPrefix: rrnPrefix,
                customerPhone: formData.phone ?? '',
                depositBankCode: formData.bankCode!,
                depositAccountNo: formData.accountNo!,
                documents,
                uploadedDocumentIds,
            });

            onNext({ ...formData, rrn: `${rrnFront}-${rrnBack}` }, result.applicationId);
        } catch (err) {
            setFieldErrors({ submit: extractApiError(err) });
        }
    };

    const isUploading = files.some((f) => f.status === '업로드 중...');

    const uploadedFileNames = files
        .filter((f) => f.documentId)
        .map((f) => f.name.toLowerCase());

    const coveredDocs = REQUIRED_DOCS.map((doc) => ({
        ...doc,
        covered: uploadedFileNames.some((name) =>
            doc.keywords.some((kw) => name.includes(kw)),
        ),
    }));

    const allDocsCovered = coveredDocs.every((d) => d.covered);

    const isNextDisabled =
        submitMutation.isPending ||
        isUploading ||
        !allDocsCovered ||
        agreedDocs.filter((d) => d.isMandatory).some((d) => !d.agreed) ||
        !formData.userName ||
        !formData.accountNo;

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                {/* 본인 정보 */}
                <div className="space-y-6">
                    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                        <div className="flex items-center gap-2 mb-4">
                            <User className="w-5 h-5 text-gray-400" />
                            <h3 className="text-sm font-bold text-gray-900">본인 정보</h3>
                        </div>
                        <div className="space-y-4">
                            <div>
                                <label htmlFor="userName" className="block text-[11px] text-gray-500 mb-1">
                                    성명
                                </label>
                                <input
                                    id="userName"
                                    type="text"
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
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
                                    <p className="mt-1 text-xs text-red-500">{fieldErrors.userName}</p>
                                )}
                            </div>
                            <div>
                                <label className="block text-[11px] text-gray-500 mb-1">
                                    주민등록번호
                                </label>
                                <div className="flex items-center gap-1.5">
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
                                        className="w-28 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none text-center tracking-widest"
                                    />
                                    <span className="text-gray-500 font-bold">-</span>
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
                                        className="w-8 px-1 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none text-center"
                                    />
                                    <span className="text-gray-400 text-sm tracking-widest select-none">●●●●●●</span>
                                </div>
                                {fieldErrors.rrn && (
                                    <p className="mt-1 text-xs text-red-500">{fieldErrors.rrn}</p>
                                )}
                            </div>
                            <div>
                                <label htmlFor="phone" className="block text-[11px] text-gray-500 mb-1">
                                    연락처
                                </label>
                                <input
                                    id="phone"
                                    type="text"
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    placeholder="010-1234-5678"
                                    value={formData.phone}
                                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                        <div className="flex items-center gap-2 mb-4">
                            <Building2 className="w-5 h-5 text-gray-400" />
                            <h3 className="text-sm font-bold text-gray-900">대출금 입금 계좌</h3>
                        </div>
                        <div className="space-y-4">
                            <div>
                                <label htmlFor="bank" className="block text-[11px] text-gray-500 mb-1">
                                    은행 선택
                                </label>
                                <select
                                    id="bank"
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm outline-none"
                                    value={formData.bankCode}
                                    disabled={isBankListLoading}
                                    onChange={(e) => {
                                        const selected = bankList?.find(b => b.bankCode === e.target.value);
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
                                {fieldErrors.bank && (
                                    <p className="mt-1 text-xs text-red-500">{fieldErrors.bank}</p>
                                )}
                            </div>
                            <div>
                                <label htmlFor="accountNo" className="block text-[11px] text-gray-500 mb-1">
                                    계좌 번호
                                </label>
                                <input
                                    id="accountNo"
                                    type="text"
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm outline-none"
                                    placeholder="숫자만 입력"
                                    value={formData.accountNo}
                                    onChange={(e) => setFormData({ ...formData, accountNo: e.target.value.replace(/[^0-9]/g, '') })}
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
                                <p className="mt-1 text-[10px] text-gray-400">
                                    * 계좌번호는 '-' 없이 숫자만 입력해 주세요.
                                </p>
                                {fieldErrors.accountNo && (
                                    <p className="mt-1 text-xs text-red-500">{fieldErrors.accountNo}</p>
                                )}
                            </div>
                            <div>
                                <label htmlFor="accountHolder" className="block text-[11px] text-gray-500 mb-1">
                                    예금주
                                </label>
                                <input
                                    id="accountHolder"
                                    type="text"
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm outline-none"
                                    value={formData.accountHolder}
                                    onChange={(e) =>
                                        setFormData({ ...formData, accountHolder: e.target.value })
                                    }
                                />
                            </div>
                        </div>
                    </div>
                </div>

                {/* 서류 업로드 */}
                <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                    <div className="flex items-center gap-2 mb-4">
                        <Upload className="w-5 h-5 text-gray-400" />
                        <h3 className="text-sm font-bold text-gray-900">서류 업로드</h3>
                    </div>

                    {/* 필수 서류 체크리스트 */}
                    <div className="mb-4 space-y-1.5">
                        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">필수 서류 (4종)</p>
                        {coveredDocs.map((doc) => (
                            <div key={doc.label} className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs border ${doc.covered ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-gray-50 border-gray-100 text-gray-500'}`}>
                                <CheckCircle2 className={`w-3.5 h-3.5 flex-shrink-0 ${doc.covered ? 'text-emerald-500' : 'text-gray-300'}`} />
                                <span className="font-medium">{doc.label}</span>
                                {!doc.covered && (
                                    <span className="ml-auto text-[10px] text-gray-400">{doc.hint}</span>
                                )}
                            </div>
                        ))}
                    </div>

                    <p className="text-[10px] text-gray-500 mb-3">파일명에 위 키워드가 포함되어야 합니다. (PDF, 최대 10MB)</p>

                    <input
                        type="file"
                        ref={fileInputRef}
                        className="hidden"
                        multiple
                        accept=".pdf"
                        onChange={handleFileUpload}
                    />
                    <div
                        onClick={() => fileInputRef.current?.click()}
                        className="border-2 border-dashed border-gray-200 rounded-xl p-8 text-center hover:border-blue-400 hover:bg-blue-50/30 transition-all cursor-pointer group"
                    >
                        <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3 group-hover:bg-blue-100 transition-colors">
                            <Upload className="w-6 h-6 text-gray-400 group-hover:text-blue-600" />
                        </div>
                        <p className="text-xs text-gray-600 font-medium">클릭하거나 파일을 드래그하세요</p>
                    </div>

                    <div className="mt-6 space-y-3">
                        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">
                            업로드된 파일
                        </p>
                        {files.length === 0 && (
                            <p className="text-xs text-gray-400 text-center py-4">업로드된 파일이 없습니다.</p>
                        )}
                        {files.map((file) => (
                            <div
                                key={file.id}
                                className="p-3 bg-gray-50 border border-gray-100 rounded-lg flex items-center gap-3"
                            >
                                <div className="w-8 h-8 bg-white border border-gray-200 rounded flex items-center justify-center">
                                    {file.status === '업로드 중...' ? (
                                        <Loader2 className="w-4 h-4 text-blue-500 animate-spin" />
                                    ) : (
                                        <FileType className="w-4 h-4 text-red-500" />
                                    )}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex justify-between mb-1">
                                        <p className="text-xs font-medium text-gray-900 truncate">{file.name}</p>
                                        <span className={`text-[10px] ${file.status === '업로드 실패' ? 'text-red-500' : 'text-gray-500'}`}>
                                            {file.status}
                                        </span>
                                    </div>
                                    <div className="h-1 bg-gray-200 rounded-full overflow-hidden">
                                        <div
                                            className={`h-full transition-all duration-500 ${file.status === '업로드 실패' ? 'bg-red-400' : 'bg-emerald-500'}`}
                                            style={{ width: `${file.progress}%` }}
                                        />
                                    </div>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => handleFileDelete(file.id)}
                                    disabled={file.status === '업로드 중...'}
                                    className="text-gray-400 hover:text-red-500 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                                >
                                    <X className="w-4 h-4" />
                                </button>
                            </div>
                        ))}
                    </div>
                </div>

                {/* 약관 동의 */}
                <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                    <div className="flex items-center gap-2 mb-4">
                        <FileText className="w-5 h-5 text-gray-400" />
                        <h3 className="text-sm font-bold text-gray-900">약관 동의</h3>
                    </div>
                    <p className="text-[10px] text-gray-500 mb-4">
                        각 항목의 <strong>내용 보기</strong>를 눌러 확인 후 동의해주세요.
                    </p>

                    {isDocsLoading ? (
                        <div className="flex items-center justify-center py-8">
                            <Loader2 className="w-6 h-6 text-emerald-500 animate-spin" />
                        </div>
                    ) : (
                        <>
                            <button
                                type="button"
                                onClick={handleAllAgreed}
                                disabled={!agreedDocs.every((d) => viewedDocs.has(d.documentType))}
                                className={`w-full p-3 mb-1 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-colors ${
                                    agreedDocs.every((d) => viewedDocs.has(d.documentType))
                                        ? 'bg-gray-900 text-white hover:bg-gray-800'
                                        : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                                }`}
                            >
                                <CheckCircle2 className="w-4 h-4" />
                                전체 약관에 동의합니다
                            </button>
                            {agreedDocs.some((d) => !viewedDocs.has(d.documentType)) ? (
                                <p className="text-[10px] text-amber-600 mb-3 text-center">내용 보기를 먼저 클릭해주세요.</p>
                            ) : (
                                <div className="mb-3" />
                            )}

                            <div className="space-y-2">
                                {agreedDocs.map((doc) => (
                                    <div
                                        key={doc.documentType}
                                        className="p-3 bg-gray-50 rounded-lg border border-gray-100 hover:border-blue-200 transition-colors"
                                    >
                                        <div className="flex items-center gap-3 mb-2">
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
                                                className={`flex-1 text-xs text-gray-900 font-medium ${viewedDocs.has(doc.documentType) ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'}`}
                                            >
                                                {doc.documentName}
                                            </label>
                                            <span
                                                className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${
                                                    doc.isMandatory
                                                        ? 'bg-red-50 text-red-600'
                                                        : 'bg-blue-50 text-blue-600'
                                                }`}
                                            >
                                                {doc.isMandatory ? '필수' : '선택'}
                                            </span>
                                        </div>
                                        <div className="flex justify-end">
                                            <button
                                                type="button"
                                                onClick={() => openModal(doc)}
                                                className="text-[10px] text-blue-600 font-medium hover:underline"
                                            >
                                                내용 보기
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}
                </div>
            </div>

            {fieldErrors.submit && (
                <p className="text-sm text-red-500 text-center">{fieldErrors.submit}</p>
            )}

            <div className="flex justify-between pt-6 border-t border-gray-200">
                <button
                    type="button"
                    onClick={onBack}
                    className="flex items-center gap-2 px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-sm hover:bg-gray-50 transition-colors"
                >
                    <ChevronLeft className="w-4 h-4" />
                    이전으로
                </button>
                <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={isNextDisabled}
                    className={`flex items-center gap-2 px-6 py-3 rounded-xl font-bold text-sm transition-all shadow-md ${
                        isNextDisabled
                            ? 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                            : 'bg-slate-900 text-white hover:bg-slate-800'
                    }`}
                >
                    {submitMutation.isPending ? (
                        <>
                            <Loader2 className="w-4 h-4 animate-spin" />
                            심사 요청 중...
                        </>
                    ) : (
                        <>
                            심사 요청하기
                            <ChevronRight className="w-4 h-4" />
                        </>
                    )}
                </button>
            </div>

            {/* 약관 모달 */}
            {isModalOpen && activeDoc && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-3xl shadow-2xl w-full max-w-xl overflow-hidden">
                        <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <FileText className="w-5 h-5 text-slate-400" />
                                <h3 className="text-sm font-bold">{activeDoc.documentName}</h3>
                            </div>
                            <button
                                type="button"
                                onClick={() => setIsModalOpen(false)}
                                className="text-slate-400 hover:text-white transition-colors"
                            >
                                <X className="w-6 h-6" />
                            </button>
                        </div>
                        <iframe
                            ref={iframeRef}
                            srcDoc={buildTermsSrcDoc(activeDoc.documentContent)}
                            className="w-full h-[400px] border-0 bg-white"
                            sandbox="allow-scripts"
                            title={activeDoc.documentName}
                        />
                        <div className="px-5 pt-3 pb-1 border-t border-gray-100">
                            {!hasScrolledToBottom && (
                                <p className="text-[11px] text-amber-600 text-center font-medium">
                                    약관을 끝까지 읽어야 동의할 수 있습니다.
                                </p>
                            )}
                        </div>
                        <div className="px-5 pb-5 flex justify-end gap-3">
                            <button
                                type="button"
                                onClick={() => setIsModalOpen(false)}
                                className="px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-xs"
                            >
                                닫기
                            </button>
                            <button
                                type="button"
                                onClick={handleModalAgree}
                                disabled={!hasScrolledToBottom}
                                className={`px-6 py-2.5 rounded-xl font-bold text-xs transition-colors ${
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
