# Vendor, RFQ, Quotation API (Spring Boot `/api/v1`)

This is the first migrated procurement slice. The legacy Node `/api/v0` routes remain in place during the cutover; no legacy route or data has been deleted. The new APIs persist to PostgreSQL, validate inputs and derive tenant context from the authenticated Spring Security principal.

## Vendor endpoints

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/vendors?page=0&size=50` | `VENDOR_READ` | Lists vendors in the active organization; page size is capped at 100. |
| `GET` | `/api/v1/vendors/{id}` | `VENDOR_READ` | Returns the vendor only if it belongs to the active organization. |
| `POST` | `/api/v1/vendors` | `VENDOR_CREATE` | Creates a vendor in the active organization. |
| `PUT` | `/api/v1/vendors/{id}` | `VENDOR_UPDATE` | Updates a vendor in the active organization. |
| `POST` | `/api/v1/vendors/{id}/members` | `USER_MANAGE` | Links an active organization member to this vendor. |
| `GET` | `/api/v1/vendors/{id}/documents` | `VENDOR_READ` | Lists tenant-scoped document references; vendor principals must be associated with that vendor. |
| `POST` | `/api/v1/vendors/{id}/documents` | `VENDOR_DOCUMENT_SUBMIT` or `VENDOR_UPDATE` | Registers a Cloudinary PDF, DOCX, JPG, or PNG reference. Vendor principals must be associated with that vendor. |
| `POST` | `/api/v1/vendors/{id}/documents/upload` | `VENDOR_DOCUMENT_SUBMIT` or `VENDOR_UPDATE` | Accepts multipart fields `documentType` and `file`, validates a PDF, DOCX, JPG, or PNG up to 10 MB, uploads it to Cloudinary, and persists the returned asset reference. |
| `PATCH` | `/api/v1/vendors/{id}/documents/{documentId}/verification` | `VENDOR_DOCUMENT_VERIFY` | Verifies or rejects an unreviewed document; rejection requires a reason. Review decisions are recorded with actor and time and cannot be changed. |

The API does not accept an organization ID for vendor ownership. A supplied extra `organizationId` field is ignored by the request DTO and cannot change the tenant.

## RFQ endpoints

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/rfqs?page=0&size=50` | `RFQ_READ` | Lists RFQs for the active organization. Vendor principals see only published RFQs. |
| `GET` | `/api/v1/rfqs/{id}` | `RFQ_READ` | Organization-scoped detail; draft RFQs are not visible to vendor principals. |
| `POST` | `/api/v1/rfqs` | `RFQ_CREATE` | Creates a draft with validated line items and records the request correlation ID. |
| `PUT` | `/api/v1/rfqs/{id}` | `RFQ_UPDATE` | Replaces draft RFQ fields and items; published RFQs cannot be edited. |
| `POST` | `/api/v1/rfqs/{id}/publish` | `RFQ_PUBLISH` | Publishes a future-deadline draft. |
| `POST` | `/api/v1/rfqs/{id}/close` | `RFQ_UPDATE` | Closes a published or in-progress RFQ. |

Each item requires a description and positive quantity. Budget must be positive, ISO currency is three uppercase letters, and the response deadline must be in the future. Product delivery target is stored separately from the quotation response deadline.

