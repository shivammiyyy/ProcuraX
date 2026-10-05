package com.procurax.rfq.web;

import com.procurax.rfq.domain.RfqItem;
import java.util.UUID;

public record RfqItemResponse(UUID id, String description, int quantity, String unit, String specification) {

    public static RfqItemResponse from(RfqItem item) {
        return new RfqItemResponse(item.getId(), item.getDescription(), item.getQuantity(),
                item.getUnit(), item.getSpecification());
    }
}
