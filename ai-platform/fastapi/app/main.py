from contextlib import asynccontextmanager
from typing import Annotated, AsyncIterator
from uuid import UUID

from fastapi import Depends, FastAPI, Header, Path, Response, status

from app.config import get_settings
from app.contract_intelligence import (
    ContractReviewRequest,
    ContractReviewResponse,
    review_contract,
)
from app.procurement_agent import (
    ProcurementPlan,
    ProcurementPlanRequest,
    build_procurement_plan,
)
from app.security import require_backend_service
from app.vendor_evaluation import (
    EvaluationBatch,
    EvaluationResponse,
    explain_evaluations,
)


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    get_settings()
    yield


app = FastAPI(
    title="ProcuraX AI Platform",
    version=get_settings().version,
    openapi_url=None,
    docs_url=None,
    redoc_url=None,
    lifespan=lifespan,
)


@app.get("/health/live", include_in_schema=False)
async def liveness() -> dict[str, str]:
    settings = get_settings()
    return {"status": "UP", "service": settings.service_name}


@app.get("/health/ready", include_in_schema=False)
async def readiness(response: Response) -> dict[str, str]:
    settings = get_settings()
    if not settings.kafka_bootstrap_servers.strip():
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return {"status": "NOT_READY", "service": settings.service_name}
    return {"status": "UP", "service": settings.service_name}


@app.get("/health", include_in_schema=False)
async def health() -> dict[str, str]:
    settings = get_settings()
    return {"status": "UP", "service": settings.service_name}


@app.post(
    "/internal/v1/agents/procurement/plans",
    response_model=ProcurementPlan,
    dependencies=[Depends(require_backend_service)],
)
async def create_procurement_plan(
    request: ProcurementPlanRequest,
    organization_id: Annotated[UUID, Header(alias="X-ProcuraX-Organization-Id")],
    requested_by_user_id: Annotated[UUID, Header(alias="X-ProcuraX-Actor-Id")],
) -> ProcurementPlan:
    return build_procurement_plan(request, organization_id, requested_by_user_id)


@app.post(
    "/internal/v1/agents/vendor-evaluation/explanations",
    response_model=EvaluationResponse,
    dependencies=[Depends(require_backend_service)],
)
def create_vendor_evaluation_explanations(
    request: EvaluationBatch,
    organization_id: Annotated[UUID, Header(alias="X-ProcuraX-Organization-Id")],
    requested_by_user_id: Annotated[UUID, Header(alias="X-ProcuraX-Actor-Id")],
) -> EvaluationResponse:
    return explain_evaluations(request, organization_id, requested_by_user_id)


@app.post(
    "/internal/v1/contracts/{contract_id}/review",
    response_model=ContractReviewResponse,
    dependencies=[Depends(require_backend_service)],
)
def review_contract_document(
    request: ContractReviewRequest,
    contract_id: Annotated[UUID, Path()],
    organization_id: Annotated[UUID, Header(alias="X-ProcuraX-Organization-Id")],
    requested_by_user_id: Annotated[UUID, Header(alias="X-ProcuraX-Actor-Id")],
) -> ContractReviewResponse:
    return review_contract(contract_id, request, organization_id, requested_by_user_id)
