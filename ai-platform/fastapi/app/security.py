import secrets
from typing import Annotated

from fastapi import Header, HTTPException, status
from pydantic import SecretStr

from app.config import get_settings


async def require_backend_service(
    authorization: Annotated[str | None, Header()] = None,
) -> None:
    configured_token: SecretStr | None = get_settings().service_token
    if configured_token is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Procurement planning is not enabled",
        )

    scheme, separator, supplied_token = (authorization or "").partition(" ")
    expected_token = configured_token.get_secret_value()
    if (
        not separator
        or scheme.lower() != "bearer"
        or not supplied_token.isascii()
        or not secrets.compare_digest(supplied_token, expected_token)
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Backend service authentication required",
            headers={"WWW-Authenticate": "Bearer"},
        )
