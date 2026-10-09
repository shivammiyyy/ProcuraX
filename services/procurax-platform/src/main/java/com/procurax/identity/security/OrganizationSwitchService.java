package com.procurax.identity.security;

import com.procurax.identity.service.MembershipService;
import com.procurax.identity.service.OrganizationMembership;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Switches the authenticated user's active organization for the remainder of the session.
 * Only organizations the user actually belongs to (per {@link MembershipService}) can be
 * activated; the target id is validated against real memberships, never trusted blindly.
 */
@Service
public class OrganizationSwitchService {

    private final MembershipService membershipService;
    private final SecurityContextRepository securityContextRepository;

    public OrganizationSwitchService(MembershipService membershipService,
                                      SecurityContextRepository securityContextRepository) {
        this.membershipService = membershipService;
        this.securityContextRepository = securityContextRepository;
    }

    public SecurityPrincipal switchOrganization(SecurityPrincipal current, UUID targetOrganizationId,
                                                 HttpServletRequest request, HttpServletResponse response) {
        OrganizationMembership membership = membershipService.membership(current.getUserId(), targetOrganizationId)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of the requested organization"));

        var permissions = membershipService.permissionsForRole(membership.roleId());
        SecurityPrincipal updated = current.withActiveOrganization(
                membership.organizationId(), membership.organizationName(), membership.roleName(), permissions);

        AbstractAuthenticationToken existing =
                (AbstractAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthenticationToken newToken = new OAuth2AuthenticationToken(
                updated, updated.getAuthorities(), registrationIdOf(existing));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newToken);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return updated;
    }

    private String registrationIdOf(AbstractAuthenticationToken existing) {
        if (existing instanceof OAuth2AuthenticationToken oauth2Token) {
            return oauth2Token.getAuthorizedClientRegistrationId();
        }
        return "google";
    }
}
