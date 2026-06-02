import axiosInstance from './axiosInstance';
import { prepareSecureRequest, prepareTransferSecureRequest, decryptBankResponse } from '../utils/bankCrypto';

export interface ErrorResponse {
    code: string;
    message: string;
}

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    error?: ErrorResponse;
}

export interface DecryptedRecipientResult {
    depositorName: string;
    depositBankAccountNo: string;
    depositBankName: string;
    accountStatus: string;
}

export interface TransferRecipientResponse {
    resPayload: string;
    depositBankName: string;
    accountStatus: string;
}

export interface TransferRequest {
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

        if (!response.data.success || !response.data.data?.resPayload) {
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
            depositBankName: response.data.data?.depositBankName,
            accountStatus: response.data.data?.accountStatus
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
    try {
        // 1. 보안 요청 준비 (출금/입금 은행 각각 암호화 및 통합 서명)
        const secureRequest = await prepareTransferSecureRequest(
            { 
                withdrawalAccountNo: request.withdrawalAccountNo,
                withdrawalPassword: request.withdrawalPassword,
                customerRrnPrefix: request.customerRrnPrefix,
                depositAccountNo: request.depositAccountNo // 출금 은행이 알 수 있도록 포함
            },
            { 
                depositAccountNo: request.depositAccountNo,
                withdrawalAccountNo: request.withdrawalAccountNo // 입금 은행이 알 수 있도록 포함
            },
            { 
                withdrawalBankCode: request.withdrawalBankCode,
                depositBankCode: request.depositBankCode,
                amount: request.amount
            }
        );

        if (!secureRequest) {
            throw new Error('이체 보안 요청 준비 실패');
        }

        const { payload, headers, withdrawAesKey } = secureRequest;

        // 2. 요청 전송
        const response = await axiosInstance.post<ApiResponse<TransferResponse>>('/bank/transfer', payload, {
            headers
        });

        // 3. 응답 복호화 (출금 후 잔액 정보는 출금 은행의 응답이므로 withdrawAesKey 사용)
        if (response.data.success && (response.data.data as any)?.resPayload) {
            const decryptedData = await decryptBankResponse(
                (response.data.data as any).resPayload,
                withdrawAesKey
            );
            return {
                ...response.data,
                data: { ...response.data.data, ...decryptedData }
            };
        }

        return response.data;
    } catch (error) {
        console.error('executeTransfer Error:', error);
        return {
            success: false,
            data: null as any,
            error: { code: 'CLIENT_ERROR', message: '이체 요청 중 오류가 발생했습니다.' }
        };
    }
};
