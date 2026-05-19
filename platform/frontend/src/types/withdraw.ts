export interface WithdrawData {
    sourceAccount: {
        bankName: string;
        accountNumber: string;
        balance?: number;
    };
    birthDate: string;
    amount: string;
    fee: number;
}

export interface WithdrawResult {
    balanceBefore: number;
    balanceAfter: number;
    transactionId: string;
    dateTime: string;
}
