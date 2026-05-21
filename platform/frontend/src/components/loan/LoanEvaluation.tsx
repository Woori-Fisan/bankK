import React, { useEffect } from 'react';
import { Loader2, CheckCircle2, XCircle, Info, ChevronRight } from 'lucide-react';
import type { LoanData, EvaluationResult, LoanProduct } from '../../pages/LoanApplication';
import { useEvaluationSSE, extractApiError } from '../../hooks/useLoan';
import { formatAmount } from '../../utils/formatter';

interface LoanEvaluationProps {
    loanData: LoanData;
    applicationId: string;
    onApproved: (result: EvaluationResult) => void;
    onRejected: (reason: string) => void;
}

const LoanEvaluation: React.FC<LoanEvaluationProps> = ({
    applicationId,
    onApproved,
    onRejected,
}) => {
    const { data, error } = useEvaluationSSE(applicationId);

    useEffect(() => {
        if (!data) return;
        if (data.evaluationStatus === 'APPROVED') {
            const minRate = Math.min(...(data.availableProducts ?? []).map(x => x.interestRate));
            const products: LoanProduct[] = (data.availableProducts ?? []).map((p, i) => ({
                id: i + 1,
                loanProductCode: p.loanProductCode,
                name: p.loanProductName,
                rate: p.interestRate,
                limit: p.maxAmount,
                tags: p.interestRate === minRate ? ['최저금리'] : [],
                period: p.loanPeriodMonths,
            }));
            onApproved({
                status: 'APPROVED',
                limit: data.approvedLimit ?? undefined,
                evaluationId: data.evaluationId ?? undefined,
                products,
            });
        } else if (data.evaluationStatus === 'REJECTED' || data.evaluationStatus === 'FAILED') {
            onRejected(data.rejectionMessage ?? '심사 거절');
        }
    }, [data]);

    if (error) {
        return (
            <div className="flex flex-col items-center justify-center py-20 bg-white border border-gray-200 rounded-3xl">
                <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mb-6">
                    <XCircle className="w-8 h-8 text-red-500" />
                </div>
                <h2 className="text-xl font-bold text-gray-900 mb-2">심사 상태 조회 중 오류가 발생했습니다</h2>
                <p className="text-sm text-red-500 mb-8">{error instanceof Error ? error.message : extractApiError(error)}</p>
                <button
                    type="button"
                    onClick={() => onRejected('심사 상태 조회 실패')}
                    className="px-8 py-3 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors"
                >
                    목록으로 돌아가기
                </button>
            </div>
        );
    }

    if (!data || data.evaluationStatus === 'PENDING') {
        return (
            <div className="flex flex-col items-center justify-center py-20 bg-white border border-gray-200 rounded-3xl">
                <div className="relative mb-6">
                    <Loader2 className="w-16 h-16 text-emerald-500 animate-spin" />
                    <div className="absolute inset-0 flex items-center justify-center">
                        <div className="w-8 h-8 bg-emerald-50 rounded-full" />
                    </div>
                </div>
                <h2 className="text-xl font-bold text-gray-900 mb-2">심사 중입니다</h2>
                <p className="text-sm text-gray-500 text-center max-w-xs mb-6">
                    잠시만 기다려 주세요. 심사 결과를 자동으로 조회합니다.
                    <br />
                    <span className="text-[11px] opacity-70">Application ID: {applicationId}</span>
                </p>
                <div className="flex gap-1.5 mb-6">
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse delay-75" />
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse delay-150" />
                </div>
                <p className="text-[11px] text-gray-400 font-medium">심사 결과를 기다리는 중...</p>
            </div>
        );
    }

    if (data.evaluationStatus === 'REJECTED' || data.evaluationStatus === 'FAILED') {
        return (
            <div className="flex flex-col items-center justify-center py-20 bg-white border border-gray-200 rounded-3xl">
                <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mb-6">
                    <XCircle className="w-8 h-8 text-red-500" />
                </div>
                <h2 className="text-xl font-bold text-gray-900 mb-2">대출 심사가 거절되었습니다</h2>
                <p className="text-sm text-gray-500 mb-8 tracking-tight">Application ID: {applicationId}</p>
                <div className="w-full max-w-sm bg-red-50 border border-red-100 rounded-2xl p-6 text-left">
                    <h3 className="text-xs font-bold text-red-600 mb-3 uppercase tracking-wider">거절 사유</h3>
                    <p className="text-sm text-red-800 font-medium leading-relaxed">
                        {data.rejectionMessage ?? '심사 거절'}
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => onRejected(data.rejectionMessage ?? '심사 거절')}
                    className="mt-10 px-8 py-3 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors"
                >
                    목록으로 돌아가기
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="bg-white border border-gray-200 rounded-2xl p-6">
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h2 className="text-xl font-bold text-gray-900">대출 심사 결과</h2>
                        <p className="text-[11px] text-gray-500 mt-1 uppercase tracking-tight">
                            Application ID: {applicationId}
                        </p>
                    </div>
                    <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 text-emerald-600 rounded-full border border-emerald-100">
                        <CheckCircle2 className="w-4 h-4" />
                        <span className="text-xs font-bold">승인</span>
                    </div>
                </div>

                <div className="bg-gray-50 rounded-2xl p-8 text-center mb-6 border border-gray-100">
                    <p className="text-xs text-gray-500 mb-2">최종 승인 한도</p>
                    <p className="text-4xl font-bold text-gray-900">
                        ₩ {formatAmount(data.approvedLimit ?? 0)}
                    </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">최저 금리</p>
                        <p className="text-lg font-bold text-gray-900">
                            {data.availableProducts && data.availableProducts.length > 0
                                ? `${Math.min(...data.availableProducts.map(p => p.interestRate))}%`
                                : '-'}
                        </p>
                        <p className="text-[10px] text-gray-500">상품별 상이 / 연</p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">심사 완료</p>
                        <p className="text-sm font-bold text-gray-900">
                            {data.completedAt ? new Date(data.completedAt).toLocaleString('ko-KR') : '-'}
                        </p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">심사 ID</p>
                        <p className="text-sm font-bold text-gray-900 break-all">
                            {data.evaluationId ?? '-'}
                        </p>
                    </div>
                </div>

                <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-5">
                    <div className="flex items-center gap-2 text-emerald-700 font-bold text-xs mb-4">
                        <Info className="w-4 h-4" />
                        다음 단계 안내
                    </div>
                    <div className="space-y-4">
                        {[
                            { step: 1, title: '추천 상품 선택', desc: '심사 결과 기준 추천 상품 목록에서 상품을 선택합니다.' },
                            { step: 2, title: '계약 서류 확인 및 동의', desc: '선택한 상품의 약관 및 계약 서류를 확인하고 동의합니다.' },
                            { step: 3, title: '대출 실행', desc: '동의 완료 후 대출금이 지정 계좌로 입금됩니다.' },
                        ].map((item) => (
                            <div key={item.step} className="flex gap-3">
                                <div className="w-5 h-5 bg-emerald-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center shrink-0">
                                    {item.step}
                                </div>
                                <div>
                                    <p className="text-xs font-bold text-gray-900 mb-0.5">{item.title}</p>
                                    <p className="text-[10px] text-gray-500">{item.desc}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div className="flex justify-end pt-6 border-t border-gray-200">
                <button
                    type="button"
                    onClick={() => {
                        const minRate = Math.min(...(data.availableProducts ?? []).map(x => x.interestRate));
                        const products: LoanProduct[] = (data.availableProducts ?? []).map((p, i) => ({
                            id: i + 1,
                            loanProductCode: p.loanProductCode,
                            name: p.loanProductName,
                            rate: p.interestRate,
                            limit: p.maxAmount,
                            tags: p.interestRate === minRate ? ['최저금리'] : [],
                            period: p.loanPeriodMonths,
                        }));
                        onApproved({
                            status: 'APPROVED',
                            limit: data.approvedLimit ?? undefined,
                            evaluationId: data.evaluationId ?? undefined,
                            products,
                        });
                    }}
                    className="flex items-center gap-2 px-6 py-3 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200"
                >
                    상품 선택하기
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
};

export default LoanEvaluation;
