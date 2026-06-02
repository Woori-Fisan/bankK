import React, { useState } from 'react';
import LoanRequestForm from '../components/loan/LoanRequestForm';
import LoanEvaluation from '../components/loan/LoanEvaluation';
import LoanProductSelection from '../components/loan/LoanProductSelection';
import LoanContractForm from '../components/loan/LoanContractForm';
import LoanExecutionConfirm from '../components/loan/LoanExecutionConfirm';
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
    phone?: string;
    bank?: string;
    bankCode?: string;
    accountNo?: string;
}

const LoanApplication: React.FC = () => {
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
                        onBack={() => setStep('GUIDE')}
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
                        onNext={() => setStep('CONFIRM')}
                        onBack={() => setStep('SELECTION')}
                    />
                );
            case 'CONFIRM':
                return (
                    <LoanExecutionConfirm
                        loanData={loanData}
                        product={selectedProduct!}
                        evaluationId={evaluationId!}
                        onNext={(result) => { setExecutionResult(result); setStep('RESULT'); }}
                        onBack={() => setStep('CONTRACT')}
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
                            setStep('GUIDE');
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

    const stepGroups: { label: string; steps: LoanStep[] }[] = [
        { label: '1. 신청서작성', steps: ['FORM'] },
        { label: '2. 심사 및 상품선택', steps: ['EVALUATION', 'SELECTION'] },
        { label: '3. 계약서확인', steps: ['CONTRACT'] },
        { label: '4. 실행완료', steps: ['CONFIRM', 'RESULT'] },
    ];

    const currentStepIndex = stepGroups.findIndex(g => g.steps.includes(step));

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50">
            <div className="max-w-7xl mx-auto px-10 py-12 w-full min-h-full flex flex-col">
                <PageHeader 
                    title="신용 대출 신청"
                    description="대행기관 직원을 위한 대출 신청 프로세스입니다."
                />

                <div className="flex items-center mb-10">
                    {stepGroups.map((group, i) => {
                        const isActive = group.steps.includes(step);
                        const isDone = i < currentStepIndex;
                        const label = group.label.replace(/^\d+\. /, '');
                        return (
                            <React.Fragment key={group.label}>
                                {i > 0 && (
                                    <div className={`flex-1 h-0.5 mx-4 ${isDone ? 'bg-slate-700' : 'bg-gray-200'}`} />
                                )}
                                <div className="flex flex-col items-center gap-2 shrink-0">
                                    <div className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-all ${
                                        isActive ? 'bg-slate-900 text-white border-slate-900 shadow-md' :
                                        isDone  ? 'bg-slate-600 text-white border-slate-600' :
                                                  'bg-white text-gray-400 border-gray-200'
                                    }`}>
                                        {i + 1}
                                    </div>
                                    <span className={`text-[11px] font-black uppercase tracking-wider ${
                                        isActive ? 'text-slate-900' : isDone ? 'text-slate-500' : 'text-gray-400'
                                    }`}>{label}</span>
                                </div>
                            </React.Fragment>
                        );
                    })}
                </div>

                <main className="flex-1">
                    {renderStep()}
                </main>
            </div>
        </div>
    );
};

export default LoanApplication;
