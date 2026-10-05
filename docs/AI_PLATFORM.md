# FastAPI AI platform

`ai-platform/fastapi/` hosts isolated AI planning, advisory vendor evaluation, and future contract-intelligence work. Spring Boot remains the business authority: this service does not connect to PostgreSQL or mutate procurement records.

## Procurement planning

`POST /internal/v1/agents/procurement/plans` accepts a validated procurement brief and returns a tenant-attributed draft RFQ proposal, recommended evaluation criteria, and human-review notes. Planning is deterministic in this phase: it does not call an LLM, infer vendors or prices, create or publish the RFQ, access PostgreSQL, or consume Kafka events. The proposal's `proposed_rfq` shape maps to Spring's RFQ creation DTO; a later trusted backend integration must invoke Spring's permission-checked API on the user's behalf.

The endpoint requires a backend-only bearer token configured as `PROCURAX_AI_SERVICE_TOKEN` (at least 32 characters) plus organization and actor UUID headers supplied by the authenticated backend. If no token is configured, the endpoint returns `503`. Never expose the token to browser code or accept organization/actor headers directly from an unauthenticated client. The plan is not persisted by this service, and `requires_human_review` is always true.

The service also provides `/health`, `/health/live`, and `/health/ready`. OpenAPI documentation is disabled. Readiness confirms the service configuration is valid; it does not claim that Kafka or Spring is reachable. Spring API origin and Kafka bootstrap settings are reserved for later authenticated tool integration and event consumers; this phase makes no outbound calls and does not subscribe to topics.

## Vendor evaluation explanations

`POST /internal/v1/agents/vendor-evaluation/explanations` accepts up to 100 tenant-scoped quotation assessments from Spring: the official score, six deterministic factor scores and weights, and the legacy model's three inputs (`price_difference`, `delivery_days`, `compliance_score`). It returns the unchanged official score, the legacy ONNX output, weighted factor contributions, and review signals. Inputs and caller organization/actor context must come from authenticated, permission-checked Spring operations.

`models/vendor_model.onnx` is copied from the existing `Backend/ml_model/vendor_model.onnx` artifact. The service validates its expected single float input with three features and single output; inference batches quotations on CPU. Factor contributions are sorted by weighted points; scores at least 75 are listed as strengths and those below 50 as review areas. These labels are explanation aids, not purchasing policy. The ONNX output is uncalibrated advisory information only; this endpoint never ranks/selects vendors, changes the score calculated by Spring, or persists data. Human review remains required.

## Contract evidence retrieval

`POST /internal/v1/contracts/{contract_id}/review` accepts contract text supplied by an authenticated Spring service (up to 100,000 characters) and returns evidence for five clause topics: termination, liability, payment, confidentiality, and governing law. Text is chunked with overlap and retrieved per clause using BM25-style lexical ranking. Each excerpt includes offsets into the submitted text; the service does not retain or log contract content.

This is an evidence-finding aid, not legal advice: `NO_MATCHING_EVIDENCE` means only that this bounded keyword retrieval found no match, not that a clause is absent. The response explicitly asks for full-document human review. This phase does not yet parse uploaded PDF/DOCX files, persist chunks or embeddings, use pgvector, call an LLM, or decide legal adequacy. Spring must authenticate and authorize document access before sending the contract text and must not expose the service token or forward untrusted tenant/actor headers.

## Local development

The service is built and run with the main Compose stack. It is exposed only on the local host at `http://localhost:8001` by default and is reachable to other Compose services as `http://ai-platform:8000`.

```powershell
docker compose up -d --build ai-platform
curl.exe http://localhost:8001/health/ready
```

Set `AI_PLATFORM_PORT` to override the loopback host port. The container runs as a non-root user and contains no database credentials. Run the focused tests with:

```powershell
docker run --rm -v "${PWD}\ai-platform\fastapi:/app" -w /app python:3.12-slim sh -c "pip install --no-cache-dir -r requirements-dev.txt && pytest -q"
```

Spring-to-agent integration, Kafka event handlers, LLM provider integration, and retrieval are later phases; route user requests through Spring authentication and authorization rather than directly to this service.
