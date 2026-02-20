package br.com.dialogosistemas.chat_service.infra.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "message_read_receipts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}))
public class MessageReadReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    public MessageReadReceiptEntity() {}

    public MessageReadReceiptEntity(UUID userId, Instant readAt) {
        this.userId = userId;
        this.readAt = readAt;
    }

    public void assignMessage(MessageEntity message) {
        this.message = message;
    }

    public UUID getId() { return id; }
    public MessageEntity getMessage() { return message; }
    public UUID getUserId() { return userId; }
    public Instant getReadAt() { return readAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageReadReceiptEntity that = (MessageReadReceiptEntity) o;
        return Objects.equals(userId, that.userId) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, message);
    }
}