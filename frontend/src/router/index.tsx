import { useEffect } from 'react';
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { clearSession, getAccessToken } from '../services/session';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import { InvoicesPage } from '../pages/invoices/InvoicesPage';
import { LoginPage } from '../pages/login/LoginPage';
import { PaymentConfigPage } from '../pages/merchant/PaymentConfigPage';
import { PublicPaymentPage } from '../pages/payment/PublicPaymentPage';
import { RegisterPage } from '../pages/register/RegisterPage';

function RequireAuth() {
  const location = useLocation();

  if (!getAccessToken()) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}

function PublicOnlyRoute() {
  if (getAccessToken()) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}

function RootRedirect() {
  return <Navigate to={getAccessToken() ? '/dashboard' : '/login'} replace />;
}

function LogoutRedirect() {
  const navigate = useNavigate();

  useEffect(() => {
    clearSession();
    navigate('/login', { replace: true });
  }, [navigate]);

  return null;
}

export function AppRouter() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/invoices" element={<InvoicesPage />} />
        <Route path="/invoices/:invoiceId" element={<InvoicesPage />} />
        <Route path="/settings/payment-config" element={<PaymentConfigPage />} />
        <Route path="/logout" element={<LogoutRedirect />} />
      </Route>
      <Route path="/pay/:publicId" element={<PublicPaymentPage />} />
      <Route path="/" element={<RootRedirect />} />
      <Route path="*" element={<RootRedirect />} />
    </Routes>
  );
}
