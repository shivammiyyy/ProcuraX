# ProcuraX V2 Migration Plan

## Status

- **DONE:** Phase 1 repository analysis and migration planning.
- **IN PROGRESS:** Phase 2 backend foundation. Spring Boot skeleton (`services/procurax-platform`) and `docker-compose.yml` (postgres, redis, kafka, kafka-ui, backend) are done and verified healthy. Remaining for Phase 2: Flyway core schema (V1), UUID/audit base entity, error format, correlation-ID filter.
- **TODO:** Phases 2–13, delivered incrementally while keeping the existing application runnable.
- **BLOCKERS:** Local JDK is 17, so the Java 21 module builds via Docker (`docker compose build backend`). Default host ports 5432/8080 may be taken locally; override via `.env` (see `.env.example`). Otherwise none for planning. OAuth/OIDC client credentials and AI-provider credentials will be needed for locally exercising those integrations; they must be supplied through environment variables.

## Backend technology decision

The requested alternatives are not one interchangeable stack: Spring Boot and Vert.x are Java frameworks, not Python frameworks. The selected target is **Java 21 with Spring Boot 3.x** because it directly supports the requested Spring Security, JPA, Flyway, and Spring Kafka architecture. Vert.x is not required for the initial modular backend and would add a second programming model without a demonstrated need. The existing Node.js backend remains available during migration; no existing service is replaced until its successor is implemented and verified.

The initial target is a **modular Spring Boot application with clear domain boundaries**, rather than a premature collection of independently deployed services. The proposed modules/packages are procurement, vendor, contract, agent, payment, notification, and audit. They can be split into deployable services later if operational needs justify it.

## Repository findings

### Current application structure

- `Frontend/` is a React 19 + Vite JavaScript application. React Router is used for navigation, Tailwind CSS is enabled, Axios is the API client, and Lucide icons are installed. TypeScript configuration files exist, but the application routes, pages, components, and API modules currently use `.jsx`/`.js`. TanStack Query, Zustand, and Recharts are not currently dependencies.
- `Backend/` is an ES-module Node.js + Express 5 application using Mongoose and MongoDB. It is the only backend implementation present; there is no Java or Python backend, SQL schema, Kafka setup, or Docker Compose configuration.
- The repository contains existing UI screenshots under `img/`. No `docs/` directory or automated backend test suite is present.

### Frontend routes

Routes are declared in `Frontend/src/App.jsx`:

| Route | Existing behavior |
|---|---|
| `/login`, `/signup` | Login and signup pages |
| `/`, `/dashboard` | Buyer/vendor dashboard |
| `/rfqs`, `/rfqs/create`, `/rfqs/:id` | RFQ list, creation, and details |
| `/quotations`, `/quotations/create/:rfqId`, `/quotations/:id` | Quotation list, submission, and details |
| `/contracts`, `/contracts/create/:quotationId`, `/contracts/:id` | Contract list, creation, and details |

Most application routes are wrapped with `ProtectedRoute`. That component checks only whether a token is present; its callers' role prop is not enforced there. The frontend should remain a presentation layer and must not be treated as an authorization boundary.

### Backend routes

`Backend/index.js` mounts the following API groups under `/api/v0`:

| Route group | Operations |
|---|---|
| `/auth` | Register email, verify email/signup, login, get current user |
| `/rfq` | Create/list/get/update/delete RFQs; list quotations for an RFQ |
| `/quotation` | Create/list/get/update/delete quotations |
| `/contract` | Create/list/get/update contracts |

There are no current vendor, purchase-order, approval, payment, order, agent, audit, or SSE API groups. The server defaults to port `3000`, while the frontend Axios client defaults to `http://localhost:4000/api/v0`; local configuration should explicitly resolve this mismatch.

### Models and workflows

- **Users/authentication:** `Backend/models/userModel.js` stores email, password hash, company name, a single buyer/vendor/admin role, verification state, and OTP. `authController.js` implements email OTP signup plus password login and issues a seven-day JWT. `authMiddleware.js` verifies the bearer JWT and loads the user. No organization membership, permission model, OIDC login, or tenant context exists.
- **RFQs:** `rfqModel.js` stores request title, description, RFQ/RFP type, budget, deadline, category, status, buyer reference, and attachment metadata. `rfqController.js` supports creation, role-dependent listing, retrieval, editing, deletion, and quotation lookup. Creating an RFQ emails all registered vendors.
- **Quotations:** `quotationModel.js` stores RFQ/vendor references, price, delivery time, compliance fields, ONNX score, attachments, and status. Vendors submit and edit their own open-RFQ quotes; buyers can update quote status and accept/reject quotes. The controller calculates a compliance feature and calls the existing ONNX inference helper.
- **Vendor evaluation:** `Backend/utils/complianceScore.js` loads `ml_model/vendor_model.onnx` and predicts from three features. It returns `null` on inference failure. The current workflow does not provide configurable scoring weights or independently persisted score explanations.
- **Contracts:** `contractModel.js` stores buyer/vendor/RFQ/quotation references, contract text/file, dates, status, and Gemini audit report/warnings. `contractController.js` creates and audits contracts, lists/retrieves them, and updates status/dates. Contract creation also emails the buyer and vendor.
- **Files:** `Backend/middleware/upload..js` uses Multer with Cloudinary storage for RFQ, quotation, and contract documents. Existing upload fields and model fields differ (`attachments` vs `attachment`), so migration must preserve and normalize stored document references rather than silently dropping them.
- **AI:** `Backend/utils/geminiService.js` sends contract text to Gemini and parses a JSON audit response. It reports failure when the API key is missing or the response cannot be parsed. This is contract-audit integration, not an agent tool execution framework; no agent sessions, tool registry, policy gate, RAG, or agent action audit exists.
- **Notifications:** Notifications currently mean SMTP email side effects from auth/RFQ/quotation/contract controllers. There is no notification model/API, durable event delivery, Kafka, outbox, or live event stream.

