import { useEffect, useState } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { refreshAccessToken } from '../api/auth';
import { decodeJwt } from '../utils/jwt';

export const useAuthInit = () => {
    const [isInitializing, setIsInitializing] = useState(true);
    const { setUserId, setLoginTime, setAccessToken, setTokenExpiry, clearAuth } = useAuthStore();

    useEffect(() => {
        const init = async () => {
            const savedUserId = localStorage.getItem('userId');
            const savedLoginTime = localStorage.getItem('loginTime');
            if (savedUserId) setUserId(savedUserId);
            if (savedLoginTime) setLoginTime(savedLoginTime);

            const refreshResult = await refreshAccessToken();
            if (refreshResult && refreshResult.accessToken) {
                setAccessToken(refreshResult.accessToken);
                if (refreshResult.refreshTokenExpiresIn) {
                    setTokenExpiry(Date.now() + refreshResult.refreshTokenExpiresIn * 1000);
                } else {
                    const decoded = decodeJwt(refreshResult.accessToken);
                    if (decoded?.exp) {
                        setTokenExpiry(decoded.exp * 1000);
                    }
                }
            } else {
                clearAuth();
                localStorage.removeItem('userId');
                localStorage.removeItem('loginTime');
            }

            setIsInitializing(false);
        };

        init();
    }, []);

    return { isInitializing };
};
