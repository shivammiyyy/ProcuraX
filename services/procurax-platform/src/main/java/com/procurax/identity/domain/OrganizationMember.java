package com.procurax.identity.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Binds a {@link User} to an {@link Organization} with exactly one {@link Role}.
 * A user may hold memberships in several organizations, but only one role per organization.
 */
@Entity
@Table(name = "organization_members")
public class OrganizationMember extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(nullable = false)
    private String status = "ACTIVE";

    protected OrganizationMember() {
        super();
    }

    public OrganizationMember(UUID organizationId, UUID userId, UUID roleId) {
        super(organizationId);
        this.userId = userId;
        this.roleId = roleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getStatus() {
        return status;
    }
}
