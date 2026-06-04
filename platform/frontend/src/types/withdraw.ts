import type { ApiCommonResponse } from './common';

/**
 * 출금 신청 요청 데이터 구조 (API 전송용 DTO)
 */
export interface WithdrawalRequest {
    withdrawalBankCode: string;
    withdrawalAccountNo: string;
    withdrawalPassword: string;
    customerRrnPrefix: string;
    amount: number;
}

/**
 * 출금 신청 시 내부 상태 관리용 데이터 구조
 */
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

/**
 * UI 노출용 출금 결과 데이터 구조
 */
export interface WithdrawResult {
    balanceBefore: number;
    balanceAfter: number;
    transactionId: string;
    dateTime: string;
}

/**
 * 출금 처리 성공 시 반환되는 API 데이터 구조
 */
export interface WithdrawResponse {
    transactionId: string;
    transactionDate: string;
    balanceAfter: string;
}

/**
 * ApiCommonResponse를 활용한 최종 출금 API 응답 타입
 */
export type WithdrawApiResponse = ApiCommonResponse<WithdrawResponse>;