## Quotation endpoints

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/quotations?page=0&size=50` | `QUOTE_READ` | Organization-scoped; vendor principals see only their associated vendors' quotes. |
| `GET` | `/api/v1/quotations/{id}` | `QUOTE_READ` | Vendors can retrieve only their own quotes; non-members receive not-found. |
| `POST` | `/api/v1/quotations` | `QUOTE_SUBMIT` | Submits for an associated vendor against a published RFQ. An optional vendor ID is accepted only if the principal has a persisted membership for it; the total amount is always derived server-side. |
| `PUT` | `/api/v1/quotations/{id}` | `QUOTE_SUBMIT` | Edits an associated vendor's submitted quote while the RFQ is published. |
| `PATCH` | `/api/v1/quotations/{id}/status` | `QUOTE_EVALUATE` | Moves a quote to `UNDER_REVIEW`, `ACCEPTED`, or `REJECTED`; only one quote per RFQ can be accepted. |
| `GET` | `/api/v1/rfqs/{rfqId}/quotations` | `QUOTE_READ` | Buyers see organization quotes; vendors see only their own quote(s). |

Quotations must price every RFQ item exactly once and match each requested quantity. Currency must match the RFQ. The server sums line amounts; any client-supplied total is not part of the DTO. A vendor membership is required to submit or retrieve a quote, and duplicate submissions for a vendor/RFQ are rejected. If the user has multiple vendor memberships, the request must select one of those memberships; the supplied ID is not treated as proof of access.

## Contracts

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/contracts?page=0&size=50` | `CONTRACT_READ` | Lists contracts in the active organization; page size is capped at 100. |
| `GET` | `/api/v1/contracts/{id}` | `CONTRACT_READ` | Returns a tenant-scoped contract and its vendor name. |
| `POST` | `/api/v1/contracts` | `CONTRACT_CREATE` | Creates a draft from an accepted quotation with supplied draft terms and dates. |
| `POST` | `/api/v1/contracts/{id}/document` | `CONTRACT_CREATE` | Accepts one PDF or DOCX up to 10 MB for a draft and stores it as an authenticated Cloudinary asset. |
| `GET` | `/api/v1/contracts/{id}/document/download` | `CONTRACT_READ` | Validates tenant and vendor membership, then returns a signed Cloudinary download URL expiring in five minutes with `Cache-Control: no-store`. |
| `GET` | `/api/v1/contracts/{id}/ai-reviews` | `CONTRACT_READ` | Lists tenant-scoped immutable evidence retrieval snapshots. |
| `POST` | `/api/v1/contracts/{id}/ai-reviews` | `CONTRACT_APPROVE` | Calls the authenticated internal AI service on draft terms, validates the response identity and human-review flags, persists cited evidence, and records an audit/outbox event. |
| `GET` | `/api/v1/contracts/{id}/audits` | `CONTRACT_READ` | Lists organization-scoped human review findings. |
| `POST` | `/api/v1/contracts/{id}/audits` | `CONTRACT_APPROVE` | Records an attributed review finding and marks the draft audit status `COMPLETED`. |
| `GET` | `/api/v1/contracts/{id}/decisions` | `CONTRACT_READ` | Lists the immutable contract decision record. |
| `POST` | `/api/v1/contracts/{id}/decision` | `CONTRACT_APPROVE` | Records one maker-checker approve/reject decision; rejection requires a reason. |

The API derives the RFQ and vendor from the accepted quotation; IDs and organization scope are not accepted from the client. Contract terms are capped at 100,000 characters, date ranges are validated, and the draft is persisted with `DRAFT`/`PENDING` status plus a transactional audit/outbox event. Authorized reviewers may add attributed findings with constrained risk levels and optional confidence (0–1); findings are stored in the existing tenant-constrained `contract_audits` table and event payloads omit finding text. `COMPLETED` means a reviewer recorded a finding, not that the contract is approved or risk-free. A different organization member with `CONTRACT_APPROVE` may then make the single terminal approval/rejection decision; the creator cannot decide their own draft, and rejection requires a reason. The immutable `contract_decisions` row and contract status transition are transactional and audited. Vendor principals can list/read only contracts associated with their persisted vendor memberships. Document uploads are validated by extension, MIME type, signature, and size; one upload is allowed per draft. Uploaded assets use Cloudinary authenticated delivery, and contract DTOs never return their Cloudinary URL. The download endpoint repeats the tenant and vendor-membership checks before creating a five-minute signed Cloudinary download URL; its response is not cacheable. Contract evidence review calls FastAPI with server-derived organization/actor context and a backend-only service token. The response identity, retrieval method, and human-review flags are checked before an immutable tenant-scoped snapshot is stored and audited; event payloads contain only the review ID and retrieval method. FastAPI uses ephemeral lexical retrieval and does not persist the source contract; Spring persists only its cited review result. PDF/DOCX extraction, vector retrieval, legal determination and contract execution remain out of scope.

## Deterministic evaluation

The official persisted score is calculated in Spring Boot using configurable weights in `application.yaml` (default: price 30%, delivery 20%, quality 20%, compliance 15%, historical performance 10%, payment terms 5%). Weights must be non-negative and sum to 1.0. Inputs and per-factor values are stored with the score. The LLM does not set the official score.

Compliance keys are `isoCertification` (boolean), `materialGrade` (`A+`, `A`, `B`, `C`), `environmentalStandards` (boolean), and `documentSubmission` (boolean). FastAPI now exposes a backend-authenticated advisory explanation endpoint using the legacy ONNX model and Spring's deterministic factor breakdown; orchestration and persisted explanation attachment remain future integration work. The AI output does not replace the deterministic score.

