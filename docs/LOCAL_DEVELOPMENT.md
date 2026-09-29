# Local Development

## Current migration setup

The existing React/Express/MongoDB application remains in `Frontend/` and `Backend/`. The new Java 21/Spring Boot application is being introduced separately in `services/procurax-platform/`; it does not replace the existing `/api/v0` endpoints yet.

Copy `.env.example` to `.env` if you want to override the local PostgreSQL password, then start the new backend foundation and its dependencies:

```powershell
docker compose up -d --build
```

The Spring Boot application listens on `http://localhost:8080`. Its health endpoint is `http://localhost:8080/actuator/health`, and the generated OpenAPI UI is at `http://localhost:8080/swagger-ui`. Kafka UI is available at `http://localhost:8081`.

This first compose setup provides PostgreSQL, Redis, Kafka, Kafka UI, and the new Spring Boot backend. The legacy frontend/backend and their MongoDB dependency are not yet part of this compose file; they remain runnable using their existing setup instructions in the root README.

To stop the services while retaining the local PostgreSQL volume:

```powershell
docker compose down
```
