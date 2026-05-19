import React from 'react';

interface DepositInfoFormProps {
    depositBank: string;
    depositAccount: string;
    recipientName: string;
    onBankChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onAccountChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

const DepositInfoForm: React.FC<DepositInfoFormProps> = ({
    depositBank,
    depositAccount,
    recipientName,
    onBankChange,
    onAccountChange,
}) => {
    return (
        <div className="space-y-4">
            <h3 className="text-sm font-medium text-gray-500">입금 정보</h3>
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                    <label className="text-xs text-gray-400">입금 은행</label>
                    <input
                        type="text"
                        value={depositBank}
                        onChange={onBankChange}
                        className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900"
                        placeholder="은행 입력"
                    />
                </div>
                <div className="space-y-2">
                    <label className="text-xs text-gray-400">입금 계좌번호</label>
                    <input
                        type="text"
                        value={depositAccount}
                        onChange={onAccountChange}
                        className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-gray-900"
                        placeholder="계좌번호 입력"
                    />
                </div>
            </div>
            <div className="flex justify-between items-center px-4 py-3 border border-emerald-100 bg-emerald-50/30 rounded-lg">
                <span className="text-sm text-gray-500">수취인</span>
                <span className="text-base font-bold text-emerald-700">{recipientName}</span>
            </div>
        </div>
    );
};

export default DepositInfoForm;
