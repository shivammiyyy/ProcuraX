package com.procurax.identity.repository;

import com.procurax.identity.domain.OrganizationMember;
import com.procurax.identity.service.OrganizationMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    @Query("""
            select new com.procurax.identity.service.OrganizationMembership(
                m.id, m.organizationId, o.name, m.roleId, r.name)
            from OrganizationMember m, Organization o, Role r
            where m.userId = :userId
              and o.id = m.organizationId
              and r.id = m.roleId
              and m.status = 'ACTIVE'
            order by m.createdAt
            """)
    List<OrganizationMembership> findMembershipsByUserId(@Param("userId") UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
