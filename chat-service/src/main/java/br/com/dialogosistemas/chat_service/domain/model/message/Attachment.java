package br.com.dialogosistemas.chat_service.domain.model.message;

import java.time.Instant;
import java.util.UUID;

public record Attachment(
        UUID fileId,
        String originalFileName,
        String contentType,
        long sizeInBytes,
        String storagePath,
        Instant uploadedAt
) {
    public Attachment {
        if (fileId == null) throw new IllegalArgumentException("fileId cannot be null");
        if (originalFileName == null || originalFileName.isBlank()) throw new IllegalArgumentException("originalFileName cannot be empty");
        if (contentType == null || contentType.isBlank()) throw new IllegalArgumentException("contentType cannot be empty");
        if (sizeInBytes <= 0) throw new IllegalArgumentException("sizeInBytes must be positive");
        if (storagePath == null || storagePath.isBlank()) throw new IllegalArgumentException("storagePath cannot be empty");
        if (uploadedAt == null) throw new IllegalArgumentException("uploadedAt cannot be null");
    }
}
