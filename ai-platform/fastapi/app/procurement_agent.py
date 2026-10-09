from datetime import datetime, timezone
from decimal import Decimal
from typing import Annotated, Literal
from uuid import UUID, uuid4

from pydantic import BaseModel, ConfigDict, Field, StringConstraints, field_validator


NonBlankText = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1)]
CurrencyCode = Annotated[str, StringConstraints(pattern=r"^[A-Z]{3}$")]
Objective = Literal["balanced", "lowest_cost", "quality_first", "fastest_delivery"]


class ProcurementItem(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    description: NonBlankText = Field(max_length=500)
    quantity: int = Field(gt=0, le=1_000_000)
    unit: str = Field(default="unit", min_length=1, max_length=30)
    specification: str = Field(default="", max_length=5_000)


class ProcurementPlanRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    summary: NonBlankText = Field(max_length=10_000)
    category: NonBlankText = Field(max_length=100)
    budget: Decimal = Field(gt=0, max_digits=14, decimal_places=2)
    currency: CurrencyCode
    response_deadline: datetime
    delivery_days: int = Field(ge=1, le=365)
    objective: Objective = "balanced"
    items: list[ProcurementItem] = Field(min_length=1, max_length=100)

    @field_validator("response_deadline")
    @classmethod
    def require_future_timezone_aware_deadline(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("response_deadline must include a timezone")
        if value <= datetime.now(timezone.utc):
            raise ValueError("response_deadline must be in the future")
        return value


class ProposedRfqItem(BaseModel):
    description: str
    quantity: int
    unit: str
    specification: str


class ProposedRfq(BaseModel):
    title: str
    description: str
    requestType: Literal["RFQ", "RFP"]
    category: str
    budget: Decimal
    currency: str
    deadline: datetime
    deliveryDays: int
    items: list[ProposedRfqItem]


class ProcurementPlan(BaseModel):
    plan_id: UUID
    organization_id: UUID
    requested_by_user_id: UUID
    status: Literal["READY_FOR_REVIEW"] = "READY_FOR_REVIEW"
    action: Literal["CREATE_RFQ_DRAFT"] = "CREATE_RFQ_DRAFT"
    proposed_rfq: ProposedRfq
    recommended_evaluation_criteria: list[str]
    review_notes: list[str]
    requires_human_review: Literal[True] = True


def build_procurement_plan(
    request: ProcurementPlanRequest,
    organization_id: UUID,
    requested_by_user_id: UUID,
) -> ProcurementPlan:
    objective_criteria = {
        "balanced": ["price", "delivery", "quality", "compliance"],
        "lowest_cost": ["price", "compliance", "delivery", "quality"],
        "quality_first": ["quality", "compliance", "price", "delivery"],
        "fastest_delivery": ["delivery", "compliance", "price", "quality"],
    }
    title = f"{request.category} sourcing request"[:255]
    request_type: Literal["RFQ", "RFP"] = (
        "RFP" if request.objective == "quality_first" else "RFQ"
    )
    review_notes = [
        "Review the proposed scope, budget, response deadline, and delivery target before creating the RFQ.",
        "No vendors, prices, or compliance claims have been inferred by the planner.",
    ]
    if any(not item.specification for item in request.items):
        review_notes.append("Add technical specifications to any line item that needs them.")

    return ProcurementPlan(
        plan_id=uuid4(),
        organization_id=organization_id,
        requested_by_user_id=requested_by_user_id,
        proposed_rfq=ProposedRfq(
            title=title,
            description=request.summary,
            requestType=request_type,
            category=request.category,
            budget=request.budget,
            currency=request.currency,
            deadline=request.response_deadline,
            deliveryDays=request.delivery_days,
            items=[
                ProposedRfqItem(
                    description=item.description,
                    quantity=item.quantity,
                    unit=item.unit,
                    specification=item.specification,
                )
                for item in request.items
            ],
        ),
        recommended_evaluation_criteria=objective_criteria[request.objective],
        review_notes=review_notes,
    )
