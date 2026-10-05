package com.procurax.approval.repository;

import com.procurax.approval.domain.ApprovalStep;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, UUID> {

    List<ApprovalStep> findAllByOrganizationIdAndRequestIdOrderByStepOrder(
            UUID organizationId, UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select step from ApprovalStep step where step.organizationId = :organizationId "
            + "and step.requestId = :requestId and step.stepOrder = 1")
    Optional<ApprovalStep> findFirstStepForUpdate(@Param("organizationId") UUID organizationId,
                                                  @Param("requestId") UUID requestId);
}
