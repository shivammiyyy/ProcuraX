export interface OrganizationSummary {
  id: string;
  name: string;
  role: string;
}

export interface SessionUser {
  userId: string;
  email: string;
  fullName: string;
  activeOrganization: OrganizationSummary;
  permissions: string[];
  organizations: OrganizationSummary[];
}

export interface RfqItem {
  id?: string;
  description: string;
  quantity: number;
  unit?: string;
  specification?: string;
}

export interface Rfq {
  id: string;
  title: string;
  description: string;
  requestType: string;
  category: string;
  budget: number;
  currency: string;
  deadline: string;
  deliveryDays: number;
  status: string;
  items: RfqItem[];
}

export interface Quotation {
  id: string;
  rfqId: string;
  vendorId: string;
  totalAmount: number;
  currency: string;
  deliveryDays: number;
  paymentTermsDays: number;
  status: string;
  qualityRating?: number;
  compliance?: Record<string, unknown>;
  items?: { rfqItemId: string; unitPrice: number; quantity: number }[];
  vendorScore?: { score: number; explanation?: string; factors?: Record<string, number> };
}

export interface Vendor {
  id: string;
  name: string;
  contactEmail: string | null;
  category: string;
  status: string;
  riskLevel: string;
  paymentTermsDays: number;
  performanceScore: number;
}

export interface VendorDocument {
  id: string;
  vendorId: string;
  documentType: string;
  fileName: string;
  storageUrl: string;
  verificationStatus: "UNVERIFIED" | "VERIFIED" | "REJECTED";
  rejectionReason?: string;
  createdAt: string;
}

export interface Contract {
  id: string;
  rfqId: string;
  quotationId: string;
  vendorId: string;
  vendorName: string;
  content: string;
  documentFileName: string | null;
  startDate: string;
  endDate: string;
  status: string;
  auditStatus: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ContractAudit {
  id: string;
  contractId: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  finding: string;
  clause: string | null;
  explanation: string | null;
  recommendation: string | null;
  confidence: number | null;
  createdBy: string | null;
  createdAt: string;
}

export interface ContractDecision {
  id: string;
  contractId: string;
  decision: "APPROVED" | "REJECTED";
  comment: string;
  decidedBy: string;
  decidedAt: string;
}

export interface ContractAiReview {
  id: string;
  contractId: string;
  requestedByUserId: string;
  createdAt: string;
  retrievalMethod: "ephemeral_bm25";
  result: {
    clauses: {
      clause: string;
      question: string;
      evidence_status: "EVIDENCE_FOUND" | "NO_MATCHING_EVIDENCE";
      citations: {
        chunk_id: number;
        start_character: number;
        end_character: number;
        relevance_score: number;
        excerpt: string;
      }[];
      reviewer_note: string;
    }[];
    requires_human_review: true;
    document_persisted: false;
  };
}

export interface ContractAiAnalysis {
  id: string;
  contractId: string;
  reviewId: string;
  requestedByUserId: string;
  createdAt: string;
  model: string;
  result: {
    summaries: {
      clause: string;
      summary: string;
      reviewer_questions: string[];
      cited_chunk_ids: number[];
    }[];
    advisory_only: true;
    requires_human_review: true;
  };
}

export interface ContractSemanticSearch {
  contractId: string;
  embeddingModel: string;
  matches: {
    reviewId: string;
    chunkId: number;
    startCharacter: number;
    endCharacter: number;
    excerpt: string;
    cosineSimilarity: number;
  }[];
}

export interface PurchaseOrder extends BusinessRecord {
  poNumber: string;
  approvalRequestId: string;
  rfqId: string;
  quotationId: string;
  vendorId: string;
  vendorName: string;
  vendorContactEmail: string | null;
  totalAmount: number;
  currency: string;
  deliveryDays: number;
  paymentTermsDays: number;
  status: string;
  issuedAt: string;
  createdAt: string;
  version: number;
  items: { id: string; rfqItemId: string; description: string; specification?: string; quantity: number; unit?: string; unitPrice: number; lineTotal: number }[];
}

export interface PaymentMandate extends BusinessRecord {
  purchaseOrderId: string;
  maximumAmount: number;
  currency: string;
  expiresAt: string;
  status: string;
}

export interface Payment extends BusinessRecord {
  purchaseOrderId: string;
  mandateId: string;
  amount: number;
  currency: string;
  status: string;
  sandboxAuthorizationReference: string;
  authorizedAt: string | null;
  capturedAt: string | null;
  refundedAt: string | null;
  receipts: { id: string; operation: string; sandboxReference: string; amount: number; currency: string; createdAt: string }[];
}

export interface Shipment extends BusinessRecord {
  purchaseOrderId: string;
  trackingNumber: string;
  carrier: string;
  status: string;
  expectedAt?: string;
  shippedAt: string;
  deliveredAt?: string;
  exceptionReason?: string;
  items: { id: string; purchaseOrderItemId: string; description: string; quantity: number }[];
}

export interface Invoice extends BusinessRecord {
  purchaseOrderId: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  amount: number;
  currency: string;
  status: string;
  createdAt: string;
}

export interface Reconciliation extends BusinessRecord {
  purchaseOrderId: string;
  invoiceId: string;
  paymentIntentId?: string;
  status: string;
  findings: unknown[];
  reconciledAt: string;
}

export type BusinessRecord = Record<string, unknown> & { id: string };
