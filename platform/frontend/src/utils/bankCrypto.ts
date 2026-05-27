import axiosInstance from '../api/axiosInstance';
import { useBankKeyStore } from '../store/useBankKeyStore';

/**
 * 모든 은행의 최신 RSA 공개키를 서버로부터 조회하여 Zustand 저장소에 업데이트합니다.
 */
export const refreshBankPublicKeys = async (): Promise<void> => {
    try {
        const response = await axiosInstance.get('/keys/public');
        
        // 백엔드 ApiResponse 구조: response.data.data 에 Map<String, BankRsaKeyResponse> 가 들어있음
        const keys = response.data?.data;
        
        if (keys) {
            useBankKeyStore.getState().setAllBankKeys(keys);
            console.log('모든 은행의 공개키가 성공적으로 갱신되었습니다.', keys);
        }
    } catch (error) {
        console.error('은행 공개키 갱신 실패:', error);
    }
};
