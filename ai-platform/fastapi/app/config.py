from functools import lru_cache

from pydantic import AnyHttpUrl, Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="PROCURAX_AI_",
        env_file=".env",
        extra="ignore",
    )

    service_name: str = "procurax-ai-platform"
    version: str = "0.1.0"
    spring_api_base_url: AnyHttpUrl = "http://backend:8080"
    kafka_bootstrap_servers: str = Field(default="kafka:29092", min_length=1)
    service_token: SecretStr | None = None
    ollama_base_url: AnyHttpUrl = "http://host.docker.internal:11434"
    ollama_chat_model: str = Field(default="qwen3:8b", min_length=1)
    ollama_embedding_model: str = Field(default="qwen3-embedding:0.6b", min_length=1)
    ollama_embedding_dimensions: int = Field(default=1024, ge=1, le=4096)
    ollama_timeout_seconds: float = Field(default=120.0, gt=0, le=600)

    @field_validator("service_token", mode="before")
    @classmethod
    def validate_service_token(cls, value: object) -> object:
        if value is None or (isinstance(value, str) and not value.strip()):
            return None
        if isinstance(value, str) and len(value) < 32:
            raise ValueError("service token must contain at least 32 characters")
        return value


@lru_cache
def get_settings() -> Settings:
    return Settings()
