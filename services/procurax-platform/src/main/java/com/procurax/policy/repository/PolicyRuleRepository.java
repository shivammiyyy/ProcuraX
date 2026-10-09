package com.procurax.policy.repository;

import com.procurax.policy.domain.PolicyRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, UUID> {

    List<PolicyRule> findAllByOrganizationIdOrderByName(UUID organizationId);

    List<PolicyRule> findAllByOrganizationIdAndEnabledTrueOrderByName(UUID organizationId);

    Optional<PolicyRule> findByOrganizationIdAndId(UUID organizationId, UUID id);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
            UUID organizationId, String name, UUID id);
}
