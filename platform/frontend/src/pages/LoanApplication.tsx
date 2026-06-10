import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import LoanRequestForm from '../components/loan/LoanRequestForm';
import LoanEvaluation from '../components/loan/LoanEvaluation';
import LoanProductSelection from '../components/loan/LoanProductSelection';
import LoanContractForm from '../components/loan/LoanContractForm';
import LoanResult from '../components/loan/LoanResult';
import type { EvaluationStatusResponse, ExecutionResponse } from '../api/loanApi';
import PageHeader from '../components/common/PageHeader';

export type LoanStep = 'FORM' | 'EVALUATION' | 'SELECTION' | 'CONTRACT' | 'CONFIRM' | 'RESULT';

export interface LoanProduct {
    id: number;
    loanProductCode: string;
    name: string;
    rate: number;
    limit: number;
    tags: string[];
    period?: number;
    executeAmount?: number;
}

export interface EvaluationResult {
    status?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'FAILED';
    reason?: string;
    limit?: number;
    evaluationId?: string;
    products?: LoanProduct[];
}

export interface LoanData {
    userName?: string;
    rrn?: string;
    bank?: string;
    bankCode?: string;
    accountNo?: string;
}

const LoanApplication: React.FC = () => {
    const navigate = useNavigate();
    const [step, setStep] = useState<LoanStep>('FORM');
    const [loanData, setLoanData] = useState<LoanData>({});
    const [sseData, setSseData] = useState<EvaluationStatusResponse | null>(null);
    const [sseError, setSseError] = useState<Error | null>(null);
    const [evaluationResult, setEvaluationResult] = useState<EvaluationResult | null>(null);
    const [evaluationId, setEvaluationId] = useState<string | null>(null);
    const [selectedProduct, setSelectedProduct] = useState<LoanProduct | null>(null);
    const [executionResult, setExecutionResult] = useState<ExecutionResponse | null>(null);

    const handleNext = (nextStep: LoanStep, data?: Partial<LoanData>) => {
        if (data) {
            setLoanData((prev) => ({ ...prev, ...data }));
        }
        setStep(nextStep);
    };

    const renderStep = () => {
        switch (step) {
            case 'FORM':
                return (
                    <LoanRequestForm
                        onNext={(data, _loanNo) => {
                            handleNext('EVALUATION', data);
                        }}
                        onBack={() => navigate('/main')}
                        onSseMessage={setSseData}
                        onSseError={setSseError}
                    />
                );
            case 'EVALUATION':
                return (
                    <LoanEvaluation
                        loanData={loanData}
                        sseData={sseData}
                        sseError={sseError}
                        onApproved={(result) => {
                            setEvaluationResult(result);
                            setEvaluationId(result.evaluationId ?? null);
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
                        approvedLimit={evaluationResult?.limit}
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
                        evaluationId={evaluationId!}
                        onNext={(result) => { setExecutionResult(result); setStep('RESULT'); }}
                        onBack={() => setStep('SELECTION')}
                    />
                );
            case 'RESULT':
                return (
                    <LoanResult
                        loanData={loanData}
                        product={selectedProduct}
                        evaluationResult={evaluationResult}
                        executionResult={executionResult}
                        onReset={() => {
                            setStep('FORM');
                            setLoanData({});
                            setSseData(null);
                            setSseError(null);
                            setEvaluationResult(null);
                            setEvaluationId(null);
                            setSelectedProduct(null);
                            setExecutionResult(null);
                        }}
                    />
                );
            default:
                return null;
        }
    };

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50">
            <div className="max-w-7xl mx-auto px-10 py-12 w-full min-h-full flex flex-col">
                <PageHeader 
                    title="신용 대출 신청"
                    description="대행기관 직원을 위한 대출 신청 프로세스입니다."
                />

                <main className="flex-1 mt-8">
                    {renderStep()}
                </main>
            </div>
        </div>
    );
};

export default LoanApplication;
