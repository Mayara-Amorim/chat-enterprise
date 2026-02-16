package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.Objects;

public class ConversationParticipant {

    private final UserId userId;
    private Integer unreadCount;
    private Instant lastReadAt;

    // Construtor para novos participantes
    public ConversationParticipant(UserId userId) {
        this.userId = userId;
        this.unreadCount = 0;
        this.lastReadAt = Instant.now();
    }


    public ConversationParticipant(UserId userId, Integer unreadCount, Instant lastReadAt) {
        this.userId = userId;
        this.unreadCount = unreadCount;
        this.lastReadAt = lastReadAt;
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public void markAsRead() {
        this.unreadCount = 0;
        this.lastReadAt = Instant.now();
    }

    // Getters
    public UserId getUserId() { return userId; }
    public Integer getUnreadCount() { return unreadCount; }
    public Instant getLastReadAt() { return lastReadAt; }

    // Equals e HashCode baseados na identidade do usuário nesta conversa
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationParticipant that = (ConversationParticipant) o;
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}