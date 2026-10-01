package com.procurax.identity.service;

import java.util.UUID;

/** A user's membership in one organization, with the resolved organization/role names. */
public record OrganizationMembership(
        UUID membershipId,
        UUID organizationId,
        String organizationName,
        UUID roleId,
        String roleName) {
}
