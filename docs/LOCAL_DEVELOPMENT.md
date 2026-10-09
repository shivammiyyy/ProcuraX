# Local Development

## Current migration setup

The legacy Express/MongoDB backend has been retired and removed. The Java 21/Spring Boot application in `services/procurax-platform/` is the only business API.

Copy `.env.example` to `.env` if you want to override the local PostgreSQL password, then start the new backend foundation and its dependencies:

```powershell
docker compose up -d --build
```

The Vert.x gateway listens on `http://localhost:8088`; use it as the entry point for `/api/v1`, OAuth, and OpenAPI requests. Its health endpoint is `http://localhost:8088/health`. Spring Boot is reachable only by other Compose services and is not published on a host port; this makes the gateway the local edge entry point. Kafka UI is available at `http://localhost:8081`.

The app can start without external OAuth credentials for local infrastructure development. Google login is enabled by activating the `oauth` Spring profile after adding `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to `.env`:

```dotenv
SPRING_PROFILES_ACTIVE=oauth
GOOGLE_CLIENT_ID=your-google-oauth-client-id
GOOGLE_CLIENT_SECRET=your-google-oauth-client-secret
```

The V2 TypeScript workspace is available at `http://localhost:5173/workspace`. Start it from `Frontend/` with `npm install` and `npm run dev`; Vite proxies `/api`, OAuth authorization/callback, and logout requests through the gateway on port 8088 by default. Copy `Frontend/.env.example` to `Frontend/.env.local` to override `VITE_DEV_API_PROXY`. Register the local OIDC callback URL `http://localhost:5173/login/oauth2/code/google` with the Google client. For a deployed frontend, set `VITE_API_URL` to the same-origin public API/gateway origin and configure the corresponding public OIDC redirect URI.

When the profile is not active, business API endpoints remain protected and return 401; OAuth login is not registered.

Spring vendor-document multipart uploads require Cloudinary credentials in `.env`:

```dotenv
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

Without these values, the backend still starts but returns `503 DOCUMENT_STORAGE_UNAVAILABLE` for multipart upload requests.

The Compose stack provides PostgreSQL, Redis, Kafka, Kafka UI, the Spring Boot backend, the Vert.x gateway, and the FastAPI AI-platform foundation. The AI service health endpoint is available at `http://localhost:8001/health/ready` (override with `AI_PLATFORM_PORT`). It is loopback-only. The gateway forwards cookies and CSRF headers unchanged, replaces client-supplied forwarding/correlation headers, and streams request and response bodies. It does not replace Spring authentication or authorization. The legacy frontend/backend and their MongoDB dependency are not part of this compose file; they remain available for rollback using their existing setup instructions in the root README.

To stop the services while retaining the local PostgreSQL volume:

```powershell
docker compose down
```

## Kafka integration test

With Docker running, from `services/procurax-platform`: `mvn -Dtest=OutboxKafkaIntegrationTest test`. When Maven itself runs inside a container, mount `/var/run/docker.sock` and set `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`.

## Dead-letter replay

Replay is an authenticated Spring operation, not direct Kafka console publishing. Only an active principal with `OUTBOX_REPLAY` (seeded for `PLATFORM_ADMIN` only) can request replay, and the event must belong to that principal's active organization and be in `DEAD_LETTERED` state. Require a 10–500 character reason. Each event can be replayed at most three times. Each request and reason is added to the audit trail. The original event ID is retained so idempotent consumers can ignore duplicates. Endpoint: `POST /api/v1/operations/outbox/{eventId}/replay` with JSON `{"reason":"..."}`. Replay returns the event to `PENDING`; normal outbox dispatch publishes it to its original topic.
