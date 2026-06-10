import axiosInstance from './axiosInstance';
import { prepareSecureRequest, decryptBankResponse } from '../utils/bankCrypto';
import type { ApiResponse } from './transfer'; // ApiResponse 타입 재사용
import { extractApiErrorMessage } from '../utils/apiError';

export interface InquiryRequest {
    bankCode: string;
    accountNo: string;
    customerRrnPrefix: string;
    customerName: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
}

export interface BalanceInquiryResponse {
    resPayload: string; // 암호화된 잔액 정보
    status: string;
}

export interface DecryptedBalanceResult {
    balance: string;
    status: string;
}

export interface HistoryInquiryResponse {
    resPayload: string; // 암호화된 거래 내역 정보
    status: string;
    totalCount: number;
    totalPages: number;
}

export interface DecryptedHistoryResult {
    history: any[]; // 복호화된 상세 내역 배열
    status: string;
    totalCount: number;
    totalPages: number;
}

/**
 * 잔액 조회 API 호출
 */
export const fetchBalance = async (request: InquiryRequest): Promise<ApiResponse<DecryptedBalanceResult>> => {
    try {
        // 1. 보안 요청 준비 (암호화 + 서명 + 키ID 통합 처리)
        const secureRequest = await prepareSecureRequest(
            { 
                accountNo: request.accountNo,
                customerRrnPrefix: request.customerRrnPrefix, 
                customerName: request.customerName
            }, // 민감 데이터
            { bankCode: request.bankCode }, // 비민감 데이터 (서명에 포함)
            request.bankCode
        );

        if (!secureRequest) {
            throw new Error('잔액 조회 보안 요청 준비 실패');
        }

        const { payload, headers, aesKey } = secureRequest;

        // 2. 요청 전송
        const response = await axiosInstance.post<ApiResponse<BalanceInquiryResponse>>('/bank/inquiry/balance', payload, {
            headers
        });

        if (!response.data.success || !response.data.data?.resPayload) {
            return response.data as any;
        }

        // 3. 응답 복호화
        const decryptedSensitiveData = await decryptBankResponse(
            response.data.data.resPayload,
            aesKey
        );

        // 4. 데이터 병합 후 반환
        return {
            success: true,
            data: {
                balance: String(decryptedSensitiveData.balance),
                status: response.data.data?.status
            }
        };

    } catch (error) {
        console.error('fetchBalance Error:', error);
        return {
            success: false,
            data: null as any,
            error: { code: 'CLIENT_ERROR', message: extractApiErrorMessage(error, '잔액 조회 중 오류가 발생했습니다.') }
        };
    }
};

/**
 * 거래 내역 조회 API 호출
 */
export const fetchTransactionHistory = async (request: InquiryRequest): Promise<ApiResponse<DecryptedHistoryResult>> => {
    try {
        // 1. 보안 요청 준비
        const secureRequest = await prepareSecureRequest(
            { 
                accountNo: request.accountNo,
                customerRrnPrefix: request.customerRrnPrefix ,
                customerName: request.customerName
            }, // 민감 데이터
            { 
                bankCode: request.bankCode,
                startDate: request.startDate,
                endDate: request.endDate,
                page: request.page,
                size: request.size
            }, // 비민감 데이터 및 페이징 조건
            request.bankCode
        );

        if (!secureRequest) {
            throw new Error('거래 내역 조회 보안 요청 준비 실패');
        }

        const { payload, headers, aesKey } = secureRequest;

        // 2. 요청 전송
        const response = await axiosInstance.post<ApiResponse<HistoryInquiryResponse>>('/bank/inquiry/history', payload, {
            headers
        });

        if (!response.data.success || !response.data.data?.resPayload) {
            return response.data as any;
        }

        // 3. 응답 복호화
        const decryptedSensitiveData = await decryptBankResponse(
            response.data.data.resPayload,
            aesKey
        );

        // 4. 데이터 병합 후 반환
        return {
            success: true,
            data: {
                history: decryptedSensitiveData.history || [],
                status: response.data.data?.status,
                totalCount: response.data.data?.totalCount,
                totalPages: response.data.data?.totalPages
            }
        };


    } catch (error) {
        console.error('fetchTransactionHistory Error:', error);
        return {
            success: false,
            data: null as any,
            error: { code: 'CLIENT_ERROR', message: extractApiErrorMessage(error, '거래 내역 조회 중 오류가 발생했습니다.') }
        };
    }
};
