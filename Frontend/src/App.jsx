import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';

// Dashboard
import DashboardPage from './pages/dashboard/DashboardPage';

// RFQ Pages
import RfqListPage from './pages/rfqs/RfqListPage';
import RfqCreatePage from './pages/rfqs/RfqCreatePage';
import RfqDetailsPage from './pages/rfqs/RfqDetailsPage';

// Quotation Pages
import QuotationListPage from './pages/Quotations/QuotationListPage';
import QuotationCreatePage from './pages/Quotations/QuotationCreatePage';
import QuotationDetailsPage from './pages/Quotations/QuotationDetailsPage';

// Contract Pages
import ContractCreatePage from './pages/contract/ContractCreatePage';
import ContractListPage from './pages/contract/ContractListPage';
import ContractDetailsPage from './pages/contract/ContractDetailsPage';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          {/* Protected Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />

          {/* RFQs */}
          <Route
            path="/rfqs"
            element={
              <ProtectedRoute>
                <RfqListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/rfqs/create"
            element={
              <ProtectedRoute role="buyer">
                <RfqCreatePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/rfqs/:id"
            element={
              <ProtectedRoute>
                <RfqDetailsPage />
              </ProtectedRoute>
            }
          />

          {/* Quotations */}
          <Route
            path="/quotations"
            element={
              <ProtectedRoute>
                <QuotationListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/quotations/create/:rfqId"
            element={
              <ProtectedRoute role="vendor">
                <QuotationCreatePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/quotations/:id"
            element={
              <ProtectedRoute>
                <QuotationDetailsPage />
              </ProtectedRoute>
            }
          />

          {/* Contracts */}
          <Route
            path="/contracts"
            element={
              <ProtectedRoute>
                <ContractListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/contracts/create/:quotationId"
            element={
              <ProtectedRoute role="buyer">
                <ContractCreatePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/contracts/:id"
            element={
              <ProtectedRoute>
                <ContractDetailsPage />
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;