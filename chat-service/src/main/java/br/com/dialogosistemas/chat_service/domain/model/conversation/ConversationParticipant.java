package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.Objects;

public class ConversationParticipant {

    private final UserId userId;
    private Integer unreadCount;
    private Instant lastReadAt;
    private ParticipantRole role;
    private Instant leftAt;
    private boolean deletedConversation;

    public ConversationParticipant(UserId userId) {
        this(userId, 0, null, ParticipantRole.MEMBER, null, false);
    }

    public ConversationParticipant(UserId userId, Integer unreadCount, Instant lastReadAt, ParticipantRole role) {
        this(userId, unreadCount, lastReadAt, role, null, false);
    }

    public ConversationParticipant(UserId userId, Integer unreadCount, Instant lastReadAt,
                                   ParticipantRole role, Instant leftAt, boolean deletedConversation) {
        this.userId = userId;
        this.unreadCount = unreadCount != null ? unreadCount : 0;
        this.lastReadAt = lastReadAt;
        this.role = role != null ? role : ParticipantRole.MEMBER;
        this.leftAt = leftAt;
        this.deletedConversation = deletedConversation;
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public static ConversationParticipant create(UserId userId, ParticipantRole role) {
        return new ConversationParticipant(userId, 0, null, role, null, false);
    }

    public void markAsRead() {
        this.unreadCount = 0;
        this.lastReadAt = Instant.now();
    }

    public void leave(boolean deleteConversation) {
        this.leftAt = Instant.now();
        this.deletedConversation = deleteConversation;
    }

    public void markLeftAt(Instant instant) {
        this.leftAt = instant;
    }

    public void deleteFromInbox() {
        this.deletedConversation = true;
    }

    public void promoteToAdmin() {
        this.role = ParticipantRole.ADMIN;
    }

    public void demoteToMember() {
        this.role = ParticipantRole.MEMBER;
    }

    public boolean isActive() {
        return this.leftAt == null;
    }

    public boolean hasAdminPrivileges() {
        return this.role == ParticipantRole.OWNER || this.role == ParticipantRole.ADMIN;
    }

    public UserId getUserId() { return userId; }
    public Integer getUnreadCount() { return unreadCount; }
    public Instant getLastReadAt() { return lastReadAt; }
    public ParticipantRole getRole() { return role; }
    public Instant getLeftAt() { return leftAt; }
    public boolean isDeletedConversation() { return deletedConversation; }

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
