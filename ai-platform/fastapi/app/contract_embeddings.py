from math import isfinite
from typing import Annotated
from uuid import UUID

import httpx
from fastapi import HTTPException, status
from pydantic import BaseModel, ConfigDict, Field

from app.config import Settings
from app.contract_intelligence import chunk_contract_text

MAX_EMBEDDING_CHUNKS = 100


class EmbeddingRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    contract_text: Annotated[str, Field(min_length=1, max_length=100_000)]


class EmbeddedChunk(BaseModel):
    chunk_id: int
    start_character: int
    end_character: int
    text: str
    embedding: list[float]


class EmbeddingResponse(BaseModel):
    contract_id: UUID
    organization_id: UUID
    requested_by_user_id: UUID
    model: str
    dimensions: int
    chunks: list[EmbeddedChunk]


def _split(text: str) -> list[tuple[int, int, str]]:
    chunks = chunk_contract_text(text)
    if len(chunks) > MAX_EMBEDDING_CHUNKS:
        raise HTTPException(status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                            detail="Contract contains too many indexable chunks")
    return [(chunk.start_character, chunk.end_character, chunk.text) for chunk in chunks]


def embed_contract(
    contract_id: UUID,
    request: EmbeddingRequest,
    organization_id: UUID,
    requested_by_user_id: UUID,
    settings: Settings,
    transport: httpx.BaseTransport | None = None,
) -> EmbeddingResponse:
    source_chunks = _split(request.contract_text)
    try:
        with httpx.Client(
            base_url=str(settings.ollama_base_url),
            timeout=settings.ollama_timeout_seconds,
            transport=transport,
        ) as client:
            response = client.post(
                "/api/embed",
                json={
                    "model": settings.ollama_embedding_model,
                    "input": [chunk[2] for chunk in source_chunks],
                    "truncate": False,
                },
            )
            response.raise_for_status()
            vectors = response.json()["embeddings"]
    except (httpx.HTTPError, KeyError, ValueError) as exception:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="The embedding model could not index the contract",
        ) from exception

    if not isinstance(vectors, list) or len(vectors) != len(source_chunks):
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                            detail="The embedding model returned an invalid chunk count")
    chunks: list[EmbeddedChunk] = []
    for index, ((start, end, text), vector) in enumerate(zip(source_chunks, vectors, strict=True), start=1):
        if (
            not isinstance(vector, list)
            or len(vector) != settings.ollama_embedding_dimensions
            or any(not isinstance(value, (int, float)) or not isfinite(value) for value in vector)
        ):
            raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                                detail="The embedding model returned an invalid vector")
        chunks.append(EmbeddedChunk(
            chunk_id=index,
            start_character=start,
            end_character=end,
            text=text,
            embedding=vector,
        ))
    return EmbeddingResponse(
        contract_id=contract_id,
        organization_id=organization_id,
        requested_by_user_id=requested_by_user_id,
        model=settings.ollama_embedding_model,
        dimensions=settings.ollama_embedding_dimensions,
        chunks=chunks,
    )
