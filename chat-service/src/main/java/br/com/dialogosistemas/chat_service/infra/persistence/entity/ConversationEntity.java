package br.com.dialogosistemas.chat_service.infra.persistence.entity;

import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConversationType type;

    @Column(name = "title")
    private String title;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_message_content")
    private String lastMessageContent;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MessageEntity> messages = new ArrayList<>();

    // Mapeando corretamente para a entidade de banco, e não a de domínio
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConversationParticipantEntity> participants = new HashSet<>();

    public ConversationEntity() {}

    public ConversationEntity(UUID id, UUID tenantId, ConversationType type, String title, UUID creatorId, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.title = title;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
    }

    public void addParticipant(ConversationParticipantEntity participantEntity) {
        this.participants.add(participantEntity);
        participantEntity.assignConversation(this);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public ConversationType getType() { return type; }
    public void setType(ConversationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public UUID getCreatorId() { return creatorId; }
    public void setCreatorId(UUID creatorId) { this.creatorId = creatorId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<MessageEntity> getMessages() { return messages; }
    public void setMessages(List<MessageEntity> messages) { this.messages = messages; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public String getLastMessageContent() { return lastMessageContent; }
    public void setLastMessageContent(String lastMessageContent) { this.lastMessageContent = lastMessageContent; }
    public Set<ConversationParticipantEntity> getParticipants() { return participants; }
    public void setParticipants(Set<ConversationParticipantEntity> participants) { this.participants = participants; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationEntity that = (ConversationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}