package com.procurax.identity.web;

import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.OrganizationSwitchService;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.identity.service.MembershipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session-scoped identity endpoints: who am I, and which organization am I acting as. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OrganizationContext organizationContext;
    private final MembershipService membershipService;
    private final OrganizationSwitchService organizationSwitchService;

    public AuthController(OrganizationContext organizationContext, MembershipService membershipService,
                           OrganizationSwitchService organizationSwitchService) {
        this.organizationContext = organizationContext;
        this.membershipService = membershipService;
        this.organizationSwitchService = organizationSwitchService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        return CurrentUserResponse.from(principal, membershipService.membershipsFor(principal.getUserId()));
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/organizations/{organizationId}/switch")
    public CurrentUserResponse switchOrganization(@PathVariable UUID organizationId,
                                                   HttpServletRequest request, HttpServletResponse response) {
        SecurityPrincipal current = organizationContext.currentPrincipal();
        SecurityPrincipal updated = organizationSwitchService.switchOrganization(
                current, organizationId, request, response);
        return CurrentUserResponse.from(updated, membershipService.membershipsFor(updated.getUserId()));
    }
}
