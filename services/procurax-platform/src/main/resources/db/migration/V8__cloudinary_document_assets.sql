ALTER TABLE vendor_documents
    ADD COLUMN cloudinary_public_id VARCHAR(255),
    ADD COLUMN cloudinary_resource_type VARCHAR(20),
    ADD CONSTRAINT ck_vendor_document_cloudinary_metadata
        CHECK ((cloudinary_public_id IS NULL) = (cloudinary_resource_type IS NULL)),
    ADD CONSTRAINT ck_vendor_document_cloudinary_resource_type
        CHECK (cloudinary_resource_type IS NULL OR cloudinary_resource_type IN ('image', 'raw'));
