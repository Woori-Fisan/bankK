import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '../components/common/Layout';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import DashboardPage from '../pages/DashboardPage';
import MetricsPage from '../pages/LogDashboardPage';
import MetricDashboardPage from '../pages/MetricDashboardPage';
import PrivateRoute from './PrivateRoute';

const Router = () => {

  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />
      <Route path="signup" element={<SignupPage />} />

      <Route element={<PrivateRoute />}>
        <Route element={<Layout />}>
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="metrics" element={<MetricsPage />} />
          <Route path="metric-dashboard" element={<MetricDashboardPage />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default Router;
