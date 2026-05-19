import axios from 'axios';

/**
 * 잔액 조회 API 호출
 * payload에 jwsSignature, encryptedKey 등을 포함한 완성된 DTO가 전달됩니다.
 */
export const fetchBalance = async (payload: object) => {
    const response = await axios.post('/api/v1/bank/inquiry/balance', payload);
    return response.data;
};

/**
 * 거래 내역 조회 API 호출
 * payload에 jwsSignature, encryptedKey 등을 포함한 완성된 DTO가 전달됩니다.
 */
export const fetchTransactionHistory = async (payload: object) => {
    const response = await axios.post('/api/v1/bank/inquiry/history', payload);
    return response.data;
};
