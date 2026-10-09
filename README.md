# ProcuraX

ProcuraX is a personal project evolving into an agentic procurement and commerce platform. The migration is in progress: the default React application now opens the Spring-session workspace, while legacy Express/MongoDB source is retained temporarily for rollback and domain-by-domain retirement.

This is an independent project. It does not use or represent any company’s proprietary APIs, credentials, internal systems, branding, or confidential data.

## Current architecture

```text
React + TypeScript + Vite
          |
     Vert.x gateway
          |
 Spring Boot modular monolith ---- FastAPI AI service
          |                              |
 PostgreSQL / Flyway                ephemeral evidence retrieval
 Redis sessions
 Kafka transactional outbox
```

The Spring service owns authentication, organization membership, permissions, procurement records, policy decisions, approvals, sandbox payments, fulfillment, reconciliation, and audit events. Tenant scope and business permissions are enforced on the backend. The AI service is an advisory layer; it does not write to the business database or approve transactions.

## Available V2 workflows

- Google OIDC session entry, organization switching, server-enforced RBAC, and tenant-scoped API access.
- RFQ drafts and multi-line editing, publishing, quotation submission/review, deterministic vendor scoring, approvals, and purchase orders.
- Vendor management and document verification; contract drafts, human review findings, maker-checker decisions, authenticated document upload, short-lived signed download links, and cited AI evidence reviews.
- Sandbox payment mandates and authorization/capture/refund; shipment, invoice metadata, and immutable reconciliation findings.
- Transactional outbox foundations and organization-scoped SSE updates.

Payments are sandbox-only. Contract AI can retrieve cited lexical evidence, extract text from PDF/DOCX uploads, create tenant-scoped pgvector indexes with local Ollama `qwen3-embedding:0.6b`, semantically search the latest indexed draft, and summarize cited evidence with `qwen3:8b`. Spring stores bounded chunk text/vectors and review/analysis snapshots and keeps human review mandatory; this is not legal advice. Kafka business consumers, production gateway controls, frontend retirement gaps, and production operations remain in progress. See [docs/MIGRATION_PLAN.md](docs/MIGRATION_PLAN.md) and the feature-specific documents in `docs/`.

## Run the V2 workspace locally

Prerequisites: Docker Desktop with Compose and Node.js 20 or later.

1. Copy `.env.example` to `.env` and configure values required for the features you intend to use.
2. Start PostgreSQL, Redis, Kafka, the Spring service, gateway, and AI service:

   ```powershell
   docker compose up -d --build
   ```

3. For Google sign-in, configure `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `SPRING_PROFILES_ACTIVE=oauth` in `.env`. Register `http://localhost:5173/login/oauth2/code/google` as the local OAuth callback. For contract AI reviews, set the same `PROCURAX_AI_SERVICE_TOKEN` (at least 32 characters) for Spring and FastAPI. Install Ollama on the host and pull `qwen3:8b`; Compose connects through `host.docker.internal`. Cloudinary credentials are needed to store contract attachments; temporary AI review uploads are not stored.
4. Start the frontend:

   ```powershell
   Set-Location Frontend
   Copy-Item .env.example .env.local
   npm ci
   npm run dev
   ```

   Open `http://localhost:5173`. Vite proxies API and OIDC requests through the gateway at `http://localhost:8088`. Compose does not publish Spring on a host port.

Kafka UI is available at `http://localhost:8081`; the AI health endpoint is `http://localhost:8001/health/ready`. See [docs/LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md) for configuration and troubleshooting. Local compose defaults are for development, not production deployment.

## Validation commands

```powershell
# Frontend
Set-Location Frontend
npm run typecheck
npm run build

# FastAPI
Set-Location ..\ai-platform\fastapi
python -m pytest

# Gateway (requires Java 21 and Maven)
Set-Location ..\..\gateway\vertx-gateway
mvn test
```

The Spring module targets Java 21 and uses Testcontainers for PostgreSQL integration tests; run `mvn test` in `services/procurax-platform` with Java 21 and Docker available.

## Project layout

```text
Frontend/                      React/Vite application and TypeScript workspace
backend/                        Legacy Express/MongoDB API (migration/rollback only)
services/procurax-platform/     Spring Boot modular backend and Flyway migrations
gateway/vertx-gateway/          Vert.x HTTP gateway
ai-platform/fastapi/            Authenticated advisory AI service
docs/                           Migration, security, API, and operations documentation
```

The legacy API is not the V2 authentication model. Do not use legacy JWT/local-storage authentication as authorization for the new workspace.

## Maintainer

**Shivam Jha** · [GitHub](https://github.com/shivammiyyy)
