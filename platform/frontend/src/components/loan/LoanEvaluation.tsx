import React, { useEffect } from 'react';
import { Loader2, CheckCircle2, XCircle, Info, ChevronRight } from 'lucide-react';
import type { LoanData, EvaluationResult, LoanProduct } from '../../pages/LoanApplication';
import type { EvaluationStatusResponse } from '../../api/loanApi';
import { formatAmount } from '../../utils/formatter';

interface LoanEvaluationProps {
    loanData: LoanData;
    sseData: EvaluationStatusResponse | null;
    sseError: Error | null;
    onApproved: (result: EvaluationResult) => void;
    onRejected: (reason: string) => void;
}

const LoanEvaluation: React.FC<LoanEvaluationProps> = ({
    sseData,
    sseError,
    onApproved,
    onRejected,
}) => {
    const isRejected = sseData?.evaluationStatus === 'REJECTED' ||
                       sseData?.evaluationStatus === 'SYSTEM_ERROR';

    useEffect(() => {
        if (!sseData) return;
        if (isRejected) {
            onRejected(
                sseData.rejectionMessage ??
                (sseData.evaluationStatus === 'SYSTEM_ERROR'
                    ? '은행 내부 오류가 발생했습니다. IT 지원팀에 문의해주세요.'
                    : '심사 거절'),
            );
        }
    }, [sseData]);

    if (sseError) {
        return (
            <div className="flex flex-col items-center justify-center py-28 bg-white rounded-2xl shadow-sm border border-gray-100">
                <div className="w-28 h-28 bg-red-50 rounded-full flex items-center justify-center mb-6">
                    <XCircle className="w-14 h-14 text-red-500" />
                </div>
                <h2 className="text-3xl font-black text-gray-900 tracking-tight mb-3">심사 상태 조회 중 오류가 발생했습니다</h2>
                <p className="text-base text-red-500 mb-8">{sseError.message}</p>
                <button
                    type="button"
                    onClick={() => onRejected('심사 상태 조회 실패')}
                    className="px-12 py-5 bg-slate-900 text-white rounded-2xl font-black text-lg hover:bg-slate-800 active:scale-[0.98] transition-all"
                >
                    목록으로 돌아가기
                </button>
            </div>
        );
    }

    if (!sseData) {
        return (
            <div className="flex flex-col items-center justify-center py-28 bg-white rounded-2xl shadow-sm border border-gray-100">
                <div className="relative mb-8">
                    <Loader2 className="w-28 h-28 text-emerald-500 animate-spin" />
                    <div className="absolute inset-0 flex items-center justify-center">
                        <div className="w-14 h-14 bg-emerald-50 rounded-full" />
                    </div>
                </div>
                <h2 className="text-3xl font-bold text-gray-900 mb-3">심사 중입니다</h2>
                <p className="text-base text-gray-500 text-center max-w-xs mb-6">
                    잠시만 기다려 주세요. 심사 결과를 자동으로 조회합니다.
                </p>
                <div className="flex gap-1.5 mb-6">
                    <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
                    <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse delay-75" />
                    <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse delay-150" />
                </div>
                <p className="text-sm text-gray-400 font-medium">심사 결과를 기다리는 중...</p>
            </div>
        );
    }

    if (isRejected) {
        return (
            <div className="flex flex-col items-center justify-center py-28 bg-white rounded-2xl shadow-sm border border-gray-100">
                <div className="w-28 h-28 bg-red-50 rounded-full flex items-center justify-center mb-6">
                    <XCircle className="w-14 h-14 text-red-500" />
                </div>
                <h2 className="text-3xl font-black text-gray-900 tracking-tight mb-3">
                    {sseData!.evaluationStatus === 'SYSTEM_ERROR'
                        ? '은행 시스템 오류가 발생했습니다'
                        : '대출 심사가 거절되었습니다'}
                </h2>
                <div className="w-full max-w-sm bg-red-50 border border-red-100 rounded-2xl p-6 text-left mt-4">
                    <h3 className="text-sm font-bold text-red-600 mb-3 uppercase tracking-wider">사유</h3>
                    <p className="text-base text-red-800 font-medium leading-relaxed">
                        {sseData!.rejectionMessage ??
                            (sseData!.evaluationStatus === 'SYSTEM_ERROR'
                                ? '은행 내부 오류가 발생했습니다. IT 지원팀에 문의해주세요.'
                                : '심사 거절')}
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => onRejected(sseData!.rejectionMessage ?? '심사 거절')}
                    className="mt-10 px-12 py-5 bg-slate-900 text-white rounded-2xl font-black text-lg hover:bg-slate-800 active:scale-[0.98] transition-all"
                >
                    목록으로 돌아가기
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-10">
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h2 className="text-3xl font-bold text-gray-900">대출 심사 결과</h2>
                        <p className="text-base text-gray-500 mt-1.5 break-all">
                            대출 번호: {sseData!.evaluationId ?? '-'}
                        </p>
                    </div>
                    <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 text-emerald-600 rounded-full border border-emerald-100">
                        <CheckCircle2 className="w-5 h-5" />
                        <span className="text-sm font-bold">승인</span>
                    </div>
                </div>

                <div className="bg-gray-50 rounded-2xl p-10 text-center mb-6 border border-gray-100">
                    <p className="text-sm text-gray-500 mb-2 font-medium">최종 승인 한도</p>
                    <p className="text-5xl font-bold text-gray-900">
                        ₩ {formatAmount(sseData!.approvedLimit ?? 0)}
                    </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-6">
                        <p className="text-sm font-bold uppercase text-gray-400 mb-2">최저 금리</p>
                        <p className="text-2xl font-bold text-gray-900">
                            {sseData!.availableProducts && sseData!.availableProducts.length > 0
                                ? `${Math.min(...sseData!.availableProducts.map((p) => p.interestRate))}%`
                                : '-'}
                        </p>
                        <p className="text-sm text-gray-500 mt-1">상품별 상이 / 연</p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-6">
                        <p className="text-sm font-bold uppercase text-gray-400 mb-2">추천 상품 수</p>
                        <p className="text-2xl font-bold text-gray-900">
                            {sseData!.availableProducts?.length ?? 0}개
                        </p>
                    </div>
                </div>

                <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-6">
                    <div className="flex items-center gap-2 text-emerald-700 font-bold text-sm mb-5">
                        <Info className="w-5 h-5" />
                        다음 단계 안내
                    </div>
                    <div className="space-y-5">
                        {[
                            { step: 1, title: '추천 상품 선택', desc: '심사 결과 기준 추천 상품 목록에서 상품을 선택합니다.' },
                            { step: 2, title: '계약 서류 확인 및 동의', desc: '선택한 상품의 약관 및 계약 서류를 확인하고 동의합니다.' },
                            { step: 3, title: '대출 실행', desc: '동의 완료 후 대출금이 지정 계좌로 입금됩니다.' },
                        ].map((item) => (
                            <div key={item.step} className="flex gap-3">
                                <div className="w-7 h-7 bg-emerald-500 text-white text-sm font-bold rounded-full flex items-center justify-center shrink-0">
                                    {item.step}
                                </div>
                                <div>
                                    <p className="text-base font-bold text-gray-900 mb-0.5">{item.title}</p>
                                    <p className="text-sm text-gray-500">{item.desc}</p>
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
                        const products: LoanProduct[] = (sseData!.availableProducts ?? []).map((p, i) => ({
                            id: i + 1,
                            loanProductCode: p.loanProductCode,
                            name: p.loanProductName,
                            rate: p.interestRate,
                            limit: p.maxAmount,
                            tags: p.interestRate === Math.min(...(sseData!.availableProducts ?? []).map((x) => x.interestRate))
                                ? ['최저금리']
                                : [],
                            period: p.loanPeriodMonths,
                        }));
                        onApproved({
                            status: 'APPROVED',
                            limit: sseData!.approvedLimit ?? undefined,
                            evaluationId: sseData!.evaluationId ?? undefined,
                            products,
                        });
                    }}
                    className="flex items-center gap-2 px-12 py-5 bg-slate-900 text-white rounded-2xl font-black text-lg hover:bg-slate-800 active:scale-[0.98] transition-all shadow-lg shadow-slate-200"
                >
                    상품 선택하기
                    <ChevronRight className="w-5 h-5" />
                </button>
            </div>
        </div>
    );
};

export default LoanEvaluation;
