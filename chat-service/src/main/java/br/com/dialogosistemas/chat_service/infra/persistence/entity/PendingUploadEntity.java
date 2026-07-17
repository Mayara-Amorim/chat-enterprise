package br.com.dialogosistemas.chat_service.infra.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_uploads")
public class PendingUploadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_in_bytes", nullable = false)
    private long sizeInBytes;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public PendingUploadEntity() {}

    public PendingUploadEntity(UUID id, UUID conversationId, UUID userId,
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

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getUserId() { return userId; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getSizeInBytes() { return sizeInBytes; }
    public String getStoragePath() { return storagePath; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
