import React from 'react';
import { useTransferStore } from '../../../store/useTransferStore';

interface Props {
    setError: (err: string | null) => void;
}

const InquiryInputSection: React.FC<Props> = ({ setError }) => {
    const { fromBank, fromAccountNumber, fromName, updateData } = useTransferStore();
    const banks = ['국민은행', '우리은행', '신한은행', '하나은행'];

    return (
        <div className="space-y-8">
            <div className="grid grid-cols-2 gap-6">
                <div className="flex flex-col gap-2">
                    <label className="text-sm font-medium text-gray-500 ml-1">출금 은행</label>
                    <div className="relative">
                        <select
                            value={fromBank}
                            onChange={(e) => {
                                updateData({ fromBank: e.target.value });
                                setError(null);
                            }}
                            className="w-full px-5 py-4 bg-white border border-gray-200 rounded-xl text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 appearance-none transition-all cursor-pointer"
                        >
                            <option value="" disabled>은행 선택</option>
                            {banks.map(b => <option key={b} value={b}>{b}</option>)}
                        </select>
                        <div className="absolute right-5 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                            </svg>
                        </div>
                    </div>
                </div>
                <div className="flex flex-col gap-2">
                    <label className="text-sm font-medium text-gray-500 ml-1">출금 계좌번호</label>
                    <input
                        type="text"
                        inputMode="numeric"
                        value={fromAccountNumber}
                        onChange={(e) => {
                            updateData({ fromAccountNumber: e.target.value.replace(/[^0-9]/g, '') });
                            setError(null);
                        }}
                        className="w-full px-5 py-4 bg-white border border-gray-200 rounded-xl text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition-all"
                    />
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <label className="text-sm font-medium text-gray-500 ml-1">이름</label>
                <input
                    type="text"
                    value={fromName}
                    onChange={(e) => {
                        updateData({ fromName: e.target.value });
                        setError(null);
                    }}
                    className="w-full px-5 py-4 bg-white border border-gray-200 rounded-xl text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition-all"
                />
            </div>
        </div>
    );
};

export default InquiryInputSection;
