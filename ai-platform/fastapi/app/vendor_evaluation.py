from functools import lru_cache
from pathlib import Path
from uuid import UUID

import numpy as np
import onnxruntime as ort
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "vendor_model.onnx"
FACTOR_NAMES = ("price", "delivery", "quality", "compliance", "performance", "paymentTerms")
MAX_EVALUATIONS = 100


class LegacyFeatures(BaseModel):
    price_difference: float = Field(ge=-1_000_000_000, le=1_000_000_000)
    delivery_days: int = Field(ge=1, le=365)
    compliance_score: float = Field(ge=0, le=100)


class EvaluationInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    quotation_id: UUID
    vendor_id: UUID
    official_score: float = Field(ge=0, le=100)
    factors: dict[str, float]
    weights: dict[str, float]
    legacy_features: LegacyFeatures

    @field_validator("factors")
    @classmethod
    def validate_factors(cls, value: dict[str, float]) -> dict[str, float]:
        if set(value) != set(FACTOR_NAMES):
            raise ValueError(f"factors must contain exactly: {', '.join(FACTOR_NAMES)}")
        if any(not 0 <= score <= 100 for score in value.values()):
            raise ValueError("factor scores must be between 0 and 100")
        return value

    @field_validator("weights")
    @classmethod
    def validate_weights(cls, value: dict[str, float]) -> dict[str, float]:
        if set(value) != set(FACTOR_NAMES):
            raise ValueError(f"weights must contain exactly: {', '.join(FACTOR_NAMES)}")
        if any(not 0 <= weight <= 1 for weight in value.values()):
            raise ValueError("factor weights must be between 0 and 1")
        if not np.isclose(sum(value.values()), 1.0, rtol=0, atol=1e-6):
            raise ValueError("factor weights must sum to 1")
        return value


class EvaluationBatch(BaseModel):
    evaluations: list[EvaluationInput] = Field(min_length=1, max_length=MAX_EVALUATIONS)

    @model_validator(mode="after")
    def quotation_ids_are_unique(self) -> "EvaluationBatch":
        ids = [evaluation.quotation_id for evaluation in self.evaluations]
        if len(ids) != len(set(ids)):
            raise ValueError("quotation_id values must be unique within a batch")
        return self


class FactorContribution(BaseModel):
    factor: str
    factor_score: float
    weight: float
    weighted_points: float


class EvaluationExplanation(BaseModel):
    quotation_id: UUID
    vendor_id: UUID
    official_score: float
    legacy_model_score: float
    model_version: str = "legacy-onnx-v1"
    factor_contributions: list[FactorContribution]
    strengths: list[str]
    review_areas: list[str]
    summary: str
    advisory_only: bool = True


class EvaluationResponse(BaseModel):
    organization_id: UUID
    requested_by_user_id: UUID
    explanations: list[EvaluationExplanation]
    requires_human_review: bool = True
    official_scores_unchanged: bool = True


@lru_cache(maxsize=1)
def _model_session() -> ort.InferenceSession:
    if not MODEL_PATH.is_file():
        raise RuntimeError("Vendor evaluation model artifact is unavailable")
    session = ort.InferenceSession(
        str(MODEL_PATH),
        providers=["CPUExecutionProvider"],
    )

    inputs = session.get_inputs()
    outputs = session.get_outputs()
    if (
        len(inputs) != 1
        or inputs[0].name != "input"
        or inputs[0].type != "tensor(float)"
        or inputs[0].shape[-1] != 3
        or len(outputs) != 1
    ):
        raise RuntimeError("Vendor evaluation model signature is unsupported")
    return session


def _build_explanation(
    evaluation: EvaluationInput,
    model_score: float,
) -> EvaluationExplanation:
    contributions = [
        FactorContribution(
            factor=name,
            factor_score=evaluation.factors[name],
            weight=evaluation.weights[name],
            weighted_points=round(evaluation.factors[name] * evaluation.weights[name], 2),
        )
        for name in FACTOR_NAMES
    ]
    contributions.sort(key=lambda item: (-item.weighted_points, item.factor))
    strengths = [
        item.factor
        for item in contributions
        if item.factor_score >= 75
    ]
    review_areas = [
        item.factor
        for item in contributions
        if item.factor_score < 50
    ]
    explanation = (
        "The official score and factor breakdown were calculated by Spring. "
        "The legacy ONNX score is advisory and uses only price difference, "
        "delivery days, and compliance score; it does not change the official score."
    )
    return EvaluationExplanation(
        quotation_id=evaluation.quotation_id,
        vendor_id=evaluation.vendor_id,
        official_score=evaluation.official_score,
        legacy_model_score=model_score,
        factor_contributions=contributions,
        strengths=strengths,
        review_areas=review_areas,
        summary=explanation,
    )


def explain_evaluations(
    batch: EvaluationBatch,
    organization_id: UUID,
    requested_by_user_id: UUID,
) -> EvaluationResponse:
    features = np.asarray(
        [
            [
                evaluation.legacy_features.price_difference,
                evaluation.legacy_features.delivery_days,
                evaluation.legacy_features.compliance_score,
            ]
            for evaluation in batch.evaluations
        ],
        dtype=np.float32,
    )
    session = _model_session()
    output = session.run([session.get_outputs()[0].name], {"input": features})[0]
    scores = np.asarray(output, dtype=np.float64).reshape(-1)
    if len(scores) != len(batch.evaluations) or not np.isfinite(scores).all():
        raise RuntimeError("Vendor evaluation model returned invalid output")

    explanations = [
        _build_explanation(evaluation, round(float(score), 2))
        for evaluation, score in zip(batch.evaluations, scores, strict=True)
    ]
    return EvaluationResponse(
        organization_id=organization_id,
        requested_by_user_id=requested_by_user_id,
        explanations=explanations,
    )
