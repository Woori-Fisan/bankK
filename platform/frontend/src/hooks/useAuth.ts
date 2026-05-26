import { useAuthStore } from '../store/useAuthStore';

/**
 * 사용자 권한 관련 중앙 집중화된 훅
 */
export const useAuth = () => {
    const { userId, setUserId, userRole, setUserRole, accessToken, setAccessToken, loginTime, setLoginTime, tokenExpiry, setTokenExpiry, isAdmin, clearAuth } = useAuthStore();

    return {
        userId,
        setUserId,
        userRole,
        setUserRole,
        accessToken,
        setAccessToken,
        loginTime,
        setLoginTime,
        tokenExpiry,
        setTokenExpiry,
        isAdmin: isAdmin(), // 관리자 여부 (AGENCY_ADMIN)
        clearAuth
    };
};

