import React from 'react';
import { User } from 'lucide-react';
import Card from '../../common/Card';
import Input from '../../common/Input';
import RrnInput from '../../common/RrnInput';
import AccountInputSection from '../../common/AccountInputSection';
import type { LoanData } from '../../../pages/LoanApplication';
import type { BankOption } from '../../../api/loanApi';

interface LoanCustomerSectionProps {
    formData: LoanData;
    onFormDataChange: (data: LoanData) => void;
    rrnFront: string;
    rrnBack: string;
    onRrnFrontChange: (val: string) => void;
    onRrnBackChange: (val: string) => void;
    onBlur: (field: keyof LoanData | 'rrn' | 'bank') => void;
    bankList: BankOption[] | undefined;
    fieldErrors: Partial<Record<keyof LoanData | 'rrn' | 'bank', string>>;
}

const LoanCustomerSection: React.FC<LoanCustomerSectionProps> = ({
    formData,
    onFormDataChange,
    rrnFront,
    rrnBack,
    onRrnFrontChange,
    onRrnBackChange,
    onBlur,
    bankList,
    fieldErrors,
}) => {
    return (
        <Card padding="lg" className="border-slate-100 shadow-sm">
            <div className="space-y-10">
                {/* 1. 고객 정보 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <Input
                        label="1. 고객 성명"
                        icon={User}
                        placeholder="예) 홍길동"
                        value={formData.userName}
                        onChange={(e) => onFormDataChange({ ...formData, userName: e.target.value })}
                        onBlur={() => onBlur('userName')}
                        error={fieldErrors.userName}
                    />
                </div>

                {/* 2. 주민등록번호 */}
                <div className="pt-8 border-t border-slate-50">
                    <RrnInput
                        rrnFront={rrnFront}
                        rrnBack={rrnBack}
                        onRrnFrontChange={onRrnFrontChange}
                        onRrnBackChange={onRrnBackChange}
                        onBlur={() => onBlur('rrn')}
                        error={fieldErrors.rrn}
                        label="3. 주민등록번호"
                    />
                </div>

                {/* 3. 대출금 입금 계좌 */}
                <div className="pt-8 border-t border-slate-50">
                    <AccountInputSection
                        title="4. 대출금 입금 계좌"
                        bankCode={formData.bankCode || ''}
                        accountNumber={formData.accountNo || ''}
                        banks={bankList || []}
                        onBankChange={(name, code) => {
                            onFormDataChange({ ...formData, bank: name, bankCode: code });
                        }}
                        onAccountChange={(val) => onFormDataChange({ ...formData, accountNo: val })}
                        onBlur={onBlur}
                        error={{ bankCode: fieldErrors.bank, accountNumber: fieldErrors.accountNo }}
                    />
                </div>
            </div>
        </Card>
    );
};

export default LoanCustomerSection;
