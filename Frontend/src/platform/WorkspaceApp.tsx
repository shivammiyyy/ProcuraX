import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  ArrowUpRight,
  ArrowLeft,
  Bell,
  Boxes,
  Building2,
  ChevronDown,
  CircleDollarSign,
  ClipboardCheck,
  FileCheck2,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  PackageCheck,
  ReceiptText,
  Plus,
  RefreshCw,
  Shield,
  ShieldCheck,
  ShoppingCart,
  Sparkles,
  Truck,
  X,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  apiErrorMessage,
  addVendorMember,
  addContractAudit,
  createContractDocumentDownload,
  decideContract,
  authorizePayment,
  beginGoogleLogin,
  createRfq,
  createInvoice,
  createContract,
  createPaymentMandate,
  createShipment,
  createVendor,
  decideApproval,
  getQuotation,
  getContract,
  getInvoice,
  getPayment,
  getPurchaseOrder,
  getReconciliation,
  getRfq,
  getShipment,
  getSession,
  logout,
  operatePayment,
  publishRfq,
  reconcileInvoice,
  revokePaymentMandate,
  switchOrganization,
  updateShipmentStatus,
  updateRfq,
  updateQuotationStatus,
  updateVendor,
  uploadContractDocument,
  listVendorDocuments,
  listContractAudits,
  listContractDecisions,
  uploadVendorDocument,
  verifyVendorDocument,
  submitQuotation,
} from "./api";
import {
  useApprovals,
  useContracts,
  useInvoices,
  usePaymentMandates,
  usePayments,
  usePurchaseOrders,
  useQuotations,
  useReconciliations,
  useRfqs,
  useShipments,
  useVendors,
} from "./queries";
import { SessionContext, useSession } from "./session";
import type { BusinessRecord, Rfq, SessionUser } from "./types";

const apiBase = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");

export default function WorkspaceApp() {
  const queryClient = useQueryClient();
  const sessionQuery = useQuery({ queryKey: ["session"], queryFn: getSession, retry: false });
  const [user, setUser] = useState<SessionUser | null>(null);
  const [sessionInitialized, setSessionInitialized] = useState(false);

  useEffect(() => {
    setUser(sessionQuery.data ?? null);
    setSessionInitialized(true);
  }, [sessionQuery.data]);

  if (sessionQuery.isPending || !sessionInitialized) {
    return <FullPageMessage title="Loading your workspace" detail="Checking your secure session…" />;
  }
  if (sessionQuery.isError) {
    return (
      <FullPageMessage
        title="Workspace unavailable"
        detail={apiErrorMessage(sessionQuery.error)}
        action={<button className="primary-button" onClick={() => void sessionQuery.refetch()}>Try again</button>}
      />
    );
  }
  if (!user) return <SignInPage />;

  return (
    <SessionContext.Provider value={{ user, updateUser: setUser }}>
      <WorkspaceShell
        onSignOut={async () => {
          await logout();
          await queryClient.clear();
          setUser(null);
        }}
      />
    </SessionContext.Provider>
  );
}

function FullPageMessage({ title, detail, action }: { title: string; detail: string; action?: ReactNode }) {
  return (
    <main className="workspace-auth-page">
      <div className="auth-brand"><Boxes size={23} /> ProcuraX</div>
      <section className="auth-card">
        <div className="auth-icon"><ShieldCheck size={23} /></div>
        <h1>{title}</h1>
        <p>{detail}</p>
        {action}
      </section>
    </main>
  );
}

function SignInPage() {
  return (
    <main className="workspace-auth-page">
      <div className="auth-brand"><Boxes size={23} /> ProcuraX</div>
      <section className="auth-card">
        <div className="auth-icon"><ShieldCheck size={23} /></div>
        <p className="eyebrow">PROCUREMENT WORKSPACE</p>
        <h1>Securely sign in</h1>
        <p>Continue with your organization identity provider to access procurement data.</p>
        <button className="primary-button auth-login" onClick={beginGoogleLogin}>
          Continue with Google
        </button>
        <div className="auth-footnote">Your organization and permissions are verified by ProcuraX.</div>
      </section>
      <div className="auth-legal">Secure, organization-scoped procurement operations</div>
    </main>
  );
}

