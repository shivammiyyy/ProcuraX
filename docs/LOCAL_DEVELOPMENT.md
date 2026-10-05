# Local Development

## Current migration setup

The existing React/Express/MongoDB application remains in `Frontend/` and `Backend/`. The new Java 21/Spring Boot application is being introduced separately in `services/procurax-platform/`; it does not replace the existing `/api/v0` endpoints yet.

Copy `.env.example` to `.env` if you want to override the local PostgreSQL password, then start the new backend foundation and its dependencies:

```powershell
docker compose up -d --build
```

The Vert.x gateway listens on `http://localhost:8088`; use it as the entry point for the new `/api/v1` Spring APIs. Its health endpoint is `http://localhost:8088/health`. Spring Boot remains directly available on `http://localhost:8080` during migration, with health at `/actuator/health` and OpenAPI UI at `/swagger-ui`. Kafka UI is available at `http://localhost:8081`.

The app can start without external OAuth credentials for local infrastructure development. Google login is enabled by activating the `oauth` Spring profile after adding `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to `.env`:

```dotenv
SPRING_PROFILES_ACTIVE=oauth
GOOGLE_CLIENT_ID=your-google-oauth-client-id
GOOGLE_CLIENT_SECRET=your-google-oauth-client-secret
```

The V2 TypeScript workspace is available at `http://localhost:5173/workspace`. Start it from `Frontend/` with `npm install` and `npm run dev`; Vite proxies `/api`, OAuth authorization/callback, and logout requests to Spring at port 8080 by default. Copy `Frontend/.env.example` to `Frontend/.env.local` to override `VITE_DEV_API_PROXY`. Register the local OIDC callback URL `http://localhost:5173/login/oauth2/code/google` with the Google client. For a deployed frontend, set `VITE_API_URL` to the same-origin public API/gateway origin and configure the corresponding public OIDC redirect URI.

When the profile is not active, business API endpoints remain protected and return 401; OAuth login is not registered.

Spring vendor-document multipart uploads require Cloudinary credentials in `.env`:

```dotenv
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

Without these values, the backend still starts but returns `503 DOCUMENT_STORAGE_UNAVAILABLE` for multipart upload requests.

The Compose stack provides PostgreSQL, Redis, Kafka, Kafka UI, the Spring Boot backend, the Vert.x gateway, and the FastAPI AI-platform foundation. The AI service health endpoint is available at `http://localhost:8001/health/ready` (override with `AI_PLATFORM_PORT`). It is loopback-only and currently has no agent or procurement APIs. The gateway forwards cookies and CSRF headers unchanged, replaces client-supplied forwarding/correlation headers, and streams request and response bodies. It does not replace Spring authentication or authorization. The legacy frontend/backend and their MongoDB dependency are not yet part of this compose file; they remain runnable using their existing setup instructions in the root README.

To stop the services while retaining the local PostgreSQL volume:

```powershell
docker compose down
```
