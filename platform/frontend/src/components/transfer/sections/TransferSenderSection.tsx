import React from 'react';
import { User, Wallet, CheckCircle2, Loader2 } from 'lucide-react';
import Card from '../../common/Card';
import Input from '../../common/Input';
import RrnInput from '../../common/RrnInput';
import AccountInputSection from '../../common/AccountInputSection';
import type { BankOption } from '../../../api/loanApi';
import { formatAmount } from '../../../utils/formatter';

interface TransferSenderSectionProps {
    fromName: string;
    fromBank: string;
    fromAccountNumber: string;
    customerRrnPrefix: string;
    balance: string;
    banks: BankOption[];
    onNameChange: (val: string) => void;
    onRrnFrontChange: (val: string) => void;
    onRrnBackChange: (val: string) => void;
    onBankChange: (name: string, code: string) => void;
    onAccountChange: (val: string) => void;
    onBlur: () => void;
    isSenderInquired: boolean;
    isSenderInquiring: boolean;
    isRecipientInquired: boolean;
}

const TransferSenderSection: React.FC<TransferSenderSectionProps> = ({
    fromName,
    fromBank,
    fromAccountNumber,
    customerRrnPrefix,
    balance,
    banks,
    onNameChange,
    onRrnFrontChange,
    onRrnBackChange,
    onBankChange,
    onAccountChange,
    onBlur,
    isSenderInquired,
    isSenderInquiring,
    isRecipientInquired,
}) => {
    return (
        <Card padding="lg" className={`border-slate-100 shadow-sm transition-all duration-500 ${!isRecipientInquired ? 'opacity-50 pointer-events-none grayscale' : 'opacity-100'}`}>
            <div className="space-y-10">
                {/* 1. 보내는 분 성명 */}
                <div className="space-y-6">
                    <div className="flex items-center gap-2 px-1">
                        <User className="w-4 h-4 text-emerald-500" />
                        <h3 className="text-sm font-bold text-slate-700">2. 보내는 분 성명</h3>
                    </div>
                    <div className="max-w-md">
                        <Input
                            label="고객 성명"
                            placeholder="예) 홍길동"
                            value={fromName}
                            onChange={(e) => onNameChange(e.target.value)}
                            onBlur={onBlur}
                        />
                    </div>
                </div>

                {/* 3. 주민등록번호 */}
                <div className="pt-8 border-t border-slate-50">
                    <RrnInput 
                        label="3. 주민등록번호"
                        rrnFront={customerRrnPrefix.slice(0, 6)}
                        rrnBack={customerRrnPrefix.length >= 7 ? customerRrnPrefix.charAt(6) : ''}
                        onRrnFrontChange={onRrnFrontChange}
                        onRrnBackChange={onRrnBackChange}
                        onBlur={onBlur}
                        isChecking={isSenderInquiring}
                    />
                </div>

                {/* 4. 계좌 정보 */}
                <div className="pt-8 border-t border-slate-50">
                    <AccountInputSection 
                        title="4. 보내는 분 계좌 정보"
                        bankCode={fromBank}
                        accountNumber={fromAccountNumber}
                        banks={banks}
                        onBankChange={onBankChange}
                        onAccountChange={onAccountChange}
                        onBlur={onBlur}
                    />
                </div>

                {/* 결과 영역 미리 할당 */}
                <div className="pt-4 h-[100px] flex items-center">
                    {isSenderInquired ? (
                        <div className="w-full p-5 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-center justify-between animate-in fade-in zoom-in-95">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-emerald-600 shadow-sm border border-emerald-100">
                                    <Wallet className="w-5 h-5" />
                                </div>
                                <div>
                                    <p className="text-[10px] font-black text-emerald-600 uppercase tracking-widest">내 계좌 확인 완료</p>
                                    <div className="flex items-baseline gap-1 mt-0.5">
                                        <span className="text-xs font-bold text-emerald-600 mr-0.5">잔액</span>
                                        <span className="text-lg font-black text-slate-900">₩ {formatAmount(balance)}</span>
                                    </div>
                                </div>
                            </div>
                            <CheckCircle2 className="w-6 h-6 text-emerald-500" />
                        </div>
                    ) : (
                        <div className="w-full p-6 bg-slate-50/50 rounded-2xl border border-dashed border-slate-200 flex items-center justify-between">
                            <p className="text-sm text-slate-400 font-medium italic">
                                {isSenderInquiring ? '내 계좌 조회 중입니다...' : '출금 계좌 정보를 입력하면 자동으로 조회됩니다.'}
                            </p>
                            {isSenderInquiring && <Loader2 className="w-5 h-5 text-emerald-500 animate-spin" />}
                        </div>
                    )}
                </div>
            </div>
        </Card>
    );
};

export default TransferSenderSection;
