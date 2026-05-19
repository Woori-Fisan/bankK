export interface TransferState {
    // State
    step: number;
    fromBank: string;
    fromAccountNumber: string;
    fromName: string;
    toBank: string;
    toAccountNumber: string;
    toName: string;
    amount: number;

    // Actions
    setStep: (step: number) => void;
    nextStep: () => void;
    prevStep: () => void;
    updateData: (data: Partial<Omit<TransferState, 'nextStep' | 'prevStep' | 'updateData' | 'reset' | 'setStep'>>) => void;
    reset: () => void;
}
