import asyncio
from collections.abc import Coroutine
from typing import Any, TypeVar
from uuid import UUID

import httpx
import pytest
from pydantic import ValidationError

from app import security
from app.config import Settings
from app.main import app
from app.vendor_evaluation import (
    EvaluationBatch,
    explain_evaluations,
)


ORGANIZATION_ID = UUID("11111111-1111-4111-8111-111111111111")
ACTOR_ID = UUID("22222222-2222-4222-8222-222222222222")
QUOTE_ID = UUID("33333333-3333-4333-8333-333333333333")
VENDOR_ID = UUID("44444444-4444-4444-8444-444444444444")
SERVICE_TOKEN = "test-only-backend-service-token-value-32"
T = TypeVar("T")


def evaluation_payload() -> dict[str, object]:
    return {
        "quotation_id": str(QUOTE_ID),
        "vendor_id": str(VENDOR_ID),
        "official_score": 82.5,
        "factors": {
            "price": 90,
            "delivery": 80,
            "quality": 95,
            "compliance": 70,
            "performance": 75,
            "paymentTerms": 60,
        },
        "weights": {
            "price": 0.30,
            "delivery": 0.20,
            "quality": 0.20,
            "compliance": 0.15,
            "performance": 0.10,
            "paymentTerms": 0.05,
        },
        "legacy_features": {
            "price_difference": 1250,
            "delivery_days": 20,
            "compliance_score": 80,
        },
    }


def run_async(coroutine: Coroutine[Any, Any, T]) -> T:
    return asyncio.run(coroutine)


def test_legacy_model_adds_advisory_explanation_without_changing_official_score() -> None:
    batch = EvaluationBatch.model_validate({"evaluations": [evaluation_payload()]})
    response = explain_evaluations(batch, ORGANIZATION_ID, ACTOR_ID)
    explanation = response.explanations[0]

    assert response.organization_id == ORGANIZATION_ID
    assert response.requested_by_user_id == ACTOR_ID
    assert response.requires_human_review is True
    assert response.official_scores_unchanged is True
    assert explanation.quotation_id == QUOTE_ID
    assert explanation.vendor_id == VENDOR_ID
    assert explanation.official_score == 82.5
    assert 0 <= explanation.legacy_model_score <= 100
    assert explanation.model_version == "legacy-onnx-v1"
    assert explanation.advisory_only is True
    assert explanation.strengths == ["price", "quality", "delivery", "performance"]
    assert explanation.review_areas == []
    assert explanation.factor_contributions[0].factor == "price"
    assert "does not change the official score" in explanation.summary


def test_evaluation_endpoint_requires_backend_token_and_returns_scoped_explanation(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(
        security,
        "get_settings",
        lambda: Settings(service_token=SERVICE_TOKEN),
    )

    async def send_requests() -> None:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            path = "/internal/v1/agents/vendor-evaluation/explanations"
            body = {"evaluations": [evaluation_payload()]}
            response = await client.post(path, json=body)
            assert response.status_code == 401

            response = await client.post(
                path,
                headers={
                    "Authorization": f"Bearer {SERVICE_TOKEN}",
                    "X-ProcuraX-Organization-Id": str(ORGANIZATION_ID),
                    "X-ProcuraX-Actor-Id": str(ACTOR_ID),
                },
                json=body,
            )

        assert response.status_code == 200
        data = response.json()
        assert data["organization_id"] == str(ORGANIZATION_ID)
        assert data["requested_by_user_id"] == str(ACTOR_ID)
        assert data["official_scores_unchanged"] is True
        assert data["explanations"][0]["official_score"] == 82.5

    run_async(send_requests())


def test_evaluation_rejects_invalid_weights_and_duplicate_quotes() -> None:
    invalid_weight = evaluation_payload()
    invalid_weight["weights"] = {
        "price": 0.45,
        "delivery": 0.2,
        "quality": 0.2,
        "compliance": 0.1,
        "performance": 0.05,
        "paymentTerms": 0.05,
    }
    with pytest.raises(ValidationError):
        EvaluationBatch.model_validate({"evaluations": [invalid_weight]})

    repeated = evaluation_payload()
    with pytest.raises(ValidationError):
        EvaluationBatch.model_validate({"evaluations": [repeated, repeated]})
