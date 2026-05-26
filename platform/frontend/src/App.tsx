import { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import Router from './routes/Router';
import { useAuthStore } from './store/useAuthStore';
import { decodeJwt } from './utils/jwt';
import { refreshAccessToken } from './api/auth';

function App() {
  const { setAccessToken, setUserRole, clearAuth } = useAuthStore();
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = await refreshAccessToken();
        if (token) {
          setAccessToken(token);
          const decoded = decodeJwt(token);
          if (decoded && decoded.role) {
            setUserRole(decoded.role);
          } else {
            clearAuth();
          }
        } else {
          clearAuth();
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        clearAuth();
      } finally {
        setIsInitialized(true);
      }
    };

    initializeAuth();
  }, [setAccessToken, setUserRole, clearAuth]);

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
