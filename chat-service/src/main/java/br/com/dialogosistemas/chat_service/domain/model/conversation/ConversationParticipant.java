package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.Objects;

public class ConversationParticipant {

    private final UserId userId;
    private Integer unreadCount;
    private Instant lastReadAt;
    private final ParticipantRole role;


    // Construtor para novos participantes
    public ConversationParticipant(UserId userId) {
        this(userId, 0, null, ParticipantRole.MEMBER);
    }


    public ConversationParticipant(UserId userId, Integer unreadCount, Instant lastReadAt, ParticipantRole role) {
        this.userId = userId;
        this.unreadCount = unreadCount != null ? unreadCount : 0;
        this.lastReadAt = lastReadAt;
        this.role = role != null ? role : ParticipantRole.MEMBER;
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public static ConversationParticipant create(UserId userId, ParticipantRole role) {
        return new ConversationParticipant(userId, 0, null, role);
    }

    public void markAsRead() {
        this.unreadCount = 0;
        this.lastReadAt = Instant.now();
    }

    // Getters
    public UserId getUserId() { return userId; }
    public Integer getUnreadCount() { return unreadCount; }
    public Instant getLastReadAt() { return lastReadAt; }
    public ParticipantRole getRole() { return role; }

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
