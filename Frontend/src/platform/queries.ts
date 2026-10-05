import { useQuery } from "@tanstack/react-query";
import { useSession } from "./session";
import {
  listApprovals,
  listContracts,
  listInvoices,
  listPaymentMandates,
  listPayments,
  listPurchaseOrders,
  listQuotations,
  listReconciliations,
  listRfqs,
  listShipments,
  listVendors,
} from "./api";
import type { Invoice, Payment, PaymentMandate, PurchaseOrder, Reconciliation, Shipment, Vendor } from "./types";

export function useRfqs() {
  const { user } = useSession();
  return useQuery({
    queryKey: ["rfqs", user.activeOrganization.id],
    queryFn: listRfqs,
    enabled: user.permissions.includes("RFQ_READ"),
  });
}

export function useQuotations() {
  const { user } = useSession();
  return useQuery({
    queryKey: ["quotations", user.activeOrganization.id],
    queryFn: listQuotations,
    enabled: user.permissions.includes("QUOTE_READ"),
  });
}

export function usePurchaseOrders() {
  const { user } = useSession();
  return useQuery({
    queryKey: ["purchase-orders", user.activeOrganization.id],
    queryFn: listPurchaseOrders,
    enabled: user.permissions.includes("PO_READ"),
  });
}

export function useApprovals() {
  const { user } = useSession();
  return useQuery({
    queryKey: ["approvals", user.activeOrganization.id],
    queryFn: listApprovals,
    enabled: user.permissions.includes("APPROVAL_READ"),
  });
}

export function useContracts() {
  const { user } = useSession();
  return useQuery({
    queryKey: ["contracts", user.activeOrganization.id],
    queryFn: listContracts,
    enabled: user.permissions.includes("CONTRACT_READ"),
  });
}

export function useVendors() {
  const { user } = useSession();
  return useQuery<Vendor[]>({
    queryKey: ["vendors", user.activeOrganization.id],
    queryFn: listVendors,
    enabled: user.permissions.includes("VENDOR_READ"),
  });
}

export function usePaymentMandates() {
  const { user } = useSession();
  return useQuery<PaymentMandate[]>({
    queryKey: ["payment-mandates", user.activeOrganization.id],
    queryFn: listPaymentMandates,
    enabled: user.permissions.includes("PAYMENT_READ"),
  });
}

export function usePayments() {
  const { user } = useSession();
  return useQuery<Payment[]>({
    queryKey: ["payments", user.activeOrganization.id],
    queryFn: listPayments,
    enabled: user.permissions.includes("PAYMENT_READ"),
  });
}

export function useShipments() {
  const { user } = useSession();
  return useQuery<Shipment[]>({
    queryKey: ["shipments", user.activeOrganization.id],
    queryFn: listShipments,
    enabled: user.permissions.includes("SHIPMENT_READ"),
  });
}

export function useInvoices() {
  const { user } = useSession();
  return useQuery<Invoice[]>({
    queryKey: ["invoices", user.activeOrganization.id],
    queryFn: listInvoices,
    enabled: user.permissions.includes("INVOICE_READ"),
  });
}

export function useReconciliations() {
  const { user } = useSession();
  return useQuery<Reconciliation[]>({
    queryKey: ["reconciliations", user.activeOrganization.id],
    queryFn: listReconciliations,
    enabled: user.permissions.includes("RECONCILIATION_READ"),
  });
}
