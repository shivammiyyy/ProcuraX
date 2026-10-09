import axios from "axios";
import type {
  BusinessRecord,
  Contract,
  ContractAiAnalysis,
  ContractAiReview,
  ContractSemanticSearch,
  ContractAudit,
  ContractDecision,
  Invoice,
  Payment,
  PaymentMandate,
  PurchaseOrder,
  Quotation,
  Reconciliation,
  Rfq,
  SessionUser,
  Shipment,
  Vendor,
  VendorDocument,
} from "./types";

const api = axios.create({
  baseURL: (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, ""),
  withCredentials: true,
  headers: { Accept: "application/json" },
});

function csrfToken(): string | undefined {
  const cookie = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith("XSRF-TOKEN="))
    ?.slice("XSRF-TOKEN=".length);
  return cookie ? decodeURIComponent(cookie) : undefined;
}

api.interceptors.request.use((config) => {
  if (["post", "put", "patch", "delete"].includes(config.method?.toLowerCase() ?? "")) {
    const token = csrfToken();
    if (token) config.headers.set("X-XSRF-TOKEN", token);
  }
  return config;
});

export async function getSession(): Promise<SessionUser | null> {
  try {
    await api.get<{ token: string }>("/api/v1/auth/csrf");
    const response = await api.get<SessionUser>("/api/v1/auth/me");
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) return null;
    throw error;
  }
}

export async function switchOrganization(organizationId: string): Promise<SessionUser> {
  const response = await api.post<SessionUser>(`/api/v1/auth/organizations/${organizationId}/switch`);
  return response.data;
}

export async function logout(): Promise<void> {
  await api.post("/logout");
}

export async function listRfqs(): Promise<Rfq[]> {
  const response = await api.get<Rfq[]>("/api/v1/rfqs", { params: { page: 0, size: 100 } });
  return response.data;
}

export async function getRfq(id: string): Promise<Rfq> {
  const response = await api.get<Rfq>(`/api/v1/rfqs/${id}`);
  return response.data;
}

export async function listQuotations(): Promise<Quotation[]> {
  const response = await api.get<Quotation[]>("/api/v1/quotations", { params: { page: 0, size: 100 } });
  return response.data;
}

export async function getQuotation(id: string): Promise<Quotation> {
  const response = await api.get<Quotation>(`/api/v1/quotations/${id}`);
  return response.data;
}

export async function updateQuotationStatus(id: string, status: "UNDER_REVIEW" | "ACCEPTED" | "REJECTED"): Promise<Quotation> {
  const response = await api.patch<Quotation>(`/api/v1/quotations/${id}/status`, { status });
  return response.data;
}

export async function listPurchaseOrders(): Promise<PurchaseOrder[]> {
  const response = await api.get<PurchaseOrder[]>("/api/v1/purchase-orders", {
    params: { page: 0, size: 100 },
  });
  return response.data;
}

export async function getPurchaseOrder(id: string): Promise<PurchaseOrder> {
  const response = await api.get<PurchaseOrder>(`/api/v1/purchase-orders/${id}`);
  return response.data;
}

export async function listContracts(): Promise<Contract[]> {
  const response = await api.get<Contract[]>("/api/v1/contracts", { params: { page: 0, size: 100 } });
  return response.data;
}

export async function getContract(id: string): Promise<Contract> {
  const response = await api.get<Contract>(`/api/v1/contracts/${id}`);
  return response.data;
}

export async function uploadContractDocument(id: string, file: File): Promise<{ contractId: string; fileName: string }> {
  const data = new FormData();
  data.append("file", file);
  const response = await api.post<{ contractId: string; fileName: string }>(
    `/api/v1/contracts/${id}/document`,
    data,
  );
  return response.data;
}

export async function createContractDocumentDownload(id: string): Promise<{ downloadUrl: string; expiresAt: string }> {
  const response = await api.get<{ downloadUrl: string; expiresAt: string }>(
    `/api/v1/contracts/${id}/document/download`,
    { headers: { "Cache-Control": "no-store" } },
  );
  return response.data;
}

export async function listContractAiReviews(id: string): Promise<ContractAiReview[]> {
  const response = await api.get<ContractAiReview[]>(`/api/v1/contracts/${id}/ai-reviews`);
  return response.data;
}

export async function runContractAiReview(id: string): Promise<ContractAiReview> {
  const response = await api.post<ContractAiReview>(`/api/v1/contracts/${id}/ai-reviews`);
  return response.data;
}

export async function uploadContractForAiReview(id: string, file: File): Promise<ContractAiReview> {
  const data = new FormData();
  data.append("file", file);
  const response = await api.post<ContractAiReview>(`/api/v1/contracts/${id}/ai-reviews/from-document`, data);
  return response.data;
}

