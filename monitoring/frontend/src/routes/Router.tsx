import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from '../pages/LoginPage';

const Router = () => {

  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default Router;
