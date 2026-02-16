package br.com.dialogosistemas.chat_service.infra.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants")
public class ConversationParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    public ConversationParticipantEntity() {}

    public ConversationParticipantEntity(UUID userId, Integer unreadCount, Instant lastReadAt) {
        this.userId = userId;
        this.unreadCount = unreadCount != null ? unreadCount : 0;
        this.lastReadAt = lastReadAt;
    }

    // Método utilitário para o Hibernate gerenciar relacionamento bidirecional
    public void assignConversation(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public ConversationEntity getConversation() { return conversation; }
    public void setConversation(ConversationEntity conversation) { this.conversation = conversation; }
    public Integer getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }
    public Instant getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(Instant lastReadAt) { this.lastReadAt = lastReadAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationParticipantEntity that = (ConversationParticipantEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}