function WorkspaceShell({ onSignOut }: { onSignOut: () => Promise<void> }) {
  const { user, updateUser } = useSession();
  const queryClient = useQueryClient();
  const streamAllowed = user.permissions.includes("EVENT_STREAM_READ");
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [streamConnected, setStreamConnected] = useState(false);
  const [actionError, setActionError] = useState("");
  const orgSwitch = useMutation({
    mutationFn: switchOrganization,
    onSuccess: async (nextUser) => {
      updateUser(nextUser);
      await queryClient.invalidateQueries();
      setActionError("");
    },
    onError: (error) => setActionError(apiErrorMessage(error)),
  });
  const signOut = useMutation({
    mutationFn: onSignOut,
    onError: (error) => setActionError(apiErrorMessage(error)),
  });

  useEffect(() => {
    if (!streamAllowed) return;
    const stream = new EventSource(`${apiBase}/api/v1/events`, { withCredentials: true });
    stream.onopen = () => setStreamConnected(true);
    stream.onerror = () => setStreamConnected(false);
    stream.addEventListener("procurement", () => {
      void queryClient.invalidateQueries({ queryKey: ["rfqs", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["rfq", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["quotations", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["quotation", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["approvals", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["purchase-orders", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["purchase-order", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["contracts", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["contract", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["contract-audits", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["contract-decisions", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["vendors", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["vendor-documents", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["payments", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["payment", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["payment-mandates", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["shipments", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["shipment", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["invoices", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["invoice", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["reconciliations", user.activeOrganization.id] });
      void queryClient.invalidateQueries({ queryKey: ["reconciliation", user.activeOrganization.id] });
    });
    return () => {
      stream.close();
      setStreamConnected(false);
    };
  }, [queryClient, streamAllowed, user.activeOrganization.id]);

  const location = useLocation();
  const title = useMemo(() => {
    if (location.pathname.endsWith("/rfqs")) return "Requests for quotation";
    if (location.pathname.endsWith("/quotations")) return "Vendor quotations";
    if (location.pathname.endsWith("/purchase-orders")) return "Purchase orders";
    if (location.pathname.includes("/contracts")) return "Contracts";
    if (location.pathname.endsWith("/approvals")) return "Approvals";
    if (location.pathname.endsWith("/vendors")) return "Vendors";
    if (location.pathname.endsWith("/payments")) return "Payments";
    if (location.pathname.endsWith("/fulfillment")) return "Fulfillment";
    return "Overview";
  }, [location.pathname]);

  return (
    <div className="workspace-frame">
      <aside className={`workspace-sidebar ${mobileNavOpen ? "is-open" : ""}`}>
        <Link className="workspace-logo" to="/workspace" onClick={() => setMobileNavOpen(false)}>
          <span className="workspace-logo-mark"><Boxes size={21} /></span>
          <span>Procura<span className="logo-accent">X</span><small>PROCUREMENT CLOUD</small></span>
        </Link>
        <div className="workspace-org-label">WORKSPACE</div>
        <div className="workspace-org">
          <span className="org-avatar"><Building2 size={16} /></span>
          <span className="org-copy"><strong>{user.activeOrganization.name}</strong><small>{user.activeOrganization.role.replaceAll("_", " ")}</small></span>
          {user.organizations.length > 1 && (
            <select
              aria-label="Switch organization"
              value={user.activeOrganization.id}
              disabled={orgSwitch.isPending}
              onChange={(event) => orgSwitch.mutate(event.target.value)}
            >
              {user.organizations.map((organization) => (
                <option key={organization.id} value={organization.id}>{organization.name}</option>
              ))}
            </select>
          )}
          {user.organizations.length > 1 && <ChevronDown className="org-switch-chevron" size={13} />}
        </div>
        <nav className="workspace-navigation" aria-label="Workspace navigation">
          <p>PROCUREMENT</p>
          <WorkspaceNavItem to="/workspace" icon={<LayoutDashboard size={18} />} label="Overview" end onNavigate={() => setMobileNavOpen(false)} />
          {user.permissions.includes("RFQ_READ") && <WorkspaceNavItem to="/workspace/rfqs" icon={<FileText size={18} />} label="RFQs" onNavigate={() => setMobileNavOpen(false)} />}
          {user.permissions.includes("QUOTE_READ") && <WorkspaceNavItem to="/workspace/quotations" icon={<FileCheck2 size={18} />} label="Quotations" onNavigate={() => setMobileNavOpen(false)} />}
          {user.permissions.includes("PO_READ") && <WorkspaceNavItem to="/workspace/purchase-orders" icon={<ShoppingCart size={18} />} label="Purchase orders" onNavigate={() => setMobileNavOpen(false)} />}
          {user.permissions.includes("CONTRACT_READ") && <WorkspaceNavItem to="/workspace/contracts" icon={<FileCheck2 size={18} />} label="Contracts" onNavigate={() => setMobileNavOpen(false)} />}
          {user.permissions.includes("APPROVAL_READ") && <WorkspaceNavItem to="/workspace/approvals" icon={<ClipboardCheck size={18} />} label="Approvals" onNavigate={() => setMobileNavOpen(false)} />}
          {user.permissions.includes("VENDOR_READ") && <WorkspaceNavItem to="/workspace/vendors" icon={<Building2 size={18} />} label="Vendors" onNavigate={() => setMobileNavOpen(false)} />}
          {user.permissions.includes("PAYMENT_READ") && <WorkspaceNavItem to="/workspace/payments" icon={<CircleDollarSign size={18} />} label="Payments" onNavigate={() => setMobileNavOpen(false)} />}
          {(user.permissions.includes("SHIPMENT_READ") || user.permissions.includes("INVOICE_READ") || user.permissions.includes("RECONCILIATION_READ")) && <WorkspaceNavItem to="/workspace/fulfillment" icon={<Truck size={18} />} label="Fulfillment" onNavigate={() => setMobileNavOpen(false)} />}
        </nav>
        <div className="sidebar-bottom">
          <div className={`stream-status ${streamConnected ? "is-live" : ""}`}>
            <span className="stream-dot" />
            <span>{!streamAllowed ? "Live updates unavailable" : streamConnected ? "Live updates connected" : "Live updates reconnecting"}</span>
          </div>
          <div className="sidebar-user">
            <div className="user-avatar">{initials(user.fullName)}</div>
            <div className="user-info"><strong>{user.fullName}</strong><small>{user.email}</small></div>
            <button aria-label="Sign out" title="Sign out" onClick={() => signOut.mutate()} disabled={signOut.isPending}>
              <LogOut size={17} />
            </button>
          </div>
        </div>
        {mobileNavOpen && <button className="mobile-close" onClick={() => setMobileNavOpen(false)} aria-label="Close menu"><X /></button>}
      </aside>

      <main className="workspace-main">
        <header className="workspace-topbar">
          <button className="mobile-menu" onClick={() => setMobileNavOpen(true)} aria-label="Open menu"><Menu /></button>
          <div className="breadcrumb"><span>Workspace</span><ChevronDown size={14} /><strong>{title}</strong></div>
          <div className="topbar-actions">
            {streamAllowed && <div className={`connection-pill ${streamConnected ? "online" : ""}`}><span />{streamConnected ? "Live" : "Reconnecting"}</div>}
            <button className="icon-button" aria-label="Notifications" title="Notifications"><Bell size={18} /></button>
            <div className="topbar-avatar" title={user.fullName}>{initials(user.fullName)}</div>
          </div>
        </header>
        {actionError && <div className="workspace-alert" role="alert">{actionError}</div>}
        <div className="workspace-content">
          <Routes>
            <Route index element={<DashboardPage />} />
            <Route path="rfqs" element={<RfqsPage />} />
            <Route path="rfqs/:id" element={<RfqDetailsPage />} />
            <Route path="quotations" element={<QuotationsPage />} />
            <Route path="quotations/new/:rfqId" element={<QuotationCreatePage />} />
            <Route path="quotations/:id" element={<QuotationDetailsPage />} />
            <Route path="purchase-orders" element={<PurchaseOrdersPage />} />
            <Route path="purchase-orders/:id" element={<PurchaseOrderDetailsPage />} />
            <Route path="contracts" element={<ContractsPage />} />
            <Route path="contracts/:id" element={<ContractDetailsPage />} />
            <Route path="approvals" element={<ApprovalsPage />} />
            <Route path="vendors" element={<VendorsPage />} />
            <Route path="payments" element={<PaymentsPage />} />
            <Route path="payments/:id" element={<PaymentDetailsPage />} />
            <Route path="fulfillment" element={<FulfillmentPage />} />
            <Route path="fulfillment/shipments/:id" element={<ShipmentDetailsPage />} />
            <Route path="fulfillment/invoices/:id" element={<InvoiceDetailsPage />} />
            <Route path="fulfillment/reconciliations/:id" element={<ReconciliationDetailsPage />} />
            <Route path="*" element={<Navigate to="/workspace" replace />} />
          </Routes>
        </div>
      </main>
    </div>
  );
}

function WorkspaceNavItem({ to, icon, label, end = false, onNavigate }: { to: string; icon: ReactNode; label: string; end?: boolean; onNavigate: () => void }) {
  return (
    <NavLink to={to} end={end} onClick={onNavigate} className={({ isActive }) => `workspace-nav-item ${isActive ? "active" : ""}`}>
      {icon}<span>{label}</span>
    </NavLink>
  );
}

function DashboardPage() {
  const { user } = useSession();
  const rfqs = useRfqs();
  const quotations = useQuotations();
  const purchaseOrders = usePurchaseOrders();
  const approvals = useApprovals();
  const anyLoading = rfqs.isLoading || quotations.isLoading || purchaseOrders.isLoading || approvals.isLoading;
  const chartData = useMemo(() => {
    const counts = new Map<string, number>();
    [...(rfqs.data ?? []), ...(quotations.data ?? [])].forEach((record) => {
      const status = record.status.toLowerCase().replaceAll("_", " ");
      counts.set(status, (counts.get(status) ?? 0) + 1);
    });
    return [...counts.entries()].map(([status, count]) => ({
      status: status.replace(/\b\w/g, (letter) => letter.toUpperCase()),
      count,
    }));
  }, [rfqs.data, quotations.data]);
  const metrics = [
    { label: "Active RFQs", value: rfqs.data?.filter((record) => ["PUBLISHED", "OPEN"].includes(record.status.toUpperCase())).length, icon: FileText, color: "blue", permitted: user.permissions.includes("RFQ_READ"), trend: "RFQ pipeline" },
    { label: "Quotations", value: quotations.data?.length, icon: FileCheck2, color: "violet", permitted: user.permissions.includes("QUOTE_READ"), trend: "Vendor responses" },
    { label: "Purchase orders", value: purchaseOrders.data?.length, icon: ShoppingCart, color: "green", permitted: user.permissions.includes("PO_READ"), trend: "Committed orders" },
    { label: "Approvals", value: approvals.data?.filter((item) => String(item.status ?? "").toUpperCase().includes("PENDING")).length, icon: ClipboardCheck, color: "amber", permitted: user.permissions.includes("APPROVAL_READ"), trend: "Awaiting decision" },
  ].filter((metric) => metric.permitted);
  const queryErrors = [rfqs, quotations, purchaseOrders, approvals].filter((query) => query.isError);

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">PROCUREMENT INTELLIGENCE</p>
          <h1>Good to see you, {user.fullName.split(" ")[0]}</h1>
          <p className="page-subtitle">Your procurement activity, approvals, and supplier responses in one place.</p>
        </div>
        {user.permissions.includes("RFQ_CREATE") && (
          <Link className="primary-button" to="/workspace/rfqs"><Plus size={17} /> Create RFQ</Link>
        )}
      </div>
      {queryErrors.map((query, index) => <div className="inline-error query-error" role="alert" key={index}>{apiErrorMessage(query.error)}</div>)}

      {anyLoading ? <div className="metric-grid">{[1, 2, 3, 4].map((key) => <div className="metric-card loading-skeleton" key={key} />)}</div> : (
        <section className="metric-grid">
          {metrics.map(({ label, value, icon: Icon, color, trend }) => (
            <article className="metric-card" key={label}>
              <div className={`metric-icon ${color}`}><Icon size={19} /></div>
              <div className="metric-label">{label}</div>
              <div className="metric-value">{value ?? "—"}</div>
              <div className="metric-trend"><ArrowUpRight size={14} />{trend}</div>
            </article>
          ))}
          {metrics.length === 0 && <EmptyState title="No workspace permissions" detail="Ask an organization administrator to grant procurement access." />}
        </section>
      )}

      <section className="dashboard-grid">
        <article className="surface-card activity-chart">
          <div className="section-heading">
            <div><h2>Procurement activity</h2><p>RFQ and quotation status overview</p></div>
            <span className="period-select">Current workspace</span>
          </div>
          {chartData.length ? (
            <div className="chart-area">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid stroke="#edf1f6" vertical={false} />
                  <XAxis dataKey="status" axisLine={false} tickLine={false} tick={{ fill: "#758198", fontSize: 12 }} />
                  <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: "#758198", fontSize: 12 }} />
                  <Tooltip cursor={{ fill: "#f4f7fb" }} />
                  <Bar dataKey="count" name="Items" fill="#3574e8" radius={[7, 7, 0, 0]} maxBarSize={48} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : <EmptyState title="Your activity will appear here" detail="Create an RFQ or receive a quotation to start tracking progress." />}
        </article>
        <article className="surface-card quick-actions">
          <div className="section-heading"><div><h2>Quick actions</h2><p>Get work moving</p></div><Sparkles size={18} className="subtle-icon" /></div>
          <div className="quick-action-list">
            {user.permissions.includes("RFQ_CREATE") && <QuickAction to="/workspace/rfqs" icon={<FileText />} title="Create a request" detail="Start a new RFQ" />}
            {user.permissions.includes("APPROVAL_READ") && <QuickAction to="/workspace/approvals" icon={<ClipboardCheck />} title="Review approvals" detail="See pending decisions" />}
            {user.permissions.includes("PO_READ") && <QuickAction to="/workspace/purchase-orders" icon={<PackageCheck />} title="Track purchase orders" detail="Review issued orders" />}
            {user.permissions.includes("QUOTE_READ") && <QuickAction to="/workspace/quotations" icon={<FileCheck2 />} title="Compare quotations" detail="Review supplier responses" />}
            {user.permissions.includes("RECONCILIATION_READ") && <div className="quick-action-readonly"><Activity size={17} /><span>Reconciliation insights are available through the procurement API.</span></div>}
          </div>
        </article>
      </section>

      <section className="surface-card recent-section">
        <div className="section-heading"><div><h2>Recently updated RFQs</h2><p>Latest activity in your organization</p></div>
          {user.permissions.includes("RFQ_READ") && <Link className="text-link" to="/workspace/rfqs">View all <ArrowUpRight size={15} /></Link>}
        </div>
        {rfqs.isError ? <InlineError error={rfqs.error} /> :
          <RfqTable rfqs={(rfqs.data ?? []).slice(0, 5)} showEmpty={!user.permissions.includes("RFQ_READ") || !rfqs.isLoading} canQuote={user.permissions.includes("QUOTE_SUBMIT")} />}
      </section>
    </>
  );
}

function RfqsPage() {
  const { user } = useSession();
  const queryClient = useQueryClient();
  const rfqs = useRfqs();
  const [showCreate, setShowCreate] = useState(false);
  const createMutation = useMutation({
    mutationFn: createRfq,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["rfqs", user.activeOrganization.id] });
      setShowCreate(false);
    },
  });
  const publishMutation = useMutation({
    mutationFn: publishRfq,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["rfqs", user.activeOrganization.id] }),
  });

  return (
    <>
      <div className="page-heading">
        <div><p className="eyebrow">SOURCING</p><h1>Requests for quotation</h1><p className="page-subtitle">Create and manage tenant-scoped sourcing requests.</p></div>
        {user.permissions.includes("RFQ_CREATE") && <button className="primary-button" onClick={() => setShowCreate(!showCreate)}>{showCreate ? <X size={17} /> : <Plus size={17} />}{showCreate ? "Close form" : "Create RFQ"}</button>}
      </div>
      {showCreate && <CreateRfqForm onSubmit={(request) => createMutation.mutate(request)} isPending={createMutation.isPending} error={createMutation.error} />}
      {rfqs.isLoading ? <LoadingPanel /> : rfqs.isError ? <InlineError error={rfqs.error} /> :
        <section className="surface-card table-card">
          <div className="section-heading"><div><h2>All requests</h2><p>{rfqs.data?.length ?? 0} requests in this organization</p></div><button className="quiet-button" onClick={() => void rfqs.refetch()}><RefreshCw size={15} /> Refresh</button></div>
          <RfqTable
            rfqs={rfqs.data ?? []}
            showEmpty
            canPublish={user.permissions.includes("RFQ_PUBLISH")}
            canQuote={user.permissions.includes("QUOTE_SUBMIT")}
            onPublish={(id) => publishMutation.mutate(id)}
            publishingId={publishMutation.variables}
          />
          {publishMutation.isError && <InlineError error={publishMutation.error} />}
        </section>}
      {!user.permissions.includes("RFQ_READ") && <PermissionNotice />}
    </>
  );
}

function CreateRfqForm({ onSubmit, isPending, error }: { onSubmit: (request: Parameters<typeof createRfq>[0]) => void; isPending: boolean; error: unknown }) {
  return <RfqForm onSubmit={onSubmit} isPending={isPending} error={error} submitLabel="Create draft RFQ" />;
}

function RfqForm({ initial, onSubmit, isPending, error, submitLabel }: {
  initial?: Rfq;
  onSubmit: (request: Parameters<typeof createRfq>[0]) => void;
  isPending: boolean;
  error: unknown;
  submitLabel: string;
}) {
  const [items, setItems] = useState(() => initial?.items.map((item) => ({
    key: item.id ?? crypto.randomUUID(),
    description: item.description,
    quantity: item.quantity,
    unit: item.unit ?? "",
    specification: item.specification ?? "",
  })) ?? [{ key: crypto.randomUUID(), description: "", quantity: 1, unit: "", specification: "" }]);
  const [deadline, setDeadline] = useState(() => {
    if (initial) return toLocalDateTimeInput(new Date(initial.deadline));
    const date = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
    return date.toISOString().slice(0, 16);
  });
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    onSubmit({
      title: String(values.get("title")),
      description: String(values.get("description")),
      requestType: String(values.get("requestType")) as "RFQ" | "RFP",
      category: String(values.get("category")),
      budget: Number(values.get("budget")),
      currency: String(values.get("currency")).toUpperCase(),
      deadline: new Date(deadline).toISOString(),
      deliveryDays: Number(values.get("deliveryDays")),
      items: items.map(({ key: _key, ...item }) => item),
    });
  };
  return (
    <form className="surface-card rfq-form" onSubmit={submit}>
      <div className="section-heading"><div><h2>{initial ? "Edit draft request" : "New sourcing request"}</h2><p>All amounts, dates, and requirements are submitted to the secure procurement API.</p></div></div>
      <div className="form-grid">
        <label>Title<input name="title" required maxLength={255} defaultValue={initial?.title} placeholder="e.g. Q4 office equipment" /></label>
        <label>Category<input name="category" required maxLength={100} defaultValue={initial?.category} placeholder="Office supplies" /></label>
        <label>Request type<select name="requestType" defaultValue={initial?.requestType ?? "RFQ"}><option value="RFQ">Request for quotation</option><option value="RFP">Request for proposal</option></select></label>
        <label>Budget<input name="budget" type="number" min="0.01" step="0.01" required defaultValue={initial?.budget} placeholder="0.00" /></label>
        <label>Currency<input name="currency" defaultValue={initial?.currency ?? "USD"} maxLength={3} minLength={3} required /></label>
        <label>Delivery window (days)<input name="deliveryDays" type="number" min="1" max="365" defaultValue={initial?.deliveryDays ?? 30} required /></label>
        <label>Response deadline<input type="datetime-local" value={deadline} min={toLocalDateTimeInput(new Date())} onChange={(event) => setDeadline(event.target.value)} required /></label>
        <label className="form-wide">Description<textarea name="description" required maxLength={10000} rows={3} defaultValue={initial?.description} placeholder="Provide scope and context for suppliers." /></label>
      </div>
      <div className="rfq-items-editor">
        <div className="section-heading"><div><h3>Line items</h3><p>Specify each requested product or service; vendors quote every item.</p></div>
          <button type="button" className="table-action" disabled={items.length >= 100} onClick={() => setItems((current) => [...current, { key: crypto.randomUUID(), description: "", quantity: 1, unit: "", specification: "" }])}><Plus size={14} /> Add line</button>
        </div>
        {items.map((item, index) => <div className="rfq-item-editor-row" key={item.key}>
          <div className="rfq-item-heading"><strong>Line {index + 1}</strong>{items.length > 1 && <button type="button" className="quiet-button" onClick={() => setItems((current) => current.filter((_, itemIndex) => itemIndex !== index))}>Remove</button>}</div>
          <label>Item description<input value={item.description} maxLength={500} required onChange={(event) => setItems((current) => current.map((value, itemIndex) => itemIndex === index ? { ...value, description: event.target.value } : value))} /></label>
          <label>Quantity<input type="number" min="1" required value={item.quantity} onChange={(event) => setItems((current) => current.map((value, itemIndex) => itemIndex === index ? { ...value, quantity: Number(event.target.value) } : value))} /></label>
          <label>Unit<input value={item.unit} maxLength={30} placeholder="each" onChange={(event) => setItems((current) => current.map((value, itemIndex) => itemIndex === index ? { ...value, unit: event.target.value } : value))} /></label>
          <label>Specifications<textarea value={item.specification} maxLength={5000} rows={2} placeholder="Optional item requirements" onChange={(event) => setItems((current) => current.map((value, itemIndex) => itemIndex === index ? { ...value, specification: event.target.value } : value))} /></label>
        </div>)}
      </div>
      {error ? <div className="inline-error">{apiErrorMessage(error)}</div> : null}
      <div className="form-footer"><span>Drafts can be edited; published requests are locked.</span><button className="primary-button" disabled={isPending}>{isPending ? "Saving…" : submitLabel}</button></div>
    </form>
  );
}

function QuotationsPage() {
  const { user } = useSession();
  const quotations = useQuotations();
  const rfqs = useRfqs();
  const titleByRfq = new Map((rfqs.data ?? []).map((rfq) => [rfq.id, rfq.title]));
  return (
    <>
      <PageIntro eyebrow="SUPPLIER RESPONSES" title="Vendor quotations" detail="Review submitted quotations linked to your organization's RFQs." />
      {quotations.isLoading ? <LoadingPanel /> : quotations.isError ? <InlineError error={quotations.error} /> :
        <section className="surface-card table-card"><div className="section-heading"><div><h2>All quotations</h2><p>{quotations.data?.length ?? 0} supplier responses</p></div></div>
          {quotations.data?.length ? <div className="table-scroll"><table><thead><tr><th>RFQ</th><th>Amount</th><th>Delivery</th><th>Status</th><th>Quotation</th><th>Actions</th></tr></thead><tbody>
            {quotations.data.map((quotation) => <tr key={quotation.id}><td><Link className="table-primary-link" to={`/workspace/quotations/${quotation.id}`}><strong>{titleByRfq.get(quotation.rfqId) ?? "Linked RFQ"}</strong><small>{quotation.rfqId}</small></Link></td><td>{formatMoney(quotation.totalAmount, quotation.currency)}</td><td>{quotation.deliveryDays} days</td><td><StatusBadge value={quotation.status} /></td><td><code>{quotation.id.slice(0, 8)}</code></td><td>{user.permissions.includes("QUOTE_EVALUATE") && <Link className="table-action" to={`/workspace/quotations/${quotation.id}`}>Review</Link>}</td></tr>)}
          </tbody></table></div> : <EmptyState title="No quotations yet" detail="Supplier quotations will appear as vendors respond to your requests." />}
        </section>}
    </>
  );
}

function RfqDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const rfq = useQuery({
    queryKey: ["rfq", user.activeOrganization.id, id],
    queryFn: () => getRfq(id),
    enabled: Boolean(id),
  });
  const publish = useMutation({
    mutationFn: publishRfq,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["rfq", user.activeOrganization.id, id] });
      await queryClient.invalidateQueries({ queryKey: ["rfqs", user.activeOrganization.id] });
    },
  });
  const update = useMutation({
    mutationFn: (request: Parameters<typeof createRfq>[0]) => updateRfq(id, request),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["rfq", user.activeOrganization.id, id] }),
        queryClient.invalidateQueries({ queryKey: ["rfqs", user.activeOrganization.id] }),
      ]);
      setEditing(false);
    },
  });
  if (rfq.isLoading) return <LoadingPanel />;
  if (rfq.isError) return <InlineError error={rfq.error} />;
  if (!rfq.data) return <EmptyState title="RFQ not found" detail="The request may have been removed or you may not have access." />;
  const request = rfq.data;
  return (
    <>
      <div className="detail-back"><Link to="/workspace/rfqs">← All RFQs</Link></div>
      <div className="page-heading"><div><p className="eyebrow">{request.requestType} · {request.category}</p><h1>{request.title}</h1><p className="page-subtitle">{request.description}</p></div>
        <StatusBadge value={request.status} />
      </div>
      <div className="detail-metrics">
        <DetailMetric label="Budget" value={formatMoney(request.budget, request.currency)} icon={<CircleDollarSign size={17} />} />
        <DetailMetric label="Response deadline" value={new Date(request.deadline).toLocaleString()} icon={<Activity size={17} />} />
        <DetailMetric label="Delivery window" value={`${request.deliveryDays} days`} icon={<Truck size={17} />} />
        <DetailMetric label="Items" value={String(request.items.length)} icon={<PackageCheck size={17} />} />
      </div>
      {editing && request.status.toUpperCase() === "DRAFT" && user.permissions.includes("RFQ_UPDATE") &&
        <RfqForm key={request.id} initial={request} onSubmit={(body) => update.mutate(body)}
          isPending={update.isPending} error={update.error} submitLabel="Save draft changes" />}
      <section className="surface-card table-card"><div className="section-heading"><div><h2>Requested items</h2><p>Line-item specifications supplied to invited vendors</p></div>
        {request.status.toUpperCase() === "DRAFT" && user.permissions.includes("RFQ_UPDATE") &&
          <button className="quiet-button" onClick={() => setEditing(!editing)}>{editing ? "Close editor" : "Edit draft"}</button>}
        {request.status.toUpperCase() === "DRAFT" && user.permissions.includes("RFQ_PUBLISH") && <button className="primary-button" disabled={publish.isPending} onClick={() => publish.mutate(request.id)}>{publish.isPending ? "Publishing…" : "Publish request"}</button>}
        {request.status.toUpperCase() === "PUBLISHED" && user.permissions.includes("QUOTE_SUBMIT") && <Link className="primary-button" to={`/workspace/quotations/new/${request.id}`}><Plus size={16} /> Submit quotation</Link>}
      </div>
        {publish.isError && <InlineError error={publish.error} />}
        <div className="table-scroll"><table><thead><tr><th>Item</th><th>Quantity</th><th>Unit</th><th>Specifications</th></tr></thead><tbody>
          {request.items.map((item) => <tr key={item.id}><td><strong>{item.description}</strong></td><td>{item.quantity}</td><td>{item.unit || "—"}</td><td>{item.specification || "—"}</td></tr>)}
        </tbody></table></div>
      </section>
    </>
  );
}

function QuotationCreatePage() {
  const { rfqId = "" } = useParams();
  const { user } = useSession();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const rfqQuery = useQuery({
    queryKey: ["rfq", user.activeOrganization.id, rfqId],
    queryFn: () => getRfq(rfqId),
    enabled: Boolean(rfqId),
  });
  const [unitPrices, setUnitPrices] = useState<Record<string, string>>({});
  const [deliveryDays, setDeliveryDays] = useState("30");
  const [paymentTermsDays, setPaymentTermsDays] = useState("30");
  const [qualityRating, setQualityRating] = useState("3");
  const [materialGrade, setMaterialGrade] = useState<"A+" | "A" | "B" | "C">("A");
  const submit = useMutation({
    mutationFn: submitQuotation,
    onSuccess: async (quotation) => {
      await queryClient.invalidateQueries({ queryKey: ["quotations", user.activeOrganization.id] });
      navigate(`/workspace/quotations/${quotation.id}`);
    },
  });
  if (rfqQuery.isLoading) return <LoadingPanel />;
  if (rfqQuery.isError) return <InlineError error={rfqQuery.error} />;
  const rfq = rfqQuery.data;
  if (!rfq) return <EmptyState title="RFQ not found" detail="The request may have been removed or you may not have access." />;
  if (!user.permissions.includes("QUOTE_SUBMIT")) return <PermissionNotice />;
  if (rfq.status.toUpperCase() !== "PUBLISHED") return <EmptyState title="This request is not accepting quotations" detail="Only published RFQs can receive a vendor response." />;
  const total = rfq.items.reduce((sum, item) => sum + (Number(unitPrices[item.id ?? ""]) || 0) * item.quantity, 0);
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    submit.mutate({
      rfqId: rfq.id,
      currency: rfq.currency,
      deliveryDays: Number(deliveryDays),
      paymentTermsDays: Number(paymentTermsDays),
      qualityRating: Number(qualityRating),
      compliance: {
        isoCertification: new FormData(event.currentTarget).get("isoCertification") === "on",
        materialGrade,
        environmentalStandards: new FormData(event.currentTarget).get("environmentalStandards") === "on",
        documentSubmission: new FormData(event.currentTarget).get("documentSubmission") === "on",
      },
      items: rfq.items.map((item) => ({
        rfqItemId: item.id ?? "",
        unitPrice: Number(unitPrices[item.id ?? ""]),
        quantity: item.quantity,
      })),
    });
  };
  return (
    <>
      <div className="detail-back"><Link to={`/workspace/rfqs/${rfq.id}`}>← Back to RFQ</Link></div>
      <PageIntro eyebrow="VENDOR RESPONSE" title="Submit a quotation" detail={`Responding to ${rfq.title}. Currency is fixed to the RFQ's ${rfq.currency} budget currency.`} />
      <form className="surface-card rfq-form" onSubmit={handleSubmit}>
        <div className="section-heading"><div><h2>Priced line items</h2><p>Enter your unit price for each requested quantity.</p></div></div>
        <div className="table-scroll"><table><thead><tr><th>Item</th><th>Requested quantity</th><th>Unit</th><th>Unit price ({rfq.currency})</th></tr></thead><tbody>
          {rfq.items.map((item) => <tr key={item.id}><td><strong>{item.description}</strong><small>{item.specification}</small></td><td>{item.quantity}</td><td>{item.unit || "—"}</td><td><input aria-label={`Unit price for ${item.description}`} className="inline-price-input" type="number" min="0" step="0.01" required value={unitPrices[item.id ?? ""] ?? ""} onChange={(event) => setUnitPrices((prices) => ({ ...prices, [item.id ?? ""]: event.target.value }))} /></td></tr>)}
        </tbody></table></div>
        <div className="quotation-summary"><strong>Indicative total</strong><strong>{formatMoney(total, rfq.currency)}</strong></div>
        <div className="form-grid quotation-meta">
          <label>Delivery (days)<input type="number" min="1" max="365" value={deliveryDays} onChange={(event) => setDeliveryDays(event.target.value)} required /></label>
          <label>Payment terms (days)<input type="number" min="0" max="365" value={paymentTermsDays} onChange={(event) => setPaymentTermsDays(event.target.value)} required /></label>
          <label>Quality rating (0-5)<input type="number" min="0" max="5" step="0.1" value={qualityRating} onChange={(event) => setQualityRating(event.target.value)} required /></label>
          <label>Material grade<select value={materialGrade} onChange={(event) => setMaterialGrade(event.target.value as typeof materialGrade)}><option>A+</option><option>A</option><option>B</option><option>C</option></select></label>
        </div>
        <div className="compliance-panel"><strong>Supplier self-declarations</strong><p>Confirm only standards and documentation that your organization can substantiate.</p>
          <label><input type="checkbox" name="isoCertification" /> ISO certification</label>
          <label><input type="checkbox" name="environmentalStandards" /> Environmental standards</label>
          <label><input type="checkbox" name="documentSubmission" /> Required documentation can be supplied</label>
        </div>
        {submit.isError && <InlineError error={submit.error} />}
        <div className="form-footer"><span>Submission is attributed to your authenticated vendor membership.</span><button className="primary-button" disabled={submit.isPending || total <= 0}>{submit.isPending ? "Submitting…" : "Submit quotation"}</button></div>
      </form>
    </>
  );
}

function QuotationDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const quotation = useQuery({
    queryKey: ["quotation", user.activeOrganization.id, id],
    queryFn: () => getQuotation(id),
    enabled: Boolean(id),
  });
  const queryClient = useQueryClient();
  const review = useMutation({
    mutationFn: ({ id: quotationId, status }: { id: string; status: "UNDER_REVIEW" | "ACCEPTED" | "REJECTED" }) =>
      updateQuotationStatus(quotationId, status),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["quotation", user.activeOrganization.id, id] }),
        queryClient.invalidateQueries({ queryKey: ["quotations", user.activeOrganization.id] }),
        queryClient.invalidateQueries({ queryKey: ["rfqs", user.activeOrganization.id] }),
        queryClient.invalidateQueries({ queryKey: ["approvals", user.activeOrganization.id] }),
      ]);
    },
  });
  if (quotation.isLoading) return <LoadingPanel />;
  if (quotation.isError) return <InlineError error={quotation.error} />;
  if (!quotation.data) return <EmptyState title="Quotation not found" detail="The quotation may have been removed or you may not have access." />;
  const quote = quotation.data;
  return (
    <>
      <div className="detail-back"><Link to="/workspace/quotations">← All quotations</Link></div>
      <div className="page-heading"><div><p className="eyebrow">VENDOR QUOTATION</p><h1>Quotation details</h1><p className="page-subtitle">Linked RFQ {quote.rfqId}</p></div><div className="detail-heading-actions"><StatusBadge value={quote.status} />
        {user.permissions.includes("QUOTE_EVALUATE") && ["SUBMITTED", "UNDER_REVIEW"].includes(quote.status) && <>
          <button className="table-action" disabled={review.isPending || quote.status === "UNDER_REVIEW"} onClick={() => review.mutate({ id: quote.id, status: "UNDER_REVIEW" })}>Mark under review</button>
          <button className="approval-approve" disabled={review.isPending} onClick={() => review.mutate({ id: quote.id, status: "ACCEPTED" })}>Accept</button>
          <button className="approval-reject" disabled={review.isPending} onClick={() => review.mutate({ id: quote.id, status: "REJECTED" })}>Reject</button>
        </>}
      </div></div>
      {review.isError && <InlineError error={review.error} />}
      <div className="detail-metrics">
        <DetailMetric label="Total amount" value={formatMoney(quote.totalAmount, quote.currency)} icon={<CircleDollarSign size={17} />} />
        <DetailMetric label="Delivery" value={`${quote.deliveryDays} days`} icon={<Truck size={17} />} />
        <DetailMetric label="Payment terms" value={`${quote.paymentTermsDays} days`} icon={<Activity size={17} />} />
        <DetailMetric label="Vendor score" value={quote.vendorScore ? `${quote.vendorScore.score.toFixed(1)} / 100` : "Pending"} icon={<Sparkles size={17} />} />
      </div>
      <section className="surface-card table-card"><div className="section-heading"><div><h2>Quoted line items</h2><p>Supplier pricing and calculated totals</p></div></div>
        <div className="table-scroll"><table><thead><tr><th>RFQ line item</th><th>Quantity</th><th>Unit price</th><th>Line total</th></tr></thead><tbody>
          {(quote.items ?? []).map((item) => <tr key={item.rfqItemId}><td><small>{item.rfqItemId}</small></td><td>{item.quantity}</td><td>{formatMoney(item.unitPrice, quote.currency)}</td><td>{formatMoney(item.unitPrice * item.quantity, quote.currency)}</td></tr>)}
        </tbody></table></div>
        {quote.vendorScore?.explanation && <div className="score-explanation"><Sparkles size={16} /><span>{quote.vendorScore.explanation}</span></div>}
      </section>
    </>
  );
}

