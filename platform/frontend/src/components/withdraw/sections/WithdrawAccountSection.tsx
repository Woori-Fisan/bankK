import React from 'react';

interface WithdrawAccountSectionProps {
    bankName: string;
    accountNumber: string;
    onBankChange: (val: string) => void;
    onAccountChange: (val: string) => void;
    onBlur?: () => void;
}

const WithdrawAccountSection: React.FC<WithdrawAccountSectionProps> = ({
    bankName,
    accountNumber,
    onBankChange,
    onAccountChange,
    onBlur,
}) => {
    const banks = ['국민은행', '우리은행', '신한은행', '하나은행'];

    return (
        <section className="space-y-4">
            <h3 className="text-sm font-medium text-gray-500">출금 정보</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                    <label className="text-xs font-semibold text-gray-400">출금 은행</label>
                    <div className="relative group">
                        <select
                            value={bankName}
                            onChange={(e) => onBankChange(e.target.value)}
                            onBlur={onBlur}
                            className="w-full px-4 py-3 bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900 transition-all appearance-none cursor-pointer"
                        >
                            <option value="" disabled>은행 선택</option>
                            {banks.map((b) => (
                                <option key={b} value={b}>{b}</option>
                            ))}
                        </select>
                        <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                            </svg>
                        </div>
                    </div>
                </div>
                <div className="space-y-2">
                    <label className="text-xs font-semibold text-gray-400">출금 계좌번호</label>
                    <input
                        type="text"
                        inputMode="numeric"
                        value={accountNumber}
                        onChange={(e) => onAccountChange(e.target.value.replace(/[^0-9]/g, ''))}
                        onBlur={onBlur}
                        className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900 transition-shadow"
                        placeholder="'-' 없이 숫자만 입력"
                    />
                </div>
            </div>
        </section>
    );
};

export default WithdrawAccountSection;
