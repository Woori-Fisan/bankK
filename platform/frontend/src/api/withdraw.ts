import axiosInstance from './axiosInstance';
import type { WithdrawalRequest, WithdrawApiResponse } from '../types/withdraw';
import { prepareSecureRequest, decryptBankResponse } from '../utils/bankCrypto';

/**
 * 출금 실행 API 호출
 * @param request 출금 신청 정보 (암호화 및 서명 포함)
 * @returns 출금 처리 결과 (트랜잭션 ID, 잔액 등)
 */
export const executeWithdraw = async (request: WithdrawalRequest): Promise<WithdrawApiResponse> => {
    try {
        // 1. 보안 요청 준비 (암호화 및 서명)
        const secureRequest = await prepareSecureRequest(
            { 
                withdrawalAccountNo: request.withdrawalAccountNo,
                withdrawalPassword: request.withdrawalPassword,
                customerRrnPrefix: request.customerRrnPrefix
            },
            { 
                amount: request.amount,
                withdrawalBankCode: request.withdrawalBankCode
            },
            request.withdrawalBankCode
        );

        if (!secureRequest) {
            throw new Error('출금 보안 요청 준비 실패');
        }

        const { payload, headers, aesKey } = secureRequest;

        // 2. API 전송
        const response = await axiosInstance.post<WithdrawApiResponse>('/bank/withdrawals', payload, {
            headers: { ...headers, 'X-Idempotency-Key': crypto.randomUUID() }
        });

        // 3. 응답 복호화 (성공 시에만)
        if (response.data.success && (response.data.data as any)?.resPayload) {
            const decryptedData = await decryptBankResponse(
                (response.data.data as any).resPayload,
                aesKey
            );
            return {
                ...response.data,
                data: { ...response.data.data, ...decryptedData }
            };
        }


        return response.data;
    } catch (error) {
        console.error('출금 API 호출 실패:', error);
        throw error;
    }
};
