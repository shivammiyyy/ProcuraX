package com.procurax.identity.web;

import com.procurax.identity.security.OrganizationContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proves tenant isolation and permission enforcement: neither endpoint accepts any
 * client-supplied organization id. The active organization always comes from
 * {@link OrganizationContext}, derived from the authenticated session.
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationContext organizationContext;

    public OrganizationController(OrganizationContext organizationContext) {
        this.organizationContext = organizationContext;
    }

    @GetMapping("/current")
    public Map<String, Object> current() {
        UUID organizationId = organizationContext.currentOrganizationId();
        return Map.of("organizationId", organizationId);
    }

    @GetMapping("/current/admin-check")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public Map<String, Object> adminCheck() {
        return Map.of("granted", true, "organizationId", organizationContext.currentOrganizationId());
    }
}
