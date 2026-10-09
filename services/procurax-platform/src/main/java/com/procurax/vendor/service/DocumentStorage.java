package com.procurax.vendor.service;

import java.io.IOException;
import java.time.Instant;

public interface DocumentStorage {

    StoredDocument upload(byte[] content, String fileName, String resourceType, String folder,
                          String deliveryType) throws IOException;

    void delete(String publicId, String resourceType, String deliveryType) throws IOException;

    String createDownloadUrl(String publicId, String format, String resourceType,
                             String deliveryType, String fileName, Instant expiresAt) throws IOException;
}
