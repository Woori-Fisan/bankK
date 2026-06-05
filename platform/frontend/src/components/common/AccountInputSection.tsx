import React from 'react';
import type { BankOption } from '../../api/loanApi';
import Input from './Input';
import { Landmark, Hash } from 'lucide-react';

interface AccountInputSectionProps {
    title?: string;
    bankCode: string;
    accountNumber: string;
    banks: BankOption[];
    onBankChange: (bankName: string, bankCode: string) => void;
    onAccountChange: (val: string) => void;
    onBlur?: (field: 'bank' | 'accountNo') => void;
    error?: {
        bankCode?: string;
        accountNumber?: string;
    };
}

const AccountInputSection: React.FC<AccountInputSectionProps> = ({
    title = "계좌 정보",
    bankCode,
    accountNumber,
    banks,
    onBankChange,
    onAccountChange,
    onBlur,
    error
}) => {
    return (
        <div className="space-y-6">
            <div className="flex items-center gap-2 px-1">
                <Landmark className="w-4 h-4 text-emerald-500" />
                <h3 className="text-sm font-bold text-slate-700">{title}</h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="md:col-span-1">
                    <Input
                        as="select"
                        label="은행"
                        value={bankCode}
                        onChange={(e) => {
                            const selectedBank = banks.find(b => b.bankCode === e.target.value);
                            if (selectedBank) onBankChange(selectedBank.bankName, selectedBank.bankCode);
                        }}
                        onBlur={() => onBlur?.('bank')}
                        error={error?.bankCode}
                    >
                        <option value="">은행을 선택하세요</option>
                        {banks.map((b) => (
                            <option key={b.bankCode} value={b.bankCode}>{b.bankName}</option>
                        ))}
                    </Input>
                </div>

                <div className="md:col-span-2">
                    <Input
                        label="계좌번호"
                        icon={Hash}
                        type="text"
                        inputMode="numeric"
                        value={accountNumber}
                        onChange={(e) => onAccountChange(e.target.value.replace(/[^0-9]/g, ''))}
                        onBlur={() => onBlur?.('accountNo')}
                        error={error?.accountNumber}
                        placeholder="'-' 없이 숫자만 입력"
                        helperText="* 계좌번호는 '-' 없이 숫자만 입력해 주세요."
                    />
                </div>
            </div>
        </div>
    );
};

export default AccountInputSection;
