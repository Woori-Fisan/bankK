import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '../components/common/Layout';
import LoginPage from '../pages/LoginPage';
import DashboardPage from '../pages/DashboardPage';

const Router = () => {

  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />

      <Route element={<Layout />}>
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Route>
      
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default Router;
