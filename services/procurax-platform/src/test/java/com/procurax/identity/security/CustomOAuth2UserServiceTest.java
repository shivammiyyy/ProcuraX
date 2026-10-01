package com.procurax.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.procurax.identity.domain.User;
import com.procurax.identity.repository.UserRepository;
import com.procurax.identity.service.MembershipService;
import com.procurax.identity.service.OrganizationMembership;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class CustomOAuth2UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final MembershipService membershipService = mock(MembershipService.class);
    private final CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository, membershipService);

    private OAuth2User googleDelegate(String subject, String email, Boolean emailVerified, String name) {
        Map<String, Object> attributes = new java.util.HashMap<>();
        if (subject != null) {
            attributes.put("sub", subject);
        }
        attributes.put("email", email);
        if (emailVerified != null) {
            attributes.put("email_verified", emailVerified);
        }
        attributes.put("name", name);
        return new DefaultOAuth2User(List.of(() -> "OAUTH2_USER"), attributes, "email");
    }

    @Test
    void firstLoginProvisionsPersonalOrganization() {
        OAuth2User delegate = googleDelegate("subject-1", "new.user@example.com", true, "New User");
        when(userRepository.findByAuthProviderAndProviderSubject("google", "subject-1")).thenReturn(Optional.empty());

        User savedUser = new User("new.user@example.com", "New User", "google", "subject-1");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(membershipService.membershipsFor(any())).thenReturn(List.of());

        UUID orgId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        OrganizationMembership provisioned = new OrganizationMembership(
                UUID.randomUUID(), orgId, "New User's Organization", roleId, "ORG_ADMIN");
        when(membershipService.provisionPersonalOrganization(savedUser)).thenReturn(provisioned);
        when(membershipService.permissionsForRole(roleId)).thenReturn(Set.of("USER_MANAGE", "VENDOR_READ"));

        SecurityPrincipal principal = service.buildPrincipal(delegate);

        assertThat(principal.getEmail()).isEqualTo("new.user@example.com");
        assertThat(principal.getOrganizationId()).isEqualTo(orgId);
        assertThat(principal.getRole()).isEqualTo("ORG_ADMIN");
        assertThat(principal.getPermissions()).contains("USER_MANAGE", "VENDOR_READ");
        verify(membershipService).provisionPersonalOrganization(savedUser);
    }

    @Test
    void secondLoginReusesExistingUserAndMembership() {
        OAuth2User delegate = googleDelegate("subject-2", "existing@example.com", true, "Existing User");
        User existingUser = new User("existing@example.com", "Existing User", "google", "subject-2");
        when(userRepository.findByAuthProviderAndProviderSubject("google", "subject-2"))
                .thenReturn(Optional.of(existingUser));

        UUID orgId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        OrganizationMembership existingMembership = new OrganizationMembership(
                UUID.randomUUID(), orgId, "Existing Org", roleId, "BUYER");
        when(membershipService.membershipsFor(any())).thenReturn(List.of(existingMembership));
        when(membershipService.permissionsForRole(roleId)).thenReturn(Set.of("RFQ_CREATE"));

        SecurityPrincipal principal = service.buildPrincipal(delegate);

        assertThat(principal.getOrganizationId()).isEqualTo(orgId);
        assertThat(principal.getRole()).isEqualTo("BUYER");
        verify(userRepository, org.mockito.Mockito.never()).save(any());
        verify(membershipService, org.mockito.Mockito.never()).provisionPersonalOrganization(any());
    }

    @Test
    void rejectsUnverifiedEmail() {
        OAuth2User delegate = googleDelegate("subject-3", "unverified@example.com", false, "Unverified User");

        assertThatThrownBy(() -> service.buildPrincipal(delegate))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verifyNoMoreInteractions(userRepository, membershipService);
    }

    @Test
    void rejectsMissingSubjectOrEmail() {
        OAuth2User delegate = googleDelegate(null, "missing-subject@example.com", true, "No Subject");

        assertThatThrownBy(() -> service.buildPrincipal(delegate))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
