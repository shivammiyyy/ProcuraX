package com.procurax.outbox;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/outbox")
public class OutboxOperationsController {

    private final OutboxReplayService replayService;

    public OutboxOperationsController(OutboxReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/{eventId}/replay")
    @PreAuthorize("hasAuthority('OUTBOX_REPLAY')")
    public OutboxReplayResponse replay(@PathVariable UUID eventId,
                                       @Valid @RequestBody OutboxReplayRequest request) {
        return replayService.replay(eventId, request.reason());
    }
}
