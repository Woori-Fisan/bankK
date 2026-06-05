import { create } from 'zustand';
import type { TransferState } from '../types/transfer';

const initialState = {
    step: 1,
    fromName: '',
    fromBank: '',
    fromBankName: '',
    fromAccountNumber: '',
    customerRrnPrefix: '',
    balance: '0',
    toBank: '',
    toBankName: '',
    toAccountNumber: '',
    toBankAccountNo: '',
    toName: '(주)글로벌네트워크', // 더미 결과값 유지
    amount: 0,
    password: '',
    transactionId: '',
    transactionDate: '',
    balanceAfter: '',
};

export const useTransferStore = create<TransferState>((set) => ({
    ...initialState,

    setStep: (step) => set({ step }),
    nextStep: () => set((state) => ({ step: state.step + 1 })),
    prevStep: () => set((state) => ({ step: state.step - 1 })),
    updateData: (data) => set((state) => ({ ...state, ...data })),
    reset: () => set(initialState),
}));
