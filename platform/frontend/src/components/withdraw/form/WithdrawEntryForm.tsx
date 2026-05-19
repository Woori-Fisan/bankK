import React, { useState } from 'react';
import WithdrawAccountSection from '../sections/WithdrawAccountSection';
import DepositInfoSection from '../sections/DepositInfoSection';
import AmountInputSection from '../sections/AmountInputSection';
import WithdrawFeeSection from '../sections/WithdrawFeeSection';
import type { WithdrawData } from '../../../types/withdraw';

export interface WithdrawEntryFormProps {
    initialData?: {
        depositBank: string;
        depositAccount: string;
        amount: string;
    };
    onNext: (data: WithdrawData) => void;
}

const WithdrawEntryForm: React.FC<WithdrawEntryFormProps> = ({ initialData, onNext }) => {
    // 1. 상태 관리
    const [sourceAccount] = useState({
        bankName: '국민',
        accountNumber: '123-45-67890',
        branchName: '본점영업부',
        balance: 45300000,
    });

    const [depositBank, setDepositBank] = useState(initialData?.depositBank || '신한은행');
    const [depositAccount, setDepositAccount] = useState(initialData?.depositAccount || '110-123-456789');
    const [recipientName] = useState('(주)글로벌테크');
    const [amount, setAmount] = useState(initialData?.amount || '15000000');
    const [fee] = useState(0);
    
    // 2. 핸들러
    const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value.replace(/[^0-9]/g, '');
        setAmount(val);
    };

    const handleQuickAmountAdd = (val: number) => {
        const current = parseInt(amount || '0', 10);
        setAmount((current + val).toString());
    };

    const handleAllIn = () => {
        setAmount(sourceAccount.balance.toString());
    };

    const handleSubmit = () => {
        onNext({
            sourceAccount,
            depositInfo: {
                bankName: depositBank,
                accountNumber: depositAccount,
                recipientName,
            },
            amount,
            fee,
        });
    };

    return (
        <div className="w-full max-w-2xl mx-auto bg-white rounded-[2rem] shadow-xl shadow-gray-200/50 border border-gray-100 overflow-hidden">
            <div className="p-8 md:p-12 space-y-10">
                <header className="border-b border-gray-50 pb-6">
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">출금 신청</h2>
                </header>

                <div className="space-y-10">
                    <WithdrawAccountSection 
                        bankName={sourceAccount.bankName}
                        accountNumber={sourceAccount.accountNumber}
                        branchName={sourceAccount.branchName}
                        balance={sourceAccount.balance}
                    />

                    <DepositInfoSection 
                        depositBank={depositBank}
                        depositAccount={depositAccount}
                        recipientName={recipientName}
                        onBankChange={(e) => setDepositBank(e.target.value)}
                        onAccountChange={(e) => setDepositAccount(e.target.value)}
                    />

                    <AmountInputSection 
                        amount={amount}
                        onAmountChange={handleAmountChange}
                        onQuickAmountAdd={handleQuickAmountAdd}
                        onAllIn={handleAllIn}
                    />

                    <WithdrawFeeSection fee={fee} isWaived={true} />
                </div>

                <footer className="pt-6">
                    <button
                        type="button"
                        onClick={handleSubmit}
                        className="w-full py-5 bg-emerald-800 text-white text-xl font-black rounded-2xl hover:bg-emerald-900 active:scale-[0.98] transition-all shadow-lg shadow-emerald-800/20"
                    >
                        다음
                    </button>
                </footer>
            </div>
        </div>
    );
};

export default WithdrawEntryForm;
