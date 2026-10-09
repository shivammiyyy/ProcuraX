import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import { useParams } from 'react-router-dom';

const WorkspaceApp = lazy(() => import('./platform/WorkspaceApp'));

function LegacyRfqRedirect() {
  const { id } = useParams();
  return <Navigate to={id ? `/workspace/rfqs/${id}` : '/workspace/rfqs'} replace />;
}

function LegacyQuotationRedirect() {
  const { id, rfqId } = useParams();
  const destination = rfqId
    ? `/workspace/quotations/new/${rfqId}`
    : id
      ? `/workspace/quotations/${id}`
      : '/workspace/quotations';
  return <Navigate to={destination} replace />;
}

function LegacyContractRedirect() {
  const { id } = useParams();
  return <Navigate to={id ? `/workspace/contracts/${id}` : '/workspace/contracts'} replace />;
}

function App() {
  return (
    <Router>
      <Suspense fallback={<div className="workspace-startup">Loading ProcuraX…</div>}>
        <Routes>
          <Route path="/" element={<Navigate to="/workspace" replace />} />
          <Route path="/login" element={<Navigate to="/workspace" replace />} />
          <Route path="/signup" element={<Navigate to="/workspace" replace />} />
          <Route path="/dashboard" element={<Navigate to="/workspace" replace />} />
          <Route path="/workspace/*" element={<WorkspaceApp />} />
          <Route path="/rfqs" element={<LegacyRfqRedirect />} />
          <Route path="/rfqs/create" element={<Navigate to="/workspace/rfqs" replace />} />
          <Route path="/rfqs/:id" element={<LegacyRfqRedirect />} />
          <Route path="/quotations" element={<LegacyQuotationRedirect />} />
          <Route path="/quotations/create/:rfqId" element={<LegacyQuotationRedirect />} />
          <Route path="/quotations/:id" element={<LegacyQuotationRedirect />} />
          <Route path="/contracts" element={<LegacyContractRedirect />} />
          <Route path="/contracts/create/:quotationId" element={<Navigate to="/workspace/contracts" replace />} />
          <Route path="/contracts/:id" element={<LegacyContractRedirect />} />
          <Route path="*" element={<Navigate to="/workspace" replace />} />
        </Routes>
      </Suspense>
    </Router>
  );
}

export default App;