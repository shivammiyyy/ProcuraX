package com.procurax.identity.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Derives the current organization strictly from the authenticated principal.
 *
 * <p><b>The organization id supplied by a client request is never trusted.</b> Every
 * organization-scoped query or write must go through {@link #currentOrganizationId()}, not
 * through a path/query/body parameter, so a user from one organization can never reach
 * another organization's data (IDOR prevention).
 */
@Component
public class OrganizationContext {

    public SecurityPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No authenticated organization context");
        }
        Object raw = authentication.getPrincipal();
        if (raw instanceof SecurityPrincipal principal) {
            return principal;
        }
        if (raw instanceof SecurityOidcUser oidcUser) {
            return oidcUser.getPrincipal();
        }
        throw new AccessDeniedException("No authenticated organization context");
    }

    public UUID currentOrganizationId() {
        return currentPrincipal().getOrganizationId();
    }
}
