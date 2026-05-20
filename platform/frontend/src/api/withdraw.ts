import axios from 'axios';
import type { WithdrawalRequest, WithdrawApiResponse } from '../types/withdraw';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1/bank',
    headers: {
        'Content-Type': 'application/json',
    },
});

/**
 * 출금 실행 API 호출
 * @param request 출금 신청 정보 (암호화 및 서명 포함)
 * @returns 출금 처리 결과 (트랜잭션 ID, 잔액 등)
 */
export const executeWithdraw = async (request: WithdrawalRequest): Promise<WithdrawApiResponse> => {
    const response = await api.post<WithdrawApiResponse>('/withdrawals', request);
    return response.data;
};
