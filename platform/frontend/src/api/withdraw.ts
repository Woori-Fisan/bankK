import axiosInstance from './axiosInstance';
import type { WithdrawalRequest, WithdrawApiResponse } from '../types/withdraw';
import { prepareSecureRequest, decryptBankResponse } from '../utils/bankCrypto';

export interface WithdrawResponsePayload {
    resPayload: string; // 암호화된 잔액 정보
    transactionId: string;
    transactionDate: string;
}

/**
 * 출금 실행 API 호출
 * @param request 출금 신청 정보 (암호화 및 서명 포함)
 * @returns 출금 처리 결과 (트랜잭션 ID, 잔액 등)
 */
export const executeWithdraw = async (request: WithdrawalRequest): Promise<WithdrawApiResponse> => {
    try {
        // 1. 보안 요청 준비 (암호화 + 서명 + 키ID 통합 처리)
        const secureRequest = await prepareSecureRequest(
            { 
                withdrawalAccountNo: request.withdrawalAccountNo,
                withdrawalPassword: request.withdrawalPassword,
                customerRrnPrefix: request.customerRrnPrefix
            }, // 민감 데이터
            { 
                withdrawalBankCode: request.withdrawalBankCode,
                amount: request.amount
            }, // 비민감 데이터 (서명에 포함)
            request.withdrawalBankCode
        );

        if (!secureRequest) {
            throw new Error('출금 보안 요청 준비 실패');
        }

        const { payload, headers, aesKey } = secureRequest;

        // 2. 요청 전송
        // 서버의 응답 구조가 WithdrawResponsePayload (resPayload 포함) 라고 가정
        const response = await axiosInstance.post<WithdrawApiResponse>('/bank/withdrawals', payload, {
            headers
        });

        if (!response.data.success || !(response.data.data as any).resPayload) {
            return response.data;
        }

        // 3. 응답 복호화
        const decryptedSensitiveData = await decryptBankResponse(
            (response.data.data as any).resPayload,
            aesKey
        );

        // 4. 데이터 병합 후 반환
        const responseData = response.data.data as any;
        return {
            ...response.data,
            data: {
                transactionId: responseData.transactionId,
                transactionDate: responseData.transactionDate,
                balanceAfter: decryptedSensitiveData.balanceAfter
            }
        };

    } catch (error) {
        console.error('executeWithdraw Error:', error);
        return {
            success: false,
            data: null as any,
            error: { code: 'CLIENT_ERROR', message: '출금 요청 처리 중 오류가 발생했습니다.' }
        };
    }
};
