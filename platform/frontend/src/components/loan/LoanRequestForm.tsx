import React, { useState, useRef } from 'react';
import {
    User, Building2, Upload, FileText, X, ChevronLeft, ChevronRight,
    FileType, CheckCircle2, Globe, Loader2,
} from 'lucide-react';
import type { LoanData } from '../../pages/LoanApplication';
import { useReviewDocuments, useSubmitLoanEvaluation, useBankList, extractApiError } from '../../hooks/useLoan';
import type { ReviewDocument } from '../../api/loanApi';
import { isValidAccountNumber } from '../../utils/validator';

interface AgreedDoc extends ReviewDocument {
    agreed: boolean;
}

interface LoanRequestFormProps {
    onNext: (data: LoanData, applicationId: string) => void;
    onBack: () => void;
}

const LoanRequestForm: React.FC<LoanRequestFormProps> = ({ onNext, onBack }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [formData, setFormData] = useState<LoanData>({
        userName: '',
        rrn: '',
        phone: '',
        bank: '',
        bankCode: '',
        accountNo: '',
        accountHolder: '',
    });
    const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof LoanData | 'submit', string>>>({});

    const [files, setFiles] = useState<{ id: number; name: string; progress: number; status: string }[]>([]);
    const [agreedDocs, setAgreedDocs] = useState<AgreedDoc[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeDoc, setActiveDoc] = useState<AgreedDoc | null>(null);

    const { data: docsData, isLoading: isDocsLoading } = useReviewDocuments();
    const { data: bankList, isLoading: isBankListLoading } = useBankList();
    const submitMutation = useSubmitLoanEvaluation();

    React.useEffect(() => {
        if (docsData?.documents) {
            setAgreedDocs(docsData.documents.map((d) => ({ ...d, agreed: false })));
        }
    }, [docsData]);

    const handleTermToggle = (documentType: string) => {
        setAgreedDocs((prev) =>
            prev.map((d) => (d.documentType === documentType ? { ...d, agreed: !d.agreed } : d)),
        );
    };

    const handleAllAgreed = () => {
        const allAgreed = agreedDocs.every((d) => d.agreed);
        setAgreedDocs((prev) => prev.map((d) => ({ ...d, agreed: !allAgreed })));
    };

    const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selected = e.target.files;
        if (!selected) return;
        const newFiles = Array.from(selected).map((file, idx) => ({
            id: Date.now() + idx,
            name: file.name,
            progress: 100,
            status: '완료',
        }));
        setFiles((prev) => [...prev, ...newFiles]);
    };

    const handleFileDelete = (id: number) => {
        setFiles((prev) => prev.filter((f) => f.id !== id));
    };

    const openModal = (doc: AgreedDoc) => {
        setActiveDoc(doc);
        setIsModalOpen(true);
    };

    const handleModalAgree = () => {
        if (activeDoc) handleTermToggle(activeDoc.documentType);
        setIsModalOpen(false);
    };

    const validate = (): boolean => {
        const errors: typeof fieldErrors = {};
        if (!formData.userName?.trim()) errors.userName = '성명을 입력해주세요.';
        if (!formData.rrn?.trim()) {
            errors.rrn = '주민등록번호를 입력해주세요.';
        } else if (!/^\d{6}-?\d{7}$/.test(formData.rrn.trim())) {
            errors.rrn = '올바른 주민등록번호 형식을 입력해주세요. (예: 900101-1234567)';
        }
        if (!formData.bankCode) errors.bank = '은행을 선택해주세요.';
        if (!formData.accountNo?.trim()) {
            errors.accountNo = '계좌번호를 입력해주세요.';
        } else if (!isValidAccountNumber(formData.accountNo)) {
            errors.accountNo = '올바른 계좌번호 형식을 입력해주세요. (10~14자리 숫자)';
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
        const rrnRaw = formData.rrn!.replace(/-/g, '');
        const rrnPrefix = rrnRaw.slice(0, 7);

        try {
            const agreedAt = new Date().toISOString();
            const documents = agreedDocs
                .filter((d) => d.agreed)
                .map((d) => ({
                    documentType: d.documentType,
                    agreedAt,
                }));

            const result = await submitMutation.mutateAsync({
                bankCode: formData.bankCode!,
                customerName: formData.userName!,
                customerRrnPrefix: rrnPrefix,
                customerPhone: formData.phone ?? '',
                depositBankCode: formData.bankCode!,
                depositAccountNo: formData.accountNo!,
                documents,
            });

            onNext(formData, result.applicationId);
        } catch (err) {
            setFieldErrors({ submit: extractApiError(err) });
        }
    };

    const isNextDisabled =
        submitMutation.isPending ||
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
                                <label htmlFor="rrn" className="block text-[11px] text-gray-500 mb-1">
                                    주민등록번호
                                </label>
                                <input
                                    id="rrn"
                                    type="text"
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    placeholder="900101-1234567"
                                    value={formData.rrn}
                                    onChange={(e) => setFormData({ ...formData, rrn: e.target.value })}
                                    onBlur={() => {
                                        if (!formData.rrn?.trim())
                                            setFieldErrors((p) => ({ ...p, rrn: '주민등록번호를 입력해주세요.' }));
                                        else if (!/^\d{6}-?\d{7}$/.test(formData.rrn.trim()))
                                            setFieldErrors((p) => ({
                                                ...p,
                                                rrn: '올바른 형식을 입력해주세요. (예: 900101-1234567)',
                                            }));
                                        else setFieldErrors((p) => ({ ...p, rrn: undefined }));
                                    }}
                                />
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
                                    onChange={(e) => setFormData({ ...formData, accountNo: e.target.value })}
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
                    <p className="text-[10px] text-gray-500 mb-4">필수 서류를 업로드해주세요. (PDF, 최대 10MB)</p>

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
                                    <FileType className="w-4 h-4 text-red-500" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex justify-between mb-1">
                                        <p className="text-xs font-medium text-gray-900 truncate">{file.name}</p>
                                        <span className="text-[10px] text-gray-500">{file.status}</span>
                                    </div>
                                    <div className="h-1 bg-gray-200 rounded-full overflow-hidden">
                                        <div
                                            className="h-full bg-emerald-500 transition-all duration-500"
                                            style={{ width: `${file.progress}%` }}
                                        />
                                    </div>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => handleFileDelete(file.id)}
                                    className="text-gray-400 hover:text-red-500 transition-colors"
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
                                className="w-full p-3 mb-4 bg-gray-900 text-white rounded-lg text-xs font-bold flex items-center justify-center gap-2"
                            >
                                <CheckCircle2 className="w-4 h-4" />
                                전체 약관에 동의합니다
                            </button>

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
                                                className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                            />
                                            <label
                                                htmlFor={`doc-${doc.documentType}`}
                                                className="flex-1 text-xs text-gray-900 font-medium cursor-pointer"
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
                        <div className="h-[400px] bg-gray-50 flex flex-col items-center justify-center p-10 text-center">
                            <div className="w-16 h-16 bg-white border border-gray-200 rounded-2xl flex items-center justify-center mb-4 text-gray-300">
                                <Globe className="w-8 h-8" />
                            </div>
                            <h4 className="text-sm font-bold text-gray-900 mb-2">
                                CDN에서 약관 내용을 불러옵니다
                            </h4>
                            <p className="text-[11px] text-gray-500 mb-6 break-all">
                                {activeDoc.documentUrl}
                            </p>
                            <div className="p-4 bg-blue-50 text-blue-600 rounded-xl text-[11px] font-medium max-w-sm">
                                실제 배포 시 이 영역은 iframe으로 약관 HTML이 렌더링됩니다.
                            </div>
                        </div>
                        <div className="p-5 border-t border-gray-100 flex justify-end gap-3">
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
                                className="px-6 py-2.5 bg-slate-900 text-white rounded-xl font-bold text-xs hover:bg-slate-800"
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
