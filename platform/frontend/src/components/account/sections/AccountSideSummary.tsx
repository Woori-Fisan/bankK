import React from 'react';
import { ChevronRight } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';

interface AccountSideSummaryProps {
    userName: string;
    bankName: string;
    accountNo: string;
    onSubmit: () => void;
    isNextDisabled: boolean;
}

const AccountSideSummary: React.FC<AccountSideSummaryProps> = ({
    userName,
    bankName,
    accountNo,
    onSubmit,
    isNextDisabled,
}) => {
    return (
        <div className="sticky top-10 space-y-6">
            <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[420px]">
                <div className="space-y-6">
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">조회 요약</span>
                    </div>
                    
                    <div className="space-y-6">
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">고객명</p>
                            <p className="text-sm font-black text-slate-900">{userName || '미입력'}</p>
                        </div>
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">대상 은행</p>
                            <p className="text-sm font-black text-slate-900">{bankName || '은행 미선택'}</p>
                        </div>
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-slate-400 uppercase">계좌 번호</p>
                            <p className="text-sm font-black text-slate-900 font-mono">{accountNo || '번호 미입력'}</p>
                        </div>
                    </div>
                </div>

                <Button
                    onClick={onSubmit}
                    disabled={isNextDisabled}
                    variant={isNextDisabled ? 'secondary' : 'primary'}
                    size="xl"
                    fullWidth
                    className={`h-16 rounded-2xl text-lg font-black shadow-lg transition-all group ${
                        isNextDisabled ? 'bg-slate-200 text-slate-400' : 'bg-slate-900 text-white hover:bg-slate-800'
                    }`}
                >
                    계좌 조회하기
                    <ChevronRight className={`w-5 h-5 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                </Button>
            </Card>
        </div>
    );
};

export default AccountSideSummary;
