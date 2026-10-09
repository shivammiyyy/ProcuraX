package com.procurax.vendor.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vendors")
public class Vendor extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel = "MEDIUM";

    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays = 30;

    @Column(name = "performance_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal performanceScore = BigDecimal.valueOf(50);

    protected Vendor() {
        super();
    }

    public Vendor(UUID organizationId, String name, String contactEmail, String category, int paymentTermsDays) {
        super(organizationId);
        this.name = name;
        this.contactEmail = contactEmail;
        this.category = category;
        this.paymentTermsDays = paymentTermsDays;
    }

    public String getName() { return name; }
    public String getContactEmail() { return contactEmail; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public String getRiskLevel() { return riskLevel; }
    public int getPaymentTermsDays() { return paymentTermsDays; }
    public BigDecimal getPerformanceScore() { return performanceScore; }

    public void update(String name, String contactEmail, String category, int paymentTermsDays) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.category = category;
        this.paymentTermsDays = paymentTermsDays;
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}
