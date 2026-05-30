import React, { useState, useEffect } from 'react';
import { FileText, Receipt, Info, ChevronLeft, ChevronRight, X, Loader2 } from 'lucide-react';
import type { LoanProduct, LoanData } from '../../pages/LoanApplication';
import { useContractDocuments, extractApiError } from '../../hooks/useLoan';
import type { ContractDocument } from '../../api/loanApi';
import { formatAmount } from '../../utils/formatter';

interface AgreedContractDoc extends ContractDocument {
    agreed: boolean;
}

interface LoanContractFormProps {
    product: LoanProduct;
    loanData: LoanData;
    evaluationId: string;
    onNext: () => void;
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

    useEffect(() => {
        if (data?.documents) {
            setAgreedDocs(data.documents.map((d) => ({ ...d, agreed: false })));
        }
    }, [data]);

    const handleTermToggle = (documentType: string) => {
        setAgreedDocs((prev) =>
            prev.map((d) => (d.documentType === documentType ? { ...d, agreed: !d.agreed } : d)),
        );
    };

    const openModal = (doc: AgreedContractDoc) => {
        setViewedDocs(prev => new Set([...prev, doc.documentType]));
        setActiveDoc(doc);
        setIsModalOpen(true);
    };

    const handleModalAgree = () => {
        if (activeDoc) {
            setAgreedDocs(prev =>
                prev.map(d => d.documentType === activeDoc.documentType ? { ...d, agreed: true } : d)
            );
        }
        setIsModalOpen(false);
    };

    const mandatoryDocs = agreedDocs.filter((d) => d.isMandatory);
    const optionalDocs = agreedDocs.filter((d) => !d.isMandatory);
    const isNextDisabled = mandatoryDocs.some((d) => !d.agreed);
    const agreedCount = agreedDocs.filter((d) => d.agreed).length;

