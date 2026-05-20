import React from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import InfoSummaryBoxSection from '../section/InfoSummaryBoxSection';
import StepActionSection from '../section/StepActionSection';
import { useTransferStore } from '../../../store/useTransferStore';
import { formatAmount } from '../../../utils/formatter';

const WithdrawalConfirmForm: React.FC = () => {
    const { fromBank, fromAccountNumber, balance, prevStep, nextStep } = useTransferStore();

    return (
        <div className="w-full">
            <StepHeaderSection 
                title="출금 계좌 정보 확인"
                description="선택하신 출금 계좌의 정보를 확인해 주세요."
            />
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12 flex flex-col items-center">
                <InfoSummaryBoxSection 
                    label1="출금 계좌번호"
                    value1={fromAccountNumber}
                    label2="출금 은행"
                    value2={fromBank}
                    label3="현재 잔액"
                    value3={`₩ ${formatAmount(balance)}`}
                />
                <StepActionSection 
                    onPrev={prevStep}
                    onNext={nextStep}
                />
            </div>
        </div>
    );
};

export default WithdrawalConfirmForm;
