import { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import Router from './routes/Router';
import { useAuthStore } from './store/useAuthStore';
import { decodeJwt } from './utils/jwt';
import { refreshAccessToken } from './api/auth';

function App() {
  const { setAccessToken, setUserId, setUserRole, setLoginTime, setTokenExpiry, clearAuth } = useAuthStore();
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = await refreshAccessToken();
        if (token) {
          setAccessToken(token);
          const decoded = decodeJwt(token);
          if (decoded) {
            // loginId 필드에서 사용자 ID 추출 (없으면 sub 또는 id 사용)
            const userId = decoded.loginId || decoded.sub || decoded.id;
            if (userId) setUserId(userId);
            
            if (decoded.role) {
              setUserRole(decoded.role);
            }

            // 토큰 만료 시간 설정
            if (decoded.exp) {
              setTokenExpiry(decoded.exp * 1000);
            }

            // localStorage에서 접속 시간 복구
            const savedLoginTime = localStorage.getItem('loginTime');
            if (savedLoginTime) {
              setLoginTime(savedLoginTime);
            }
          } else {
            clearAuth();
            localStorage.removeItem('loginTime');
          }
        } else {
          clearAuth();
          localStorage.removeItem('loginTime');
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        clearAuth();
        localStorage.removeItem('loginTime');
      } finally {
        setIsInitialized(true);
      }
    };

    initializeAuth();
  }, [setAccessToken, setUserId, setUserRole, setLoginTime, clearAuth]);

  if (!isInitialized) {
    return <div>로딩 중...</div>; // 또는 스플래시 화면
  }

  return (
    <BrowserRouter>
      <Router />
    </BrowserRouter>
  );
}

export default App;
