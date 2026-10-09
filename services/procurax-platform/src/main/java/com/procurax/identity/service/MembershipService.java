package com.procurax.identity.service;

import com.procurax.identity.domain.Organization;
import com.procurax.identity.domain.OrganizationMember;
import com.procurax.identity.domain.Role;
import com.procurax.identity.domain.User;
import com.procurax.identity.repository.OrganizationMemberRepository;
import com.procurax.identity.repository.OrganizationRepository;
import com.procurax.identity.repository.RoleRepository;
import com.procurax.identity.repository.PermissionRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Resolves organization membership, roles and permissions for a user, and provisions a
 * personal organization the first time a user signs in with no existing membership.
 *
 * <p>Auto-provisioning a personal organization on first login is a deliberate choice for this
 * demo/personal-project deployment; a production rollout would instead assign organizations
 * through an invitation workflow.
 */
@Service
public class MembershipService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public MembershipService(OrganizationRepository organizationRepository,
                              OrganizationMemberRepository memberRepository,
                              RoleRepository roleRepository,
                              PermissionRepository permissionRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public List<OrganizationMembership> membershipsFor(UUID userId) {
        return memberRepository.findMembershipsByUserId(userId);
    }

    public Optional<OrganizationMembership> membership(UUID userId, UUID organizationId) {
        return membershipsFor(userId).stream()
                .filter(m -> m.organizationId().equals(organizationId))
                .findFirst();
    }

    public Set<String> permissionsForRole(UUID roleId) {
        return Set.copyOf(permissionRepository.findPermissionNamesByRoleId(roleId));
    }

    @Transactional
    public OrganizationMembership provisionPersonalOrganization(User user) {
        Role adminRole = roleRepository.findByName("ORG_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ORG_ADMIN role is not seeded; check Flyway V1"));

        String slug = uniqueSlug(slugify(user.getEmail()));
        Organization organization = organizationRepository.save(new Organization(displayName(user), slug));
        OrganizationMember member = memberRepository.save(
                new OrganizationMember(organization.getId(), user.getId(), adminRole.getId()));

        return new OrganizationMembership(
                member.getId(), organization.getId(), organization.getName(), adminRole.getId(), adminRole.getName());
    }

    private String displayName(User user) {
        String name = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail();
        return name + "'s Organization";
    }

    private String slugify(String email) {
        String local = email.substring(0, Math.max(email.indexOf('@'), 0)).toLowerCase(Locale.ROOT);
        String cleaned = local.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return cleaned.isBlank() ? "org" : cleaned;
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int attempts = 0;
        while (organizationRepository.findBySlug(candidate).isPresent() && attempts < 10) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            attempts++;
        }
        return candidate;
    }
}
