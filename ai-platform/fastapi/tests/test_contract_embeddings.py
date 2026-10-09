import json
from uuid import UUID

import httpx
import pytest
from fastapi import HTTPException

from app.config import Settings
from app.contract_embeddings import EmbeddingRequest, embed_contract

CONTRACT_ID = UUID("55555555-5555-4555-8555-555555555555")
ORGANIZATION_ID = UUID("11111111-1111-4111-8111-111111111111")
ACTOR_ID = UUID("22222222-2222-4222-8222-222222222222")
DIMENSIONS = 1024


def test_embeddings_are_batched_and_return_exact_source_offsets() -> None:
    text = "Payment is due in 30 days. " * 100
    seen: list[dict] = []

    def handler(request: httpx.Request) -> httpx.Response:
        body = json.loads(request.content)
        seen.append(body)
        return httpx.Response(200, json={
            "embeddings": [[0.01] * DIMENSIONS for _ in body["input"]],
        })

    result = embed_contract(
        CONTRACT_ID,
        EmbeddingRequest(contract_text=text),
        ORGANIZATION_ID,
        ACTOR_ID,
        Settings(),
        transport=httpx.MockTransport(handler),
    )

    assert seen[0]["model"] == "qwen3-embedding:0.6b"
    assert seen[0]["truncate"] is False
    assert result.dimensions == DIMENSIONS
    assert result.contract_id == CONTRACT_ID
    assert result.organization_id == ORGANIZATION_ID
    assert len(result.chunks) == len(seen[0]["input"])
    for chunk in result.chunks:
        assert text[chunk.start_character:chunk.end_character] == chunk.text
        assert len(chunk.embedding) == DIMENSIONS


@pytest.mark.parametrize(
    "embeddings",
    [
        [],
        [[0.01] * (DIMENSIONS - 1)],
        [[float("nan")] * DIMENSIONS],
    ],
)
def test_invalid_embedding_output_is_rejected(embeddings: list[list[float]]) -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(200, json={"embeddings": embeddings}))
    with pytest.raises(HTTPException) as error:
        embed_contract(
            CONTRACT_ID,
            EmbeddingRequest(contract_text="Short agreement."),
            ORGANIZATION_ID,
            ACTOR_ID,
            Settings(),
            transport=transport,
        )
    assert error.value.status_code == 502


def test_embedding_model_failure_is_a_bad_gateway() -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(503))
    with pytest.raises(HTTPException) as error:
        embed_contract(
            CONTRACT_ID,
            EmbeddingRequest(contract_text="Short agreement."),
            ORGANIZATION_ID,
            ACTOR_ID,
            Settings(),
            transport=transport,
        )
    assert error.value.status_code == 502
