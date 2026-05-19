import React, { useState } from 'react';
import { AlertTriangle, ChevronLeft, ShieldCheck } from 'lucide-react';
import PinpadModal from '../pinpad/PinpadModal';
import { LoanData, LoanProduct } from '../../pages/LoanApplication';

interface LoanExecutionConfirmProps {
    loanData: LoanData;
    product: LoanProduct;
    onNext: () => void;
    onBack: () => void;
}

const LoanExecutionConfirm: React.FC<LoanExecutionConfirmProps> = ({ loanData, product, onNext, onBack }) => {
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);

    const handleExecution = () => {
        setIsPinpadOpen(true);
    };

    const handlePinComplete = (pin: string) => {
        // Mock API call
        console.log('Executing loan with pin:', pin);
        setIsPinpadOpen(false);
        onNext();
    };

    const maturityDate = new Date();
    maturityDate.setMonth(maturityDate.getMonth() + (product.period || 0));
    const maturityDateString = maturityDate.toISOString().split('T')[0];

    return (
        <div className="flex flex-col items-center justify-center min-h-[500px]">
            <div className="bg-white border border-gray-200 rounded-3xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in slide-in-from-bottom-4 duration-500">
                <div className="p-8 border-b border-gray-100">
                    <h2 className="text-xl font-bold text-gray-900 mb-1">대출 실행 최종 확인</h2>
                    <p className="text-sm text-gray-500">아래 내용을 최종 확인 후 대출을 실행해 주세요.</p>
                </div>

                <div className="p-8 space-y-6">
                    <div className="bg-gray-50 rounded-2xl p-6 space-y-4 border border-gray-100">
                        <div className="flex justify-between items-center">
                            <span className="text-[11px] text-gray-400 font-bold uppercase">신청인</span>
                            <span className="text-xs font-bold text-gray-900">{loanData.userName}</span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-[11px] text-gray-400 font-bold uppercase">상품명</span>
                            <span className="text-xs font-bold text-gray-900">{product.name}</span>
                        </div>
                        <div className="flex justify-between items-center pt-2 border-t border-gray-200">
                            <span className="text-[11px] text-gray-400 font-bold uppercase">대출 금액</span>
                            <span className="text-xl font-bold text-emerald-600">₩ {product.limit.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-[11px] text-gray-400 font-bold uppercase">적용 금리</span>
                            <span className="text-xs font-bold text-gray-900">{product.rate}% (고정)</span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-[11px] text-gray-400 font-bold uppercase">대출 기간</span>
                            <span className="text-xs font-bold text-gray-900">{product.period}개월</span>
                        </div>
                        <div className="flex justify-between items-center pt-2 border-t border-gray-200">
                            <span className="text-[11px] text-gray-400 font-bold uppercase">입금 계좌</span>
                            <span className="text-xs font-bold text-gray-900">{loanData.bank}은행 {loanData.accountNo}</span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-[11px] text-gray-400 font-bold uppercase">만기일</span>
                            <span className="text-xs font-bold text-gray-900">{maturityDateString}</span>
                        </div>
                    </div>

                    <div className="bg-amber-50 border border-amber-100 rounded-xl p-4 flex gap-3">
                        <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0" />
                        <p className="text-[11px] text-amber-700 leading-relaxed font-medium">
                            대출 실행 후에는 취소가 불가합니다. 위 내용을 다시 한번 확인해 주세요.
                        </p>
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button 
                            onClick={onBack}
                            className="flex-1 py-3.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-sm hover:bg-gray-50 transition-colors"
                        >
                            이전으로
                        </button>
                        <button 
                            onClick={handleExecution}
                            className="flex-[2] py-3.5 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200 flex items-center justify-center gap-2"
                        >
                            <ShieldCheck className="w-4 h-4" />
                            대출 실행
                        </button>
                    </div>
                </div>
            </div>

            <PinpadModal 
                isOpen={isPinpadOpen}
                onClose={() => setIsPinpadOpen(false)}
                onComplete={handlePinComplete}
                title="계좌 비밀번호 입력"
            />
        </div>
    );
};

export default LoanExecutionConfirm;