function PurchaseOrdersPage() {
  const { user } = useSession();
  const orders = usePurchaseOrders();
  return (
    <>
      <PageIntro eyebrow="COMMITMENTS" title="Purchase orders" detail="Issued orders created after the required approval workflow." />
      {orders.isLoading ? <LoadingPanel /> : orders.isError ? <InlineError error={orders.error} /> :
        <section className="surface-card table-card"><div className="section-heading"><div><h2>Issued orders</h2><p>{orders.data?.length ?? 0} purchase orders in this organization</p></div></div>
          {orders.data?.length ? <div className="table-scroll"><table><thead><tr><th>PO number</th><th>Vendor</th><th>Issued</th><th>Amount</th><th>Status</th><th>Items</th></tr></thead><tbody>
            {orders.data.map((order) => <tr key={order.id}><td><Link className="table-primary-link" to={`/workspace/purchase-orders/${order.id}`}><strong>{order.poNumber}</strong><small>{order.id}</small></Link></td><td>{order.vendorName}</td><td>{new Date(order.issuedAt).toLocaleDateString()}</td><td>{formatMoney(order.totalAmount, order.currency)}</td><td><StatusBadge value={order.status} /></td><td>{order.items.length}</td></tr>)}
          </tbody></table></div> : <EmptyState title="No purchase orders yet" detail="Orders created after the required approval workflow will appear here." />}
        </section>}
      {!user.permissions.includes("PO_READ") && <PermissionNotice />}
    </>
  );
}

function PurchaseOrderDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const order = useQuery({
    queryKey: ["purchase-order", user.activeOrganization.id, id],
    queryFn: () => getPurchaseOrder(id),
    enabled: Boolean(id) && user.permissions.includes("PO_READ"),
  });
  if (!user.permissions.includes("PO_READ")) return <PermissionNotice />;
  if (order.isLoading) return <LoadingPanel />;
  if (order.isError) return <InlineError error={order.error} />;
  if (!order.data) return <EmptyState title="Purchase order not found" detail="This order may have been removed or you may not have access." />;
  const record = order.data;
  return <>
    <div className="detail-back"><Link to="/workspace/purchase-orders"><ArrowLeft size={13} /> All purchase orders</Link></div>
    <div className="page-heading"><div><p className="eyebrow">ISSUED PURCHASE ORDER</p><h1>{record.poNumber}</h1><p className="page-subtitle">Approved procurement commitment · vendor {record.vendorName}</p></div><StatusBadge value={record.status} /></div>
    <div className="detail-metrics">
      <DetailMetric label="Order total" value={formatMoney(record.totalAmount, record.currency)} icon={<CircleDollarSign size={17} />} />
      <DetailMetric label="Delivery window" value={`${record.deliveryDays} days`} icon={<Truck size={17} />} />
      <DetailMetric label="Payment terms" value={`${record.paymentTermsDays} days`} icon={<Activity size={17} />} />
      <DetailMetric label="Issued" value={new Date(record.issuedAt).toLocaleDateString()} icon={<PackageCheck size={17} />} />
    </div>
    <section className="surface-card table-card"><div className="section-heading"><div><h2>Committed line items</h2><p>Immutable item and price snapshot from the approved quotation</p></div></div>
      <div className="table-scroll"><table><thead><tr><th>Item</th><th>Quantity</th><th>Unit price</th><th>Line total</th><th>Specification</th></tr></thead><tbody>
        {record.items.map((item) => <tr key={item.id}><td><strong>{item.description}</strong></td><td>{item.quantity} {item.unit}</td><td>{formatMoney(item.unitPrice, record.currency)}</td><td>{formatMoney(item.lineTotal, record.currency)}</td><td>{item.specification || "—"}</td></tr>)}
      </tbody></table></div>
      <div className="order-provenance"><span>RFQ <code>{record.rfqId}</code></span><span>Quotation <code>{record.quotationId}</code></span><span>Approval <code>{record.approvalRequestId}</code></span></div>
    </section>
  </>;
}

function ContractsPage() {
  const { user } = useSession();
  const contracts = useContracts();
  const quotations = useQuotations();
  const vendors = useVendors();
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const acceptedQuotes = (quotations.data ?? []).filter((quotation) => quotation.status === "ACCEPTED");
  const vendorNames = new Map((vendors.data ?? []).map((vendor) => [vendor.id, vendor.name]));
  const create = useMutation({
    mutationFn: createContract,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["contracts", user.activeOrganization.id] });
      setShowCreate(false);
    },
  });
  return (
    <>
      <div className="page-heading"><div><p className="eyebrow">AGREEMENTS</p><h1>Contracts</h1><p className="page-subtitle">Draft tenant-scoped agreements from accepted supplier quotations.</p></div>
        {user.permissions.includes("CONTRACT_CREATE") && <button className="primary-button" onClick={() => setShowCreate(!showCreate)}><Plus size={16} /> Draft contract</button>}
      </div>
      {showCreate && user.permissions.includes("CONTRACT_CREATE") &&
        <ContractCreateForm quotations={acceptedQuotes} vendorNames={vendorNames} pending={create.isPending}
          error={create.error} onSubmit={(request) => create.mutate(request)} />}
      {contracts.isLoading ? <LoadingPanel /> : contracts.isError ? <InlineError error={contracts.error} /> :
        <section className="surface-card table-card"><div className="section-heading"><div><h2>Organization contracts</h2><p>{contracts.data?.length ?? 0} drafted agreements</p></div></div>
          {contracts.data?.length ? <div className="table-scroll"><table><thead><tr><th>Vendor</th><th>Term</th><th>Status</th><th>Audit</th><th>Quotation</th></tr></thead><tbody>
            {contracts.data.map((contract) => <tr key={contract.id}>
              <td><Link className="table-primary-link" to={`/workspace/contracts/${contract.id}`}><strong>{contract.vendorName}</strong><small>{contract.id}</small></Link></td>
              <td>{contract.startDate} – {contract.endDate}</td><td><StatusBadge value={contract.status} /></td>
              <td><StatusBadge value={contract.auditStatus} /></td><td><code>{contract.quotationId.slice(0, 8)}</code></td>
            </tr>)}
          </tbody></table></div> : <EmptyState title="No contracts yet" detail="Create a draft agreement from an accepted quotation." />}
        </section>}
      {create.isError && !showCreate && <InlineError error={create.error} />}
      {!user.permissions.includes("CONTRACT_READ") && <PermissionNotice />}
    </>
  );
}

function ContractCreateForm({ quotations, vendorNames, pending, error, onSubmit }: {
  quotations: ReturnType<typeof useQuotations>["data"];
  vendorNames: Map<string, string>;
  pending: boolean;
  error: unknown;
  onSubmit: (request: { quotationId: string; content: string; startDate: string; endDate: string }) => void;
}) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    onSubmit({
      quotationId: String(values.get("quotationId")),
      content: String(values.get("content")),
      startDate: String(values.get("startDate")),
      endDate: String(values.get("endDate")),
    });
  };
  return <form className="surface-card contract-form" onSubmit={submit}>
    <div className="section-heading"><div><h2>Draft agreement</h2><p>Only accepted quotations are eligible. The draft remains pending legal review.</p></div></div>
    <label>Accepted quotation<select name="quotationId" required disabled={!quotations?.length}>
      {quotations?.length ? quotations.map((quotation) => <option key={quotation.id} value={quotation.id}>
        {vendorNames.get(quotation.vendorId) ?? quotation.vendorId} · {formatMoney(quotation.totalAmount, quotation.currency)}
      </option>) : <option value="">No accepted quotations available</option>}
    </select></label>
    <div className="form-grid">
      <label>Start date<input name="startDate" type="date" required /></label>
      <label>End date<input name="endDate" type="date" required /></label>
    </div>
    <label>Draft terms<textarea name="content" rows={7} maxLength={100000} required placeholder="Enter agreement terms for human legal review." /></label>
    {error ? <InlineError error={error} /> : null}
    <div className="form-footer"><span>Creating a draft does not approve or execute a contract.</span>
      <button className="primary-button" disabled={pending || !quotations?.length}>{pending ? "Creating…" : "Create draft"}</button></div>
  </form>;
}

function ContractDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const contract = useQuery({
    queryKey: ["contract", user.activeOrganization.id, id],
    queryFn: () => getContract(id),
    enabled: Boolean(id) && user.permissions.includes("CONTRACT_READ"),
  });
  const audits = useQuery({
    queryKey: ["contract-audits", user.activeOrganization.id, id],
    queryFn: () => listContractAudits(id),
    enabled: Boolean(id) && user.permissions.includes("CONTRACT_READ"),
  });
  const decisions = useQuery({
    queryKey: ["contract-decisions", user.activeOrganization.id, id],
    queryFn: () => listContractDecisions(id),
    enabled: Boolean(id) && user.permissions.includes("CONTRACT_READ"),
  });
  const queryClient = useQueryClient();
  const addAudit = useMutation({
    mutationFn: (request: Parameters<typeof addContractAudit>[1]) => addContractAudit(id, request),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["contract", user.activeOrganization.id, id] }),
        queryClient.invalidateQueries({ queryKey: ["contract-audits", user.activeOrganization.id, id] }),
        queryClient.invalidateQueries({ queryKey: ["contracts", user.activeOrganization.id] }),
      ]);
    },
  });
  const decision = useMutation({
    mutationFn: ({ value, comment }: { value: "APPROVE" | "REJECT"; comment: string }) =>
      decideContract(id, value, comment),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["contract", user.activeOrganization.id, id] }),
        queryClient.invalidateQueries({ queryKey: ["contract-decisions", user.activeOrganization.id, id] }),
        queryClient.invalidateQueries({ queryKey: ["contracts", user.activeOrganization.id] }),
      ]);
    },
  });
  const uploadDocument = useMutation({
    mutationFn: (file: File) => uploadContractDocument(id, file),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["contract", user.activeOrganization.id, id] }),
        queryClient.invalidateQueries({ queryKey: ["contracts", user.activeOrganization.id] }),
      ]);
    },
  });
  const downloadDocument = useMutation({
    mutationFn: () => createContractDocumentDownload(id),
    onSuccess: ({ downloadUrl }) => {
      window.location.assign(downloadUrl);
    },
  });
  if (!user.permissions.includes("CONTRACT_READ")) return <PermissionNotice />;
  if (contract.isLoading) return <LoadingPanel />;
  if (contract.isError) return <InlineError error={contract.error} />;
  if (!contract.data) return <EmptyState title="Contract not found" detail="This agreement may have been removed or you may not have access." />;
  const record = contract.data;
  return <>
    <div className="detail-back"><Link to="/workspace/contracts"><ArrowLeft size={13} /> All contracts</Link></div>
    <div className="page-heading"><div><p className="eyebrow">AGREEMENT DRAFT</p><h1>{record.vendorName}</h1><p className="page-subtitle">Contract {record.id}</p></div><div className="detail-heading-actions"><StatusBadge value={record.status} /><StatusBadge value={record.auditStatus} /></div></div>
    <div className="detail-metrics">
      <DetailMetric label="Start date" value={record.startDate} icon={<Activity size={17} />} />
      <DetailMetric label="End date" value={record.endDate} icon={<Activity size={17} />} />
      <DetailMetric label="RFQ" value={record.rfqId.slice(0, 8)} icon={<FileText size={17} />} />
      <DetailMetric label="Quotation" value={record.quotationId.slice(0, 8)} icon={<FileCheck2 size={17} />} />
    </div>
    <section className="surface-card contract-content"><div className="section-heading"><div><h2>Draft terms</h2><p>Unformatted text provided for review; no AI approval or legal validation is implied.</p></div></div>
      <pre>{record.content}</pre>
    </section>
    <section className="surface-card contract-content">
      <div className="section-heading"><div><h2>Contract document</h2>
        <p>PDF or DOCX, up to 10 MB. Stored as an authenticated Cloudinary asset; no public storage URL is exposed.</p>
      </div></div>
      {record.documentFileName ? <div className="form-footer"><span className="muted-copy">Attached: <strong>{record.documentFileName}</strong></span>
        <button className="secondary-button" type="button" disabled={downloadDocument.isPending}
          onClick={() => downloadDocument.mutate()}>
          {downloadDocument.isPending ? "Preparing secure link…" : "Download document"}
        </button>
      </div> :
        user.permissions.includes("CONTRACT_CREATE") && record.status === "DRAFT" ?
          <form className="contract-form audit-entry-form" onSubmit={(event) => {
            event.preventDefault();
            const file = (new FormData(event.currentTarget).get("file"));
            if (file instanceof File) uploadDocument.mutate(file);
          }}>
            <label>Choose a PDF or DOCX file<input name="file" type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" required /></label>
            {uploadDocument.isError && <InlineError error={uploadDocument.error} />}
            <div className="form-footer"><span>The attached document cannot be replaced on this draft.</span>
              <button className="primary-button" disabled={uploadDocument.isPending}>
                {uploadDocument.isPending ? "Uploading…" : "Upload document"}
              </button></div>
          </form> : <EmptyState title="No document attached" detail="A document can be attached to a draft contract by an authorized user." />}
      {downloadDocument.isError && <InlineError error={downloadDocument.error} />}
    </section>
    <section className="surface-card contract-content contract-audits">
      <div className="section-heading"><div><h2>Human review findings</h2><p>Findings are attributed to the authenticated reviewer and recorded in the audit trail.</p></div><StatusBadge value={record.auditStatus} /></div>
      {audits.isLoading ? <LoadingPanel /> : audits.isError ? <InlineError error={audits.error} /> :
        audits.data?.length ? <div className="contract-audit-list">{audits.data.map((audit) => <article className="contract-audit-item" key={audit.id}>
          <div><StatusBadge value={audit.riskLevel} /><strong>{audit.finding}</strong><small>{new Date(audit.createdAt).toLocaleString()} · reviewer {audit.createdBy ?? "unknown"}</small></div>
          {audit.clause && <p><b>Clause:</b> {audit.clause}</p>}
          {audit.explanation && <p><b>Explanation:</b> {audit.explanation}</p>}
          {audit.recommendation && <p><b>Recommendation:</b> {audit.recommendation}</p>}
          {audit.confidence !== null && <small>Confidence: {(audit.confidence * 100).toFixed(1)}%</small>}
        </article>)}</div> : <EmptyState title="No review findings recorded" detail="An authorized reviewer can add a finding below." />}
      {user.permissions.includes("CONTRACT_APPROVE") && record.status === "DRAFT" &&
        <ContractAuditForm key={audits.data?.length ?? 0} pending={addAudit.isPending} error={addAudit.error}
          onSubmit={(request) => addAudit.mutate(request)} />}
    </section>
    <section className="surface-card contract-content contract-audits">
      <div className="section-heading"><div><h2>Approval decision</h2><p>Approval is a separate maker-checker decision; the contract creator cannot approve their own draft.</p></div><StatusBadge value={record.status} /></div>
      {decisions.isLoading ? <LoadingPanel /> : decisions.isError ? <InlineError error={decisions.error} /> :
        decisions.data?.length ? decisions.data.map((item) => <article className="contract-audit-item" key={item.id}>
          <div><StatusBadge value={item.decision} /><strong>{item.comment || "No additional comment"}</strong>
            <small>{new Date(item.decidedAt).toLocaleString()} · approver {item.decidedBy}</small></div>
        </article>) : <EmptyState title="Decision pending" detail="An authorized approver can approve or reject this draft." />}
      {decision.isError && <InlineError error={decision.error} />}
      {user.permissions.includes("CONTRACT_APPROVE") && record.status === "DRAFT" &&
        <ContractDecisionForm pending={decision.isPending} onSubmit={(value, comment) => decision.mutate({ value, comment })} />}
    </section>
  </>;
}

