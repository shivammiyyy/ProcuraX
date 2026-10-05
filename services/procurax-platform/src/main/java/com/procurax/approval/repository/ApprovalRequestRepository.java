package com.procurax.approval.repository;

import com.procurax.approval.domain.ApprovalRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    List<ApprovalRequest> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<ApprovalRequest> findByOrganizationIdAndId(UUID organizationId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ApprovalRequest request where request.organizationId = :organizationId "
            + "and request.id = :requestId")
    Optional<ApprovalRequest> findForUpdate(@Param("organizationId") UUID organizationId,
                                            @Param("requestId") UUID requestId);
}
