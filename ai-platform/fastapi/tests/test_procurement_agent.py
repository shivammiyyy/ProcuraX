import asyncio
from collections.abc import Coroutine
from datetime import datetime, timedelta, timezone
from typing import Any, TypeVar
from uuid import UUID

import httpx
import pytest
from pydantic import ValidationError

from app import security
from app.config import Settings
from app.main import app
from app.procurement_agent import ProcurementPlanRequest


ORGANIZATION_ID = UUID("11111111-1111-4111-8111-111111111111")
ACTOR_ID = UUID("22222222-2222-4222-8222-222222222222")
SERVICE_TOKEN = "test-only-backend-service-token-value-32"
T = TypeVar("T")


def valid_request() -> dict[str, object]:
    return {
        "summary": "Source replacement air filters for the central facility.",
        "category": "Facilities",
        "budget": "12500.00",
        "currency": "USD",
        "response_deadline": (datetime.now(timezone.utc) + timedelta(days=14)).isoformat(),
        "delivery_days": 30,
        "objective": "quality_first",
        "items": [
            {
                "description": "Industrial air filter",
                "quantity": 12,
                "unit": "each",
                "specification": "MERV 13 compatible",
            }
        ],
    }


def run_async(coroutine: Coroutine[Any, Any, T]) -> T:
    return asyncio.run(coroutine)


def test_procurement_plan_requires_backend_service_authentication(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(
        security,
        "get_settings",
        lambda: Settings(service_token=SERVICE_TOKEN),
    )

    async def check_authentication() -> None:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            path = "/internal/v1/agents/procurement/plans"
            response = await client.post(path, json=valid_request())
            assert response.status_code == 401

            response = await client.post(
                path,
                headers=[
                    (b"authorization", b"Bearer caf\xe9"),
                    (b"x-procurax-organization-id", str(ORGANIZATION_ID).encode()),
                    (b"x-procurax-actor-id", str(ACTOR_ID).encode()),
                ],
                json=valid_request(),
            )
            assert response.status_code == 401

            response = await client.post(
                path,
                headers={"Authorization": f"Bearer {SERVICE_TOKEN}"},
                json=valid_request(),
            )
            assert response.status_code == 422

    run_async(check_authentication())


def test_procurement_plan_returns_tenant_attributed_rfq_proposal(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(
        security,
        "get_settings",
        lambda: Settings(service_token=SERVICE_TOKEN),
    )

    async def create_plan() -> None:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            response = await client.post(
                "/internal/v1/agents/procurement/plans",
                headers={
                    "Authorization": f"Bearer {SERVICE_TOKEN}",
                    "X-ProcuraX-Organization-Id": str(ORGANIZATION_ID),
                    "X-ProcuraX-Actor-Id": str(ACTOR_ID),
                },
                json=valid_request(),
            )

        assert response.status_code == 200
        plan = response.json()
        assert plan["organization_id"] == str(ORGANIZATION_ID)
        assert plan["requested_by_user_id"] == str(ACTOR_ID)
        assert plan["status"] == "READY_FOR_REVIEW"
        assert plan["action"] == "CREATE_RFQ_DRAFT"
        assert plan["requires_human_review"] is True
        assert plan["proposed_rfq"]["requestType"] == "RFP"
        assert plan["proposed_rfq"]["items"][0]["quantity"] == 12
        assert plan["recommended_evaluation_criteria"][0] == "quality"
        assert any("No vendors, prices" in note for note in plan["review_notes"])

    run_async(create_plan())


def test_planner_rejects_naive_or_expired_response_deadlines() -> None:
    payload = valid_request()
    payload["response_deadline"] = (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
    with pytest.raises(ValidationError):
        ProcurementPlanRequest.model_validate(payload)

    payload["response_deadline"] = (datetime.now() + timedelta(days=1)).isoformat()
    with pytest.raises(ValidationError):
        ProcurementPlanRequest.model_validate(payload)


def test_planner_rejects_invalid_currency_and_empty_items() -> None:
    payload = valid_request()
    payload["currency"] = "usd"
    with pytest.raises(ValidationError):
        ProcurementPlanRequest.model_validate(payload)

    payload["currency"] = "USD"
    payload["items"] = []
    with pytest.raises(ValidationError):
        ProcurementPlanRequest.model_validate(payload)


def test_unconfigured_backend_token_disables_planning(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(security, "get_settings", lambda: Settings())

    async def check_disabled_route() -> None:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            response = await client.post(
                "/internal/v1/agents/procurement/plans",
                headers={
                    "Authorization": f"Bearer {SERVICE_TOKEN}",
                    "X-ProcuraX-Organization-Id": str(ORGANIZATION_ID),
                    "X-ProcuraX-Actor-Id": str(ACTOR_ID),
                },
                json=valid_request(),
            )
            assert response.status_code == 503

    run_async(check_disabled_route())