For contracts, the FastAPI foundation has a backend-authenticated lexical evidence retrieval endpoint for five common clause topics. It returns source excerpts but does not parse uploads, persist embeddings, or provide legal advice. See [AI_PLATFORM.md](AI_PLATFORM.md#contract-evidence-retrieval).

## Purchasing policies

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/policies/rules` | `POLICY_READ` | Lists policy rules for the active organization. |
| `POST` | `/api/v1/policies/rules` | `POLICY_MANAGE` | Creates a policy rule; optional category scopes it, otherwise it applies to all categories. |
| `PUT` | `/api/v1/policies/rules/{id}` | `POLICY_MANAGE` | Updates an organization-owned rule; another organization's rule is not found. |
| `POST` | `/api/v1/policies/evaluate` | `POLICY_EVALUATE` | Evaluates category, amount, currency and quote count against active rules and records an audit/outbox event. |

Supported rules are maximum purchase amount, approval threshold, allowed currencies and minimum quote count. Amount rules specify an ISO currency; if it differs from the proposed purchase currency, evaluation requires manual review because no exchange-rate conversion is performed. If no active rule applies—including when all rules are scoped to other categories—the result is `APPROVAL_REQUIRED`. Other outcomes are `POLICY_PASSED` and `BLOCKED`. These are policy results only: `POLICY_PASSED` is not an approval, purchase order, payment authorization or permission to buy. The approval and purchase-order workflows are described below.

Flyway V11 adds category scopes and currencies to existing policy rules. Previously configured amount/approval thresholds are disabled and marked `XXX` pending administrator reconfiguration with an explicit currency; they are not silently assumed to use any currency.

## Human approvals

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/approvals` | `APPROVAL_READ` | Lists approval requests in the active organization. |
| `GET` | `/api/v1/approvals/{id}` | `APPROVAL_READ` | Returns a tenant-scoped request, policy snapshot and decision step. |
| `POST` | `/api/v1/approvals` | `APPROVAL_REQUEST` | Re-evaluates an accepted quotation against active policies and creates a request only when human approval is required. Amount, currency, category and quote count must match the tenant-scoped RFQ/quotation. |
| `POST` | `/api/v1/approvals/{id}/decision` | `APPROVAL_DECIDE` | Allows only the assigned, active organization approver to approve or reject a pending request. Rejection requires a reason. |

The request body contains the accepted quotation ID, purchase subject, matching category/amount/currency/quote count, justification and an approver user ID. The requester and approver are derived/validated against organization membership; a requester cannot approve their own request. A blocked purchase cannot be escalated through this API, and a purchase that passes policy does not create an unnecessary approval. The approval binds to the RFQ and quotation and stores the policy evaluation ID and decision snapshot so subsequent policy edits do not rewrite the approval record. Approve/reject and request creation write audit and `procurax.approval.v1` outbox events transactionally. Approval is not a purchase order or payment authorization; a single assigned approver is supported in this phase.

## Purchase orders

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/purchase-orders?page=0&size=50` | `PO_READ` | Lists tenant-scoped purchase orders; page size is capped at 100. |
| `GET` | `/api/v1/purchase-orders/{id}` | `PO_READ` | Returns a tenant-scoped order and its item snapshots. |
| `POST` | `/api/v1/purchase-orders` | `PO_CREATE` | Issues one PO from an accepted quotation and its linked, approved request. |

The create body supplies the approval request ID and quotation ID. Both must belong to the active organization; the quotation must remain accepted, its RFQ must be awardable, and the approved request must be for that exact RFQ/quotation and amount/currency. The service copies vendor and quoted RFQ item details into an immutable order snapshot, verifies item quantities and the summed total, and prevents duplicate issuance per approval or quotation. PO creation and `PURCHASE_ORDER_ISSUED` audit/outbox event are atomic. `ISSUED` is the platform order state; external vendor notification, cancellation, fulfillment, and payment are separate integrations/phases.

## Payment sandbox

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/payment-mandates` | `PAYMENT_READ` | Lists organization mandates. |
| `POST` | `/api/v1/payment-mandates` | `PAYMENT_MANDATE_MANAGE` | Creates an active mandate for an issued PO, with expiry supplied by the caller; amount and currency are copied from the PO. |
| `POST` | `/api/v1/payment-mandates/{id}/revoke` | `PAYMENT_MANDATE_MANAGE` | Revokes an active mandate before payment starts. |
| `POST` | `/api/v1/payments/authorize` | `PAYMENT_AUTHORIZE` | Creates one sandbox authorization for an issued PO and its active mandate. |
| `POST` | `/api/v1/payments/{id}/capture` | `PAYMENT_CAPTURE` | Captures the authorized full amount in the sandbox. |
| `POST` | `/api/v1/payments/{id}/refund` | `PAYMENT_REFUND` | Refunds the captured full amount in the sandbox. |
| `GET` | `/api/v1/payments` or `/api/v1/payments/{id}` | `PAYMENT_READ` | Lists or reads tenant-scoped payment state and receipts. |

Authorization, capture and refund require a unique `Idempotency-Key` header (16–100 permitted ASCII characters). Retries with the same key and operation return the prior result; reuse for a different resource or completed operation conflicts. Only one payment intent can be created per PO. The service derives amount/currency from the PO, enforces the mandate scope and expiry, and writes a separate sandbox receipt plus audit/outbox event for each state transition. The sandbox processor creates internal references only; there is no external payment provider, real money movement, card/account input, PAN/CVV storage, or partial-refund API. Mandates cannot be revoked after authorization begins.

## Fulfillment and reconciliation

| Method | Path | Required permission | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/shipments?page=0&size=50` or `/api/v1/shipments/{id}` | `SHIPMENT_READ` | Lists or returns tenant-scoped shipment records and their lines. |
| `POST` | `/api/v1/shipments` | `SHIPMENT_MANAGE` | Records a PO-bound shipment with tracking details and line quantities. |
| `POST` | `/api/v1/shipments/{id}/status` | `SHIPMENT_MANAGE` | Moves an in-transit shipment to `DELIVERED` or records an `EXCEPTION` with a reason. |
| `GET` | `/api/v1/invoices?page=0&size=50` or `/api/v1/invoices/{id}` | `INVOICE_READ` | Lists or returns tenant-scoped invoice metadata. |
| `POST` | `/api/v1/invoices` | `INVOICE_MANAGE` | Records one invoice against an issued PO. |
| `POST` | `/api/v1/reconciliations` | `RECONCILIATION_EXECUTE` | Runs a comparison for an invoice and returns `MATCHED` or `EXCEPTION` with findings. |
| `GET` | `/api/v1/reconciliations?page=0&size=50` or `/api/v1/reconciliations/{id}` | `RECONCILIATION_READ` | Lists or returns immutable reconciliation runs. |

Shipment lines reference PO lines and enforce cumulative shipped quantities not exceeding the ordered quantities. Reconciliation requires every PO quantity to be covered by delivered shipment lines, an invoice matching the PO amount/currency, and a `CAPTURED` sandbox payment with an exact capture receipt. A refunded payment, incomplete delivery or mismatch produces an exception finding rather than silently passing. Each run is an immutable point-in-time assessment; rerunning after state changes creates a separate run. Invoice recording currently accepts metadata only (no file upload or vendor portal), and payment reconciliation checks the platform sandbox ledger, not external bank settlement. Carrier integrations, vendor callbacks, partial invoicing, bank feeds and automated exception resolution remain future work.

## Tenant controls

- Each repository query for these endpoints includes the principal's active `organizationId`.
- Vendor, RFQ, quotation, item and score IDs are never sufficient authorization by themselves.
- A vendor cannot view another vendor's quote in the same organization.
- Vendor membership assignment requires `USER_MANAGE` and an active organization membership.
- Flyway V6 adds composite tenant-consistency foreign keys, preventing cross-organization vendor/RFQ/quotation/document/contract links at the database level.

Responses use DTOs, never JPA entities. Errors use the common API error format with correlation IDs. Vendor, vendor-document, RFQ, quotation, policy, approval, purchase-order, payment, shipment, invoice and reconciliation changes write corresponding Kafka outbox and audit rows in their PostgreSQL transaction; delivery, retries, DLT, and consumer idempotency are described in [OUTBOX.md](OUTBOX.md).

## Migration boundaries

Spring now stores and verifies tenant-scoped vendor-document metadata, accepts existing HTTPS Cloudinary resource references, and provides a Cloudinary-backed multipart endpoint for vendor documents. Tenant-scoped contract drafts, review/decision APIs, authenticated Cloudinary document upload, permission-checked short-lived download links, and persisted cited evidence retrieval are available. PDF/DOCX text extraction, vector indexing, and broader contract intelligence remain. Legacy route retirement, the legacy ONNX inference model, notification emails, and portions of the React API client still need replacement/integration work.
