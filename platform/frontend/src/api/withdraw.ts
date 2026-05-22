import axiosInstance from './axiosInstance';
import type { WithdrawalRequest, WithdrawApiResponse } from '../types/withdraw';

/**
 * 출금 실행 API 호출
 * @param request 출금 신청 정보 (암호화 및 서명 포함)
 * @returns 출금 처리 결과 (트랜잭션 ID, 잔액 등)
 */
export const executeWithdraw = async (request: WithdrawalRequest): Promise<WithdrawApiResponse> => {
    const response = await axiosInstance.post<WithdrawApiResponse>('/bank/withdrawals', request);
    return response.data;
};
