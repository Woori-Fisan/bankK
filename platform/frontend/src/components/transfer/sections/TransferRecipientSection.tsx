import React from 'react';
import { User, CheckCircle2, Loader2 } from 'lucide-react';
import Card from '../../common/Card';
import AccountInputSection from '../../common/AccountInputSection';
import type { BankOption } from '../../../api/loanApi';

interface TransferRecipientSectionProps {
    toBank: string;
    toAccountNumber: string;
    banks: BankOption[];
    onBankChange: (name: string, code: string) => void;
    onAccountChange: (val: string) => void;
    onBlur: () => void;
    isRecipientInquired: boolean;
    isRecipientInquiring: boolean;
    toName: string;
}

const TransferRecipientSection: React.FC<TransferRecipientSectionProps> = ({
    toBank,
    toAccountNumber,
    banks,
    onBankChange,
    onAccountChange,
    onBlur,
    isRecipientInquired,
    isRecipientInquiring,
    toName,
}) => {
    return (
        <Card padding="lg" className="border-slate-100 shadow-sm">
            <div className="space-y-8">
                <AccountInputSection 
                    title="1. 입금 계좌 정보 (받는 분)"
                    bankCode={toBank}
                    accountNumber={toAccountNumber}
                    banks={banks}
                    onBankChange={onBankChange}
                    onAccountChange={onAccountChange}
                    onBlur={onBlur}
                />
                
                {/* 결과 영역 미리 할당 */}
                <div className="pt-4 h-[100px] flex items-center">
                    {isRecipientInquired ? (
                        <div className="w-full p-5 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-center justify-between animate-in fade-in zoom-in-95">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-emerald-600 shadow-sm border border-emerald-100">
                                    <User className="w-5 h-5" />
                                </div>
                                <div>
                                    <p className="text-[10px] font-black text-emerald-600 uppercase tracking-widest">수취인 확인 완료</p>
                                    <p className="text-xl font-black text-slate-900">{toName}</p>
                                </div>
                            </div>
                            <CheckCircle2 className="w-6 h-6 text-emerald-500" />
                        </div>
                    ) : (
                        <div className="w-full p-6 bg-slate-50/50 rounded-2xl border border-dashed border-slate-200 flex items-center justify-between">
                            <p className="text-sm text-slate-400 font-medium italic">
                                {isRecipientInquiring ? '수취인 조회 중입니다...' : '수취인 정보를 입력하면 자동으로 조회됩니다.'}
                            </p>
                            {isRecipientInquiring && <Loader2 className="w-5 h-5 text-emerald-500 animate-spin" />}
                        </div>
                    )}
                </div>
            </div>
        </Card>
    );
};

export default TransferRecipientSection;
