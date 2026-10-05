import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import { lazy, Suspense } from 'react';

const LoginPage = lazy(() => import('./pages/auth/LoginPage'));
const SignupPage = lazy(() => import('./pages/auth/SignupPage'));
const RfqListPage = lazy(() => import('./pages/rfqs/RfqListPage'));
const RfqCreatePage = lazy(() => import('./pages/rfqs/RfqCreatePage'));
const RfqDetailsPage = lazy(() => import('./pages/rfqs/RfqDetailsPage'));
const QuotationListPage = lazy(() => import('./pages/Quotations/QuotationListPage'));
const QuotationCreatePage = lazy(() => import('./pages/Quotations/QuotationCreatePage'));
const QuotationDetailsPage = lazy(() => import('./pages/Quotations/QuotationDetailsPage'));
const ContractCreatePage = lazy(() => import('./pages/contract/ContractCreatePage'));
const ContractListPage = lazy(() => import('./pages/contract/ContractListPage'));
const ContractDetailsPage = lazy(() => import('./pages/contract/ContractDetailsPage'));
const WorkspaceApp = lazy(() => import('./platform/WorkspaceApp'));

function App() {
  return (
    <AuthProvider>
      <Router>
        <Suspense fallback={<div className="workspace-startup">Loading ProcuraX…</div>}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/" element={<Navigate to="/workspace" replace />} />
            <Route path="/dashboard" element={<Navigate to="/workspace" replace />} />
            <Route path="/workspace/*" element={<WorkspaceApp />} />
            <Route path="/rfqs" element={<ProtectedRoute><RfqListPage /></ProtectedRoute>} />
            <Route path="/rfqs/create" element={<ProtectedRoute role="buyer"><RfqCreatePage /></ProtectedRoute>} />
            <Route path="/rfqs/:id" element={<ProtectedRoute><RfqDetailsPage /></ProtectedRoute>} />
            <Route path="/quotations" element={<ProtectedRoute><QuotationListPage /></ProtectedRoute>} />
            <Route path="/quotations/create/:rfqId" element={<ProtectedRoute role="vendor"><QuotationCreatePage /></ProtectedRoute>} />
            <Route path="/quotations/:id" element={<ProtectedRoute><QuotationDetailsPage /></ProtectedRoute>} />
            <Route path="/contracts" element={<ProtectedRoute><ContractListPage /></ProtectedRoute>} />
            <Route path="/contracts/create/:quotationId" element={<ProtectedRoute role="buyer"><ContractCreatePage /></ProtectedRoute>} />
            <Route path="/contracts/:id" element={<ProtectedRoute><ContractDetailsPage /></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </Router>
    </AuthProvider>
  );
}

export default App;