export async function listContractAiAnalyses(id: string): Promise<ContractAiAnalysis[]> {
  const response = await api.get<ContractAiAnalysis[]>(`/api/v1/contracts/${id}/ai-analyses`);
  return response.data;
}

export async function analyzeContractAiReview(id: string, reviewId: string): Promise<ContractAiAnalysis> {
  const response = await api.post<ContractAiAnalysis>(`/api/v1/contracts/${id}/ai-reviews/${reviewId}/analysis`);
  return response.data;
}

export async function searchContractEvidence(id: string, query: string): Promise<ContractSemanticSearch> {
  const response = await api.post<ContractSemanticSearch>(`/api/v1/contracts/${id}/semantic-search`, { query });
  return response.data;
}

export async function createContract(request: {
  quotationId: string;
  content: string;
  startDate: string;
  endDate: string;
}): Promise<Contract> {
  const response = await api.post<Contract>("/api/v1/contracts", request);
  return response.data;
}

export async function listContractAudits(id: string): Promise<ContractAudit[]> {
  const response = await api.get<ContractAudit[]>(`/api/v1/contracts/${id}/audits`);
  return response.data;
}

export async function addContractAudit(id: string, request: {
  riskLevel: ContractAudit["riskLevel"];
  finding: string;
  clause?: string;
  explanation?: string;
  recommendation?: string;
  confidence?: number;
}): Promise<ContractAudit> {
  const response = await api.post<ContractAudit>(`/api/v1/contracts/${id}/audits`, request);
  return response.data;
}

export async function listContractDecisions(id: string): Promise<ContractDecision[]> {
  const response = await api.get<ContractDecision[]>(`/api/v1/contracts/${id}/decisions`);
  return response.data;
}

export async function decideContract(
  id: string,
  decision: "APPROVE" | "REJECT",
  comment: string,
): Promise<ContractDecision> {
  const response = await api.post<ContractDecision>(`/api/v1/contracts/${id}/decision`, { decision, comment });
  return response.data;
}

export async function listApprovals(): Promise<BusinessRecord[]> {
  const response = await api.get<BusinessRecord[]>("/api/v1/approvals");
  return response.data;
}

export interface CreateRfq {
  title: string;
  description: string;
  requestType: "RFQ" | "RFP";
  category: string;
  budget: number;
  currency: string;
  deadline: string;
  deliveryDays: number;
  items: { description: string; quantity: number; unit: string; specification: string }[];
}

export async function createRfq(request: CreateRfq): Promise<Rfq> {
  const response = await api.post<Rfq>("/api/v1/rfqs", request);
  return response.data;
}

export async function updateRfq(id: string, request: CreateRfq): Promise<Rfq> {
  const response = await api.put<Rfq>(`/api/v1/rfqs/${id}`, request);
  return response.data;
}

export async function publishRfq(id: string): Promise<Rfq> {
  const response = await api.post<Rfq>(`/api/v1/rfqs/${id}/publish`);
  return response.data;
}

export interface SubmitQuotation {
  rfqId: string;
  currency: string;
  deliveryDays: number;
  paymentTermsDays: number;
  qualityRating: number;
  compliance: {
    isoCertification: boolean;
    materialGrade: "A+" | "A" | "B" | "C";
    environmentalStandards: boolean;
    documentSubmission: boolean;
  };
  items: { rfqItemId: string; unitPrice: number; quantity: number }[];
}

export async function submitQuotation(request: SubmitQuotation): Promise<Quotation> {
  const response = await api.post<Quotation>("/api/v1/quotations", request);
  return response.data;
}

export async function decideApproval(id: string, decision: "APPROVE" | "REJECT", comment: string): Promise<BusinessRecord> {
  const response = await api.post<BusinessRecord>(`/api/v1/approvals/${id}/decision`, { decision, comment });
  return response.data;
}

export async function listVendors(): Promise<Vendor[]> {
  const response = await api.get<Vendor[]>("/api/v1/vendors", { params: { page: 0, size: 100 } });
  return response.data;
}

export async function createVendor(request: Pick<Vendor, "name" | "contactEmail" | "category" | "paymentTermsDays">): Promise<Vendor> {
  const response = await api.post<Vendor>("/api/v1/vendors", request);
  return response.data;
}

export async function updateVendor(id: string, request: Pick<Vendor, "name" | "contactEmail" | "category" | "paymentTermsDays">): Promise<Vendor> {
  const response = await api.put<Vendor>(`/api/v1/vendors/${id}`, request);
  return response.data;
}

export async function addVendorMember(vendorId: string, userId: string): Promise<void> {
  await api.post(`/api/v1/vendors/${vendorId}/members`, { userId });
}

export async function listVendorDocuments(vendorId: string): Promise<VendorDocument[]> {
  const response = await api.get<VendorDocument[]>(`/api/v1/vendors/${vendorId}/documents`);
  return response.data;
}

