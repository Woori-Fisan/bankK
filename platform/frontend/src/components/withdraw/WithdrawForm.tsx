import React, { useState } from 'react';
import WithdrawAccountCard from './WithdrawAccountCard';
import DepositInfoForm from './DepositInfoForm';
import AmountInputSection from './AmountInputSection';
import WithdrawFeeInfo from './WithdrawFeeInfo';

const WithdrawForm: React.FC = () => {
    // 임시 데이터 (실제로는 API나 Store에서 가져와야 함)
    const [sourceAccount] = useState({
        bankName: '국민',
        accountNumber: '123-45-67890',
        branchName: '본점영업부',
        balance: 45300000,
    });

    const [depositBank, setDepositBank] = useState('신한은행');
    const [depositAccount, setDepositAccount] = useState('110-123-456789');
    const [recipientName] = useState('(주)글로벌테크');
    const [amount, setAmount] = useState('15000000');
    const [fee] = useState(0);

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
        console.log('출금 실행:', {
            sourceAccount,
            depositBank,
            depositAccount,
            amount,
        });
        // TODO: PinPadModal 오픈 및 암호화 처리
    };

    return (
        <div className="max-w-2xl mx-auto bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="p-8 space-y-8">
                <header>
                    <h2 className="text-2xl font-bold text-gray-900">출금 신청</h2>
                </header>

                <div className="space-y-6">
                    <section>
                        <h3 className="text-sm font-medium text-gray-500 mb-3">출금 정보</h3>
                        <WithdrawAccountCard 
                            bankName={sourceAccount.bankName}
                            accountNumber={sourceAccount.accountNumber}
                            branchName={sourceAccount.branchName}
                            balance={sourceAccount.balance}
                        />
                    </section>

                    <DepositInfoForm 
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

                    <WithdrawFeeInfo fee={fee} isWaived={true} />
                </div>

                <footer className="pt-4">
                    <button
                        type="button"
                        onClick={handleSubmit}
                        className="w-full py-4 bg-emerald-700 text-white text-lg font-bold rounded-xl hover:bg-emerald-800 active:bg-emerald-900 transition-colors shadow-lg shadow-emerald-700/20"
                    >
                        출금 실행
                    </button>
                </footer>
            </div>
        </div>
    );
};

export default WithdrawForm;
