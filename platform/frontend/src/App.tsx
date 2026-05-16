import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/common/Layout';
import MainPage from './pages/MainPage';
import LoginPage from './pages/LoginPage';
import PinpadTestPage from './pages/PinpadTestPage';
import AccountInquiry from './pages/AccountInquiry';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<Layout />}>
          <Route index element={<MainPage />} />
          <Route path="main" element={<MainPage />} />
          <Route path="inquiry" element={<AccountInquiry />} />
          <Route path="pinpad-test" element={<PinpadTestPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;

