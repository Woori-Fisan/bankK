import React from 'react';
import WithdrawConfirmSection from '../sections/WithdrawConfirmSection';
import type { WithdrawData } from '../../../types/withdraw';

export interface WithdrawConfirmFormProps {
    data: WithdrawData;
    onConfirm: () => void;
    onBack: () => void;
}

const WithdrawConfirmForm: React.FC<WithdrawConfirmFormProps> = ({
    data,
    onConfirm,
    onBack,
}) => {
    return (
        <div className="w-full max-w-5xl mx-auto bg-white rounded-[2rem] shadow-xl shadow-gray-200/50 border border-gray-100 overflow-hidden transition-all duration-300">
            <div className="p-8 md:p-12 space-y-10 w-full">
                <header className="border-b border-gray-50 pb-6 flex items-center gap-4">
                    <button 
                        onClick={onBack}
                        className="p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-400 hover:text-gray-900"
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M15 19l-7-7 7-7" />
                        </svg>
                    </button>
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">정보 확인</h2>
                </header>

                <WithdrawConfirmSection 
                    sourceAccount={data.sourceAccount}
                    birthDate={data.birthDate}
                    amount={data.amount}
                    fee={data.fee}
                />

                <footer className="pt-6 grid grid-cols-2 gap-4">
                    <button
                        type="button"
                        onClick={onBack}
                        className="py-5 bg-gray-100 text-gray-600 text-xl font-black rounded-2xl hover:bg-gray-200 active:scale-[0.98] transition-all"
                    >
                        수정하기
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        className="py-5 bg-emerald-800 text-white text-xl font-black rounded-2xl hover:bg-emerald-900 active:scale-[0.98] transition-all shadow-lg shadow-emerald-800/20"
                    >
                        확인 완료
                    </button>
                </footer>
            </div>
        </div>
    );
};

export default WithdrawConfirmForm;
