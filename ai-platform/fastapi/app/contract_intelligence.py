import math
import re
import unicodedata
from collections import Counter
from typing import Annotated
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, StringConstraints


MAX_CONTRACT_CHARACTERS = 100_000
CHUNK_SIZE = 1_200
CHUNK_OVERLAP = 160
MAX_CITATIONS_PER_CLAUSE = 3
TOKEN_PATTERN = re.compile(r"[^\W_]+", re.UNICODE)
STOP_WORDS = frozenset(
    {
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "in", "is", "it", "of", "on", "or", "that", "the", "this", "to",
        "under", "with",
    }
)


class ClauseDefinition(BaseModel):
    clause: str
    question: str
    search_terms: tuple[str, ...]


CLAUSE_DEFINITIONS = (
    ClauseDefinition(
        clause="termination",
        question="Does the agreement define termination rights, notice, or a cure period?",
        search_terms=(
            "termination",
            "terminate",
            "notice",
            "cure period",
            "material breach",
            "expiry",
        ),
    ),
    ClauseDefinition(
        clause="liability",
        question="Does the agreement address liability limits, damages, or indemnification?",
        search_terms=(
            "liability",
            "limitation of liability",
            "indirect damages",
            "consequential damages",
            "indemnification",
            "indemnity",
        ),
    ),
    ClauseDefinition(
        clause="payment",
        question="Does the agreement specify payment timing, invoices, or late payment?",
        search_terms=(
            "payment",
            "invoice",
            "net 30",
            "net 60",
            "late payment",
            "fees",
        ),
    ),
    ClauseDefinition(
        clause="confidentiality",
        question="Does the agreement address confidential information or non-disclosure?",
        search_terms=(
            "confidential",
            "non-disclosure",
            "proprietary information",
            "trade secret",
        ),
    ),
    ClauseDefinition(
        clause="governing_law",
        question="Does the agreement identify governing law, jurisdiction, or venue?",
        search_terms=(
            "governing law",
            "jurisdiction",
            "venue",
            "courts",
            "laws of",
        ),
    ),
)


class ContractReviewRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True, extra="forbid")

    contract_text: Annotated[str, StringConstraints(min_length=1, max_length=MAX_CONTRACT_CHARACTERS)]


class EvidenceCitation(BaseModel):
    chunk_id: int
    start_character: int
    end_character: int
    relevance_score: float
    excerpt: str = Field(max_length=CHUNK_SIZE)


class ClauseEvidence(BaseModel):
    clause: str = Field(max_length=50)
    question: str = Field(max_length=500)
    evidence_status: Literal["EVIDENCE_FOUND", "NO_MATCHING_EVIDENCE"]
    citations: list[EvidenceCitation] = Field(max_length=MAX_CITATIONS_PER_CLAUSE)
    reviewer_note: str = Field(max_length=500)


class ContractReviewResponse(BaseModel):
    contract_id: UUID
    organization_id: UUID
    requested_by_user_id: UUID
    clauses: list[ClauseEvidence]
    retrieval_method: Literal["ephemeral_bm25"]
    document_persisted: Literal[False] = False
    requires_human_review: Literal[True] = True


class _Chunk(BaseModel):
    chunk_id: int
    start_character: int
    end_character: int
    text: str
    terms: list[str]


def _tokens(text: str) -> list[str]:
    return [
        token
        for token in TOKEN_PATTERN.findall(unicodedata.normalize("NFKC", text).casefold())
        if token not in STOP_WORDS
    ]


def chunk_contract_text(text: str) -> list[_Chunk]:
    normalized = text
    chunks: list[_Chunk] = []
    start = 0
    while start < len(normalized):
        end = min(start + CHUNK_SIZE, len(normalized))
        if end < len(normalized):
            boundary = max(
                normalized.rfind("\n", start + CHUNK_SIZE // 2, end),
                normalized.rfind(" ", start + CHUNK_SIZE // 2, end),
            )
            if boundary > start:
                end = boundary
        content = normalized[start:end].strip()
        if content:
            leading_trim = len(normalized[start:end]) - len(normalized[start:end].lstrip())
            content_start = start + leading_trim
            chunks.append(
                _Chunk(
                    chunk_id=len(chunks) + 1,
                    start_character=content_start,
                    end_character=content_start + len(content),
                    text=content,
                    terms=_tokens(content),
                )
            )
        if end == len(normalized):
            break
        start = max(start + 1, end - CHUNK_OVERLAP)
    return chunks


def _rank_chunks(chunks: list[_Chunk], search_terms: tuple[str, ...]) -> list[tuple[_Chunk, float]]:
    query_terms = set(_tokens(" ".join(search_terms)))
    if not chunks or not query_terms:
        return []

    term_frequencies = [Counter(chunk.terms) for chunk in chunks]
    document_frequency = {
        term: sum(1 for frequencies in term_frequencies if frequencies[term] > 0)
        for term in query_terms
    }
    average_length = sum(len(chunk.terms) for chunk in chunks) / len(chunks)
    average_length = max(average_length, 1.0)
    k1 = 1.2
    b = 0.75
    ranked: list[tuple[_Chunk, float]] = []

    for chunk, frequencies in zip(chunks, term_frequencies, strict=True):
        score = 0.0
        for term in query_terms:
            frequency = frequencies[term]
            if frequency == 0:
                continue
            inverse_document_frequency = math.log(
                1 + (len(chunks) - document_frequency[term] + 0.5)
                / (document_frequency[term] + 0.5)
            )
            denominator = frequency + k1 * (
                1 - b + b * len(chunk.terms) / average_length
            )
            score += inverse_document_frequency * frequency * (k1 + 1) / denominator
        normalized_text = unicodedata.normalize("NFKC", chunk.text).casefold()
        matching_phrases = sum(
            1 for term in search_terms
            if " " in term and term in normalized_text
        )
        score += matching_phrases * 2
        if score > 0:
            ranked.append((chunk, score))
    ranked.sort(key=lambda item: (-item[1], item[0].chunk_id))
    return ranked[:MAX_CITATIONS_PER_CLAUSE]


def review_contract(
    contract_id: UUID,
    request: ContractReviewRequest,
    organization_id: UUID,
    requested_by_user_id: UUID,
) -> ContractReviewResponse:
    text = request.contract_text
    chunks = chunk_contract_text(text)
    clauses: list[ClauseEvidence] = []

    for definition in CLAUSE_DEFINITIONS:
        ranked = _rank_chunks(chunks, definition.search_terms)
        citations = [
            EvidenceCitation(
                chunk_id=chunk.chunk_id,
                start_character=chunk.start_character,
                end_character=chunk.end_character,
                relevance_score=round(score, 4),
                excerpt=chunk.text,
            )
            for chunk, score in ranked
        ]
        clauses.append(
            ClauseEvidence(
                clause=definition.clause,
                question=definition.question,
                evidence_status="EVIDENCE_FOUND" if citations else "NO_MATCHING_EVIDENCE",
                citations=citations,
                reviewer_note=(
                    "Review the cited language in the full agreement; retrieval does not determine "
                    "whether the clause is legally adequate."
                    if citations
                    else "No matching language was retrieved. This is not proof that the clause is absent; "
                    "review the full agreement."
                ),
            )
        )

    return ContractReviewResponse(
        contract_id=contract_id,
        organization_id=organization_id,
        requested_by_user_id=requested_by_user_id,
        clauses=clauses,
        retrieval_method="ephemeral_bm25",
    )
