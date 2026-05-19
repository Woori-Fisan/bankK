import React from 'react';

interface WithdrawAccountSectionProps {
    bankName: string;
    accountNumber: string;
    onBankChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onAccountChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

const WithdrawAccountSection: React.FC<WithdrawAccountSectionProps> = ({
    bankName,
    accountNumber,
    onBankChange,
    onAccountChange,
}) => {
    return (
        <section className="space-y-4">
            <h3 className="text-sm font-medium text-gray-500">출금 정보</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                    <label className="text-xs font-semibold text-gray-400">출금 은행</label>
                    <input
                        type="text"
                        value={bankName}
                        onChange={onBankChange}
                        className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900 transition-shadow"
                        placeholder="출금 은행명을 입력하세요"
                    />
                </div>
                <div className="space-y-2">
                    <label className="text-xs font-semibold text-gray-400">출금 계좌번호</label>
                    <input
                        type="text"
                        value={accountNumber}
                        onChange={onAccountChange}
                        className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900 transition-shadow"
                        placeholder="출금 계좌번호를 입력하세요"
                    />
                </div>
            </div>
        </section>
    );
};

export default WithdrawAccountSection;
