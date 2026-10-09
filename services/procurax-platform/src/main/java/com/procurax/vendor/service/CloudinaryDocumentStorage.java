package com.procurax.vendor.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.procurax.common.error.BusinessException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryDocumentStorage implements DocumentStorage {

    private final Cloudinary cloudinary;

    public CloudinaryDocumentStorage(
            @Value("${procurax.storage.cloudinary.cloud-name:}") String cloudName,
            @Value("${procurax.storage.cloudinary.api-key:}") String apiKey,
            @Value("${procurax.storage.cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()
                ? null
                : new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret, "secure", true));
    }

    @Override
    public StoredDocument upload(byte[] content, String fileName, String resourceType, String folder,
                                 String deliveryType) throws IOException {
        Cloudinary client = client();
        try {
            Map<?, ?> result = client.uploader().upload(content, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", resourceType,
                    "type", deliveryType,
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false,
                    "filename_override", fileName));
            return new StoredDocument(requiredString(result, "secure_url"),
                    requiredString(result, "public_id"), requiredString(result, "resource_type"),
                    requiredString(result, "type"));
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "DOCUMENT_STORAGE_FAILED",
                    "Cloudinary could not store the document", exception);
        }
    }

    @Override
    public void delete(String publicId, String resourceType, String deliveryType) throws IOException {
        try {
            client().uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType, "type", deliveryType));
        } catch (RuntimeException exception) {
            throw new IOException("Cloudinary could not delete the stored document", exception);
        }
    }

    @Override
    public String createDownloadUrl(String publicId, String format, String resourceType,
                                    String deliveryType, String fileName, Instant expiresAt) throws IOException {
        Cloudinary client = client();
        try {
            return client.privateDownload(publicId, format, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "type", deliveryType,
                    "attachment", fileName,
                    "expires_at", expiresAt.getEpochSecond()));
        } catch (Exception exception) {
            throw new IOException("Cloudinary could not generate a document download URL", exception);
        }
    }

    private Cloudinary client() {
        if (cloudinary == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENT_STORAGE_UNAVAILABLE",
                    "Cloudinary document storage is not configured");
        }
        return cloudinary;
    }

    private String requiredString(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Cloudinary response is missing " + key);
        }
        return text;
    }
}
