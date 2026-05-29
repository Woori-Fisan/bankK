import React from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import InfoSummaryBoxSection from '../section/InfoSummaryBoxSection';
import StepActionSection from '../section/StepActionSection';
import { useTransferStore } from '../../../store/useTransferStore';

const RecipientConfirmForm: React.FC = () => {
    const { toName, toBankName, toBankAccountNo, prevStep, nextStep } = useTransferStore();

    return (
        <div className="w-full">
            <StepHeaderSection 
                title="수취인 정보 확인"
                description="입력하신 계좌의 수취인 정보를 확인해 주세요."
            />
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12 flex flex-col items-center">
                <InfoSummaryBoxSection 
                    label1="수취인 성명"
                    value1={toName}
                    label2="입금 은행"
                    value2={toBankName}
                    label3="입금 계좌번호"
                    value3={toBankAccountNo}
                />
                <StepActionSection 
                    onPrev={prevStep}
                    onNext={nextStep}
                />
            </div>
        </div>
    );
};

export default RecipientConfirmForm;
