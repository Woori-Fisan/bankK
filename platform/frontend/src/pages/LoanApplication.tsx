import React, { useState } from 'react';
import LoanGuide from '../components/loan/LoanGuide';
import LoanRequestForm from '../components/loan/LoanRequestForm';
import LoanEvaluation from '../components/loan/LoanEvaluation';
import LoanProductSelection from '../components/loan/LoanProductSelection';
import LoanContractForm from '../components/loan/LoanContractForm';
import LoanExecutionConfirm from '../components/loan/LoanExecutionConfirm';
import LoanResult from '../components/loan/LoanResult';

export type LoanStep = 'GUIDE' | 'FORM' | 'EVALUATION' | 'SELECTION' | 'CONTRACT' | 'CONFIRM' | 'RESULT';

export interface LoanProduct {
    id: number;
    name: string;
    rate: number;
    limit: number;
    tags: string[];
    period?: number;
}

export interface EvaluationResult {
    status?: 'PENDING' | 'APPROVED' | 'REJECTED';
    reason?: string;
    limit?: number;
    rate?: number;
    period?: number;
    bank?: string;
    account?: string;
    products?: LoanProduct[];
}

export interface LoanData {
    userName?: string;
    rrn?: string;
    phone?: string;
    bank?: string;
    accountNo?: string;
    accountHolder?: string;
}

const LoanApplication: React.FC = () => {
    const [step, setStep] = useState<LoanStep>('GUIDE');
    const [loanData, setLoanData] = useState<LoanData>({});
    const [evaluationResult, setEvaluationResult] = useState<EvaluationResult | null>(null);
    const [selectedProduct, setSelectedProduct] = useState<LoanProduct | null>(null);

    const handleNext = (nextStep: LoanStep, data?: Partial<LoanData>) => {
        if (data) {
            setLoanData((prev) => ({ ...prev, ...data }));
        }
        setStep(nextStep);
    };

    const handleBack = (prevStep: LoanStep) => {
        setStep(prevStep);
    };

    const renderStep = () => {
        switch (step) {
            case 'GUIDE':
                return <LoanGuide onNext={() => setStep('FORM')} />;
            case 'FORM':
                return (
                    <LoanRequestForm 
                        onNext={(data) => handleNext('EVALUATION', data)} 
                        onBack={() => setStep('GUIDE')} 
                    />
                );
            case 'EVALUATION':
                return (
                    <LoanEvaluation 
                        loanData={loanData}
                        onApproved={(result) => {
                            setEvaluationResult(result);
                            setStep('SELECTION');
                        }}
                        onRejected={(reason) => {
                            setEvaluationResult({ status: 'REJECTED', reason });
                            setStep('RESULT');
                        }}
                    />
                );
            case 'SELECTION':
                return (
                    <LoanProductSelection 
                        products={evaluationResult?.products || []}
                        onNext={(product) => {
                            setSelectedProduct(product);
                            setStep('CONTRACT');
                        }}
                        onBack={() => setStep('FORM')}
                    />
                );
            case 'CONTRACT':
                return (
                    <LoanContractForm 
                        product={selectedProduct!}
                        loanData={loanData}
                        onNext={() => setStep('CONFIRM')}
                        onBack={() => setStep('SELECTION')}
                    />
                );
            case 'CONFIRM':
                return (
                    <LoanExecutionConfirm 
                        loanData={loanData}
                        product={selectedProduct!}
                        onNext={() => setStep('RESULT')}
                        onBack={() => setStep('CONTRACT')}
                    />
                );
            case 'RESULT':
                return (
                    <LoanResult 
                        loanData={loanData}
                        product={selectedProduct}
                        evaluationResult={evaluationResult}
                        onReset={() => {
                            setStep('GUIDE');
                            setLoanData({});
                            setEvaluationResult(null);
                            setSelectedProduct(null);
                        }}
                    />
                );
            default:
                return <LoanGuide onNext={() => setStep('FORM')} />;
        }
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-full flex flex-col bg-gray-50">
            <header className="mb-8">
                <div className="flex items-center gap-2 text-sm text-gray-500 mb-2">
                    <span className={step === 'GUIDE' ? 'font-bold text-blue-600' : ''}>1. 서류안내</span>
                    <span>/</span>
                    <span className={step === 'FORM' ? 'font-bold text-blue-600' : ''}>2. 신청서작성</span>
                    <span>/</span>
                    <span className={['EVALUATION', 'SELECTION'].includes(step) ? 'font-bold text-blue-600' : ''}>3. 심사 및 상품선택</span>
                    <span>/</span>
                    <span className={step === 'CONTRACT' ? 'font-bold text-blue-600' : ''}>4. 계약서확인</span>
                    <span>/</span>
                    <span className={['CONFIRM', 'RESULT'].includes(step) ? 'font-bold text-blue-600' : ''}>5. 실행완료</span>
                </div>
                <h1 className="text-2xl font-bold text-gray-900">신용 대출 신청</h1>
                <p className="text-gray-500 text-sm mt-1">대행기관 직원을 위한 대출 신청 프로세스입니다.</p>
            </header>

            <main className="flex-1">
                {renderStep()}
            </main>
        </div>
    );
};

export default LoanApplication;