    return (
        <div className="space-y-8">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                <div className="lg:col-span-8 space-y-6">
                    {isLoading && (
                        <div className="flex items-center justify-center py-12 bg-white border border-gray-200 rounded-2xl">
                            <Loader2 className="w-8 h-8 text-emerald-500 animate-spin" />
                            <span className="ml-3 text-sm text-gray-500">계약 서류를 불러오는 중...</span>
                        </div>
                    )}

                    {error && (
                        <div className="p-4 bg-red-50 border border-red-200 rounded-xl">
                            <p className="text-sm text-red-600">{extractApiError(error)}</p>
                        </div>
                    )}

                    {!isLoading && !error && (
                        <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden shadow-sm">
                            <div className="p-6 border-b border-gray-100 bg-gray-50/50 flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <FileText className="w-5 h-5 text-gray-400" />
                                    <h3 className="text-base font-bold text-gray-900">계약 서류 목록</h3>
                                </div>
                                <span className="px-2 py-1 bg-emerald-50 text-emerald-600 text-xs font-bold rounded-full">
                                    {agreedCount}/{agreedDocs.length} 확인
                                </span>
                            </div>
                            <div className="p-6 space-y-4">
                                {mandatoryDocs.length > 0 && (
                                    <>
                                        <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">
                                            필수 동의
                                        </p>
                                        {mandatoryDocs.map((doc) => (
                                            <div
                                                key={doc.documentType}
                                                className="flex items-center gap-3 p-5 bg-gray-50 rounded-xl border border-gray-100 hover:border-blue-200 transition-colors"
                                            >
                                                <input
                                                    type="checkbox"
                                                    id={`contract-${doc.documentType}`}
                                                    checked={doc.agreed}
                                                    onChange={() => handleTermToggle(doc.documentType)}
                                                    disabled={!viewedDocs.has(doc.documentType)}
                                                    className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 disabled:opacity-40 disabled:cursor-not-allowed"
                                                />
                                                <label
                                                    htmlFor={`contract-${doc.documentType}`}
                                                    className={`flex-1 text-sm font-medium text-gray-900 ${viewedDocs.has(doc.documentType) ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'}`}
                                                >
                                                    {doc.documentName}
                                                </label>
                                                <span className="px-1.5 py-0.5 bg-red-50 text-red-600 text-[10px] font-bold rounded">
                                                    필수
                                                </span>
                                                <button
                                                    type="button"
                                                    onClick={() => openModal(doc)}
                                                    className="px-4 py-2 bg-white border border-gray-200 text-blue-600 text-xs font-bold rounded-lg hover:bg-blue-50 transition-colors"
                                                >
                                                    내용 보기
                                                </button>
                                            </div>
                                        ))}
                                    </>
                                )}

                                {optionalDocs.length > 0 && (
                                    <>
                                        <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mt-6 mb-2">
                                            선택 동의
                                        </p>
                                        {optionalDocs.map((doc) => (
                                            <div
                                                key={doc.documentType}
                                                className="flex items-center gap-3 p-5 bg-gray-50 rounded-xl border border-gray-100 hover:border-blue-200 transition-colors"
                                            >
                                                <input
                                                    type="checkbox"
                                                    id={`contract-${doc.documentType}`}
                                                    checked={doc.agreed}
                                                    onChange={() => handleTermToggle(doc.documentType)}
                                                    disabled={!viewedDocs.has(doc.documentType)}
                                                    className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 disabled:opacity-40 disabled:cursor-not-allowed"
                                                />
                                                <label
                                                    htmlFor={`contract-${doc.documentType}`}
                                                    className={`flex-1 text-sm font-medium text-gray-900 ${viewedDocs.has(doc.documentType) ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'}`}
                                                >
                                                    {doc.documentName}
                                                </label>
                                                <span className="px-1.5 py-0.5 bg-blue-50 text-blue-600 text-[10px] font-bold rounded">
                                                    선택
                                                </span>
                                                <button
                                                    type="button"
                                                    onClick={() => openModal(doc)}
                                                    className="px-4 py-2 bg-white border border-gray-200 text-blue-600 text-xs font-bold rounded-lg hover:bg-blue-50 transition-colors"
                                                >
                                                    내용 보기
                                                </button>
                                            </div>
                                        ))}
                                    </>
                                )}
                            </div>
                        </div>
                    )}

                    <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-5 flex gap-3">
                        <Info className="w-5 h-5 text-emerald-500 shrink-0" />
                        <p className="text-xs text-emerald-700 leading-relaxed">
                            <strong>내용 보기</strong>를 클릭해 약관을 열람한 후 동의 체크가 활성화됩니다.
                            필수 항목을 모두 확인하고 동의해야 다음 단계로 진행할 수 있습니다.
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-4">
                    <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-sm sticky top-6">
                        <div className="flex items-center gap-2 mb-6 pb-4 border-b border-gray-100">
                            <Receipt className="w-5 h-5 text-gray-400" />
                            <h3 className="text-sm font-bold text-gray-900">선택 상품 요약</h3>
                        </div>
                        <div className="space-y-4">
                            <div className="space-y-1">
                                <p className="text-xs text-gray-400 font-bold uppercase">상품명</p>
                                <p className="text-sm font-bold text-gray-900">{product.name}</p>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-1">
                                    <p className="text-xs text-gray-400 font-bold uppercase">승인 한도</p>
                                    <p className="text-sm font-bold text-gray-900">
                                        ₩ {formatAmount(product.limit)}
                                    </p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-xs text-gray-400 font-bold uppercase">적용 금리</p>
                                    <p className="text-sm font-bold text-gray-900">{product.rate}% (고정)</p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-xs text-gray-400 font-bold uppercase">대출 기간</p>
                                    <p className="text-sm font-bold text-gray-900">{product.period}개월</p>
                                </div>
                            </div>
                            <div className="pt-4 border-t border-gray-100">
                                <p className="text-xs text-gray-400 font-bold uppercase mb-1">입금 계좌</p>
                                <p className="text-sm font-bold text-gray-900">
                                    {loanData.bank} {loanData.accountNo}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="flex justify-between pt-6 border-t border-gray-200">
                <button
                    type="button"
                    onClick={onBack}
                    className="flex items-center gap-2 px-6 py-3 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-base hover:bg-gray-50"
                >
                    <ChevronLeft className="w-4 h-4" />
                    이전으로
                </button>
                <button
                    type="button"
                    onClick={onNext}
                    disabled={isNextDisabled || isLoading}
                    className={`flex items-center gap-2 px-10 py-4 rounded-xl font-bold text-base transition-all shadow-lg ${
                        isNextDisabled || isLoading
                            ? 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                            : 'bg-slate-900 text-white hover:bg-slate-800 shadow-slate-200'
                    }`}
                >
                    대출 실행하기
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>

            {isModalOpen && activeDoc && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden">
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
                            srcDoc={activeDoc.documentContent ?? '<p style="padding:16px;font-family:sans-serif;color:#555">내용을 불러올 수 없습니다.</p>'}
                            className="w-full h-[500px] border-0 bg-white"
                            sandbox="allow-scripts"
                            title={activeDoc.documentName}
                        />
                        <div className="p-5 border-t border-gray-100 flex justify-end gap-3">
                            <button
                                type="button"
                                onClick={() => setIsModalOpen(false)}
                                className="px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-sm"
                            >
                                닫기
                            </button>
                            <button
                                type="button"
                                onClick={handleModalAgree}
                                className="px-6 py-2.5 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800"
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

export default LoanContractForm;
