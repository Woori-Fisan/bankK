import axiosInstance from './axiosInstance';
import { hybridEncrypt } from '../utils/bankCrypto';
import { createJwsSignature } from '../utils/authCrypto';
import { useBankKeyStore } from '../store/useBankKeyStore';

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
 * 입금 계좌번호(depositAccountNo) 등 민감정보를 하이브리드 암호화한 전체 JWE(reqPayload)를 전송합니다.
 */
export const getRecipient = async (
    bankCode: string,
    accountNo: string
): Promise<ApiResponse<TransferRecipientResponse>> => {
    try {
        // 0. 암호화에 사용할 키 ID 조회
        const keyId = useBankKeyStore.getState().getBankKeyId(bankCode);
        if (!keyId) {
            throw new Error(`은행[${bankCode}]의 키 ID를 찾을 수 없습니다.`);
        }

        // 1. 민감 정보(계좌번호) 암호화 (JWE 전체 반환)
        const encryptionResult = await hybridEncrypt({ depositAccountNo: accountNo }, bankCode);
        
        if (!encryptionResult) {
            throw new Error('수취인 조회 암호화 실패');
        }

        // 2. JWS 전자서명 생성 (위변조 방지 + Replay Attack 방지)
        // 페이로드에는 전체 JWE 덩어리, 은행 코드, 그리고 타임스탬프를 포함
        const jwsSignature = await createJwsSignature({
            reqPayload: encryptionResult.reqPayload,
            depositBankCode: bankCode,
            timestamp: Date.now()
        });

        if (!jwsSignature) {
            throw new Error('JWS 서명 생성 실패');
        }

        // 3. 요청 객체 구성 (Body에는 민감 데이터 암호문과 은행 코드만)
        const request: TransferRecipientRequest = {
            reqPayload: encryptionResult.reqPayload,
            depositBankCode: bankCode
        };

        const response = await axiosInstance.post<ApiResponse<TransferRecipientResponse>>('/bank/transfer/recipient', request, {
            headers: {
                'x-jws-signature': jwsSignature,
                'x-bank-key-id': keyId
            }
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
