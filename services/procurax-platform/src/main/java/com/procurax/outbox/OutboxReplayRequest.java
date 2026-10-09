package com.procurax.outbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OutboxReplayRequest(
        @NotBlank @Size(min = 10, max = 500) String reason) {
}
