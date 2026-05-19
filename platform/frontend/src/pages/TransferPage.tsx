import React, { useEffect } from 'react';
import { useTransferStore } from '../store/useTransferStore';
import AccountInquiryForm from '../components/transfer/form/AccountInquiryForm';
import WithdrawalConfirmForm from '../components/transfer/form/WithdrawalConfirmForm';
import RecipientInputForm from '../components/transfer/form/RecipientInputForm';
import RecipientConfirmForm from '../components/transfer/form/RecipientConfirmForm';
import AmountInputForm from '../components/transfer/form/AmountInputForm';
import FinalConfirmForm from '../components/transfer/form/FinalConfirmForm';
import ResultForm from '../components/transfer/form/ResultForm';

const TransferPage: React.FC = () => {
    const { step, reset } = useTransferStore();

    // 페이지 이탈 시 상태 리셋 (보안)
    useEffect(() => {
        return () => {
            reset();
        };
    }, [reset]);

    const renderStep = () => {
        switch (step) {
            case 1: return <AccountInquiryForm />;
            case 2: return <WithdrawalConfirmForm />;
            case 3: return <RecipientInputForm />;
            case 4: return <RecipientConfirmForm />;
            case 5: return <AmountInputForm />;
            case 6: return <FinalConfirmForm />;
            case 7: return <ResultForm />;
            default: return null;
        }
    };

    return (
        <div className="p-10 max-w-4xl mx-auto w-full">
            {renderStep()}
        </div>
    );
};

export default TransferPage;
