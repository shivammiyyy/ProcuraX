package com.procurax.rfq.service;

import com.procurax.common.correlation.CorrelationIdFilter;
import com.procurax.common.error.BusinessException;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.rfq.domain.Rfq;
import com.procurax.rfq.domain.RfqItem;
import com.procurax.rfq.repository.RfqItemRepository;
import com.procurax.rfq.repository.RfqRepository;
import com.procurax.rfq.web.CreateRfqRequest;
import com.procurax.rfq.web.RfqItemRequest;
import com.procurax.rfq.web.RfqItemResponse;
import com.procurax.rfq.web.RfqResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RfqService {

    private final RfqRepository rfqRepository;
    private final RfqItemRepository itemRepository;
    private final OrganizationContext organizationContext;
    private final OutboxEventWriter eventWriter;

    public RfqService(RfqRepository rfqRepository, RfqItemRepository itemRepository,
                      OrganizationContext organizationContext, OutboxEventWriter eventWriter) {
        this.rfqRepository = rfqRepository;
        this.itemRepository = itemRepository;
        this.organizationContext = organizationContext;
        this.eventWriter = eventWriter;
    }

    @Transactional
    public RfqResponse create(CreateRfqRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        Rfq rfq = new Rfq(organizationId, request.title().trim(), request.description().trim(),
                request.requestType(), request.category().trim(), request.budget(), request.currency(),
                request.deadline(), request.deliveryDays(), currentCorrelationId());
        rfq.setCreatedBy(organizationContext.currentPrincipal().getUserId());
        Rfq saved = rfqRepository.save(rfq);
        saveItems(organizationId, saved.getId(), request.items());
        eventWriter.record("RFQ", saved.getId(), organizationId, "RFQ_CREATED", "procurax.rfq.v1",
                saved.getCorrelationId(), Map.of("status", saved.getStatus(), "title", saved.getTitle(),
                        "category", saved.getCategory(), "itemCount", request.items().size()));
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<RfqResponse> list(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        var results = "VENDOR".equals(principal.getRole())
                ? rfqRepository.findAllByOrganizationIdAndStatus(organizationId, "PUBLISHED",
                        PageRequest.of(page, size))
                : rfqRepository.findAllByOrganizationId(organizationId, PageRequest.of(page, size));
        return results.getContent().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public RfqResponse get(UUID rfqId) {
        Rfq rfq = requireRfq(rfqId);
        if ("VENDOR".equals(organizationContext.currentPrincipal().getRole())
                && !"PUBLISHED".equals(rfq.getStatus())) {
            throw notFound();
        }
        return response(rfq);
    }

    @Transactional
    public RfqResponse updateDraft(UUID rfqId, CreateRfqRequest request) {
        Rfq rfq = requireRfq(rfqId);
        if (!"DRAFT".equals(rfq.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "RFQ_NOT_EDITABLE",
                    "Only a draft RFQ can be edited");
        }
        rfq.updateDraft(request.title().trim(), request.description().trim(), request.requestType(),
                request.category().trim(), request.budget(), request.currency(), request.deadline(),
                request.deliveryDays());
        rfq.setUpdatedBy(organizationContext.currentPrincipal().getUserId());
        rfqRepository.save(rfq);
        UUID organizationId = rfq.getOrganizationId();
        itemRepository.deleteAllByOrganizationIdAndRfqId(organizationId, rfqId);
        saveItems(organizationId, rfqId, request.items());
        eventWriter.record("RFQ", rfq.getId(), organizationId, "RFQ_UPDATED", "procurax.rfq.v1",
                rfq.getCorrelationId(), Map.of("status", rfq.getStatus(), "title", rfq.getTitle(),
                        "category", rfq.getCategory(), "itemCount", request.items().size()));
        return response(rfq);
    }

    @Transactional
    public RfqResponse publish(UUID rfqId) {
        Rfq rfq = requireRfq(rfqId);
        try {
            rfq.publish();
        } catch (IllegalStateException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "RFQ_CANNOT_PUBLISH", ex.getMessage());
        }
        rfq.setUpdatedBy(organizationContext.currentPrincipal().getUserId());
        Rfq saved = rfqRepository.save(rfq);
        eventWriter.record("RFQ", saved.getId(), saved.getOrganizationId(), "RFQ_PUBLISHED",
                "procurax.rfq.v1", saved.getCorrelationId(),
                Map.of("status", saved.getStatus(), "deadline", saved.getDeadline()));
        return response(saved);
    }

    @Transactional
    public RfqResponse close(UUID rfqId) {
        Rfq rfq = requireRfq(rfqId);
        if (!"PUBLISHED".equals(rfq.getStatus()) && !"IN_PROGRESS".equals(rfq.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "RFQ_CANNOT_CLOSE",
                    "Only a published or in-progress RFQ can be closed");
        }
        rfq.close();
        rfq.setUpdatedBy(organizationContext.currentPrincipal().getUserId());
        Rfq saved = rfqRepository.save(rfq);
        eventWriter.record("RFQ", saved.getId(), saved.getOrganizationId(), "RFQ_CLOSED",
                "procurax.rfq.v1", saved.getCorrelationId(), Map.of("status", saved.getStatus()));
        return response(saved);
    }

    @Transactional
    public void markInProgress(UUID rfqId, UUID actorId) {
        Rfq rfq = requireRfq(rfqId);
        try {
            rfq.markInProgress();
        } catch (IllegalStateException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "RFQ_NOT_SELECTABLE", ex.getMessage());
        }
        rfq.setUpdatedBy(actorId);
        rfqRepository.save(rfq);
        eventWriter.record("RFQ", rfq.getId(), rfq.getOrganizationId(), "RFQ_AWARD_SELECTED",
                "procurax.rfq.v1", rfq.getCorrelationId(), actorId, Map.of("status", rfq.getStatus()));
    }

    @Transactional(readOnly = true)
    public Rfq requireRfq(UUID rfqId) {
        return rfqRepository.findByIdAndOrganizationId(rfqId, organizationContext.currentOrganizationId())
                .orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public List<RfqItem> items(UUID rfqId) {
        return itemRepository.findAllByOrganizationIdAndRfqIdOrderByCreatedAt(
                organizationContext.currentOrganizationId(), rfqId);
    }

    private void saveItems(UUID organizationId, UUID rfqId, List<RfqItemRequest> items) {
        List<RfqItem> entities = items.stream()
                .map(item -> new RfqItem(organizationId, rfqId, item.description().trim(), item.quantity(),
                        item.unit(), item.specification()))
                .toList();
        itemRepository.saveAll(entities);
    }

    private RfqResponse response(Rfq rfq) {
        List<RfqItemResponse> items = itemRepository
                .findAllByOrganizationIdAndRfqIdOrderByCreatedAt(rfq.getOrganizationId(), rfq.getId())
                .stream().map(RfqItemResponse::from).toList();
        return RfqResponse.from(rfq, items);
    }

    private UUID currentCorrelationId() {
        String id = CorrelationIdFilter.current();
        return id == null ? UUID.randomUUID() : UUID.fromString(id);
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "RFQ_NOT_FOUND", "RFQ not found");
    }
}