export async function uploadVendorDocument(vendorId: string, documentType: string, file: File): Promise<VendorDocument> {
  const data = new FormData();
  data.append("documentType", documentType);
  data.append("file", file);
  const response = await api.post<VendorDocument>(`/api/v1/vendors/${vendorId}/documents/upload`, data);
  return response.data;
}

export async function verifyVendorDocument(
  vendorId: string,
  documentId: string,
  status: "VERIFIED" | "REJECTED",
  rejectionReason = "",
): Promise<VendorDocument> {
  const response = await api.patch<VendorDocument>(
    `/api/v1/vendors/${vendorId}/documents/${documentId}/verification`,
    { status, rejectionReason },
  );
  return response.data;
}

export async function listPaymentMandates(): Promise<PaymentMandate[]> {
  const response = await api.get<PaymentMandate[]>("/api/v1/payment-mandates");
  return response.data;
}

export async function createPaymentMandate(purchaseOrderId: string, expiresAt: string): Promise<PaymentMandate> {
  const response = await api.post<PaymentMandate>("/api/v1/payment-mandates", { purchaseOrderId, expiresAt });
  return response.data;
}

export async function revokePaymentMandate(id: string): Promise<PaymentMandate> {
  const response = await api.post<PaymentMandate>(`/api/v1/payment-mandates/${id}/revoke`);
  return response.data;
}

export async function listPayments(): Promise<Payment[]> {
  const response = await api.get<Payment[]>("/api/v1/payments");
  return response.data;
}

export async function getPayment(id: string): Promise<Payment> {
  const response = await api.get<Payment>(`/api/v1/payments/${id}`);
  return response.data;
}

export async function authorizePayment(purchaseOrderId: string, mandateId: string): Promise<Payment> {
  const response = await api.post<Payment>("/api/v1/payments/authorize", { purchaseOrderId, mandateId }, {
    headers: { "Idempotency-Key": crypto.randomUUID() },
  });
  return response.data;
}

export async function operatePayment(id: string, operation: "capture" | "refund"): Promise<Payment> {
  const response = await api.post<Payment>(`/api/v1/payments/${id}/${operation}`, null, {
    headers: { "Idempotency-Key": crypto.randomUUID() },
  });
  return response.data;
}

export async function listShipments(): Promise<Shipment[]> {
  const response = await api.get<Shipment[]>("/api/v1/shipments", { params: { page: 0, size: 100 } });
  return response.data;
}

export async function getShipment(id: string): Promise<Shipment> {
  const response = await api.get<Shipment>(`/api/v1/shipments/${id}`);
  return response.data;
}

export async function createShipment(request: {
  purchaseOrderId: string;
  trackingNumber: string;
  carrier: string;
  expectedAt?: string;
  items: { purchaseOrderItemId: string; quantity: number }[];
}): Promise<Shipment> {
  const response = await api.post<Shipment>("/api/v1/shipments", request);
  return response.data;
}

export async function updateShipmentStatus(
  id: string,
  status: "DELIVERED" | "EXCEPTION",
  exceptionReason = "",
): Promise<Shipment> {
  const response = await api.post<Shipment>(`/api/v1/shipments/${id}/status`, { status, exceptionReason });
  return response.data;
}

export async function listInvoices(): Promise<Invoice[]> {
  const response = await api.get<Invoice[]>("/api/v1/invoices", { params: { page: 0, size: 100 } });
  return response.data;
}

export async function getInvoice(id: string): Promise<Invoice> {
  const response = await api.get<Invoice>(`/api/v1/invoices/${id}`);
  return response.data;
}

export async function createInvoice(request: {
  purchaseOrderId: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  amount: number;
  currency: string;
}): Promise<Invoice> {
  const response = await api.post<Invoice>("/api/v1/invoices", request);
  return response.data;
}

export async function listReconciliations(): Promise<Reconciliation[]> {
  const response = await api.get<Reconciliation[]>("/api/v1/reconciliations", { params: { page: 0, size: 100 } });
  return response.data;
}

export async function getReconciliation(id: string): Promise<Reconciliation> {
  const response = await api.get<Reconciliation>(`/api/v1/reconciliations/${id}`);
  return response.data;
}

export async function reconcileInvoice(invoiceId: string): Promise<Reconciliation> {
  const response = await api.post<Reconciliation>("/api/v1/reconciliations", { invoiceId });
  return response.data;
}

export function beginGoogleLogin(): void {
  window.location.assign(`${api.defaults.baseURL}/oauth2/authorization/google`);
}

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string; detail?: string }>(error)) {
    return error.response?.data?.message ?? error.response?.data?.detail ?? error.message;
  }
  return error instanceof Error ? error.message : "Something went wrong. Please try again.";
}
