package com.procurax.contract.repository;

import com.procurax.contract.domain.Contract;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Page<Contract> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<Contract> findAllByOrganizationIdAndVendorIdInOrderByCreatedAtDesc(
            UUID organizationId, Collection<UUID> vendorIds, Pageable pageable);

    Optional<Contract> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contract from Contract contract "
            + "where contract.id = :id and contract.organizationId = :organizationId")
    Optional<Contract> findForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
