package com.procurax.payment.repository;

import com.procurax.payment.domain.PaymentIntent;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {

    List<PaymentIntent> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<PaymentIntent> findByOrganizationIdAndId(UUID organizationId, UUID id);

    Optional<PaymentIntent> findByOrganizationIdAndAuthorizeIdempotencyKey(
            UUID organizationId, String idempotencyKey);

    Optional<PaymentIntent> findByOrganizationIdAndPurchaseOrderId(UUID organizationId, UUID purchaseOrderId);

    Optional<PaymentIntent> findByOrganizationIdAndCaptureIdempotencyKey(
            UUID organizationId, String idempotencyKey);

    Optional<PaymentIntent> findByOrganizationIdAndRefundIdempotencyKey(
            UUID organizationId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select intent from PaymentIntent intent "
            + "where intent.organizationId = :organizationId and intent.id = :intentId")
    Optional<PaymentIntent> findForUpdate(@Param("organizationId") UUID organizationId,
                                          @Param("intentId") UUID intentId);
}
