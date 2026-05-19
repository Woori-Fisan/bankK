import React from 'react';
import { CheckCircle2, Printer, ChevronRight } from 'lucide-react';

interface LoanResultProps {
    loanData: any;
    product: any;
    evaluationResult: any;
    onReset: () => void;
}

const LoanResult: React.FC<LoanResultProps> = ({ loanData, product, evaluationResult, onReset }) => {
    return (
        <div className="flex flex-col items-center justify-center py-10 min-h-[600px]">
            <div className="bg-white border border-gray-200 rounded-3xl shadow-2xl w-full max-w-lg p-10 text-center animate-in zoom-in duration-500">
                <div className="w-20 h-20 bg-emerald-50 rounded-full flex items-center justify-center mx-auto mb-6">
                    <CheckCircle2 className="w-10 h-10 text-emerald-500" />
                </div>
                
                <h2 className="text-2xl font-bold text-gray-900 mb-2">대출 실행이 완료되었습니다</h2>
                <p className="text-sm text-gray-500 mb-10">요청하신 대출 내역이 성공적으로 원장에 반영되었습니다.</p>

                <div className="bg-gray-50 rounded-2xl p-8 mb-8 border border-gray-100">
                    <p className="text-xs text-gray-500 mb-2 font-medium">대출금 (Loan Amount)</p>
                    <p className="text-4xl font-bold text-gray-900">₩ {product.limit.toLocaleString()}</p>
                </div>

                <div className="space-y-4 px-2">
                    <div className="flex justify-between items-center text-sm border-b border-gray-50 pb-3">
                        <span className="text-gray-400 font-medium">신청인</span>
                        <span className="text-gray-900 font-bold">{loanData.userName || '(주)글로벌테크'}</span>
                    </div>
                    <div className="flex justify-between items-center text-sm border-b border-gray-50 pb-3">
                        <span className="text-gray-400 font-medium">입금 계좌</span>
                        <span className="text-gray-900 font-bold">우리은행 100-***-***920</span>
                    </div>
                    <div className="flex justify-between items-center text-sm border-b border-gray-50 pb-3">
                        <span className="text-gray-400 font-medium">금리</span>
                        <span className="text-gray-900 font-bold">{product.rate}% (고정 금리)</span>
                    </div>
                    <div className="flex justify-between items-center text-sm border-b border-gray-50 pb-3">
                        <span className="text-gray-400 font-medium">월 상환금</span>
                        <span className="text-gray-900 font-bold">2,840,277원</span>
                    </div>
                    <div className="flex justify-between items-center text-sm">
                        <span className="text-gray-400 font-medium">최종 상환일</span>
                        <span className="text-gray-900 font-bold">2028-05-19</span>
                    </div>
                </div>

                <div className="flex gap-3 mt-12">
                    <button className="flex-1 py-3.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-sm hover:bg-gray-50 transition-colors flex items-center justify-center gap-2">
                        <Printer className="w-4 h-4" />
                        영수증 출력
                    </button>
                    <button 
                        onClick={onReset}
                        className="flex-1 py-3.5 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200"
                    >
                        목록으로 돌아가기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LoanResult;
