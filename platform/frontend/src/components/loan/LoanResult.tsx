import React, { useRef } from 'react';
import { CheckCircle2, XCircle, Printer, Info, Receipt } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import jsPDF from 'jspdf';
import { toPng } from 'html-to-image';
import type { EvaluationResult, LoanData, LoanProduct } from '../../pages/LoanApplication';
import type { ExecutionResponse } from '../../api/loanApi';
import LoanReceiptDocument from './LoanReceiptDocument';
import { formatAmount, formatDate } from '../../utils/formatter';
import Card from '../common/Card';
import { Button } from '../common/Button';

interface LoanResultProps {
    loanData: LoanData;
    product: LoanProduct | null;
    evaluationResult: EvaluationResult | null;
    executionResult?: ExecutionResponse | null;
    onReset: () => void;
}

const LoanResult: React.FC<LoanResultProps> = ({ loanData, product, evaluationResult, executionResult, onReset }) => {
    const navigate = useNavigate();
    const receiptDocRef = useRef<HTMLDivElement>(null);

    const isRejected = !product || evaluationResult?.status === 'REJECTED';

    const handleHome = () => {
        onReset();
        navigate('/main');
    };

    const amount = executionResult?.executeAmount ?? product?.executeAmount ?? product?.limit ?? 0;
    
    // 월 상환금 계산 로직
    const monthlyAmount = executionResult?.monthlyPayment ?? (() => {
        const limit = amount;
        const rate = product?.rate ?? 0;
        const p = product?.period || 12;
        const monthlyRate = (rate / 100) / 12;
        const numerator = limit * monthlyRate * Math.pow(1 + monthlyRate, p);
        const denominator = Math.pow(1 + monthlyRate, p) - 1;
        return Math.floor(numerator / denominator);
    })();

    // 만기일 포맷팅
    const maturityDateString = executionResult?.maturityDate 
        ? formatDate(executionResult.maturityDate, false, 'dot')
        : (() => {
            const d = new Date();
            d.setMonth(d.getMonth() + (product?.period || 0));
            return formatDate(d.toISOString(), false, 'dot');
        })();

    const handlePrintReceipt = async () => {
        if (!receiptDocRef.current) return;
        try {
            const imgData = await toPng(receiptDocRef.current, { pixelRatio: 2 });
            const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
            pdf.addImage(imgData, 'PNG', 0, 0, 210, (receiptDocRef.current.offsetHeight * 210) / receiptDocRef.current.offsetWidth);
            pdf.save(`대출실행확인서_${loanData.userName}_${new Date().toISOString().slice(0,10)}.pdf`);
        } catch {
            alert('PDF 생성에 실패했습니다.');
        }
    };

    if (isRejected) {
        return (
            <div className="w-full max-w-7xl mx-auto animate-in fade-in zoom-in-95 duration-500 pb-10">
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                    {/* 좌측: 거절 안내 (8컬럼) */}
                    <div className="lg:col-span-8">
                        <Card padding="xl" className="border-slate-100 shadow-sm h-full flex flex-col items-center justify-center text-center py-20">
                            <div className="w-20 h-20 bg-rose-50 rounded-full flex items-center justify-center mb-6">
                                <XCircle className="w-10 h-10 text-rose-500" />
                            </div>
                            <div className="space-y-3">
                                <h2 className="text-3xl font-black text-slate-900 tracking-tight">대출 심사가 거절되었습니다</h2>
                                <p className="text-slate-500 font-medium text-lg">{evaluationResult?.reason ?? '심사 조건을 충족하지 못했습니다.'}</p>
                            </div>
                        </Card>
                    </div>

                    {/* 우측: 요약 및 액션 (4컬럼) */}
                    <div className="lg:col-span-4">
                        <Card padding="xl" className="border-slate-100 shadow-sm flex flex-col justify-between h-full min-h-[400px]">
                            <div className="space-y-8">
                                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block mb-2">신청 정보</span>
                                <div className="space-y-4">
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm font-bold text-slate-400">신청인</span>
                                        <span className="text-base font-black text-slate-900">{loanData.userName}</span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm font-bold text-slate-400">신청 상품</span>
                                        <span className="text-base font-black text-slate-900">{product?.name || '신용 대출'}</span>
                                    </div>
                                    <div className="pt-4 border-t border-slate-50">
                                        <p className="text-[10px] font-bold text-rose-500 uppercase mb-1">상태</p>
                                        <p className="text-lg font-black text-rose-600">심사 거절</p>
                                    </div>
                                </div>
                            </div>

                            <Button variant="primary" size="xl" fullWidth onClick={handleHome} className="h-16 rounded-2xl font-black text-lg shadow-lg">
                                메인 페이지로 돌아가기
                            </Button>
                        </Card>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 animate-in fade-in zoom-in-95 duration-500 pb-10">
            {/* 숨김 PDF 문서 템플릿 */}
            {executionResult && (
                <LoanReceiptDocument
                    innerRef={receiptDocRef}
                    loanId={executionResult.loanNo}
                    borrowerName={executionResult.borrowerName ?? loanData.userName ?? ''}
                    executeAmount={executionResult.executeAmount}
                    interestRate={executionResult.interestRate}
                    repaymentPeriod={executionResult.repaymentPeriod}
                    monthlyPayment={executionResult.monthlyPayment}
                    repaymentStartDate={executionResult.startDate}
                    maturityDate={executionResult.maturityDate}
                    bank={loanData.bank ?? ''}
                    accountNo={loanData.accountNo ?? ''}
                    productName={product?.name ?? ''}
                />
            )}

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                {/* 좌측: 성공 안내 및 상세 정보 (8컬럼) */}
                <div className="lg:col-span-8">
                    <Card padding="xl" className="border-slate-100 shadow-sm h-full">
                        {/* 완료 헤더 */}
                        <div className="flex flex-col items-center text-center space-y-6 mb-12">
                            <div className="w-20 h-20 bg-emerald-50 rounded-full flex items-center justify-center">
                                <CheckCircle2 className="w-10 h-10 text-emerald-500" />
                            </div>
                            <div className="space-y-2">
                                <h2 className="text-3xl font-black text-slate-900 tracking-tight">대출 실행이 완료되었습니다</h2>
                                <p className="text-slate-500 font-medium text-base">요청하신 대출금이 지정된 계좌로 안전하게 입금되었습니다.</p>
                            </div>
                        </div>

                        {/* 상세 정보 테이블 */}
                        <div className="space-y-6">
                            <div className="flex items-center gap-2 text-slate-800">
                                <Receipt className="w-5 h-5 text-emerald-600" />
                                <h3 className="text-lg font-bold">대출 실행 상세 정보</h3>
                            </div>

                            <div className="bg-slate-50/50 rounded-3xl border border-slate-100 divide-y divide-slate-100 overflow-hidden">
                                <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
                                    <div className="p-6 space-y-1">
                                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">신청인</p>
                                        <p className="text-sm font-bold text-slate-900">{executionResult?.borrowerName ?? loanData.userName}</p>
                                    </div>
                                    <div className="p-6 space-y-1">
                                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">적용 금리</p>
                                        <p className="text-sm font-bold text-slate-900">{executionResult?.interestRate ?? product?.rate ?? '0'}% <span className="text-xs font-medium text-slate-400 ml-1">(고정금리)</span></p>
                                    </div>
                                </div>
                                <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
                                    <div className="p-6 space-y-1">
                                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">입금 계좌</p>
                                        <p className="text-sm font-bold text-slate-900">{loanData.bank} <span className="font-mono text-emerald-600">{loanData.accountNo?.slice(0, 3)}-***-***{loanData.accountNo?.slice(-3)}</span></p>
                                    </div>
                                    <div className="p-6 space-y-1">
                                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">대출 만기일</p>
                                        <p className="text-sm font-bold text-slate-600">{maturityDateString}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="flex items-start gap-3 px-2">
                                <Info className="w-5 h-5 text-slate-400 shrink-0 mt-0.5" />
                                <p className="text-xs text-slate-500 leading-relaxed font-medium">
                                    대출 원리금 상환은 지정된 계좌를 통해 매월 자동이체로 처리됩니다. 상세한 상환 스케줄은 관리 페이지의 '대출 계좌 조회' 메뉴를 통해 확인하실 수 있습니다.
                                </p>
                            </div>
                        </div>
                    </Card>
                </div>

                {/* 우측: 요약 및 액션 (4컬럼) */}
                <div className="lg:col-span-4 space-y-6">
                    <Card padding="xl" className="border-slate-100 shadow-sm text-center h-full flex flex-col justify-between">
                        <div className="space-y-8">
                            <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-widest block mb-4">최종 대출 실행 금액</span>
                                <div className="flex items-baseline justify-center gap-2">
                                    <span className="text-xl font-bold text-slate-400">₩</span>
                                    <span className="text-5xl font-black text-slate-900 tracking-tighter">
                                        {formatAmount(amount)}
                                    </span>
                                </div>
                            </div>

                            <div className="pt-8 border-t border-slate-50 space-y-4">
                                <div className="flex justify-between items-center text-sm">
                                    <span className="text-slate-400 font-medium">매월 상환금</span>
                                    <span className="text-slate-900 font-black">₩ {formatAmount(monthlyAmount)}</span>
                                </div>
                                <div className="flex justify-between items-center text-sm">
                                    <span className="text-slate-400 font-medium">상태</span>
                                    <span className="text-emerald-600 font-black">실행 완료</span>
                                </div>
                            </div>
                        </div>

                        <div className="space-y-3 mt-12">
                            <Button
                                variant="outline"
                                size="xl"
                                fullWidth
                                onClick={handlePrintReceipt}
                                className="rounded-2xl h-14 text-base font-bold gap-2 border-slate-200"
                            >
                                <Printer className="w-5 h-5" />
                                실행 확인서 출력
                            </Button>
                            <Button
                                variant="primary"
                                size="xl"
                                fullWidth
                                onClick={handleHome}
                                className="rounded-2xl h-14 text-base font-bold shadow-lg shadow-slate-200"
                            >
                                메인 페이지로 돌아가기
                            </Button>
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    );
};

export default LoanResult;
