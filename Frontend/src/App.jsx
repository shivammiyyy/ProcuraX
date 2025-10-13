import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import RfqListPage from './pages/rfqs/RfqListPage';
import RfqCreatePage from './pages/rfqs/RfqCreatePage';
import RfqDetailsPage from './pages/rfqs/RfqDetailsPage';
import QuotationListPage from './pages/Quotations/QuotationListPage';
import QuotationCreatePage from './pages/Quotations/QuotationCreatePage';
import QuotationDetailsPage from './pages/Quotations/QuotationDetailsPage';
import ProtectedRoute from './components/auth/ProtectedRoute';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/rfqs" element={<ProtectedRoute><RfqListPage /></ProtectedRoute>} />
          <Route path="/rfqs/create" element={<ProtectedRoute><RfqCreatePage /></ProtectedRoute>} />
          <Route path="/rfqs/:id" element={<ProtectedRoute><RfqDetailsPage /></ProtectedRoute>} />
          <Route path="/quotations" element={<ProtectedRoute><QuotationListPage /></ProtectedRoute>} />
          <Route path="/quotations/create/:rfqId" element={<ProtectedRoute><QuotationCreatePage /></ProtectedRoute>} />
          <Route path="/quotations/:id" element={<ProtectedRoute><QuotationDetailsPage /></ProtectedRoute>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
