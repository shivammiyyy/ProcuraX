# ProcuraX frontend migration

The frontend is being migrated incrementally. The existing JSX routes remain during the transition; `/` now opens the new `/workspace` experience, while the remaining legacy route paths are retained until their Spring-backed replacements are ready.

## Current V2 workspace slice

- TypeScript workspace shell with responsive navigation, permission-based navigation visibility, active organization switching, session-backed sign-in/out, and TanStack Query data caching.
- Spring OIDC session integration; no frontend role value or local-storage JWT authorizes V2 requests.
- Authenticated CSRF bootstrap loads Spring's readable `XSRF-TOKEN` cookie before unsafe API requests and sends it back as `X-XSRF-TOKEN`.
- Tenant-scoped dashboards and list views for RFQs, quotations, approvals and purchase orders. Spring continues to enforce permissions and organization isolation.
- RFQ draft creation, publication, details, and vendor quotation submissions using `/api/v1/rfqs` and `/api/v1/quotations`. Quotation totals are calculated server-side; compliance declarations are explicitly identified as supplier self-attestations.
- RFQ drafts support adding/removing and editing up to 100 line items, with draft-only updates sent to Spring; published requests remain read-only.
- Quotation details, vendor score explanations, and permission-gated quotation review/accept/reject; approval decisions with required rejection comments.
- Vendor directory creation/editing, active organization member assignment, and compliance-document upload/verification controls, permission-gated by server-derived authorities.
- Purchase-order list and immutable issued-order detail views with RFQ, quotation, and approval provenance.
- Tenant-scoped contract list/create/detail views, authenticated PDF/DOCX upload and five-minute signed download links, attributed human review findings, persisted cited AI evidence retrieval, and maker-checker approval/rejection decisions; drafts are based on accepted quotations. No public Cloudinary URL is returned or rendered.
- Sandbox payment mandate, authorization, capture and refund views. Browser input does not include payment credentials; idempotency keys are generated per operation.
- Payment detail, sandbox lifecycle timestamps and receipts; shipment/invoice detail and reconciliation history; reconciliation run details with linked PO/invoice references.
- Shipment and invoice metadata entry, delivery status transitions, and reconciliation findings using the fulfillment APIs.
- Authenticated `/api/v1/events` SSE updates invalidate the active organization's cached procurement views. A reconnect is followed by refetching current records.
- Procurement activity status charting with Recharts.

Contract document uploads use authenticated Cloudinary delivery and are validated server-side. Downloads require tenant-authorized link generation; signed links are short-lived and opened directly for the file transfer. Contract intelligence remains available through its existing backend path, not this workspace. Legacy paths remain during cutover and must not be treated as the target authentication model.

## Local setup

The Vite development server proxies `/api`, `/oauth2`, `/login/oauth2`, and `/logout` to Spring on `http://localhost:8080`, preserving the browser host for the local OIDC callback.

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev
```

Use `VITE_DEV_API_PROXY` in `.env.local` to change the Spring target. Set `VITE_API_URL` to the public API origin for a deployed build; leave it empty for same-origin routing. Production OIDC callback URLs must use the public host, and the reverse proxy/Spring forwarded-header configuration must be explicitly trusted before the gateway is used to build OAuth redirect URIs. The Spring OAuth profile must be active and configured with the Google client credentials, and the local provider callback must allow `http://localhost:5173/login/oauth2/code/google`.
