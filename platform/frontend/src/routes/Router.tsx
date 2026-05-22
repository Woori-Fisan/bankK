import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '../components/common/Layout';
import MainPage from '../pages/MainPage';
import LoginPage from '../pages/LoginPage';
import PinpadTestPage from '../pages/PinpadTestPage';
import AccountInquiry from '../pages/AccountInquiry';
import WithdrawPage from '../pages/WithdrawPage';
import EmployeeManagementPage from '../pages/EmployeeManagementPage';
import LoanApplication from '../pages/LoanApplication';
import TransferPage from '../pages/TransferPage';
import PrivateRoute from './PrivateRoute';
import { useAuth } from '../hooks/useAuth';

const Router = () => {
  const { isAdmin } = useAuth();

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route path="login" element={<LoginPage />} />
        <Route element={<PrivateRoute />}>
          <Route index element={<MainPage />} />
          <Route path="main" element={<MainPage />} />

          <Route path="inquiry" element={<AccountInquiry />} />
          <Route path="pinpad-test" element={<PinpadTestPage />} />
          <Route path="loan" element={<LoanApplication />} />
          <Route path="withdraw" element={<WithdrawPage />} />
          <Route path="transfer" element={<TransferPage />} />

          {/* 관리자 전용 경로 */}
          {isAdmin && (
            <Route path="employee-management" element={<EmployeeManagementPage />} />
          )}
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default Router;
