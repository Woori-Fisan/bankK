import React from 'react';
import { CheckCircle2, XCircle, Printer } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { LoanData, LoanProduct, EvaluationResult } from '../../pages/LoanApplication';

interface LoanResultProps {
    loanData: LoanData;
    product: LoanProduct | null;
    evaluationResult: EvaluationResult | null;
    onReset: () => void;
}

const LoanResult: React.FC<LoanResultProps> = ({ loanData, product, evaluationResult, onReset }) => {
    const navigate = useNavigate();

    if (!product || evaluationResult?.status === 'REJECTED') {
        return (
            <div>
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 w-full overflow-hidden animate-in zoom-in duration-500">
                    <div className="p-12 flex flex-col items-center text-center">
                        <div className="w-24 h-24 bg-red-50 rounded-full flex items-center justify-center mb-6">
                            <XCircle className="w-12 h-12 text-red-500" />
                        </div>
                        <h2 className="text-3xl font-black text-gray-900 tracking-tight mb-3">대출 심사가 거절되었습니다</h2>
                        <p className="text-base text-gray-500 mb-3">
                            {evaluationResult?.reason ?? '심사 조건을 충족하지 못했습니다.'}
                        </p>
                        <p className="text-base text-gray-400 mb-10">신청인: {loanData.userName}</p>
                        <button
                            onClick={() => { onReset(); navigate('/main'); }}
                            className="w-full max-w-sm py-5 bg-slate-900 text-white rounded-2xl font-black text-lg hover:bg-slate-800 active:scale-[0.98] transition-all"
                        >
                            처음으로 돌아가기
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    const maturityDate = new Date();
    maturityDate.setMonth(maturityDate.getMonth() + (product?.period || 0));
    const maturityDateString = maturityDate.toISOString().split('T')[0];

    const calculateMonthly = (limit: number, rate: number, p: number) => {
        const monthlyRate = (rate / 100) / 12;
        const numerator = limit * monthlyRate * Math.pow(1 + monthlyRate, p);
        const denominator = Math.pow(1 + monthlyRate, p) - 1;
        return Math.floor(numerator / denominator);
    };

    const amount = product?.executeAmount ?? product?.limit ?? 0;
    const monthlyAmount = product ? calculateMonthly(amount, product.rate, product.period || 12) : 0;

    return (
        <div>
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 w-full overflow-hidden animate-in zoom-in duration-500">
                {/* 완료 헤더 */}
                <div className="px-12 pt-12 pb-8 flex flex-col items-center text-center border-b border-gray-100">
                    <div className="w-20 h-20 bg-emerald-50 rounded-full flex items-center justify-center mb-5">
                        <CheckCircle2 className="w-10 h-10 text-emerald-500" />
                    </div>
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight mb-2">대출 실행이 완료되었습니다</h2>
                    <p className="text-base text-gray-500 font-medium">요청하신 대출 내역이 성공적으로 원장에 반영되었습니다.</p>
                </div>

                {/* 금액 강조 */}
                <div className="px-12 py-10 border-b border-gray-100 bg-gray-50 text-center">
                    <p className="text-sm text-gray-500 mb-2 font-medium">대출금 (Loan Amount)</p>
                    <p className="text-5xl font-black text-gray-900 tracking-tight">₩ {amount.toLocaleString() || '0'}</p>
                </div>

                {/* 상세 정보 */}
                <div className="px-12 py-8">
                    <div className="grid grid-cols-2 gap-x-12 gap-y-5">
                        <div className="flex justify-between items-center py-3 border-b border-gray-100">
                            <span className="text-base text-gray-400 font-medium">신청인</span>
                            <span className="text-base text-gray-900 font-bold">{loanData.userName}</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-gray-100">
                            <span className="text-base text-gray-400 font-medium">금리</span>
                            <span className="text-base text-gray-900 font-bold">{product?.rate || '0'}% (고정)</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-gray-100">
                            <span className="text-base text-gray-400 font-medium">입금 계좌</span>
                            <span className="text-base text-gray-900 font-bold">{loanData.bank} {loanData.accountNo?.slice(0, 3)}-***-***{loanData.accountNo?.slice(-3)}</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-gray-100">
                            <span className="text-base text-gray-400 font-medium">월 상환금</span>
                            <span className="text-base text-gray-900 font-bold">{monthlyAmount.toLocaleString()}원</span>
                        </div>
                        <div className="flex justify-between items-center py-3 col-span-2">
                            <span className="text-base text-gray-400 font-medium">최종 상환일</span>
                            <span className="text-base text-gray-900 font-bold">{maturityDateString}</span>
                        </div>
                    </div>
                </div>

                {/* 버튼 */}
                <div className="px-12 pb-12 grid grid-cols-2 gap-4">
                    <button className="py-5 bg-gray-100 text-gray-600 rounded-2xl font-black text-lg hover:bg-gray-200 active:scale-[0.98] transition-all flex items-center justify-center gap-2">
                        <Printer className="w-5 h-5" />
                        영수증 출력
                    </button>
                    <button
                        onClick={() => { onReset(); navigate('/main'); }}
                        className="py-5 bg-slate-900 text-white rounded-2xl font-black text-lg hover:bg-slate-800 active:scale-[0.98] transition-all shadow-lg shadow-slate-200"
                    >
                        메인 페이지로 돌아가기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LoanResult;
