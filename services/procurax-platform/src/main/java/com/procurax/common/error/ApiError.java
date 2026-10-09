package com.procurax.common.error;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String code, String message, String correlationId) {
}
