import React, { useEffect } from 'react';
import { Loader2, CheckCircle2, XCircle, ChevronRight, FileText } from 'lucide-react';
import type { LoanData, EvaluationResult, LoanProduct } from '../../pages/LoanApplication';
import type { EvaluationStatusResponse } from '../../api/loanApi';
import { formatAmount } from '../../utils/formatter';
import Card from '../common/Card';
import { Button } from '../common/Button';

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
            <div className="flex flex-col items-center justify-center py-28 bg-white rounded-3xl shadow-sm border border-slate-100 animate-in fade-in duration-500">
                <div className="w-28 h-28 bg-rose-50 rounded-full flex items-center justify-center mb-6">
                    <XCircle className="w-14 h-14 text-rose-500" />
                </div>
                <h2 className="text-3xl font-black text-slate-900 tracking-tight mb-3">심사 상태 조회 중 오류가 발생했습니다</h2>
                <p className="text-base text-rose-500 font-bold mb-8">{sseError.message}</p>
                <Button
                    variant="primary"
                    size="xl"
                    onClick={() => onRejected('심사 상태 조회 실패')}
                    className="h-16 px-12 rounded-2xl font-black text-lg"
                >
                    목록으로 돌아가기
                </Button>
            </div>
        );
    }

    if (!sseData) {
        return (
            <div className="flex flex-col items-center justify-center py-28 bg-white rounded-3xl shadow-sm border border-slate-100 animate-in fade-in duration-500">
                <div className="relative mb-8">
                    <Loader2 className="w-28 h-28 text-emerald-500 animate-spin" />
                    <div className="absolute inset-0 flex items-center justify-center">
                        <div className="w-14 h-14 bg-emerald-50 rounded-full" />
                    </div>
                </div>
                <h2 className="text-3xl font-black text-slate-900 mb-3 tracking-tight">심사 중입니다</h2>
                <p className="text-base text-slate-500 text-center max-w-xs mb-6 font-medium">
                    잠시만 기다려 주세요. 심사 결과를 자동으로 조회합니다.
                </p>
                <div className="flex gap-1.5 mb-6">
                    <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
                    <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse delay-75" />
                    <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse delay-150" />
                </div>
                <p className="text-xs text-slate-400 font-bold uppercase tracking-widest">심사 결과를 기다리는 중...</p>
            </div>
        );
    }

    if (isRejected) {
        return (
            <div className="flex flex-col items-center justify-center py-28 bg-white rounded-3xl shadow-sm border border-slate-100 animate-in fade-in duration-500">
                <div className="w-28 h-28 bg-rose-50 rounded-full flex items-center justify-center mb-6">
                    <XCircle className="w-14 h-14 text-rose-500" />
                </div>
                <h2 className="text-3xl font-black text-slate-900 tracking-tight mb-3">
                    {sseData!.evaluationStatus === 'SYSTEM_ERROR'
                        ? '은행 시스템 오류가 발생했습니다'
                        : '대출 심사가 거절되었습니다'}
                </h2>
                <div className="w-full max-w-sm bg-rose-50 border border-rose-100 rounded-2xl p-8 text-left mt-4">
                    <h3 className="text-xs font-black text-rose-600 mb-3 uppercase tracking-widest">거절 사유</h3>
                    <p className="text-base text-rose-800 font-bold leading-relaxed">
                        {sseData!.rejectionMessage ??
                            (sseData!.evaluationStatus === 'SYSTEM_ERROR'
                                ? '은행 내부 오류가 발생했습니다. IT 지원팀에 문의해주세요.'
                                : '심사 조건을 충족하지 못했습니다.')}
                    </p>
                </div>
                <Button
                    variant="primary"
                    size="xl"
                    onClick={() => onRejected(sseData!.rejectionMessage ?? '심사 거절')}
                    className="mt-10 h-16 px-12 rounded-2xl font-black text-lg"
                >
                    목록으로 돌아가기
                </Button>
            </div>
        );
    }

    const handleProceed = () => {
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
    };

    return (
        <div className="w-full animate-in fade-in duration-500">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                {/* 1. 심사 결과 상세 (8컬럼) */}
                <div className="lg:col-span-8 space-y-8">
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <div className="flex items-center justify-between mb-10 pb-6 border-b border-slate-50">
                            <div>
                                <h2 className="text-3xl font-black text-slate-900 tracking-tight">심사 결과 리포트</h2>
                                <p className="text-sm font-bold text-slate-400 mt-2 tracking-tight">
                                    심사 번호: <span className="font-mono text-slate-600">{sseData!.evaluationId ?? '-'}</span>
                                </p>
                            </div>
                            <div className="flex items-center gap-2 px-5 py-2.5 bg-emerald-50 text-emerald-600 rounded-full border border-emerald-100 shadow-sm">
                                <CheckCircle2 className="w-5 h-5" />
                                <span className="text-sm font-black uppercase tracking-widest">최종 승인</span>
                            </div>
                        </div>

                        <div className="bg-slate-50/50 rounded-3xl p-12 text-center mb-10 border border-slate-100">
                            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">최종 승인 총 한도</p>
                            <p className="text-6xl font-black text-slate-900 tracking-tighter">
                                <span className="text-2xl font-bold mr-1 opacity-40 italic">₩</span>
                                {formatAmount(sseData!.approvedLimit ?? 0)}
                            </p>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="bg-white border border-slate-100 rounded-2xl p-8 shadow-sm">
                                <p className="text-[10px] font-black uppercase text-slate-400 tracking-widest mb-2">최저 적용 금리</p>
                                <p className="text-3xl font-black text-slate-900">
                                    {sseData!.availableProducts && sseData!.availableProducts.length > 0
                                        ? `${Math.min(...sseData!.availableProducts.map((p) => p.interestRate))}%`
                                        : '-'}
                                </p>
                                <p className="text-[11px] text-slate-500 font-bold mt-2">연 금리 기준 (상품별 상이)</p>
                            </div>
                            <div className="bg-white border border-slate-100 rounded-2xl p-8 shadow-sm">
                                <p className="text-[10px] font-black uppercase text-slate-400 tracking-widest mb-2">선택 가능 상품</p>
                                <p className="text-3xl font-black text-slate-900">
                                    {sseData!.availableProducts?.length ?? 0}
                                    <span className="text-lg font-bold text-slate-400 ml-1">건</span>
                                </p>
                                <p className="text-[11px] text-slate-500 font-bold mt-2">고객 맞춤형 추천 상품</p>
                            </div>
                        </div>
                    </Card> 
                </div>

                {/* 2. 다음 단계 안내 및 액션 (4컬럼) */}
                <div className="lg:col-span-4 space-y-6">
                    <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between h-full min-h-[520px]">
                        <div className="space-y-8">
                            <div className="flex items-center gap-2 px-1">
                                <FileText className="w-4 h-4 text-emerald-500" />
                                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest">다음 단계 안내</h3>
                            </div>
                            
                            <div className="space-y-6">
                                {[
                                    { step: 1, title: '추천 상품 선택', desc: '고객님께 가장 유리한 상품을 선택합니다.' },
                                    { step: 2, title: '계약 서류 동의', desc: '온라인 약관 및 서류에 확인합니다.' },
                                    { step: 3, title: '대출금 즉시 실행', desc: '지정 계좌로 대출금이 즉시 입금됩니다.' },
                                ].map((item) => (
                                    <div key={item.step} className="flex gap-4 group">
                                        <div className="w-8 h-8 bg-slate-50 text-slate-400 text-sm font-black rounded-full flex items-center justify-center shrink-0 border border-slate-100 group-hover:bg-emerald-500 group-hover:text-white group-hover:border-emerald-500 transition-all duration-300">
                                            {item.step}
                                        </div>
                                        <div>
                                            <p className="text-sm font-black text-slate-900 mb-0.5">{item.title}</p>
                                            <p className="text-[11px] text-slate-400 font-medium leading-relaxed">{item.desc}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div className="mt-12 space-y-4">
                            <Button
                                variant="primary"
                                size="xl"
                                fullWidth
                                onClick={handleProceed}
                                className="h-20 rounded-2xl text-xl font-black shadow-xl shadow-slate-900/20 group"
                            >
                                상품 선택하기
                                <ChevronRight className="w-6 h-6 ml-1 transition-transform group-hover:translate-x-1" />
                            </Button>
                            <p className="text-[10px] text-slate-400 text-center font-bold">
                                승인된 한도 내에서 다양한 상품을 확인하세요
                            </p>
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    );
};

export default LoanEvaluation;
