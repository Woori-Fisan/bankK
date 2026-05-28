import axiosInstance from './axiosInstance';
import { prepareSecureRequest, decryptBankResponse } from '../utils/bankCrypto';

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

/**
 * 수취인 조회 응답 (네트워크 수신용)
 */
export interface TransferRecipientResponse {
    resPayload: string; // 암호화된 민감 정보
    depositBankName: string;
    accountStatus: string;
}

/**
 * 수취인 조회 최종 결과 (복호화 후 UI 사용용)
 */
export interface DecryptedRecipientResult {
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
 * 통합 보안 유틸리티를 사용하여 요청을 암호화하고, 응답의 resPayload를 복호화합니다.
 */
export const getRecipient = async (
    bankCode: string,
    accountNo: string
): Promise<ApiResponse<DecryptedRecipientResult>> => {
    try {
        // 1. 보안 요청 준비 (암호화 + 서명 + 키ID 통합 처리)
        // aesKey는 요청과 응답 사이클 동안 메모리에 유지됨
        const secureRequest = await prepareSecureRequest(
            { depositAccountNo: accountNo },
            { depositBankCode: bankCode },
            bankCode
        );

        if (!secureRequest) {
            throw new Error('보안 요청 준비 실패');
        }

        const { payload, headers, aesKey } = secureRequest;

        // 2. 요청 전송 (네트워크 타입은 TransferRecipientResponse)
        const response = await axiosInstance.post<ApiResponse<TransferRecipientResponse>>('/bank/transfer/recipient', payload, {
            headers
        });

        if (!response.data.success || !response.data.data.resPayload) {
            return response.data as any;
        }

        // 3. 응답 복호화 (메모리에 보관 중이던 aesKey 사용)
        const decryptedSensitiveData = await decryptBankResponse(
            response.data.data.resPayload,
            aesKey
        );

        // 4. 평문 데이터와 복호화된 데이터를 병합하여 반환
        const finalData: DecryptedRecipientResult = {
            depositorName: decryptedSensitiveData.depositorName,
            depositBankAccountNo: decryptedSensitiveData.depositAccountNo,
            depositBankName: response.data.data.depositBankName,
            accountStatus: response.data.data.accountStatus
        };

        return {
            success: true,
            data: finalData
        };

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
