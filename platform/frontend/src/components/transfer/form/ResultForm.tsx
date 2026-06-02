import React from 'react';
import SuccessSummarySection from '../section/SuccessSummarySection';
import ResultDetailSection from '../section/ResultDetailSection';
import { useTransferStore } from '../../../store/useTransferStore';
import { formatAmount } from '../../../utils/formatter';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import { Printer } from 'lucide-react';

const ResultForm: React.FC = () => {
    const { 
        amount, toName, toBankName, toBankAccountNo, 
        fromBankName, fromAccountNumber, 
        transactionId, transactionDate, balanceAfter, reset 
    } = useTransferStore();

    return (
        <div className="w-full max-w-7xl mx-auto space-y-6 animate-in fade-in zoom-in-95 duration-500 pb-10">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                {/* 좌측: 성공 안내 및 상세 정보 (8컬럼) */}
                <div className="lg:col-span-8 space-y-6">
                    <Card padding="xl" className="border-slate-100 shadow-sm">
                        <SuccessSummarySection />
                    </Card>

                    <Card padding="xl" className="border-slate-100 shadow-sm">
                        <ResultDetailSection 
                            toName={toName}
                            toBank={toBankName}
                            toAccountNumber={toBankAccountNo}
                            fromBank={fromBankName}
                            fromAccountNumber={fromAccountNumber}
                            transactionId={transactionId}
                            transactionDate={transactionDate}
                            balanceAfter={balanceAfter}
                        />
                    </Card>
                </div>

                {/* 우측: 요약 정보 및 액션 버튼 (4컬럼) */}
                <div className="lg:col-span-4 space-y-6">
                    <Card padding="xl" className="border-slate-100 shadow-sm text-center h-full flex flex-col justify-between">
                        <div className="space-y-8">
                            <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-widest block mb-4">최종 이체 금액</span>
                                <div className="flex items-baseline justify-center gap-2">
                                    <span className="text-xl font-bold text-slate-400">₩</span>
                                    <span className="text-6xl font-black text-slate-900 tracking-tighter">
                                        {formatAmount(amount)}
                                    </span>
                                </div>
                            </div>

                            <div className="pt-8 border-t border-slate-50 space-y-4">
                                <div className="flex justify-between items-center text-sm">
                                    <span className="text-slate-400 font-medium">수수료</span>
                                    <span className="text-slate-900 font-bold">0원 (면제)</span>
                                </div>
                                <div className="flex justify-between items-center text-sm">
                                    <span className="text-slate-400 font-medium">상태</span>
                                    <span className="text-emerald-600 font-black">이체 완료</span>
                                </div>
                            </div>
                        </div>

                        <div className="space-y-3 mt-12">
                            <Button
                                variant="outline"
                                size="xl"
                                fullWidth
                                onClick={() => alert('출력 기능은 준비 중입니다.')}
                                className="rounded-2xl h-14 text-base font-bold gap-2 border-slate-200"
                            >
                                <Printer className="w-5 h-5" />
                                이체 내역 출력
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

export default ResultForm;