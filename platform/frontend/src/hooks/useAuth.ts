import { useAuthStore } from '../store/useAuthStore';

/**
 * 사용자 권한 관련 중앙 집중화된 훅
 */
export const useAuth = () => {
    const { userRole, setUserRole, accessToken, setAccessToken, isAdmin, clearAuth } = useAuthStore();

    return {
        userRole,
        setUserRole,
        accessToken,
        setAccessToken,
        isAdmin: isAdmin(), // 관리자 여부 (AGENCY_ADMIN)
        clearAuth
    };
};

