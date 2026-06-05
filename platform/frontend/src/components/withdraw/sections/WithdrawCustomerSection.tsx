import React from 'react';
import { User } from 'lucide-react';
import Card from '../../common/Card';
import Input from '../../common/Input';
import RrnInput from '../../common/RrnInput';
import AccountInputSection from '../../common/AccountInputSection';
import type { BankOption } from '../../../api/loanApi';

interface WithdrawCustomerSectionProps {
    userName: string;
    onUserNameChange: (val: string) => void;
    birthDate: string;
    onRrnFrontChange: (val: string) => void;
    onRrnBackChange: (val: string) => void;
    onBlur: () => void;
    isCheckingBalance: boolean;
    sourceAccount: {
        bankCode: string;
        accountNumber: string;
    };
    banks: BankOption[];
    onBankChange: (name: string, code: string) => void;
    onAccountChange: (val: string) => void;
}

const WithdrawCustomerSection: React.FC<WithdrawCustomerSectionProps> = ({
    userName,
    onUserNameChange,
    birthDate,
    onRrnFrontChange,
    onRrnBackChange,
    onBlur,
    isCheckingBalance,
    sourceAccount,
    banks,
    onBankChange,
    onAccountChange,
}) => {
    return (
        <Card padding="lg" className="border-slate-100 shadow-sm">
            <div className="space-y-10">
                {/* 1단계: 고객 정보 입력 */}
                <div className="space-y-6">
                    <div className="flex items-center gap-2 px-1">
                        <User className="w-4 h-4 text-emerald-500" />
                        <h3 className="text-sm font-bold text-slate-700">1. 고객 정보</h3>
                    </div>
                    <div className="max-w-md">
                        <Input
                            label="고객 성명"
                            placeholder="예) 홍길동"
                            value={userName}
                            onChange={(e) => onUserNameChange(e.target.value)}
                        />
                    </div>
                </div>

                {/* 2. 주민등록번호 */}
                <div className="pt-8 border-t border-slate-50">
                    <RrnInput 
                        label="2. 주민등록번호"
                        rrnFront={birthDate.slice(0, 6)}
                        rrnBack={birthDate.length >= 7 ? birthDate.charAt(6) : ''}
                        onRrnFrontChange={onRrnFrontChange}
                        onRrnBackChange={onRrnBackChange}
                        onBlur={onBlur}
                        isChecking={isCheckingBalance}
                    />
                </div>

                {/* 3단계: 출금 계좌 정보 입력 */}
                <div className="pt-8 border-t border-slate-50">
                    <AccountInputSection 
                        title="3. 출금 계좌 정보"
                        bankCode={sourceAccount.bankCode}
                        accountNumber={sourceAccount.accountNumber}
                        banks={banks}
                        onBankChange={onBankChange}
                        onAccountChange={onAccountChange}
                        onBlur={onBlur}
                    />
                </div>
            </div>
        </Card>
    );
};

export default WithdrawCustomerSection;