function ContractDecisionForm({ pending, onSubmit }: {
  pending: boolean;
  onSubmit: (decision: "APPROVE" | "REJECT", comment: string) => void;
}) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    onSubmit(String(values.get("decision")) as "APPROVE" | "REJECT", String(values.get("comment")));
  };
  return <form className="contract-form audit-entry-form" onSubmit={submit}>
    <label>Decision<select name="decision" defaultValue="APPROVE"><option value="APPROVE">Approve draft</option><option value="REJECT">Reject draft</option></select></label>
    <label>Comment (required for rejection)<textarea name="comment" rows={2} maxLength={10000} /></label>
    <button className="primary-button" disabled={pending}>{pending ? "Recording decision…" : "Submit decision"}</button>
  </form>;
}

function ContractAuditForm({ pending, error, onSubmit }: {
  pending: boolean;
  error: unknown;
  onSubmit: (request: Parameters<typeof addContractAudit>[1]) => void;
}) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const confidence = String(values.get("confidence")).trim();
    onSubmit({
      riskLevel: String(values.get("riskLevel")) as "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
      finding: String(values.get("finding")),
      clause: String(values.get("clause")),
      explanation: String(values.get("explanation")),
      recommendation: String(values.get("recommendation")),
      ...(confidence ? { confidence: Number(confidence) } : {}),
    });
  };
  return <form className="contract-form audit-entry-form" onSubmit={submit}>
    <h3>Record a review finding</h3>
    <div className="form-grid">
      <label>Risk level<select name="riskLevel" defaultValue="MEDIUM"><option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option></select></label>
      <label>Confidence (optional)<input name="confidence" type="number" min="0" max="1" step="0.001" /></label>
    </div>
    <label>Finding<textarea name="finding" rows={2} maxLength={10000} required /></label>
    <label>Clause (optional)<textarea name="clause" rows={2} maxLength={10000} /></label>
    <label>Explanation (optional)<textarea name="explanation" rows={2} maxLength={10000} /></label>
    <label>Recommendation (optional)<textarea name="recommendation" rows={2} maxLength={10000} /></label>
    {error ? <InlineError error={error} /> : null}
    <button className="primary-button" disabled={pending}>{pending ? "Saving finding…" : "Record finding"}</button>
  </form>;
}

function ApprovalsPage() {
  const approvals = useApprovals();
  const { user } = useSession();
  const queryClient = useQueryClient();
  const [rejectionId, setRejectionId] = useState("");
  const [rejectionComment, setRejectionComment] = useState("");
  const decision = useMutation({
    mutationFn: ({ id, value, comment }: { id: string; value: "APPROVE" | "REJECT"; comment: string }) =>
      decideApproval(id, value, comment),
    onSuccess: () => {
      setRejectionId("");
      setRejectionComment("");
      return queryClient.invalidateQueries({ queryKey: ["approvals", user.activeOrganization.id] });
    },
  });
  return (
    <>
      <PageIntro eyebrow="HUMAN REVIEW" title="Approvals" detail="Review procurement approval requests for your active organization." />
      {approvals.isLoading ? <LoadingPanel /> : approvals.isError ? <InlineError error={approvals.error} /> :
        <section className="record-grid">{approvals.data?.length ? approvals.data.map((record) => {
          const pending = String(record.status ?? "").toUpperCase() === "PENDING";
          const assignedToCurrentUser = hasPendingApprovalStep(record.steps, user.userId);
          return <article className="surface-card approval-card" key={record.id}>
            <div className="record-icon"><ClipboardCheck size={18} /></div>
            <div className="record-main">
              <div className="record-top"><strong>{recordLabel(record)}</strong><StatusBadge value={String(record.status ?? "PENDING")} /></div>
              <p>{recordSubtitle(record)}{typeof record.category === "string" ? ` · ${record.category}` : ""}</p>
              {typeof record.justification === "string" && <p>{record.justification}</p>}
              {Array.isArray(record.steps) && <small>{record.steps.length} approval step{record.steps.length === 1 ? "" : "s"} · Requested {recordDate(record)}</small>}
              {pending && assignedToCurrentUser && user.permissions.includes("APPROVAL_DECIDE") && (
                <div className="approval-actions">
                  <button className="approval-approve" disabled={decision.isPending} onClick={() => decision.mutate({ id: record.id, value: "APPROVE", comment: "" })}>Approve</button>
                  {rejectionId === record.id ? (
                    <form className="rejection-form" onSubmit={(event) => {
                      event.preventDefault();
                      decision.mutate({ id: record.id, value: "REJECT", comment: rejectionComment.trim() });
                    }}>
                      <input aria-label="Rejection reason" value={rejectionComment} onChange={(event) => setRejectionComment(event.target.value)} maxLength={2000} placeholder="Required rejection reason" required />
                      <button className="approval-reject" disabled={decision.isPending || !rejectionComment.trim()}>Confirm rejection</button>
                    </form>
                  ) : <button className="approval-reject" disabled={decision.isPending} onClick={() => setRejectionId(record.id)}>Reject</button>}
                </div>
              )}
            </div>
          </article>;
        }) : <div className="surface-card record-empty"><EmptyState title="No approval requests" detail="Approval requests will appear here when policy requires review." /></div>}
          {decision.isError && <InlineError error={decision.error} />}
        </section>}
    </>
  );
}

function RecordListPage({ eyebrow, title, detail, query, icon, emptyTitle }: { eyebrow: string; title: string; detail: string; query: { data?: BusinessRecord[]; isLoading: boolean; isError: boolean; error: unknown }; icon: ReactNode; emptyTitle: string }) {
  return (
    <>
      <PageIntro eyebrow={eyebrow} title={title} detail={detail} />
      {query.isLoading ? <LoadingPanel /> : query.isError ? <InlineError error={query.error} /> :
        <section className="record-grid">{query.data?.length ? query.data.map((record) => (
          <article className="surface-card record-card" key={record.id}>
            <div className="record-icon">{icon}</div>
            <div className="record-main"><div className="record-top"><strong>{recordLabel(record)}</strong><StatusBadge value={String(record.status ?? "RECORDED")} /></div>
              <p>{recordSubtitle(record)}</p><small>{recordDate(record)}</small></div>
          </article>
        )) : <div className="surface-card record-empty"><EmptyState title={emptyTitle} detail="New records will be listed here when created." /></div>}</section>}
    </>
  );
}

function VendorsPage() {
  const { user } = useSession();
  const queryClient = useQueryClient();
  const vendors = useVendors();
  const [showCreate, setShowCreate] = useState(false);
  const createMutation = useMutation({
    mutationFn: createVendor,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["vendors", user.activeOrganization.id] });
      setShowCreate(false);
    },
  });
  return (
    <>
      <div className="page-heading"><div><p className="eyebrow">SUPPLIER DIRECTORY</p><h1>Vendors</h1><p className="page-subtitle">Manage suppliers and their compliance documents in this organization.</p></div>
        {user.permissions.includes("VENDOR_CREATE") && <button className="primary-button" onClick={() => setShowCreate(!showCreate)}><Plus size={16} /> Add vendor</button>}
      </div>
      {showCreate && <VendorCreateForm pending={createMutation.isPending} error={createMutation.error} onSubmit={(request) => createMutation.mutate(request)} />}
      {vendors.isLoading ? <LoadingPanel /> : vendors.isError ? <InlineError error={vendors.error} /> :
        <div className="vendor-grid">{vendors.data?.length ? vendors.data.map((vendor) => <VendorCard key={vendor.id} vendor={vendor} />) :
          <div className="surface-card vendor-empty"><EmptyState title="No vendors yet" detail="Add a supplier to start building your organization's vendor directory." /></div>}</div>}
    </>
  );
}

function VendorCreateForm({ pending, error, onSubmit }: {
  pending: boolean;
  error: unknown;
  onSubmit: (request: { name: string; contactEmail: string; category: string; paymentTermsDays: number }) => void;
}) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    onSubmit({
      name: String(values.get("name")),
      contactEmail: String(values.get("contactEmail")),
      category: String(values.get("category")),
      paymentTermsDays: Number(values.get("paymentTermsDays")),
    });
  };
  return <form className="surface-card rfq-form vendor-create-form" onSubmit={submit}>
    <div className="section-heading"><div><h2>Register vendor</h2><p>Vendor records are created within the active organization.</p></div></div>
    <div className="form-grid">
      <label>Company name<input name="name" required maxLength={200} /></label>
      <label>Contact email<input name="contactEmail" type="email" maxLength={320} /></label>
      <label>Category<input name="category" maxLength={100} /></label>
      <label>Payment terms (days)<input name="paymentTermsDays" type="number" min="0" max="365" defaultValue="30" /></label>
    </div>
    {error ? <InlineError error={error} /> : null}
    <div className="form-footer"><span>Supplier status and risk are determined by the backend.</span><button className="primary-button" disabled={pending}>{pending ? "Saving…" : "Create vendor"}</button></div>
  </form>;
}

