package com.procurax.realtime;

import com.procurax.identity.security.OrganizationContext;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/events")
public class RealtimeEventController {

    private final OrganizationContext organizationContext;
    private final RealtimeEventStream eventStream;

    public RealtimeEventController(OrganizationContext organizationContext, RealtimeEventStream eventStream) {
        this.organizationContext = organizationContext;
        this.eventStream = eventStream;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('EVENT_STREAM_READ')")
    public SseEmitter subscribe(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().mustRevalidate().getHeaderValue());
        response.setHeader("X-Accel-Buffering", "no");
        return eventStream.open(organizationContext.currentOrganizationId());
    }
}
