package br.com.dialogosistemas.chat_service.infra.persistence.entity;

import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {@Index(name = "idx_messages_conversation_created", columnList = "conversation_id, created_at DESC")
})
public class MessageEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MessageReadReceiptEntity> readReceipts = new HashSet<>();

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MessageEntity() {}
    public MessageEntity(UUID id, ConversationEntity conversation, UUID senderId, String content, MessageStatus status, Instant createdAt) {
        this.id = id;
        this.conversation = conversation;
        this.senderId = senderId;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ConversationEntity getConversation() { return conversation; }
    public void setConversation(ConversationEntity conversation) { this.conversation = conversation; }

    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public void addReadReceipt(MessageReadReceiptEntity receipt) {
        this.readReceipts.add(receipt);
        receipt.assignMessage(this);
    }

    public Set<MessageReadReceiptEntity> getReadReceipts() {
        return readReceipts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEntity that = (MessageEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}