package com.procurax.identity.security;

import com.procurax.identity.domain.User;
import com.procurax.identity.repository.UserRepository;
import com.procurax.identity.service.MembershipService;
import com.procurax.identity.service.OrganizationMembership;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Loads the Google profile returned by the OIDC provider, finds or creates the local
 * {@link User}, resolves (or provisions) an organization membership, and builds the
 * {@link SecurityPrincipal} that the rest of the application relies on.
 *
 * <p>The LLM/agent layer and the frontend never see raw provider tokens; only this service
 * talks to the provider's user-info endpoint, and {@link #buildPrincipal} is unit-testable
 * without any network call (it accepts a plain {@link OAuth2User}, which an {@link OidcUser}
 * also is).
 */
@Service
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final MembershipService membershipService;

    public CustomOAuth2UserService(UserRepository userRepository, MembershipService membershipService) {
        this.userRepository = userRepository;
        this.membershipService = membershipService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser delegate = super.loadUser(userRequest);
        SecurityPrincipal principal = buildPrincipal(delegate);
        return new SecurityOidcUser(principal, delegate.getIdToken(), delegate.getUserInfo());
    }

    @Transactional
    public SecurityPrincipal buildPrincipal(OAuth2User delegate) {
        String subject = delegate.getAttribute("sub");
        String email = delegate.getAttribute("email");
        Boolean emailVerified = delegate.getAttribute("email_verified");
        String name = delegate.getAttribute("name");

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"), "Provider profile is missing subject or email");
        }
        if (Boolean.FALSE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"), "Provider account email is not verified");
        }

        User user = userRepository.findByAuthProviderAndProviderSubject("google", subject)
                .orElseGet(() -> userRepository.save(
                        new User(email, StringUtils.hasText(name) ? name : email, "google", subject)));

        List<OrganizationMembership> memberships = membershipService.membershipsFor(user.getId());
        OrganizationMembership active = memberships.isEmpty()
                ? membershipService.provisionPersonalOrganization(user)
                : memberships.get(0);

        Set<String> permissions = membershipService.permissionsForRole(active.roleId());

        return new SecurityPrincipal(user.getId(), user.getEmail(), user.getFullName(),
                active.organizationId(), active.organizationName(), active.roleName(),
                permissions, delegate.getAttributes());
    }
}
