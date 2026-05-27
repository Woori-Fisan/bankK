import React from 'react';
import { useTransferStore } from '../../../store/useTransferStore';

interface Props {
    setError: (err: string | null) => void;
}

const InquiryInputSection: React.FC<Props> = ({ setError }) => {
    const { fromBank, fromAccountNumber, customerRrnPrefix, updateData } = useTransferStore();
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
                            {/* 테스트를 위해 BankCode 20으로 강제 지정 */}
                            {banks.map(b => <option key={b} value={'020'}>{b}</option>)}
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
                        placeholder="'-' 없이 숫자만 입력"
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
                <label className="text-sm font-medium text-gray-500 ml-1">주민등록번호</label>
                <div className="flex items-center gap-3">
                    <div className="flex-1">
                        <input
                            type="text"
                            inputMode="numeric"
                            placeholder="앞 6자리"
                            maxLength={6}
                            value={customerRrnPrefix.slice(0, 6)}
                            onChange={(e) => {
                                const val = e.target.value.replace(/[^0-9]/g, '');
                                if (val.length <= 6) {
                                    updateData({ customerRrnPrefix: val + customerRrnPrefix.slice(6, 7) });
                                    setError(null);
                                    // 6자리 다 입력하면 다음 칸으로 포커스 이동 (선택적)
                                    if (val.length === 6) {
                                        const nextInput = document.getElementById('rrn-back');
                                        nextInput?.focus();
                                    }
                                }
                            }}
                            className="w-full px-5 py-4 bg-white border border-gray-200 rounded-xl text-center text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition-all tracking-[0.2em] font-mono text-lg"
                        />
                    </div>
                    <span className="text-gray-400 font-bold text-xl">-</span>
                    <div className="flex-[1.2] flex items-center gap-2">
                        <input
                            id="rrn-back"
                            type="text"
                            inputMode="numeric"
                            maxLength={1}
                            value={customerRrnPrefix.slice(6, 7)}
                            onChange={(e) => {
                                const val = e.target.value.replace(/[^0-9]/g, '');
                                if (val.length <= 1) {
                                    updateData({ customerRrnPrefix: customerRrnPrefix.slice(0, 6) + val });
                                    setError(null);
                                }
                            }}
                            className="w-14 px-0 py-4 bg-white border border-gray-200 rounded-xl text-center text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition-all font-mono text-lg"
                        />
                        <div className="flex gap-1.5 ml-1">
                            {[...Array(6)].map((_, i) => (
                                <div key={i} className="w-3 h-3 rounded-full bg-gray-200"></div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default InquiryInputSection;
