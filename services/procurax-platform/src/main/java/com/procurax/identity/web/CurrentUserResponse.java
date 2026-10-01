package com.procurax.identity.web;

import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.identity.service.OrganizationMembership;
import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        String fullName,
        OrganizationSummary activeOrganization,
        List<String> permissions,
        List<OrganizationSummary> organizations) {

    public record OrganizationSummary(UUID id, String name, String role) {
    }

    public static CurrentUserResponse from(SecurityPrincipal principal, List<OrganizationMembership> memberships) {
        OrganizationSummary active = new OrganizationSummary(
                principal.getOrganizationId(), principal.getOrganizationName(), principal.getRole());
        List<OrganizationSummary> all = memberships.stream()
                .map(m -> new OrganizationSummary(m.organizationId(), m.organizationName(), m.roleName()))
                .toList();
        return new CurrentUserResponse(principal.getUserId(), principal.getEmail(), principal.getFullName(),
                active, List.copyOf(principal.getPermissions()), all);
    }
}
