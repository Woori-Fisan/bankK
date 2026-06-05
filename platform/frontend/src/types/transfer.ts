export interface TransferState {
    // State
    step: number;
    fromName: string;
    fromBank: string;
    fromBankName: string;
    fromAccountNumber: string;
    customerRrnPrefix: string;
    balance: string;
    toBank: string;
    toBankName: string;
    toAccountNumber: string;
    toBankAccountNo: string;
    toName: string;
    amount: number;
    password: string;

    // Result
    transactionId: string;
    transactionDate: string;
    balanceAfter: string;

    // Actions
    setStep: (step: number) => void;
    nextStep: () => void;
    prevStep: () => void;
    updateData: (data: Partial<Omit<TransferState, 'nextStep' | 'prevStep' | 'updateData' | 'reset' | 'setStep'>>) => void;
    reset: () => void;
}
