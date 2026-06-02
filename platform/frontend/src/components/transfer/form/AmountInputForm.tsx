import React from 'react';
import StepHeaderSection from '../section/StepHeaderSection';
import AmountInputSection from '../section/AmountInputSection';
import BalanceCalculationSection from '../section/BalanceCalculationSection';
import StepActionSection from '../section/StepActionSection';
import { useTransferStore } from '../../../store/useTransferStore';

const AmountInputForm: React.FC = () => {
    const { toName, toBank, toAccountNumber, amount, nextStep, setStep } = useTransferStore();

    return (
        <div className="w-full">
            <StepHeaderSection 
                title="이체 금액 입력" 
                description="이체금액을 입력합니다."
            />
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                <div className="space-y-10">
                    <div className="bg-gray-50 rounded-2xl p-6 border border-gray-100 flex justify-between items-center">
                        <div className="flex flex-col gap-1">
                            <span className="text-xs text-gray-400 font-medium">받는 분</span>
                            <span className="text-lg font-bold text-gray-900">{toName}</span>
                            <span className="text-sm text-gray-500">{toBank} {toAccountNumber}</span>
                        </div>
                        <button onClick={() => setStep(3)} className="text-sm text-emerald-600 font-bold hover:text-emerald-700 transition-colors">
                            변경
                        </button>
                    </div>

                    <AmountInputSection />
                    <BalanceCalculationSection />

                    <StepActionSection 
                        onPrev={() => setStep(3)}
                        onNext={nextStep}
                        nextDisabled={amount === 0}
                        prevLabel="취소"
                    />
                </div>
            </div>
        </div>
    );
};

export default AmountInputForm;
