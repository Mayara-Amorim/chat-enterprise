package br.com.dialogosistemas.chat_service.domain.model.upload;

import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.UUID;

public class PendingUpload {

    private final UUID id;
    private final ConversationId conversationId;
    private final UserId userId;
    private final String originalFileName;
    private final String contentType;
    private final long sizeInBytes;
    private final String storagePath;
    private final Instant createdAt;
    private final Instant expiresAt;

    public PendingUpload(UUID id, ConversationId conversationId, UserId userId,
                         String originalFileName, String contentType, long sizeInBytes,
                         String storagePath, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeInBytes = sizeInBytes;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static PendingUpload create(ConversationId conversationId, UserId userId,
                                       String originalFileName, String contentType,
                                       long sizeInBytes, String storagePath, int ttlMinutes) {
        Instant now = Instant.now();
        return new PendingUpload(
                UUID.randomUUID(), conversationId, userId,
                originalFileName, contentType, sizeInBytes, storagePath,
                now, now.plusSeconds(ttlMinutes * 60L)
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public UUID getId() { return id; }
    public ConversationId getConversationId() { return conversationId; }
    public UserId getUserId() { return userId; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getSizeInBytes() { return sizeInBytes; }
    public String getStoragePath() { return storagePath; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
