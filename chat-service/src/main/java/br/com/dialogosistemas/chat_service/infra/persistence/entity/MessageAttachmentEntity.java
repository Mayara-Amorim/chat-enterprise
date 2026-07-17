package br.com.dialogosistemas.chat_service.infra.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_attachments")
public class MessageAttachmentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_in_bytes", nullable = false)
    private long sizeInBytes;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public MessageAttachmentEntity() {}

    public MessageAttachmentEntity(UUID id, MessageEntity message, String originalFileName,
                                   String contentType, long sizeInBytes, String storagePath,
                                   Instant uploadedAt) {
        this.id = id;
        this.message = message;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeInBytes = sizeInBytes;
        this.storagePath = storagePath;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() { return id; }
    public MessageEntity getMessage() { return message; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getSizeInBytes() { return sizeInBytes; }
    public String getStoragePath() { return storagePath; }
    public Instant getUploadedAt() { return uploadedAt; }

    public void setMessage(MessageEntity message) { this.message = message; }
}
