import axios from 'axios';

export interface BalanceInquiryRequest {
    encryptedKey: string;
    jwsSignature: string;
    bankCode: string;
    accountNo: string;
    customerRrnPrefix: string;
}

export interface BalanceInquiryResponse {
    balance: string;
    status: string;
}

export interface ErrorResponse {
    code: string;
    message: string;
}

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    error?: ErrorResponse;
}

export interface TransferRecipientRequest {
    encryptedKey: string;
    jwsSignature: string;
    depositBankCode: string;
    depositAccountNo: string;
}

export interface TransferRecipientResponse {
    depositorName: string;
    depositBankName: string;
    depositBankAccountNo: string;
    accountStatus: string;
}

export interface TransferRequest {
    encryptedKey: string;
    jwsSignature: string;
    withdrawalBankCode: string;
    withdrawalAccountNo: string;
    withdrawalPassword: string;
    customerRrnPrefix: string;
    depositBankCode: string;
    depositAccountNo: string;
    amount: number;
}

export interface TransferResponse {
    transactionId: string;
    transactionDate: string;
    balanceAfter: string;
}

const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1/bank',
    headers: {
        'Content-Type': 'application/json',
    },
});

export const getBalance = async (request: BalanceInquiryRequest): Promise<ApiResponse<BalanceInquiryResponse>> => {
    const response = await api.post<ApiResponse<BalanceInquiryResponse>>('/inquiry/balance', request);
    return response.data;
};

export const getRecipient = async (request: TransferRecipientRequest): Promise<ApiResponse<TransferRecipientResponse>> => {
    const response = await api.post<ApiResponse<TransferRecipientResponse>>('/transfer/recipient', request);
    return response.data;
};

export const executeTransfer = async (request: TransferRequest): Promise<ApiResponse<TransferResponse>> => {
    const response = await api.post<ApiResponse<TransferResponse>>('/transfer', request);
    return response.data;
};