### Migration-sensitive issues observed

These are recorded to guide replacement and regression coverage, not as a blanket rewrite of the current application:

- A contract detail authorization condition appears inverted: a buyer/vendor matching the contract party is rejected, while a non-matching party is not rejected by that condition. Replace this with explicit, tested ownership/tenant checks during contract service migration.
- Signup accepts the business role from the request body, and RFQ creation broadcasts to every vendor. The target must derive permissions and organization access on the server and restrict vendor discovery/publication to policy.
- RFQ attachment metadata is read from `attachments` by the controller, while the model declares `attachment`.
- Quotation compliance is parsed as JSON in the controller despite model/UI data having different shapes; establish a validated DTO before preserving this workflow in the new API.
- The current ONNX evaluation inputs and model behavior need to be documented and regression-tested before changing scoring. ML/AI explanations must not replace deterministic scoring.
- Contract AI output needs an explicit notice that it is not legal advice; preserve the existing analysis capability and record structured findings.
- Express uses wildcard CORS by default, and the repository has no organization-level isolation or permission-based backend authorization.

## Migration principles

1. Keep `Backend/` and all current screens operational until their replacement paths are demonstrably usable.
2. Add the target Spring Boot application alongside the legacy backend first; migrate one vertical slice at a time and document temporary ownership of each API.
3. Preserve RFQ, quotation, ONNX evaluation, contract analysis, and document references. Add compatibility/data migration only when the replacement slice is ready.
4. Establish organization ownership and server-side authorization before exposing new business APIs.
5. Use PostgreSQL, UUID identifiers, Flyway, and optimistic locking for the new transactional model. Treat imported legacy records as requiring an explicit organization mapping.
6. Add Kafka through domain events and a transactional outbox, not direct database-save-then-publish calls. Keep event handlers idempotent.
7. Keep payment operations sandbox-only, calculate amounts from server-owned records, and enforce idempotency and authorization server-side.
8. Do not claim OAuth, Kafka, agent tools, SSE, or payment functionality until the corresponding implementation and tests exist.

## Phased implementation

| Phase | Scope | Status |
|---|---|---|
| 1. Repository analysis | Inventory routes, models, authentication, workflows, integrations, and risks; establish a migration sequence. | **DONE** |
| 2. Backend foundation | Add a Java 21/Spring Boot 3 modular application, PostgreSQL, Flyway, Redis, validation, OpenAPI, health checks, and local infrastructure. Preserve Node backend. | **IN PROGRESS** |
| 3. Security migration | Add OIDC/Google login, application principal, organization membership/context, permissions, secure sessions/CSRF, CORS, rate limiting, and audit hooks. | TODO |
| 4. Procurement domain | Migrate vendor, RFQ, quotation, deterministic evaluation, purchase-order, and contract capabilities behind DTO-based application services. | TODO |
| 5. Kafka infrastructure | Define versioned event envelopes/topics, development topic setup, transactional outbox publisher, retry/DLQ flow, and consumer idempotency. | TODO |
| 6. Agent service | Add structured procurement intents/plans, specialist agents, allowlisted tool registry, permission/policy gates, and auditable tool execution. | TODO |
| 7. Policy and approvals | Add persisted policy rules, approval requests/steps, deterministic policy decisions, and human approval endpoints/UI. | TODO |
| 8. Payment sandbox | Add checkout, payment intents/mandates, mock provider, authorization/capture/refund/receipt, and idempotency controls. Never store card numbers or CVVs. | TODO |
| 9. Fulfillment | Add orders, shipments, invoices, reconciliation, and correlation-linked audit history. | TODO |
| 10. Frontend redesign | Migrate incrementally to React/TypeScript enterprise shell and add dashboard, AI procurement, RFQs, vendors, approvals, payments, orders, agent activity, audit, and settings. | TODO |
| 11. Real-time updates | Add a controlled organization-scoped SSE gateway backed by consumed events; connect UI subscriptions and lifecycle updates. | TODO |
| 12. Testing | Add unit, API, security/tenant-isolation, Kafka/outbox/idempotency, agent-tool, policy, and payment tests as the corresponding phases land. | TODO |
| 13. Documentation | Maintain architecture, security, Kafka, payment, procurement, database, API, local development, and demo documentation alongside implementations. | TODO |

## First implementation boundaries

The next phase should create the backend foundation without removing existing Node routes. New APIs should use `/api/v1`; the current `/api/v0` endpoints remain in place until corresponding migrations are complete. The first usable vertical slice should establish authenticated organization context and one persisted procurement workflow before adding agent-driven high-risk actions.

The complete demo is an end-to-end acceptance target, not a claim about current functionality. Its steps will be marked complete only as the procurement, event, approval, payment, fulfillment, and audit implementations are delivered and tested.
