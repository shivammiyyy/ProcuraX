package com.procurax.payment.repository;

import com.procurax.payment.domain.PaymentMandate;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMandateRepository extends JpaRepository<PaymentMandate, UUID> {

    List<PaymentMandate> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<PaymentMandate> findByOrganizationIdAndId(UUID organizationId, UUID id);

    Optional<PaymentMandate> findByOrganizationIdAndPurchaseOrderIdAndStatus(
            UUID organizationId, UUID purchaseOrderId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select mandate from PaymentMandate mandate "
            + "where mandate.organizationId = :organizationId and mandate.id = :mandateId")
    Optional<PaymentMandate> findForUpdate(@Param("organizationId") UUID organizationId,
                                           @Param("mandateId") UUID mandateId);
}
