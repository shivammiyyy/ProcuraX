ALTER TABLE contracts
    ADD COLUMN cloudinary_public_id VARCHAR(255),
    ADD COLUMN cloudinary_resource_type VARCHAR(20),
    ADD COLUMN cloudinary_delivery_type VARCHAR(20),
    ADD COLUMN document_file_name VARCHAR(255),
    ADD CONSTRAINT ck_contract_cloudinary_metadata
        CHECK ((cloudinary_public_id IS NULL) = (cloudinary_resource_type IS NULL)
            AND (cloudinary_public_id IS NULL) = (cloudinary_delivery_type IS NULL)),
    ADD CONSTRAINT ck_contract_cloudinary_resource_type
        CHECK (cloudinary_resource_type IS NULL OR cloudinary_resource_type = 'raw'),
    ADD CONSTRAINT ck_contract_cloudinary_delivery_type
        CHECK (cloudinary_delivery_type IS NULL OR cloudinary_delivery_type = 'authenticated');
