import React from 'react';
import { ChevronRight } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import { formatAmount } from '../../../utils/formatter';

interface WithdrawSideSummaryProps {
    userName: string;
    sourceAccount: {
        bankName: string;
        accountNumber: string;
        balance?: number;
    };
    isCheckingBalance: boolean;
    onSubmit: () => void;
    isNextDisabled: boolean;
}

const WithdrawSideSummary: React.FC<WithdrawSideSummaryProps> = ({
    userName,
    sourceAccount,
    isCheckingBalance,
    onSubmit,
    isNextDisabled,
}) => {
    return (
        <div className="sticky top-10 space-y-6">
            {/* 출금 가능 잔액 카드 */}
            <Card padding="lg" className="bg-white border-slate-100 shadow-sm min-h-[280px] flex flex-col justify-between transition-all duration-500">
                <div className="space-y-6">
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-bold text-slate-700 uppercase tracking-widest">출금 요약</span>
                    </div>
                    
                    <div className="space-y-6">
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">고객명</p>
                            <p className="text-sm font-black text-slate-900">{userName || '미입력'}</p>
                        </div>
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">계좌번호</p>
                            <p className="text-sm font-black text-slate-900 font-mono">{sourceAccount.bankName} {sourceAccount.accountNumber || '미입력'}</p>
                        </div>
                    </div>
                </div>

                <div className="pt-4 mt-4 border-t border-slate-50">
                    <div className="space-y-1.5">
                        <p className="text-[10px] font-bold text-slate-400 uppercase">출금 가능 잔액</p>
                        <div className="mt-1">
                            {isCheckingBalance ? (
                                <div className="flex gap-1.5 items-baseline py-2">
                                    <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" />
                                    <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                    <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                                </div>
                            ) : sourceAccount.balance !== undefined ? (
                                <div className="flex items-baseline gap-1">
                                    <span className="text-3xl font-black text-slate-900">{formatAmount(sourceAccount.balance)}</span>
                                    <span className="text-sm font-bold text-slate-500">원</span>
                                </div>
                            ) : (
                                <p className="text-sm font-bold text-slate-300 py-2">계좌 확인 시 조회됩니다.</p>
                            )}
                        </div>
                        <p className="text-[10px] font-bold text-emerald-600">수수료 전액 면제 (0원)</p>
                    </div>
                </div>
            </Card>

            {/* 실행 버튼 */}
            <Button
                onClick={onSubmit}
                disabled={isNextDisabled}
                variant={isNextDisabled ? 'secondary' : 'primary'}
                size="xl"
                fullWidth
                className={`h-16 rounded-2xl text-xl font-black shadow-lg transition-all group ${
                    isNextDisabled ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-slate-900 text-white hover:bg-slate-800'
                }`}
            >
                다음 단계로
                <ChevronRight className={`w-6 h-6 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
            </Button>
        </div>
    );
};

export default WithdrawSideSummary;
