import { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import Router from './routes/Router';
import { useAuth } from './hooks/useAuth';
import { decodeJwt } from './utils/jwt';

function App() {
  const { setUserRole, clearAuth } = useAuth();

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      const decoded = decodeJwt(token);
      if (decoded && decoded.role) {
        setUserRole(decoded.role);
      } else {
        clearAuth();
      }
    }
  }, [setUserRole, clearAuth]);

  return (
    <BrowserRouter>
      <Router />
    </BrowserRouter>
  );
}

export default App;
