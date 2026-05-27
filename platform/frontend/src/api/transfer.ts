import axiosInstance from './axiosInstance';
import { prepareSecureRequest } from '../utils/bankCrypto';

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
    reqPayload: string; // 은행 코어용 암호화된 전체 JWE (Zero-Knowledge)
    depositBankCode: string;
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

export const getBalance = async (request: BalanceInquiryRequest): Promise<ApiResponse<BalanceInquiryResponse>> => {
    const response = await axiosInstance.post<ApiResponse<BalanceInquiryResponse>>('/bank/inquiry/balance', request);
    return response.data;
};

/**
 * 수취인 조회를 수행합니다.
 * 통합 보안 유틸리티(prepareSecureRequest)를 사용하여 암호화 및 서명을 자동 처리합니다.
 */
export const getRecipient = async (
    bankCode: string,
    accountNo: string
): Promise<ApiResponse<TransferRecipientResponse>> => {
    try {
        // 1. 보안 요청 준비 (암호화 + 서명 + 키ID 통합 처리)
        // 민감정보: 수취인 계좌번호, 비민감정보: 입금은행 코드 (서명 포함 대상)
        const secureRequest = await prepareSecureRequest(
            { depositAccountNo: accountNo },
            { depositBankCode: bankCode },
            bankCode
        );

        if (!secureRequest) {
            throw new Error('보안 요청 준비 실패');
        }

        const { payload, headers } = secureRequest;

        // 2. 요청 객체 구성 (payload에 이미 reqPayload와 depositBankCode가 포함되어 있음)
        const request: TransferRecipientRequest = {
            ...(payload as any)
        };

        const response = await axiosInstance.post<ApiResponse<TransferRecipientResponse>>('/bank/transfer/recipient', request, {
            headers
        });        
        return response.data;
    } catch (error) {
        console.error('getRecipient Error:', error);
        return {
            success: false,
            data: null as any,
            error: { code: 'CLIENT_ERROR', message: '요청 처리 중 오류가 발생했습니다.' }
        };
    }
};

export const executeTransfer = async (request: TransferRequest): Promise<ApiResponse<TransferResponse>> => {
    const response = await axiosInstance.post<ApiResponse<TransferResponse>>('/bank/transfer', request);
    return response.data;
};
