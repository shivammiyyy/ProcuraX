import asyncio
from collections.abc import Coroutine
from typing import Any, TypeVar
from uuid import UUID

import httpx
import pytest
from pydantic import ValidationError

from app import security
from app.config import Settings
from app.contract_intelligence import (
    MAX_CONTRACT_CHARACTERS,
    ContractReviewRequest,
    review_contract,
)
from app.main import app


ORGANIZATION_ID = UUID("11111111-1111-4111-8111-111111111111")
ACTOR_ID = UUID("22222222-2222-4222-8222-222222222222")
CONTRACT_ID = UUID("55555555-5555-4555-8555-555555555555")
SERVICE_TOKEN = "test-only-backend-service-token-value-32"
T = TypeVar("T")


def run_async(coroutine: Coroutine[Any, Any, T]) -> T:
    return asyncio.run(coroutine)


def test_contract_review_retrieves_relevant_source_text_with_exact_offsets() -> None:
    text = (
        "This agreement covers supply of equipment. "
        "The customer shall pay each invoice within thirty days. "
        "Either party may terminate this agreement after written notice. "
        "The governing law shall be the laws of Example State. "
        "The parties will protect confidential information."
    )
    response = review_contract(
        CONTRACT_ID,
        ContractReviewRequest(contract_text=text),
        ORGANIZATION_ID,
        ACTOR_ID,
    )

    assert response.contract_id == CONTRACT_ID
    assert response.organization_id == ORGANIZATION_ID
    assert response.requested_by_user_id == ACTOR_ID
    assert response.retrieval_method == "ephemeral_bm25"
    assert response.document_persisted is False
    assert response.requires_human_review is True

    clauses = {clause.clause: clause for clause in response.clauses}
    for name in ("termination", "payment", "confidentiality", "governing_law"):
        clause = clauses[name]
        assert clause.evidence_status == "EVIDENCE_FOUND"
        assert clause.citations
        citation = clause.citations[0]
        assert text[citation.start_character:citation.end_character] == citation.excerpt

    assert clauses["liability"].evidence_status == "NO_MATCHING_EVIDENCE"
    assert "not proof" in clauses["liability"].reviewer_note


def test_contract_text_is_required_and_bounded() -> None:
    with pytest.raises(ValidationError):
        ContractReviewRequest(contract_text=" ")
    with pytest.raises(ValidationError):
        ContractReviewRequest(contract_text="x" * (MAX_CONTRACT_CHARACTERS + 1))


def test_long_contract_is_chunked_and_citations_remain_grounded() -> None:
    filler = ("General description of the goods and services. " * 50)
    clause_text = "Payment terms require invoice payment within 45 days."
    text = filler + clause_text + (" Additional general terms." * 60)
    response = review_contract(
        CONTRACT_ID,
        ContractReviewRequest(contract_text=text),
        ORGANIZATION_ID,
        ACTOR_ID,
    )

    payment = next(clause for clause in response.clauses if clause.clause == "payment")
    assert payment.evidence_status == "EVIDENCE_FOUND"
    assert len(payment.citations) <= 3
    assert any(
        "invoice payment within 45 days" in citation.excerpt
        and text[citation.start_character:citation.end_character] == citation.excerpt
        for citation in payment.citations
    )


def test_contract_review_endpoint_requires_backend_authentication(
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
            path = f"/internal/v1/contracts/{CONTRACT_ID}/review"
            body = {"contract_text": "The parties shall pay invoices within thirty days."}
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
        result = response.json()
        assert result["contract_id"] == str(CONTRACT_ID)
        assert result["organization_id"] == str(ORGANIZATION_ID)
        assert result["document_persisted"] is False
        assert len(result["clauses"]) == 5

    run_async(send_requests())