function VendorCard({ vendor }: { vendor: import("./types").Vendor }) {
  const { user } = useSession();
  const queryClient = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  const [editing, setEditing] = useState(false);
  const [addingMember, setAddingMember] = useState(false);
  const [memberUserId, setMemberUserId] = useState("");
  const [documentType, setDocumentType] = useState("CERTIFICATE");
  const [file, setFile] = useState<File | null>(null);
  const documents = useQuery({
    queryKey: ["vendor-documents", user.activeOrganization.id, vendor.id],
    queryFn: () => listVendorDocuments(vendor.id),
    enabled: expanded,
  });
  const upload = useMutation({
    mutationFn: () => {
      if (!file) throw new Error("Select a document to upload");
      return uploadVendorDocument(vendor.id, documentType, file);
    },
    onSuccess: async () => {
      setFile(null);
      await queryClient.invalidateQueries({ queryKey: ["vendor-documents", user.activeOrganization.id, vendor.id] });
    },
  });
  const verification = useMutation({
    mutationFn: ({ documentId, status, reason }: { documentId: string; status: "VERIFIED" | "REJECTED"; reason: string }) =>
      verifyVendorDocument(vendor.id, documentId, status, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["vendor-documents", user.activeOrganization.id, vendor.id] }),
  });
  const update = useMutation({
    mutationFn: (request: { name: string; contactEmail: string; category: string; paymentTermsDays: number }) =>
      updateVendor(vendor.id, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["vendors", user.activeOrganization.id] });
      setEditing(false);
    },
  });
  const addMember = useMutation({
    mutationFn: (userId: string) => addVendorMember(vendor.id, userId),
    onSuccess: () => {
      setAddingMember(false);
      setMemberUserId("");
    },
  });
  return (
    <article className="surface-card vendor-card">
      <div className="vendor-card-top"><span className="vendor-logo"><Building2 size={19} /></span><div><strong>{vendor.name}</strong><small>{vendor.category || "Uncategorized supplier"}</small></div><StatusBadge value={vendor.status} /></div>
      <div className="vendor-facts"><span>{vendor.contactEmail || "No contact email"}</span><span>{vendor.paymentTermsDays} day terms</span></div>
      <div className="vendor-card-bottom"><span>Risk <strong>{vendor.riskLevel}</strong></span><span>Performance <strong>{vendor.performanceScore?.toFixed?.(1) ?? "—"}</strong></span>
        {user.permissions.includes("VENDOR_UPDATE") && <button className="quiet-button" onClick={() => setEditing(!editing)}>{editing ? "Close edit" : "Edit"}</button>}
        <button className="table-action" onClick={() => setExpanded(!expanded)}>{expanded ? "Hide documents" : "Documents"}</button>
      </div>
      {editing && user.permissions.includes("VENDOR_UPDATE") && <VendorEditForm vendor={vendor} pending={update.isPending} error={update.error} onSubmit={(request) => update.mutate(request)} />}
      {user.permissions.includes("USER_MANAGE") && <div className="vendor-member-control">
        {addingMember ? <form onSubmit={(event) => { event.preventDefault(); addMember.mutate(memberUserId.trim()); }}>
          <label>Organization member user ID<input value={memberUserId} onChange={(event) => setMemberUserId(event.target.value)} placeholder="UUID" required /></label>
          <button className="table-action" disabled={addMember.isPending || !memberUserId.trim()}>{addMember.isPending ? "Adding…" : "Assign"}</button>
          <button type="button" className="quiet-button" onClick={() => setAddingMember(false)}>Cancel</button>
        </form> : <button className="quiet-button" onClick={() => setAddingMember(true)}>Add organization member</button>}
        {addMember.isError && <InlineError error={addMember.error} />}
      </div>}
      {expanded && <div className="vendor-documents">
        {user.permissions.some((permission) => ["VENDOR_UPDATE", "VENDOR_DOCUMENT_SUBMIT"].includes(permission)) &&
          <form className="document-upload" onSubmit={(event) => { event.preventDefault(); upload.mutate(); }}>
            <select value={documentType} onChange={(event) => setDocumentType(event.target.value)} aria-label="Document type">
              <option value="CERTIFICATE">Certificate</option><option value="INSURANCE">Insurance</option><option value="TAX_FORM">Tax form</option><option value="OTHER">Other</option>
            </select>
            <input type="file" accept=".pdf,.docx,.jpg,.jpeg,.png" required onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
            <button className="table-action" disabled={upload.isPending || !file}>{upload.isPending ? "Uploading…" : "Upload"}</button>
          </form>}
        {upload.isError && <InlineError error={upload.error} />}
        {documents.isLoading ? <span className="muted-caption">Loading documents…</span> : documents.isError ? <InlineError error={documents.error} /> :
          documents.data?.length ? documents.data.map((document) => <div className="document-row" key={document.id}>
            <div><strong>{document.fileName}</strong><small>{document.documentType} · {document.verificationStatus}</small></div>
            {document.verificationStatus === "UNVERIFIED" && user.permissions.includes("VENDOR_DOCUMENT_VERIFY") &&
              <div className="document-review">
                <button className="approval-approve" disabled={verification.isPending} onClick={() => verification.mutate({ documentId: document.id, status: "VERIFIED", reason: "" })}>Verify</button>
                <button className="approval-reject" disabled={verification.isPending} onClick={() => {
                  const reason = window.prompt("Reason for rejecting this document");
                  if (reason?.trim()) verification.mutate({ documentId: document.id, status: "REJECTED", reason: reason.trim() });
                }}>Reject</button>
              </div>}
          </div>) : <span className="muted-caption">No compliance documents recorded.</span>}
        {verification.isError && <InlineError error={verification.error} />}
      </div>}
    </article>
  );
}

function VendorEditForm({ vendor, pending, error, onSubmit }: {
  vendor: import("./types").Vendor;
  pending: boolean;
  error: unknown;
  onSubmit: (request: { name: string; contactEmail: string; category: string; paymentTermsDays: number }) => void;
}) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    onSubmit({
      name: String(values.get("name")),
      contactEmail: String(values.get("contactEmail")),
      category: String(values.get("category")),
      paymentTermsDays: Number(values.get("paymentTermsDays")),
    });
  };
  return <form className="vendor-edit-form" onSubmit={submit}>
    <label>Vendor name<input name="name" defaultValue={vendor.name} maxLength={200} required /></label>
    <label>Contact email<input name="contactEmail" type="email" defaultValue={vendor.contactEmail ?? ""} maxLength={320} /></label>
    <label>Category<input name="category" defaultValue={vendor.category} maxLength={100} /></label>
    <label>Terms (days)<input name="paymentTermsDays" type="number" min="0" max="365" defaultValue={vendor.paymentTermsDays} /></label>
    {error ? <InlineError error={error} /> : null}
    <button className="table-action" disabled={pending}>{pending ? "Saving…" : "Save vendor"}</button>
  </form>;
}

function PaymentsPage() {
  const { user } = useSession();
  const queryClient = useQueryClient();
  const orders = usePurchaseOrders();
  const mandates = usePaymentMandates();
  const payments = usePayments();
  const [selectedOrder, setSelectedOrder] = useState("");
  const [selectedMandate, setSelectedMandate] = useState("");
  const [expiresAt, setExpiresAt] = useState(() => toLocalDateTimeInput(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)));
  const activeOrders = orders.data?.filter((order) => order.status === "ISSUED") ?? [];
  const activeMandates = mandates.data?.filter((mandate) => mandate.status === "ACTIVE" && new Date(mandate.expiresAt) > new Date()) ?? [];
  const invalidatePayments = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["payment-mandates", user.activeOrganization.id] }),
      queryClient.invalidateQueries({ queryKey: ["payments", user.activeOrganization.id] }),
    ]);
  };
  const mandateMutation = useMutation({
    mutationFn: () => createPaymentMandate(selectedOrder, new Date(expiresAt).toISOString()),
    onSuccess: invalidatePayments,
  });
  const authorizeMutation = useMutation({
    mutationFn: () => authorizePayment(selectedOrder, selectedMandate),
    onSuccess: invalidatePayments,
  });
  const revokeMutation = useMutation({ mutationFn: revokePaymentMandate, onSuccess: invalidatePayments });
  const operationMutation = useMutation({
    mutationFn: ({ id, operation }: { id: string; operation: "capture" | "refund" }) => operatePayment(id, operation),
    onSuccess: invalidatePayments,
  });
  const selectedActiveMandate = activeMandates.find((mandate) => mandate.id === selectedMandate && mandate.purchaseOrderId === selectedOrder);
  return (
    <>
      <PageIntro eyebrow="SANDBOX PAYMENT OPERATIONS" title="Payments" detail="Authorize, capture, and refund sandbox transactions against issued purchase orders. No live payment credentials are collected." />
      {user.permissions.includes("PAYMENT_MANDATE_MANAGE") && <section className="surface-card payment-panel">
        <div className="section-heading"><div><h2>Payment mandate</h2><p>Mandates authorize one specific PO amount and currency until expiry.</p></div><Shield size={18} className="subtle-icon" /></div>
        <div className="form-grid payment-form-grid">
          <label>Purchase order<select value={selectedOrder} onChange={(event) => { setSelectedOrder(event.target.value); setSelectedMandate(""); }}><option value="">Select an issued PO</option>{activeOrders.map((order) => <option key={order.id} value={order.id}>{order.poNumber} · {formatMoney(order.totalAmount, order.currency)}</option>)}</select></label>
          <label>Mandate expiry<input type="datetime-local" value={expiresAt} min={toLocalDateTimeInput(new Date())} onChange={(event) => setExpiresAt(event.target.value)} /></label>
          <div className="payment-action-cell"><button className="primary-button" disabled={!selectedOrder || mandateMutation.isPending} onClick={() => mandateMutation.mutate()}>{mandateMutation.isPending ? "Creating…" : "Create mandate"}</button></div>
        </div>
        {mandateMutation.isError && <InlineError error={mandateMutation.error} />}
        {mandates.data?.length ? <div className="table-scroll payment-table"><table><thead><tr><th>Purchase order</th><th>Maximum amount</th><th>Expires</th><th>Status</th><th>Action</th></tr></thead><tbody>
          {mandates.data.map((mandate) => <tr key={mandate.id}><td><code>{mandate.purchaseOrderId.slice(0, 8)}</code></td><td>{formatMoney(mandate.maximumAmount, mandate.currency)}</td><td>{new Date(mandate.expiresAt).toLocaleString()}</td><td><StatusBadge value={mandate.status} /></td><td>{mandate.status === "ACTIVE" && <button className="approval-reject" onClick={() => revokeMutation.mutate(mandate.id)}>Revoke</button>}</td></tr>)}
        </tbody></table></div> : null}
        {revokeMutation.isError && <InlineError error={revokeMutation.error} />}
      </section>}
      {user.permissions.includes("PAYMENT_AUTHORIZE") && <section className="surface-card payment-panel">
        <div className="section-heading"><div><h2>Authorize sandbox payment</h2><p>Both order and mandate are matched by the backend; amounts cannot be supplied by the browser.</p></div></div>
        <div className="form-grid payment-form-grid">
          <label>Purchase order<select value={selectedOrder} onChange={(event) => { setSelectedOrder(event.target.value); setSelectedMandate(""); }}><option value="">Select an issued PO</option>{activeOrders.map((order) => <option key={order.id} value={order.id}>{order.poNumber} · {formatMoney(order.totalAmount, order.currency)}</option>)}</select></label>
          <label>Active mandate<select value={selectedMandate} onChange={(event) => setSelectedMandate(event.target.value)}><option value="">Select matching mandate</option>{activeMandates.filter((mandate) => !selectedOrder || mandate.purchaseOrderId === selectedOrder).map((mandate) => <option key={mandate.id} value={mandate.id}>{formatMoney(mandate.maximumAmount, mandate.currency)} · expires {new Date(mandate.expiresAt).toLocaleDateString()}</option>)}</select></label>
          <div className="payment-action-cell"><button className="primary-button" disabled={!selectedActiveMandate || authorizeMutation.isPending} onClick={() => authorizeMutation.mutate()}>{authorizeMutation.isPending ? "Authorizing…" : "Authorize sandbox payment"}</button></div>
        </div>
        {authorizeMutation.isError && <InlineError error={authorizeMutation.error} />}
      </section>}
      {payments.isLoading ? <LoadingPanel /> : payments.isError ? <InlineError error={payments.error} /> :
        <section className="surface-card table-card payment-panel"><div className="section-heading"><div><h2>Sandbox payment ledger</h2><p>{payments.data?.length ?? 0} payment intents · internal sandbox only</p></div></div>
          {payments.data?.length ? <div className="table-scroll"><table><thead><tr><th>Payment</th><th>Purchase order</th><th>Amount</th><th>Status</th><th>Receipts</th><th>Action</th></tr></thead><tbody>
            {payments.data.map((payment) => <tr key={payment.id}><td><Link className="table-primary-link" to={`/workspace/payments/${payment.id}`}><strong>{payment.id.slice(0, 8)}</strong><small>{payment.sandboxAuthorizationReference}</small></Link></td><td><Link className="table-action" to={`/workspace/purchase-orders/${payment.purchaseOrderId}`}>{payment.purchaseOrderId.slice(0, 8)}</Link></td><td>{formatMoney(payment.amount, payment.currency)}</td><td><StatusBadge value={payment.status} /></td>
              <td>{payment.receipts.map((receipt) => <small key={receipt.id}>{receipt.operation} · {receipt.sandboxReference}</small>)}</td>
              <td>{user.permissions.includes("PAYMENT_CAPTURE") && payment.status === "AUTHORIZED" && <button className="table-action" disabled={operationMutation.isPending} onClick={() => operationMutation.mutate({ id: payment.id, operation: "capture" })}>Capture</button>}
                {user.permissions.includes("PAYMENT_REFUND") && payment.status === "CAPTURED" && <button className="approval-reject" disabled={operationMutation.isPending} onClick={() => operationMutation.mutate({ id: payment.id, operation: "refund" })}>Refund</button>}
              </td></tr>)}
          </tbody></table></div> : <EmptyState title="No sandbox payments" detail="Authorize a payment after a finance user creates a matching PO mandate." />}
          {operationMutation.isError && <InlineError error={operationMutation.error} />}
        </section>}
    </>
  );
}

