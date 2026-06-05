import React from 'react';
import { User, Info } from 'lucide-react';
import Card from '../../common/Card';
import Input from '../../common/Input';
import RrnInput from '../../common/RrnInput';
import AccountInputSection from '../../common/AccountInputSection';
import type { BankOption } from '../../../api/loanApi';

interface AccountMainSectionProps {
    formData: {
        userName: string;
        bankCode: string;
        accountNo: string;
        rrnFront: string;
        rrnBack: string;
    };
    onNameChange: (val: string) => void;
    onRrnFrontChange: (val: string) => void;
    onRrnBackChange: (val: string) => void;
    onBankChange: (name: string, code: string) => void;
    onAccountChange: (val: string) => void;
    banks: BankOption[];
    fieldErrors: {
        userName: string;
        bankCode: string;
        accountNo: string;
        rrn: string;
    };
}

const AccountMainSection: React.FC<AccountMainSectionProps> = ({
    formData,
    onNameChange,
    onRrnFrontChange,
    onRrnBackChange,
    onBankChange,
    onAccountChange,
    banks,
    fieldErrors,
}) => {
    return (
        <div className="space-y-8">
            <Card padding="lg" className="border-slate-100 shadow-sm">
                <div className="space-y-10">
                    {/* 1. 고객 정보 */}
                    <div className="space-y-6">
                        <div className="flex items-center gap-2 px-1">
                            <User className="w-4 h-4 text-emerald-500" />
                            <h3 className="text-sm font-bold text-slate-700">1. 고객 정보</h3>
                        </div>
                        <div className="max-w-md">
                            <Input
                                label="고객 성명"
                                placeholder="예) 홍길동"
                                value={formData.userName}
                                onChange={(e) => onNameChange(e.target.value)}
                                error={fieldErrors.userName}
                            />
                        </div>
                    </div>

                    {/* 2. 주민등록번호 */}
                    <div className="pt-8 border-t border-slate-50">
                        <RrnInput 
                            label="2. 주민등록번호"
                            rrnFront={formData.rrnFront}
                            rrnBack={formData.rrnBack}
                            onRrnFrontChange={onRrnFrontChange}
                            onRrnBackChange={onRrnBackChange}
                            error={fieldErrors.rrn}
                        />
                    </div>

                    {/* 3. 계좌 정보 */}
                    <div className="pt-8 border-t border-slate-50">
                        <AccountInputSection 
                            title="3. 조회 계좌 정보"
                            bankCode={formData.bankCode}
                            accountNumber={formData.accountNo}
                            banks={banks}
                            onBankChange={onBankChange}
                            onAccountChange={onAccountChange}
                            error={{
                                bankCode: fieldErrors.bankCode,
                                accountNumber: fieldErrors.accountNo
                            }}
                        />
                    </div>
                </div>
            </Card>

            <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4">
                <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                <p className="text-xs text-slate-500 leading-relaxed font-medium">
                    입력하신 정보는 본인 확인 및 계좌 조회를 위해 해당 금융기관으로 안전하게 전송됩니다. 
                    중계 플랫폼에는 고객님의 개인정보를 별도로 저장하지 않습니다.
                </p>
            </div>
        </div>
    );
};

export default AccountMainSection;
