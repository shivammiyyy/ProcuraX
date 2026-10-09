package com.procurax.rfq.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "rfq_items")
public class RfqItem extends BaseEntity {

    @Column(name = "rfq_id", nullable = false, updatable = false)
    private UUID rfqId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 30)
    private String unit;

    @Column(columnDefinition = "text")
    private String specification;

    protected RfqItem() {
        super();
    }

    public RfqItem(UUID organizationId, UUID rfqId, String description, int quantity,
                   String unit, String specification) {
        super(organizationId);
        this.rfqId = rfqId;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.specification = specification;
    }

    public UUID getRfqId() { return rfqId; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getSpecification() { return specification; }
}
