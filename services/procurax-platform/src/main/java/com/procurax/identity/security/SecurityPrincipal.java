package com.procurax.identity.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * The authenticated principal: the application user plus their currently active
 * organization, role and resolved permissions. Permissions are exposed as Spring Security
 * authorities so controllers can use {@code @PreAuthorize("hasAuthority('PO_APPROVE')")}.
 *
 * <p>Stored inside the HTTP session (backed by Redis), so this class must stay serializable.
 */
public final class SecurityPrincipal implements OAuth2User, Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String email;
    private final String fullName;
    private final UUID organizationId;
    private final String organizationName;
    private final String role;
    private final Set<String> permissions;
    private final Map<String, Object> attributes;

    public SecurityPrincipal(UUID userId, String email, String fullName, UUID organizationId,
                              String organizationName, String role, Set<String> permissions,
                              Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.role = role;
        this.permissions = Set.copyOf(permissions);
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public SecurityPrincipal withActiveOrganization(UUID newOrganizationId, String newOrganizationName,
                                                     String newRole, Set<String> newPermissions) {
        return new SecurityPrincipal(userId, email, fullName, newOrganizationId, newOrganizationName,
                newRole, newPermissions, attributes);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getName() {
        return userId.toString();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getRole() {
        return role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
}
