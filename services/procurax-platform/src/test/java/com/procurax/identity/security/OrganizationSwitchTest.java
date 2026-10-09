package com.procurax.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.procurax.identity.domain.Organization;
import com.procurax.identity.domain.OrganizationMember;
import com.procurax.identity.domain.Role;
import com.procurax.identity.domain.User;
import com.procurax.identity.repository.OrganizationMemberRepository;
import com.procurax.identity.repository.OrganizationRepository;
import com.procurax.identity.repository.RoleRepository;
import com.procurax.identity.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * Verifies that an organization switch only succeeds for organizations the user actually
 * belongs to (per real {@code organization_members} rows), proving the switch endpoint
 * cannot be used to hop into an organization the user has no membership in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrganizationSwitchTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    OrganizationMemberRepository memberRepository;
    @Autowired
    RoleRepository roleRepository;

    private User user;
    private Organization orgA;
    private Organization orgB;
    private Organization orgNotMember;
    private Role buyerRole;

    @BeforeEach
    void setUp() {
        buyerRole = roleRepository.findByName("BUYER").orElseThrow();
        user = userRepository.save(new User(
                "switcher-" + UUID.randomUUID() + "@example.com", "Switcher", "google", UUID.randomUUID().toString()));
        orgA = organizationRepository.save(new Organization("Org A", "org-a-" + UUID.randomUUID()));
        orgB = organizationRepository.save(new Organization("Org B", "org-b-" + UUID.randomUUID()));
        orgNotMember = organizationRepository.save(new Organization("Org Foreign", "org-foreign-" + UUID.randomUUID()));

        memberRepository.save(new OrganizationMember(orgA.getId(), user.getId(), buyerRole.getId()));
        memberRepository.save(new OrganizationMember(orgB.getId(), user.getId(), buyerRole.getId()));
    }

    private OAuth2AuthenticationToken tokenInOrgA() {
        SecurityPrincipal principal = new SecurityPrincipal(user.getId(), user.getEmail(), user.getFullName(),
                orgA.getId(), orgA.getName(), "BUYER", Set.of("RFQ_READ"), java.util.Map.of("sub", "x"));
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    void switchingToAMembershipOrganizationSucceeds() throws Exception {
        mvc.perform(post("/api/v1/auth/organizations/{id}/switch", orgB.getId())
                        .with(authentication(tokenInOrgA()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeOrganization.id").value(orgB.getId().toString()))
                .andExpect(jsonPath("$.activeOrganization.name").value("Org B"));
    }

    @Test
    void switchingToANonMemberOrganizationIsForbidden() throws Exception {
        mvc.perform(post("/api/v1/auth/organizations/{id}/switch", orgNotMember.getId())
                        .with(authentication(tokenInOrgA()))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void membershipRowsArePersisted() {
        assertThat(memberRepository.findByOrganizationIdAndUserId(orgA.getId(), user.getId())).isPresent();
    }
}
