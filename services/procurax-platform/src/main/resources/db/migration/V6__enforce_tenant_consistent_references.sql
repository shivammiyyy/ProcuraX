-- Keep tenant ownership consistent even if a future code path forgets to check it.
ALTER TABLE vendors
    ADD CONSTRAINT uq_vendor_org_id UNIQUE (organization_id, id);
ALTER TABLE rfqs
    ADD CONSTRAINT uq_rfq_org_id UNIQUE (organization_id, id);
ALTER TABLE rfq_items
    ADD CONSTRAINT uq_rfq_item_org_id UNIQUE (organization_id, id);
ALTER TABLE quotations
    ADD CONSTRAINT uq_quotation_org_id UNIQUE (organization_id, id);
ALTER TABLE contracts
    ADD CONSTRAINT uq_contract_org_id UNIQUE (organization_id, id);

ALTER TABLE vendor_memberships
    ADD CONSTRAINT fk_vendor_membership_vendor
        FOREIGN KEY (organization_id, vendor_id) REFERENCES vendors (organization_id, id),
    ADD CONSTRAINT fk_vendor_membership_org_member
        FOREIGN KEY (organization_id, user_id) REFERENCES organization_members (organization_id, user_id);

ALTER TABLE vendor_documents
    ADD CONSTRAINT fk_vendor_document_vendor
        FOREIGN KEY (organization_id, vendor_id) REFERENCES vendors (organization_id, id);

ALTER TABLE rfq_items
    ADD CONSTRAINT fk_rfq_item_parent
        FOREIGN KEY (organization_id, rfq_id) REFERENCES rfqs (organization_id, id);

ALTER TABLE quotations
    ADD CONSTRAINT fk_quotation_rfq
        FOREIGN KEY (organization_id, rfq_id) REFERENCES rfqs (organization_id, id),
    ADD CONSTRAINT fk_quotation_vendor
        FOREIGN KEY (organization_id, vendor_id) REFERENCES vendors (organization_id, id);

ALTER TABLE quotation_items
    ADD CONSTRAINT uq_quotation_item_org_id UNIQUE (organization_id, id),
    ADD CONSTRAINT fk_quotation_item_parent
        FOREIGN KEY (organization_id, quotation_id) REFERENCES quotations (organization_id, id),
    ADD CONSTRAINT fk_quotation_item_rfq_item
        FOREIGN KEY (organization_id, rfq_item_id) REFERENCES rfq_items (organization_id, id);

ALTER TABLE vendor_scores
    ADD CONSTRAINT fk_vendor_score_quote
        FOREIGN KEY (organization_id, quotation_id) REFERENCES quotations (organization_id, id),
    ADD CONSTRAINT fk_vendor_score_vendor
        FOREIGN KEY (organization_id, vendor_id) REFERENCES vendors (organization_id, id);

ALTER TABLE contracts
    ADD CONSTRAINT fk_contract_rfq
        FOREIGN KEY (organization_id, rfq_id) REFERENCES rfqs (organization_id, id),
    ADD CONSTRAINT fk_contract_quote
        FOREIGN KEY (organization_id, quotation_id) REFERENCES quotations (organization_id, id),
    ADD CONSTRAINT fk_contract_vendor
        FOREIGN KEY (organization_id, vendor_id) REFERENCES vendors (organization_id, id);

ALTER TABLE contract_audits
    ADD CONSTRAINT fk_contract_audit_parent
        FOREIGN KEY (organization_id, contract_id) REFERENCES contracts (organization_id, id);
