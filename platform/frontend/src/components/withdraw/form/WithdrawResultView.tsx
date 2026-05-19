import React from 'react';
import WithdrawResultHeader from '../sections/WithdrawResultHeader';
import WithdrawAmountCard from '../sections/WithdrawAmountCard';
import WithdrawDetailTable from '../sections/WithdrawDetailTable';
import type { WithdrawData } from '../../../types/withdraw';

interface WithdrawResultViewProps {
    data: WithdrawData;
    result: {
        balanceBefore: number;
        balanceAfter: number;
        transactionId: string;
        dateTime: string;
    };
    onClose: () => void;
}

const WithdrawResultView: React.FC<WithdrawResultViewProps> = ({
    data,
    result,
    onClose,
}) => {
    return (
        <div className="w-full max-w-4xl mx-auto bg-white rounded-[2rem] shadow-xl shadow-gray-200/50 border border-gray-100 overflow-hidden animate-in fade-in zoom-in duration-500">
            <div className="p-8 md:p-12 space-y-12">
                <WithdrawResultHeader />

                <div className="max-w-md mx-auto w-full">
                    <WithdrawAmountCard amount={data.amount} />
                </div>

                <div className="border-t border-gray-50 pt-10">
                    <WithdrawDetailTable 
                        recipientName={data.depositInfo.recipientName}
                        bankName={data.depositInfo.bankName}
                        accountNumber={data.depositInfo.accountNumber}
                        balanceBefore={result.balanceBefore}
                        balanceAfter={result.balanceAfter}
                        transactionId={result.transactionId}
                        dateTime={result.dateTime}
                    />
                </div>

                <footer className="pt-8 flex justify-center">
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-12 py-4 border border-gray-200 text-gray-600 text-lg font-black rounded-xl hover:bg-gray-50 active:scale-[0.98] transition-all"
                    >
                        목록으로 돌아가기
                    </button>
                </footer>
            </div>
        </div>
    );
};

export default WithdrawResultView;
