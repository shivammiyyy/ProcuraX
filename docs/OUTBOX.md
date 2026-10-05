# Kafka outbox and event delivery

Spring owns business state and writes event records into PostgreSQL in the same transaction as each RFQ/quotation change. The scheduled dispatcher claims due rows with `FOR UPDATE SKIP LOCKED`, publishes a JSON envelope to Kafka, and only then marks the row published. Delivery is at-least-once: a process crash after Kafka accepts a message but before PostgreSQL commits can cause a duplicate, so consumers must use `eventId` for idempotency.

## Event topics

| Topic | Event types |
|---|---|
| `procurax.rfq.v1` | `RFQ_CREATED`, `RFQ_UPDATED`, `RFQ_PUBLISHED`, `RFQ_CLOSED`, `RFQ_AWARD_SELECTED` |
| `procurax.quotation.v1` | `QUOTATION_SUBMITTED`, `QUOTATION_UPDATED`, `QUOTATION_STATUS_CHANGED` |
| `procurax.vendor.v1` | `VENDOR_CREATED`, `VENDOR_UPDATED`, `VENDOR_MEMBER_ADDED`, `VENDOR_DOCUMENT_REGISTERED`, `VENDOR_DOCUMENT_UPLOADED`, `VENDOR_DOCUMENT_VERIFIED`, `VENDOR_DOCUMENT_REJECTED` |
| `procurax.policy.v1` | `POLICY_RULE_CREATED`, `POLICY_RULE_UPDATED`, `POLICY_EVALUATED` |
| `procurax.approval.v1` | `APPROVAL_REQUESTED`, `APPROVAL_APPROVED`, `APPROVAL_REJECTED` |
| `procurax.purchase-order.v1` | `PURCHASE_ORDER_ISSUED` |
| `procurax.payment.v1` | `PAYMENT_MANDATE_CREATED`, `PAYMENT_MANDATE_REVOKED`, `PAYMENT_AUTHORIZED`, `PAYMENT_CAPTURED`, `PAYMENT_REFUNDED` |
| `procurax.fulfillment.v1` | `SHIPMENT_RECORDED`, `SHIPMENT_DELIVERED`, `SHIPMENT_EXCEPTION`, `INVOICE_RECORDED`, `RECONCILIATION_MATCHED`, `RECONCILIATION_EXCEPTION` |

The envelope contains `eventId`, `eventType`, `occurredAt`, `organizationId`, `aggregateType`, `aggregateId`, `correlationId`, optional `causationId`, and `payload`. The aggregate ID is the Kafka key. The dispatcher will not claim a later pending event while an earlier event for that aggregate is still pending, preserving aggregate ordering across concurrent dispatcher instances and retries.

Spring's realtime listener consumes the versioned business-topic pattern and feeds authenticated `/api/v1/events` SSE sessions. It routes only by the organization in the trusted Kafka envelope and emits event metadata (`eventId`, type, time, aggregate type and ID), never the payload. The subscriber derives its organization from the authenticated Spring session and needs `EVENT_STREAM_READ`; vendor sessions are not granted that permission. SSE is best-effort for connected clients, not a durable consumer: reconnecting clients must refetch state. The default Kafka consumer group is hostname-specific so each application instance gets a copy for its local subscribers; a configured override must remain unique per instance.

## Delivery and replay behavior

- Outbox and audit rows are inserted atomically with the domain transaction. A rollback leaves neither row.
- The dispatcher sends to the configured topic and updates `PUBLISHED` only after a broker acknowledgement.
- Failures retain the row as `PENDING`, store a bounded error, and apply capped exponential backoff.
- After `OUTBOX_MAX_ATTEMPTS` failed sends, the next delivery cycle sends to `<topic>.DLT`. A failed DLT send remains retryable; successful DLT delivery marks the row `DEAD_LETTERED`.
- `processed_events` has a `(consumer, event_id)` primary key. Call `ProcessedEventStore.claim` inside the same transaction as a consumer's database effects; it returns false for a duplicate delivery.

Configuration is under `procurax.outbox` (`OUTBOX_ENABLED`, `OUTBOX_POLL_INTERVAL_MS`, `OUTBOX_BATCH_SIZE`, `OUTBOX_MAX_ATTEMPTS`, `OUTBOX_RETRY_DELAY_SECONDS`, and `OUTBOX_SEND_TIMEOUT_SECONDS`). Set `OUTBOX_ENABLED=false` to pause dispatch without stopping business writes; queued rows remain in PostgreSQL for later delivery. Kafka producer idempotence and `acks=all` are enabled.

The implementation emits RFQ, quotation, vendor, vendor-document, policy, approval, purchase-order, sandbox payment, shipment, invoice and reconciliation lifecycle events. Payment events represent internal sandbox transitions, not instructions to a live processor. Reconciliation events describe point-in-time matching against the internal sandbox ledger, not bank settlement. Durable business/workflow Kafka consumers, broker ACL/TLS, a multi-broker production topology, event schema registry/compatibility policy, and consumer retry topics remain unimplemented. Those are deployment and later integration work, not implied by the local Compose Kafka stack.
