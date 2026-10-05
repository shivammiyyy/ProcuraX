package com.procurax.identity.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves the two security guarantees that matter most: unauthenticated requests are
 * rejected with a JSON 401, and authenticated-but-under-permissioned requests are rejected
 * with a JSON 403 — while a correctly permissioned user succeeds.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    MockMvc mvc;

    private OAuth2AuthenticationToken principalWithPermissions(String... permissions) {
        SecurityPrincipal principal = new SecurityPrincipal(
                UUID.randomUUID(), "user@example.com", "Test User",
                UUID.randomUUID(), "Test Org", "BUYER", Set.of(permissions), Map.of("sub", "test-subject"));
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    void unauthenticatedRequestReturnsJson401() throws Exception {
        mvc.perform(get("/api/v1/organizations/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedRequestResolvesOrganizationFromPrincipalOnly() throws Exception {
        OAuth2AuthenticationToken token = principalWithPermissions("VENDOR_READ");
        SecurityPrincipal principal = (SecurityPrincipal) token.getPrincipal();

        mvc.perform(get("/api/v1/organizations/current").with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(principal.getOrganizationId().toString()));
    }

    @Test
    void missingPermissionReturnsJson403() throws Exception {
        OAuth2AuthenticationToken token = principalWithPermissions("VENDOR_READ");

        mvc.perform(get("/api/v1/organizations/current/admin-check").with(authentication(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void grantedPermissionSucceeds() throws Exception {
        OAuth2AuthenticationToken token = principalWithPermissions("USER_MANAGE");

        mvc.perform(get("/api/v1/organizations/current/admin-check").with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granted").value(true));
    }

    @Test
    void authenticatedSessionCanBootstrapCookieCsrfToken() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf").with(authentication(principalWithPermissions("RFQ_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }
}
