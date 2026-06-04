import { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import Router from './routes/Router';
import { useAuthStore } from './store/useAuthStore';
import { refreshAccessToken } from './api/auth';

function App() {
  const { setLoginTime, clearAuth } = useAuthStore();
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = await refreshAccessToken();
        if (token) {
          // localStorage에서 접속 시간 복구 (그 외 상태는 refreshAccessToken 내부에서 처리됨)
          const savedLoginTime = localStorage.getItem('loginTime');
          if (savedLoginTime) {
            setLoginTime(savedLoginTime);
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
  }, [setLoginTime, clearAuth]);

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
