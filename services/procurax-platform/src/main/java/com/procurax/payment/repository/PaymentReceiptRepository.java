package com.procurax.payment.repository;

import com.procurax.payment.domain.PaymentReceipt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, UUID> {

    List<PaymentReceipt> findAllByOrganizationIdAndPaymentIntentIdOrderByCreatedAt(
            UUID organizationId, UUID paymentIntentId);
}
