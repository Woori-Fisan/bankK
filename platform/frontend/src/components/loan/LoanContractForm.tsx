import React, { useState } from 'react';
import { FileText, Receipt, Info, ChevronLeft, ChevronRight, X, Globe } from 'lucide-react';

interface LoanContractFormProps {
    product: any;
    onNext: () => void;
    onBack: () => void;
}

const LoanContractForm: React.FC<LoanContractFormProps> = ({ product, onNext, onBack }) => {
    const [terms, setTerms] = useState([
        { id: 'C001', name: '대출상품 핵심 설명서', required: true, agreed: false },
        { id: 'C002', name: '대출거래약정서', required: true, agreed: false },
        { id: 'C003', name: '은행여신거래기본약관', required: true, agreed: false },
        { id: 'C004', name: '개인(신용)정보 수집·이용·제공 동의서', required: true, agreed: false },
        { id: 'C005', name: '대출거래 추가약정서', required: false, agreed: false },
    ]);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTerm, setActiveTerm] = useState<any>(null);

    const handleTermToggle = (id: string) => {
        setTerms(prev => prev.map(t => t.id === id ? { ...t, agreed: !t.agreed } : t));
    };

    const openModal = (term: any) => {
        setActiveTerm(term);
        setIsModalOpen(true);
    };

    const handleModalAgree = () => {
        if (activeTerm) {
            handleTermToggle(activeTerm.id);
        }
        setIsModalOpen(false);
    };

    const isNextDisabled = terms.filter(t => t.required).some(t => !t.agreed);

    const agreedCount = terms.filter(t => t.agreed).length;

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                <div className="lg:col-span-8 space-y-6">
                    <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden shadow-sm">
                        <div className="p-5 border-b border-gray-100 bg-gray-50/50 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <FileText className="w-5 h-5 text-gray-400" />
                                <h3 className="text-sm font-bold text-gray-900">계약 서류 목록</h3>
                            </div>
                            <span className="px-2 py-1 bg-emerald-50 text-emerald-600 text-[10px] font-bold rounded-full">
                                {agreedCount}/{terms.length} 확인
                            </span>
                        </div>
                        <div className="p-5 space-y-3">
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">필수 동의</p>
                            {terms.filter(t => t.required).map(term => (
                                <div key={term.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl border border-gray-100 hover:border-blue-200 transition-colors">
                                    <input 
                                        type="checkbox" 
                                        checked={term.agreed} 
                                        onChange={() => handleTermToggle(term.id)}
                                        className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                    />
                                    <span className="flex-1 text-xs font-medium text-gray-900">{term.name}</span>
                                    <span className="px-1.5 py-0.5 bg-red-50 text-red-600 text-[9px] font-bold rounded">필수</span>
                                    <button 
                                        onClick={() => openModal(term)}
                                        className="px-3 py-1 bg-white border border-gray-200 text-blue-600 text-[10px] font-bold rounded-lg hover:bg-blue-50 transition-colors"
                                    >
                                        내용 보기
                                    </button>
                                </div>
                            ))}

                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mt-6 mb-2">선택 동의</p>
                            {terms.filter(t => !t.required).map(term => (
                                <div key={term.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl border border-gray-100 hover:border-blue-200 transition-colors">
                                    <input 
                                        type="checkbox" 
                                        checked={term.agreed} 
                                        onChange={() => handleTermToggle(term.id)}
                                        className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                    />
                                    <span className="flex-1 text-xs font-medium text-gray-900">{term.name}</span>
                                    <span className="px-1.5 py-0.5 bg-blue-50 text-blue-600 text-[9px] font-bold rounded">선택</span>
                                    <button 
                                        onClick={() => openModal(term)}
                                        className="px-3 py-1 bg-white border border-gray-200 text-blue-600 text-[10px] font-bold rounded-lg hover:bg-blue-50 transition-colors"
                                    >
                                        내용 보기
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-4 flex gap-3">
                        <Info className="w-5 h-5 text-emerald-500 shrink-0" />
                        <p className="text-[11px] text-emerald-700 leading-relaxed">
                            <strong>내용 보기</strong> 클릭 시 CDN(terms_url)에서 약관 내용을 iframe으로 불러옵니다. 필수 항목을 모두 확인하고 동의해야 다음 단계로 진행할 수 있습니다.
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-4">
                    <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm sticky top-6">
                        <div className="flex items-center gap-2 mb-6 pb-4 border-b border-gray-100">
                            <Receipt className="w-5 h-5 text-gray-400" />
                            <h3 className="text-sm font-bold text-gray-900">선택 상품 요약</h3>
                        </div>
                        <div className="space-y-4">
                            <div className="space-y-1">
                                <p className="text-[10px] text-gray-400 font-bold uppercase">상품명</p>
                                <p className="text-xs font-bold text-gray-900">{product.name}</p>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-1">
                                    <p className="text-[10px] text-gray-400 font-bold uppercase">대출 금액</p>
                                    <p className="text-xs font-bold text-gray-900">₩ {(product.limit / 10000).toLocaleString()}만원</p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-[10px] text-gray-400 font-bold uppercase">적용 금리</p>
                                    <p className="text-xs font-bold text-gray-900">{product.rate}% (고정)</p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-[10px] text-gray-400 font-bold uppercase">대출 기간</p>
                                    <p className="text-xs font-bold text-gray-900">{product.period}개월</p>
                                </div>
                            </div>
                            <div className="pt-4 border-t border-gray-100">
                                <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">입금 계좌</p>
                                <p className="text-xs font-bold text-gray-900">우리은행 100-293-884920</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="flex justify-between pt-6 border-t border-gray-200">
                <button 
                    onClick={onBack}
                    className="flex items-center gap-2 px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-sm hover:bg-gray-50"
                >
                    <ChevronLeft className="w-4 h-4" />
                    이전으로
                </button>
                <button 
                    onClick={onNext}
                    disabled={isNextDisabled}
                    className={`flex items-center gap-2 px-8 py-3 rounded-xl font-bold text-sm transition-all shadow-lg ${
                        isNextDisabled 
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none' 
                        : 'bg-slate-900 text-white hover:bg-slate-800 shadow-slate-200'
                    }`}
                >
                    대출 실행하기
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>

            {/* iframe 모달 */}
            {isModalOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-3xl shadow-2xl w-full max-w-xl overflow-hidden animate-in fade-in zoom-in duration-200">
                        <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <FileText className="w-5 h-5 text-slate-400" />
                                <h3 className="text-sm font-bold">{activeTerm?.name}</h3>
                            </div>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white transition-colors">
                                <X className="w-6 h-6" />
                            </button>
                        </div>
                        <div className="h-[400px] bg-gray-50 flex flex-col items-center justify-center p-10 text-center">
                            <div className="w-16 h-16 bg-white border border-gray-200 rounded-2xl flex items-center justify-center mb-4 text-gray-300">
                                <Globe className="w-8 h-8" />
                            </div>
                            <h4 className="text-sm font-bold text-gray-900 mb-2">CDN에서 약관 내용을 불러옵니다</h4>
                            <p className="text-[11px] text-gray-500 mb-6">
                                terms_url: https://cdn.bank.com/terms/{activeTerm?.id}/v1.0
                            </p>
                            <div className="p-4 bg-blue-50 text-blue-600 rounded-xl text-[11px] font-medium max-w-sm">
                                ← iframe으로 실제 HTML 약관 페이지 렌더링을 시뮬레이션합니다. 실제 구현 시에는 &lt;iframe src=&#123;terms_url&#125; /&gt; 을 사용합니다.
                            </div>
                        </div>
                        <div className="p-5 border-t border-gray-100 flex justify-end gap-3">
                            <button 
                                onClick={() => setIsModalOpen(false)}
                                className="px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-xs"
                            >
                                닫기
                            </button>
                            <button 
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

export default LoanContractForm;
