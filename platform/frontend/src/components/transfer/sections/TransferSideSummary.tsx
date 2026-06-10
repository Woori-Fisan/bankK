import React from 'react';
import { ChevronRight } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import { formatAmount } from '../../../utils/formatter';

interface TransferSideSummaryProps {
    toName: string;
    toBankName: string;
    toAccountNumber: string;
    isRecipientInquired: boolean;
    fromName: string;
    fromBankName: string;
    fromAccountNumber: string;
    balance: string;
    isSenderInquired: boolean;
    amount: number;
    onNext: () => void;
    isNextDisabled: boolean;
}

const TransferSideSummary: React.FC<TransferSideSummaryProps> = ({
    toName,
    toBankName,
    toAccountNumber,
    isRecipientInquired,
    fromName,
    fromBankName,
    fromAccountNumber,
    balance,
    isSenderInquired,
    amount,
    onNext,
    isNextDisabled,
}) => {
    return (
        <div className="sticky top-10 space-y-6">
            <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[580px]">
                <div className="space-y-8">
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-bold text-slate-700 uppercase tracking-widest">실시간 이체 현황</span>
                    </div>
                    
                    <div className="space-y-6">
                        <div className="space-y-2">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">받는 분</p>
                            {isRecipientInquired ? (
                                <div className="p-4 bg-emerald-50/50 rounded-2xl border border-emerald-100/50 animate-in slide-in-from-right-2">
                                    <p className="text-sm font-black text-slate-900">{toName}</p>
                                    <p className="text-[11px] text-emerald-600 font-bold mt-0.5">{toBankName} {toAccountNumber}</p>
                                </div>
                            ) : (
                                <p className="text-sm font-bold text-slate-300 italic pl-1">정보를 입력해 주세요</p>
                            )}
                        </div>

                        <div className="w-full h-px bg-slate-50"></div>

                        <div className="space-y-2">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">보내는 분</p>
                            {isSenderInquired ? (
                                <div className="p-4 bg-emerald-50/50 rounded-2xl border border-emerald-100/50 animate-in slide-in-from-right-2">
                                    <p className="text-sm font-black text-slate-900">{fromName}</p>
                                    <p className="text-[11px] text-slate-500 font-mono mt-0.5">{fromBankName} {fromAccountNumber}</p>
                                    <p className="text-emerald-600 font-black mt-2 pt-2 border-t border-emerald-200/30 flex justify-between items-baseline">
                                        <span className="text-[10px] uppercase">잔액</span>
                                        <span className="text-lg">₩ {formatAmount(balance)}</span>
                                    </p>
                                </div>
                            ) : (
                                <p className="text-sm font-bold text-slate-300 italic pl-1">정보 입력 및 계좌 확인이 필요합니다</p>
                            )}
                        </div>

                        <div className="w-full h-px bg-slate-50"></div>

                        <div className="space-y-1.5">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">최종 이체 금액</p>
                            <div className="flex items-baseline gap-1">
                                <span className="text-3xl font-black text-slate-900">{formatAmount(amount)}</span>
                                <span className="text-sm font-bold text-slate-500">원</span>
                            </div>
                            <p className="text-[10px] font-bold text-emerald-600">수수료 전액 면제 (0원)</p>
                        </div>
                    </div>
                </div>

                <div className="mt-10">
                    <Button
                        onClick={onNext}
                        disabled={isNextDisabled}
                        variant={isNextDisabled ? 'secondary' : 'primary'}
                        size="xl"
                        fullWidth
                        className={`h-20 rounded-2xl text-xl font-black shadow-lg transition-all group ${
                            isNextDisabled ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-slate-900 text-white hover:bg-slate-800'
                        }`}
                    >
                        다음 단계로
                        <ChevronRight className={`w-6 h-6 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                    </Button>
                </div>
            </Card>
        </div>
    );
};

export default TransferSideSummary;
