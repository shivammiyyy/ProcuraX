package com.procurax.identity.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Adapts {@link SecurityPrincipal} to {@link OidcUser} so it can flow through Spring
 * Security's OAuth2 login filter chain while still exposing our own authorities
 * (permission names) and domain fields to the rest of the application.
 */
public final class SecurityOidcUser implements OidcUser {

    private static final long serialVersionUID = 1L;

    private final SecurityPrincipal principal;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public SecurityOidcUser(SecurityPrincipal principal, OidcIdToken idToken, OidcUserInfo userInfo) {
        this.principal = principal;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public SecurityPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Map<String, Object> getClaims() {
        return principal.getAttributes();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return principal.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return principal.getAuthorities();
    }

    @Override
    public String getName() {
        return principal.getName();
    }
}