function FulfillmentPage() {
  const { user } = useSession();
  const queryClient = useQueryClient();
  const orders = usePurchaseOrders();
  const shipments = useShipments();
  const invoices = useInvoices();
  const reconciliations = useReconciliations();
  const [selectedOrder, setSelectedOrder] = useState("");
  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [invoiceAmount, setInvoiceAmount] = useState("");
  const [invoiceDate, setInvoiceDate] = useState(new Date().toISOString().slice(0, 10));
  const [invoiceDueDate, setInvoiceDueDate] = useState("");
  const [trackingNumber, setTrackingNumber] = useState("");
  const [carrier, setCarrier] = useState("");
  const [shipmentExpectedAt, setShipmentExpectedAt] = useState("");
  const [quantities, setQuantities] = useState<Record<string, string>>({});
  const issuedOrders = orders.data?.filter((order) => order.status === "ISSUED") ?? [];
  const activeOrder = issuedOrders.find((order) => order.id === selectedOrder);
  const refresh = async () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ["shipments", user.activeOrganization.id] }),
    queryClient.invalidateQueries({ queryKey: ["invoices", user.activeOrganization.id] }),
    queryClient.invalidateQueries({ queryKey: ["reconciliations", user.activeOrganization.id] }),
  ]);
  const invoiceMutation = useMutation({
    mutationFn: () => {
      if (!activeOrder) throw new Error("Select an issued purchase order");
      return createInvoice({
        purchaseOrderId: activeOrder.id,
        invoiceNumber: invoiceNumber.trim(),
        invoiceDate,
        dueDate: invoiceDueDate || undefined,
        amount: Number(invoiceAmount),
        currency: activeOrder.currency,
      });
    },
    onSuccess: async () => { await refresh(); setInvoiceNumber(""); setInvoiceAmount(""); },
  });
  const shipmentMutation = useMutation({
    mutationFn: () => {
      if (!activeOrder) throw new Error("Select an issued purchase order");
      return createShipment({
        purchaseOrderId: activeOrder.id,
        trackingNumber: trackingNumber.trim(),
        carrier: carrier.trim(),
        expectedAt: shipmentExpectedAt ? new Date(shipmentExpectedAt).toISOString() : undefined,
        items: activeOrder.items
          .filter((item) => Number(quantities[item.id]) > 0)
          .map((item) => ({ purchaseOrderItemId: item.id, quantity: Number(quantities[item.id]) })),
      });
    },
    onSuccess: async () => { await refresh(); setTrackingNumber(""); setCarrier(""); setQuantities({}); },
  });
  const shipmentStatusMutation = useMutation({
    mutationFn: ({ id, status, reason }: { id: string; status: "DELIVERED" | "EXCEPTION"; reason: string }) => updateShipmentStatus(id, status, reason),
    onSuccess: refresh,
  });
  const reconcileMutation = useMutation({ mutationFn: reconcileInvoice, onSuccess: refresh });
  return (
    <>
      <PageIntro eyebrow="ORDER OPERATIONS" title="Fulfillment & reconciliation" detail="Record shipment and invoice metadata, then reconcile against purchase order lines and sandbox payment state." />
      {(user.permissions.includes("INVOICE_MANAGE") || user.permissions.includes("SHIPMENT_MANAGE")) &&
        <section className="surface-card payment-panel">
          <div className="section-heading"><div><h2>Record fulfillment activity</h2><p>Only issued purchase orders are eligible. Server validation remains authoritative.</p></div></div>
          <label className="order-select-label">Purchase order<select value={selectedOrder} onChange={(event) => setSelectedOrder(event.target.value)}><option value="">Select an issued PO</option>{issuedOrders.map((order) => <option key={order.id} value={order.id}>{order.poNumber} · {formatMoney(order.totalAmount, order.currency)}</option>)}</select></label>
          {activeOrder && <div className="operations-forms">
            {user.permissions.includes("INVOICE_MANAGE") && <form className="operation-form" onSubmit={(event) => { event.preventDefault(); invoiceMutation.mutate(); }}>
              <h3><ReceiptText size={16} /> Record invoice metadata</h3>
              <label>Invoice number<input required maxLength={100} value={invoiceNumber} onChange={(event) => setInvoiceNumber(event.target.value)} /></label>
              <div className="form-grid"><label>Invoice date<input type="date" required value={invoiceDate} max={new Date().toISOString().slice(0, 10)} onChange={(event) => setInvoiceDate(event.target.value)} /></label>
                <label>Due date<input type="date" value={invoiceDueDate} min={invoiceDate} onChange={(event) => setInvoiceDueDate(event.target.value)} /></label></div>
              <label>Invoice amount ({activeOrder.currency})<input type="number" min="0.01" step="0.01" required value={invoiceAmount} onChange={(event) => setInvoiceAmount(event.target.value)} /></label>
              {invoiceMutation.isError && <InlineError error={invoiceMutation.error} />}
              <button className="primary-button" disabled={invoiceMutation.isPending}>{invoiceMutation.isPending ? "Recording…" : "Record invoice"}</button>
            </form>}
            {user.permissions.includes("SHIPMENT_MANAGE") && <form className="operation-form" onSubmit={(event) => { event.preventDefault(); shipmentMutation.mutate(); }}>
              <h3><Truck size={16} /> Record shipment</h3>
              <label>Tracking number<input required maxLength={100} value={trackingNumber} onChange={(event) => setTrackingNumber(event.target.value)} /></label>
              <label>Carrier<input required maxLength={120} value={carrier} onChange={(event) => setCarrier(event.target.value)} /></label>
              <label>Expected delivery<input type="datetime-local" value={shipmentExpectedAt} onChange={(event) => setShipmentExpectedAt(event.target.value)} /></label>
              <div className="shipment-lines"><strong>Shipment quantities</strong>{activeOrder.items.map((item) => <label key={item.id}><span>{item.description}<small>ordered {item.quantity} {item.unit}</small></span><input aria-label={`Shipment quantity for ${item.description}`} type="number" min="0" max={item.quantity} value={quantities[item.id] ?? ""} onChange={(event) => setQuantities((old) => ({ ...old, [item.id]: event.target.value }))} /></label>)}</div>
              {shipmentMutation.isError && <InlineError error={shipmentMutation.error} />}
              <button className="primary-button" disabled={shipmentMutation.isPending || !activeOrder.items.some((item) => Number(quantities[item.id]) > 0)}>{shipmentMutation.isPending ? "Recording…" : "Record shipment"}</button>
            </form>}
          </div>}
        </section>}
      {user.permissions.includes("SHIPMENT_READ") && <section className="surface-card table-card payment-panel">
        <div className="section-heading"><div><h2>Shipments</h2><p>Recorded tracking and delivery status</p></div></div>
        {shipments.isLoading ? <LoadingPanel /> : shipments.isError ? <InlineError error={shipments.error} /> : shipments.data?.length ?
          <div className="table-scroll"><table><thead><tr><th>Tracking</th><th>Purchase order</th><th>Carrier</th><th>Items</th><th>Status</th><th>Update</th></tr></thead><tbody>
            {shipments.data.map((shipment) => <tr key={shipment.id}><td><Link className="table-primary-link" to={`/workspace/fulfillment/shipments/${shipment.id}`}><strong>{shipment.trackingNumber}</strong><small>{shipment.expectedAt ? `Expected ${new Date(shipment.expectedAt).toLocaleDateString()}` : ""}</small></Link></td><td><Link className="table-action" to={`/workspace/purchase-orders/${shipment.purchaseOrderId}`}>{shipment.purchaseOrderId.slice(0, 8)}</Link></td><td>{shipment.carrier}</td><td>{shipment.items.map((item) => `${item.description} × ${item.quantity}`).join(", ")}</td><td><StatusBadge value={shipment.status} /></td><td>
              {user.permissions.includes("SHIPMENT_MANAGE") && shipment.status === "IN_TRANSIT" && <><button className="table-action" disabled={shipmentStatusMutation.isPending} onClick={() => shipmentStatusMutation.mutate({ id: shipment.id, status: "DELIVERED", reason: "" })}>Delivered</button><button className="approval-reject" disabled={shipmentStatusMutation.isPending} onClick={() => { const reason = window.prompt("Describe the shipment exception"); if (reason?.trim()) shipmentStatusMutation.mutate({ id: shipment.id, status: "EXCEPTION", reason: reason.trim() }); }}>Exception</button></>}</td></tr>)}
          </tbody></table></div> : <EmptyState title="No shipments recorded" detail="Shipment tracking records will appear here." />}
        {shipmentStatusMutation.isError && <InlineError error={shipmentStatusMutation.error} />}
      </section>}
      {user.permissions.includes("INVOICE_READ") && <section className="surface-card table-card payment-panel">
        <div className="section-heading"><div><h2>Invoices</h2><p>Manually recorded invoice metadata · no invoice documents are uploaded here</p></div></div>
        {invoices.isLoading ? <LoadingPanel /> : invoices.isError ? <InlineError error={invoices.error} /> : invoices.data?.length ?
          <div className="table-scroll"><table><thead><tr><th>Invoice</th><th>Purchase order</th><th>Date</th><th>Amount</th><th>Status</th><th>Reconcile</th></tr></thead><tbody>
            {invoices.data.map((invoice) => <tr key={invoice.id}><td><Link className="table-primary-link" to={`/workspace/fulfillment/invoices/${invoice.id}`}><strong>{invoice.invoiceNumber}</strong><small>{invoice.dueDate ? `Due ${invoice.dueDate}` : ""}</small></Link></td><td><Link className="table-action" to={`/workspace/purchase-orders/${invoice.purchaseOrderId}`}>{invoice.purchaseOrderId.slice(0, 8)}</Link></td><td>{invoice.invoiceDate}</td><td>{formatMoney(invoice.amount, invoice.currency)}</td><td><StatusBadge value={invoice.status} /></td><td>{user.permissions.includes("RECONCILIATION_EXECUTE") && <button className="table-action" disabled={reconcileMutation.isPending} onClick={() => reconcileMutation.mutate(invoice.id)}>Run reconciliation</button>}</td></tr>)}
          </tbody></table></div> : <EmptyState title="No invoices recorded" detail="Invoice metadata will appear after it is associated with an issued purchase order." />}
        {reconcileMutation.isError && <InlineError error={reconcileMutation.error} />}
      </section>}
      {user.permissions.includes("RECONCILIATION_READ") && <section className="surface-card table-card payment-panel">
        <div className="section-heading"><div><h2>Reconciliation runs</h2><p>Immutable point-in-time comparison of PO, invoice, delivery, and sandbox payment state</p></div></div>
        {reconciliations.isLoading ? <LoadingPanel /> : reconciliations.isError ? <InlineError error={reconciliations.error} /> : reconciliations.data?.length ?
          <div className="reconciliation-list">{reconciliations.data.map((run) => <article className="reconciliation-row" key={run.id}>
            <div><Link className="table-action" to={`/workspace/fulfillment/reconciliations/${run.id}`}>Details</Link><StatusBadge value={run.status} /><strong>PO {run.purchaseOrderId.slice(0, 8)}</strong><small>{new Date(run.reconciledAt).toLocaleString()}</small></div>
            <ul>{Array.isArray(run.findings) ? run.findings.map((finding, index) => <li key={index}>{typeof finding === "string" ? finding : JSON.stringify(finding)}</li>) : <li>No findings returned</li>}</ul>
          </article>)}</div> : <EmptyState title="No reconciliation runs" detail="Reconcile an invoice to compare it against fulfillment and sandbox payment state." />}
      </section>}
      {orders.isError && <InlineError error={orders.error} />}
    </>
  );
}

function PaymentDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const payment = useQuery({
    queryKey: ["payment", user.activeOrganization.id, id],
    queryFn: () => getPayment(id),
    enabled: Boolean(id) && user.permissions.includes("PAYMENT_READ"),
  });
  if (!user.permissions.includes("PAYMENT_READ")) return <PermissionNotice />;
  if (payment.isLoading) return <LoadingPanel />;
  if (payment.isError) return <InlineError error={payment.error} />;
  if (!payment.data) return <EmptyState title="Payment not found" detail="This payment may have been removed or you may not have access." />;
  const record = payment.data;
  return <>
    <div className="detail-back"><Link to="/workspace/payments"><ArrowLeft size={13} /> Payment ledger</Link></div>
    <div className="page-heading"><div><p className="eyebrow">SANDBOX PAYMENT</p><h1>{record.id}</h1><p className="page-subtitle">Internal payment reference {record.sandboxAuthorizationReference}</p></div><StatusBadge value={record.status} /></div>
    <div className="detail-metrics">
      <DetailMetric label="Amount" value={formatMoney(record.amount, record.currency)} icon={<CircleDollarSign size={17} />} />
      <DetailMetric label="Purchase order" value={record.purchaseOrderId.slice(0, 8)} icon={<ShoppingCart size={17} />} />
      <DetailMetric label="Mandate" value={record.mandateId.slice(0, 8)} icon={<Shield size={17} />} />
      <DetailMetric label="Receipts" value={String(record.receipts.length)} icon={<ReceiptText size={17} />} />
    </div>
    <section className="surface-card table-card"><div className="section-heading"><div><h2>Payment lifecycle</h2><p>Sandbox state timestamps and immutable operation receipts</p></div></div>
      <div className="order-provenance"><span>Authorized <code>{record.authorizedAt ? new Date(record.authorizedAt).toLocaleString() : "—"}</code></span>
        <span>Captured <code>{record.capturedAt ? new Date(record.capturedAt).toLocaleString() : "—"}</code></span>
        <span>Refunded <code>{record.refundedAt ? new Date(record.refundedAt).toLocaleString() : "—"}</code></span>
        <span>PO <Link to={`/workspace/purchase-orders/${record.purchaseOrderId}`}><code>{record.purchaseOrderId}</code></Link></span></div>
      {record.receipts.length ? <div className="table-scroll"><table><thead><tr><th>Operation</th><th>Sandbox reference</th><th>Amount</th><th>Recorded</th></tr></thead><tbody>
        {record.receipts.map((receipt) => <tr key={receipt.id}><td><StatusBadge value={receipt.operation} /></td><td><code>{receipt.sandboxReference}</code></td><td>{formatMoney(receipt.amount, receipt.currency)}</td><td>{new Date(receipt.createdAt).toLocaleString()}</td></tr>)}
      </tbody></table></div> : <EmptyState title="No receipts" detail="Payment operation receipts will appear here." />}
    </section>
  </>;
}

function ShipmentDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const shipment = useQuery({
    queryKey: ["shipment", user.activeOrganization.id, id],
    queryFn: () => getShipment(id),
    enabled: Boolean(id) && user.permissions.includes("SHIPMENT_READ"),
  });
  if (!user.permissions.includes("SHIPMENT_READ")) return <PermissionNotice />;
  if (shipment.isLoading) return <LoadingPanel />;
  if (shipment.isError) return <InlineError error={shipment.error} />;
  if (!shipment.data) return <EmptyState title="Shipment not found" detail="This shipment may have been removed or you may not have access." />;
  const record = shipment.data;
  return <>
    <div className="detail-back"><Link to="/workspace/fulfillment"><ArrowLeft size={13} /> Fulfillment</Link></div>
    <div className="page-heading"><div><p className="eyebrow">SHIPMENT TRACKING</p><h1>{record.trackingNumber}</h1><p className="page-subtitle">{record.carrier} · shipment {record.id}</p></div><StatusBadge value={record.status} /></div>
    <div className="detail-metrics">
      <DetailMetric label="Purchase order" value={record.purchaseOrderId.slice(0, 8)} icon={<ShoppingCart size={17} />} />
      <DetailMetric label="Shipped" value={new Date(record.shippedAt).toLocaleString()} icon={<Truck size={17} />} />
      <DetailMetric label="Expected" value={record.expectedAt ? new Date(record.expectedAt).toLocaleString() : "Not provided"} icon={<Activity size={17} />} />
      <DetailMetric label="Delivered" value={record.deliveredAt ? new Date(record.deliveredAt).toLocaleString() : "Not delivered"} icon={<PackageCheck size={17} />} />
    </div>
    {record.exceptionReason && <div className="inline-error" role="status">Shipment exception: {record.exceptionReason}</div>}
    <section className="surface-card table-card"><div className="section-heading"><div><h2>Shipped items</h2><p>Recorded quantities linked to purchase-order line items</p></div>
      <Link className="table-action" to={`/workspace/purchase-orders/${record.purchaseOrderId}`}>View purchase order</Link></div>
      <div className="table-scroll"><table><thead><tr><th>Item</th><th>PO line</th><th>Shipped quantity</th></tr></thead><tbody>
        {record.items.map((item) => <tr key={item.id}><td>{item.description}</td><td><code>{item.purchaseOrderItemId.slice(0, 8)}</code></td><td>{item.quantity}</td></tr>)}
      </tbody></table></div>
    </section>
  </>;
}

function InvoiceDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const invoice = useQuery({
    queryKey: ["invoice", user.activeOrganization.id, id],
    queryFn: () => getInvoice(id),
    enabled: Boolean(id) && user.permissions.includes("INVOICE_READ"),
  });
  const reconciliations = useReconciliations();
  if (!user.permissions.includes("INVOICE_READ")) return <PermissionNotice />;
  if (invoice.isLoading) return <LoadingPanel />;
  if (invoice.isError) return <InlineError error={invoice.error} />;
  if (!invoice.data) return <EmptyState title="Invoice not found" detail="This invoice may have been removed or you may not have access." />;
  const record = invoice.data;
  const relatedRuns = (reconciliations.data ?? []).filter((run) => run.invoiceId === record.id);
  return <>
    <div className="detail-back"><Link to="/workspace/fulfillment"><ArrowLeft size={13} /> Fulfillment</Link></div>
    <div className="page-heading"><div><p className="eyebrow">INVOICE METADATA</p><h1>{record.invoiceNumber}</h1><p className="page-subtitle">Invoice {record.id} · documents are not stored in this workflow</p></div><StatusBadge value={record.status} /></div>
    <div className="detail-metrics">
      <DetailMetric label="Invoice amount" value={formatMoney(record.amount, record.currency)} icon={<CircleDollarSign size={17} />} />
      <DetailMetric label="Invoice date" value={record.invoiceDate} icon={<ReceiptText size={17} />} />
      <DetailMetric label="Due date" value={record.dueDate ?? "Not provided"} icon={<Activity size={17} />} />
      <DetailMetric label="Purchase order" value={record.purchaseOrderId.slice(0, 8)} icon={<ShoppingCart size={17} />} />
    </div>
    <section className="surface-card table-card"><div className="section-heading"><div><h2>Reconciliation history</h2><p>Immutable runs performed against this invoice</p></div>
      <Link className="table-action" to={`/workspace/purchase-orders/${record.purchaseOrderId}`}>View purchase order</Link></div>
      {reconciliations.isError ? <InlineError error={reconciliations.error} /> : relatedRuns.length ?
        <div className="table-scroll"><table><thead><tr><th>Result</th><th>Run ID</th><th>Reconciled</th></tr></thead><tbody>
          {relatedRuns.map((run) => <tr key={run.id}><td><StatusBadge value={run.status} /></td><td><Link className="table-action" to={`/workspace/fulfillment/reconciliations/${run.id}`}>{run.id}</Link></td><td>{new Date(run.reconciledAt).toLocaleString()}</td></tr>)}
        </tbody></table></div> : <EmptyState title="No reconciliation runs" detail="Reconciliation results will appear after a run is created." />}
    </section>
  </>;
}

function ReconciliationDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useSession();
  const run = useQuery({
    queryKey: ["reconciliation", user.activeOrganization.id, id],
    queryFn: () => getReconciliation(id),
    enabled: Boolean(id) && user.permissions.includes("RECONCILIATION_READ"),
  });
  if (!user.permissions.includes("RECONCILIATION_READ")) return <PermissionNotice />;
  if (run.isLoading) return <LoadingPanel />;
  if (run.isError) return <InlineError error={run.error} />;
  if (!run.data) return <EmptyState title="Reconciliation not found" detail="This run may have been removed or you may not have access." />;
  const record = run.data;
  const findings = Array.isArray(record.findings) ? record.findings : [];
  return <>
    <div className="detail-back"><Link to="/workspace/fulfillment"><ArrowLeft size={13} /> Fulfillment</Link></div>
    <div className="page-heading"><div><p className="eyebrow">RECONCILIATION RUN</p><h1>{record.id}</h1><p className="page-subtitle">Immutable comparison captured {new Date(record.reconciledAt).toLocaleString()}</p></div><StatusBadge value={record.status} /></div>
    <div className="detail-metrics">
      <DetailMetric label="Purchase order" value={record.purchaseOrderId.slice(0, 8)} icon={<ShoppingCart size={17} />} />
      <DetailMetric label="Invoice" value={record.invoiceId.slice(0, 8)} icon={<ReceiptText size={17} />} />
      <DetailMetric label="Payment intent" value={record.paymentIntentId?.slice(0, 8) ?? "None"} icon={<CircleDollarSign size={17} />} />
      <DetailMetric label="Findings" value={String(findings.length)} icon={<ClipboardCheck size={17} />} />
    </div>
    <div className="order-provenance"><Link to={`/workspace/purchase-orders/${record.purchaseOrderId}`}>View purchase order</Link>
      <Link to={`/workspace/fulfillment/invoices/${record.invoiceId}`}>View invoice</Link></div>
    <section className="surface-card contract-content"><div className="section-heading"><div><h2>Reconciliation findings</h2><p>Point-in-time findings; rerun after source data changes to create a new assessment.</p></div></div>
      {findings.length ? <ul className="reconciliation-findings">{findings.map((finding, index) =>
        <li key={index}>{typeof finding === "string" ? finding : JSON.stringify(finding, null, 2)}</li>)}</ul> :
        <EmptyState title="No findings returned" detail="The reconciliation run did not include findings." />}
    </section>
  </>;
}

function RfqTable({ rfqs, showEmpty, canPublish = false, canQuote = false, onPublish, publishingId }: { rfqs: Rfq[]; showEmpty: boolean; canPublish?: boolean; canQuote?: boolean; onPublish?: (id: string) => void; publishingId?: string }) {
  if (!rfqs.length) return showEmpty ? <EmptyState title="No RFQs found" detail="Create a request to begin your procurement workflow." /> : null;
  const hasAction = canPublish || canQuote;
  return (
    <div className="table-scroll"><table><thead><tr><th>Request</th><th>Category</th><th>Budget</th><th>Deadline</th><th>Status</th>{hasAction && <th>Action</th>}</tr></thead>
      <tbody>{rfqs.map((rfq) => <tr key={rfq.id}>
        <td><Link className="table-primary-link" to={`/workspace/rfqs/${rfq.id}`}><strong>{rfq.title}</strong><small>{rfq.items?.length ?? 0} line items · {rfq.requestType}</small></Link></td>
        <td>{rfq.category}</td><td>{formatMoney(rfq.budget, rfq.currency)}</td>
        <td>{new Date(rfq.deadline).toLocaleDateString()}</td><td><StatusBadge value={rfq.status} /></td>
        {hasAction && <td>
          {canPublish && rfq.status.toUpperCase() === "DRAFT" && <button className="table-action" disabled={publishingId === rfq.id} onClick={() => onPublish?.(rfq.id)}>{publishingId === rfq.id ? "Publishing…" : "Publish"}</button>}
          {canQuote && rfq.status.toUpperCase() === "PUBLISHED" && <Link className="table-action" to={`/workspace/quotations/new/${rfq.id}`}>Respond</Link>}
        </td>}
      </tr>)}</tbody></table></div>
  );
}

function DetailMetric({ label, value, icon }: { label: string; value: string; icon: ReactNode }) {
  return <article className="detail-metric"><span>{icon}</span><small>{label}</small><strong>{value}</strong></article>;
}

function QuickAction({ to, icon, title, detail }: { to: string; icon: ReactNode; title: string; detail: string }) {
  return <Link className="quick-action" to={to}><span className="quick-icon">{icon}</span><span><strong>{title}</strong><small>{detail}</small></span><ArrowUpRight size={16} /></Link>;
}

function PageIntro({ eyebrow, title, detail }: { eyebrow: string; title: string; detail: string }) {
  return <div className="page-heading"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="page-subtitle">{detail}</p></div></div>;
}

function StatusBadge({ value }: { value: string }) {
  const normalized = value.toLowerCase().replaceAll("_", " ");
  return <span className={`status-badge status-${normalized.split(" ")[0]}`}>{normalized}</span>;
}

function EmptyState({ title, detail }: { title: string; detail: string }) {
  return <div className="empty-state"><div className="empty-state-icon"><FileText size={19} /></div><strong>{title}</strong><p>{detail}</p></div>;
}

function InlineError({ error }: { error: unknown }) {
  return <div className="inline-error" role="alert">{apiErrorMessage(error)}</div>;
}

function PermissionNotice() {
  return <div className="permission-notice"><ShieldCheck size={17} /> This view is scoped to your current organization and assigned permissions.</div>;
}

function LoadingPanel() {
  return <div className="surface-card loading-panel"><span className="loading-spinner" />Loading organization records…</div>;
}

function initials(name: string): string {
  return name.split(/\s+/).slice(0, 2).map((part) => part[0] ?? "").join("").toUpperCase();
}

function toLocalDateTimeInput(date: Date): string {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
}

function formatMoney(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
  } catch {
    return `${currency} ${amount.toLocaleString()}`;
  }
}

function recordLabel(record: BusinessRecord): string {
  for (const key of ["poNumber", "number", "purchaseOrderNumber", "subject", "title", "invoiceNumber"]) {
    if (typeof record[key] === "string" && record[key]) return record[key] as string;
  }
  return `${String(record.id).slice(0, 8)}…`;
}

function recordSubtitle(record: BusinessRecord): string {
  const amount = typeof record.totalAmount === "number" ? record.totalAmount : record.amount;
  const currency = typeof record.currency === "string" ? record.currency : "";
  if (typeof amount === "number") return `${currency} ${amount.toLocaleString()}${record.vendorId ? ` · Vendor ${String(record.vendorId).slice(0, 8)}` : ""}`;
  if (typeof record.requesterName === "string") return record.requesterName;
  if (typeof record.reason === "string") return record.reason;
  return "Organization-scoped record";
}

function recordDate(record: BusinessRecord): string {
  for (const key of ["createdAt", "created_at", "issuedAt", "requestedAt"]) {
    const value = record[key];
    if (typeof value === "string" && !Number.isNaN(Date.parse(value))) return new Date(value).toLocaleString();
  }
  return "Recent";
}

function hasPendingApprovalStep(steps: unknown, userId: string): boolean {
  if (!Array.isArray(steps)) return false;
  return steps.some((step: unknown) =>
    typeof step === "object"
    && step !== null
    && "approverUserId" in step
    && "status" in step
    && step.approverUserId === userId
    && step.status === "PENDING");
}
