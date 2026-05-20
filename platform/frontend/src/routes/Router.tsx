import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '../components/common/Layout';
import MainPage from '../pages/MainPage';
import LoginPage from '../pages/LoginPage';
import PinpadTestPage from '../pages/PinpadTestPage';
import AccountInquiry from '../pages/AccountInquiry';
import WithdrawPage from '../pages/WithdrawPage';
import LoanApplication from '../pages/LoanApplication';
import TransferPage from '../pages/TransferPage';
import PrivateRoute from './PrivateRoute';

const Router = () => {
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
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
};

export default Router;
