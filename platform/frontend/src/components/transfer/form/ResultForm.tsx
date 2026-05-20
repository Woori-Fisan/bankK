import React from 'react';
import SuccessSummarySection from '../section/SuccessSummarySection';
import ResultDetailSection from '../section/ResultDetailSection';
import { useTransferStore } from '../../../store/useTransferStore';

const ResultForm: React.FC = () => {
    const { 
        amount, toName, toBankName, toBankAccountNo, 
        fromBank, fromAccountNumber, 
        transactionId, transactionDate, balanceAfter, reset 
    } = useTransferStore();

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
            <SuccessSummarySection amount={amount} />
            
            <ResultDetailSection 
                toName={toName}
                toBank={toBankName}
                toAccountNumber={toBankAccountNo}
                fromBank={fromBank}
                fromAccountNumber={fromAccountNumber}
                transactionId={transactionId}
                transactionDate={transactionDate}
                balanceAfter={balanceAfter}
            />

            <div className="p-8 bg-gray-50 border-t border-gray-100 text-center">
                <button
                    onClick={reset}
                    className="px-12 py-4 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold hover:bg-gray-100 transition-colors shadow-sm"
                >
                    목록으로 돌아가기
                </button>
            </div>
        </div>
    );
};

export default ResultForm;
