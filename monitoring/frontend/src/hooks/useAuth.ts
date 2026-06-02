import { useAuthStore } from '../store/useAuthStore';

/**
 * 사용자 권한 관련 중앙 집중화된 훅
 */
export const useAuth = () => {
    const { userId, setUserId, accessToken, setAccessToken, loginTime, setLoginTime, tokenExpiry, setTokenExpiry, clearAuth } = useAuthStore();

    return {
        userId,
        setUserId,
        accessToken,
        setAccessToken,
        loginTime,
        setLoginTime,
        tokenExpiry,
        setTokenExpiry,
        clearAuth
    };
};

