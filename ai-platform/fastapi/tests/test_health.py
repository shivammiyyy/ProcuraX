import asyncio

import httpx

from app.main import app


def test_health_endpoints_report_service_status() -> None:
    async def check_endpoints() -> None:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            for path in ("/health", "/health/live", "/health/ready"):
                response = await client.get(path)
                assert response.status_code == 200
                assert response.json() == {
                    "status": "UP",
                    "service": "procurax-ai-platform",
                }

    asyncio.run(check_endpoints())


def test_only_health_endpoints_are_exposed() -> None:
    async def check_endpoints() -> None:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            assert (await client.get("/openapi.json")).status_code == 404
            assert (await client.get("/")).status_code == 404

    asyncio.run(check_endpoints())
