import React, { useState, useEffect } from 'react';
import { useTransferStore } from '../../../store/useTransferStore';
import { fetchBankList, type BankOption } from '../../../api/loanApi';

interface Props {
    setError: (err: string | null) => void;
}

const RecipientInputFieldsSection: React.FC<Props> = ({ setError }) => {
    const { toBank, toAccountNumber, updateData } = useTransferStore();
    const [banks, setBanks] = useState<BankOption[]>([]);

    useEffect(() => {
        const getBanks = async () => {
            try {
                const bankList = await fetchBankList();
                setBanks(bankList);
            } catch (error) {
                console.error('은행 리스트 조회 실패:', error);
            }
        };
        getBanks();
    }, []);

    return (
        <div className="space-y-8">
            <div className="flex flex-col gap-2">
                <label className="text-sm font-medium text-gray-500 ml-1">은행 선택</label>
                <div className="relative">
                    <select
                        value={toBank}
                        onChange={(e) => {
                            updateData({ toBank: e.target.value });
                            setError(null);
                        }}
                        className="w-full px-5 py-4 bg-white border border-gray-200 rounded-xl text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 appearance-none transition-all cursor-pointer"
                    >
                        <option value="" disabled>은행을 선택하세요</option>
                        {banks.map(bank => <option key={bank.bankCode} value={bank.bankCode}>{bank.bankName}</option>)}
                    </select>
                    <div className="absolute right-5 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                        </svg>
                    </div>
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <label className="text-sm font-medium text-gray-500 ml-1">계좌번호 입력</label>
                <input
                    type="text"
                    inputMode="numeric"
                    value={toAccountNumber}
                    onChange={(e) => {
                        updateData({ toAccountNumber: e.target.value.replace(/[^0-9]/g, '') });
                        setError(null);
                    }}
                    className="w-full px-5 py-6 bg-gray-50 border border-gray-200 rounded-xl text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition-all"
                    placeholder="'-' 없이 숫자만 입력"
                />
            </div>
        </div>
    );
};

export default RecipientInputFieldsSection;
