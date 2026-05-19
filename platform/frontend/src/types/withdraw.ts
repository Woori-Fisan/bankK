export interface WithdrawData {
    sourceAccount: {
        bankName: string;
        accountNumber: string;
        branchName: string;
        balance?: number;
    };
    depositInfo: {
        bankName: string;
        accountNumber: string;
        recipientName: string;
    };
    amount: string;
    fee: number;
}

export interface WithdrawResult {
    balanceBefore: number;
    balanceAfter: number;
    transactionId: string;
    dateTime: string;
}
