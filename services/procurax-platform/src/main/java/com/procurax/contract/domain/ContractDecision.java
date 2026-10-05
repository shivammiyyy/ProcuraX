package com.procurax.contract.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contract_decisions")
public class ContractDecision extends BaseEntity {

    @Column(name = "contract_id", nullable = false, updatable = false)
    private UUID contractId;

    @Column(nullable = false, length = 20, updatable = false)
    private String decision;

    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String comment;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected ContractDecision() {
        super();
    }

    public ContractDecision(UUID organizationId, UUID contractId, String decision, String comment) {
        super(organizationId);
        this.contractId = contractId;
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = Instant.now();
    }

    public UUID getContractId() { return contractId; }
    public String getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getDecidedAt() { return decidedAt; }
}
