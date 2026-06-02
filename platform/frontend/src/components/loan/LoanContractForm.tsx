import React, { useState, useEffect, useRef } from 'react';
import { FileText, Receipt, Info, ChevronLeft, ChevronRight, X, Loader2, AlertTriangle, Search } from 'lucide-react';
import type { LoanProduct, LoanData } from '../../pages/LoanApplication';
import { useContractDocuments, extractApiError } from '../../hooks/useLoan';
import type { ContractDocument } from '../../api/loanApi';
import { formatAmount } from '../../utils/formatter';
import { Button } from '../common/Button';

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
    const [hasScrolledToBottom, setHasScrolledToBottom] = useState(false);
    const iframeRef = useRef<HTMLIFrameElement>(null);

    useEffect(() => {
        if (!isModalOpen) return;
        const handler = (e: MessageEvent) => {
            if (e.data === 'terms-scrolled-to-bottom') {
                setHasScrolledToBottom(true);
            }
        };
        window.addEventListener('message', handler);
        return () => window.removeEventListener('message', handler);
    }, [isModalOpen]);

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
    
    if(scrollPos >= totalHeight - 25){
      sent = true;
      window.parent.postMessage('terms-scrolled-to-bottom','${origin}');
    }
  }
  window.addEventListener('scroll', check);
  window.addEventListener('load', function(){
    setTimeout(check, 100);
  });
  window.addEventListener('resize', check);
})();
</` + `script>
</body>
</html>`;
    };

    const openModal = (doc: AgreedContractDoc) => {
        setHasScrolledToBottom(false);
        setActiveDoc(doc);
        setIsModalOpen(true);
        setViewedDocs(prev => new Set([...prev, doc.documentType]));
    };

    const handleModalAgree = () => {
        if (activeDoc) {
            setAgreedDocs(prev =>
                prev.map(d => d.documentType === activeDoc.documentType ? { ...d, agreed: true } : d)
            );
        }
        setIsModalOpen(false);
    };

    const [isExecutionConfirmOpen, setIsExecutionConfirmOpen] = useState(false);

    const handleExecutionClick = () => {
        setIsExecutionConfirmOpen(true);
    };

    const handleActualExecution = () => {
        setIsExecutionConfirmOpen(false);
        onNext();
    };

    const mandatoryDocs = agreedDocs.filter((d) => d.isMandatory);
    const optionalDocs = agreedDocs.filter((d) => !d.isMandatory);
    const isNextDisabled = mandatoryDocs.some((d) => !d.agreed);
    const agreedCount = agreedDocs.filter((d) => d.agreed).length;

    return (
        <div className="space-y-8">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                <div className="lg:col-span-9 space-y-6">
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
                                    <h3 className="text-lg font-bold text-gray-900">계약 서류 목록</h3>
                                </div>
                                <span className="px-2 py-1 bg-emerald-50 text-emerald-600 text-xs font-bold rounded-full">
                                    {agreedCount}/{agreedDocs.length} 확인
                                </span>
                            </div>
                            <div className="p-6 space-y-4">
                                {mandatoryDocs.length > 0 && (
                                    <>
                                        <p className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-2">
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
                                                    className={`flex-1 text-base font-medium text-gray-900 ${viewedDocs.has(doc.documentType) ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'}`}
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
                                                    className={`flex-1 text-base font-medium text-gray-900 ${viewedDocs.has(doc.documentType) ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'}`}
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
                        <p className="text-sm text-emerald-700 leading-relaxed">
                            <strong>내용 보기</strong>를 클릭해 약관을 열람한 후 동의 체크가 활성화됩니다.
                            필수 항목을 모두 확인하고 동의해야 다음 단계로 진행할 수 있습니다.
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-3">
                    <div className="sticky top-6 min-h-[600px] flex flex-col justify-items-start">
                        <div className="bg-white border border-gray-200 rounded-2xl p-8 shadow-sm ">
                            <div className="flex items-center gap-2 mb-6 pb-4 border-b border-gray-100">
                                <Receipt className="w-5 h-5 text-gray-400" />
                                <h3 className="text-base font-bold text-gray-900">선택 상품 요약</h3>
                            </div>
                            <div className="space-y-5">
                                <div className="space-y-1">
                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">상품명</p>
                                    <p className="text-base font-bold text-slate-900">{product.name}</p>
                                </div>

                                <div className="h-px bg-slate-50" />

                                <div className="space-y-1">
                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">승인 한도</p>
                                    <p className="text-xl font-black text-emerald-600">
                                        ₩ {formatAmount(product.limit)}
                                    </p>
                                </div>

                                <div className="space-y-1">
                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">적용 금리</p>
                                    <p className="text-lg font-black text-slate-900">{product.rate}% <span className="text-xs font-bold text-slate-400 ml-1">(고정금리)</span></p>
                                </div>

                                <div className="space-y-1">
                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">대출 기간</p>
                                    <p className="text-lg font-black text-slate-900">{product.period}개월</p>
                                </div>
                            </div>
                        </div>

                        <div className="mt-3 space-y-3">
                            <Button
                                onClick={handleExecutionClick}
                                disabled={isNextDisabled || isLoading}
                                variant={isNextDisabled || isLoading ? 'secondary' : 'primary'}
                                size="xl"
                                fullWidth
                                className={`h-20 rounded-2xl text-xl font-black shadow-lg transition-all group ${
                                    isNextDisabled || isLoading ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-slate-900 text-white hover:bg-slate-800'
                                }`}
                            >
                                {isLoading ? (
                                    <Loader2 className="w-6 h-6 animate-spin" />
                                ) : (
                                    <>
                                        대출 진행
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
                    </div>
                </div>
            </div>

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
                                    동의하고 닫기
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
            {/* 대출 실행 최종 확인 모달 */}
            {isExecutionConfirmOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
                    <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                        {/* 헤더 */}
                        <div className="bg-emerald-600 px-10 py-8 flex items-center gap-6">
                            <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center backdrop-blur-md">
                                <AlertTriangle className="w-8 h-8 text-white" />
                            </div>
                            <div>
                                <h3 className="text-2xl font-black text-white">최종 대출 실행 확인</h3>
                                <p className="text-emerald-100 mt-1 font-medium">
                                    계약 서류 동의를 마치고 대출을 실행하시겠습니까?
                                </p>
                            </div>
                        </div>

                        <div className="p-10 space-y-8">
                            <div className="bg-slate-50 rounded-3xl p-8 space-y-5 border border-slate-100">
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">고객 성명</span>
                                    <span className="text-lg font-black text-slate-900">{loanData.userName}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">선택 상품</span>
                                    <span className="text-lg font-black text-slate-900">{product.name}</span>
                                </div>
                                
                                <div className="h-px bg-slate-200/50" />

                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">대출 실행 금액</span>
                                    <span className="text-2xl font-black text-emerald-600">₩ {formatAmount(product.limit)}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">적용 금리</span>
                                    <span className="text-lg font-black text-slate-900">{product.rate}% (고정)</span>
                                </div>

                                <div className="flex justify-between items-center pt-5 border-t border-slate-200/50">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">대출금 입금 계좌</span>
                                    <div className="text-right">
                                        <p className="text-lg font-black text-slate-900">{loanData.bank}</p>
                                        <p className="text-sm font-bold text-emerald-600 font-mono">{loanData.accountNo}</p>
                                    </div>
                                </div>
                            </div>
                            
                            <div className="flex items-start gap-3 px-2">
                                <Info className="w-5 h-5 text-slate-400 shrink-0 mt-0.5" />
                                <p className="text-xs text-slate-500 leading-relaxed font-medium">
                                    '확인 완료' 버튼을 누르면 대출이 즉시 실행되며, 지정하신 계좌로 대출금이 입금됩니다. 실행 후에는 취소가 불가하오니 신중히 확인해 주세요.
                                </p>
                            </div>
                        </div>

                        {/* 버튼 */}
                        <div className="px-10 pb-10 grid grid-cols-2 gap-4">
                            <Button
                                onClick={() => setIsExecutionConfirmOpen(false)}
                                variant="secondary"
                                size="xl"
                                className="h-16 rounded-2xl bg-slate-100 text-slate-600 hover:bg-slate-200"
                            >
                                수정하기
                            </Button>
                            <Button
                                onClick={handleActualExecution}
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
        </div>
    );
};

export default LoanContractForm;
