package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Conversation {

    private final ConversationId id;
    private final TenantId tenantId;
    private final ConversationType type;
    private String title;
    private String description;
    private final Set<ConversationParticipant> participants;
    private final List<Message> messages;
    private final UserId creatorId;
    private final Instant createdAt;
    private String lastMessagePreview;
    private Instant lastMessageAt;

    public Conversation(ConversationId id,
                        TenantId tenantId,
                        ConversationType type,
                        String title,
                        String description,
                        Instant createdAt,
                        UserId creatorId,
                        Set<ConversationParticipant> participants,
                        String lastMessagePreview,
                        Instant lastMessageAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.creatorId = creatorId;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>();
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageAt = lastMessageAt;
    }

    public static Conversation createIndividual(TenantId tenantId, UserId creator, UserId otherParticipant) {
        Set<ConversationParticipant> participants = new HashSet<>();
        participants.add(ConversationParticipant.create(creator, ParticipantRole.MEMBER));
        participants.add(ConversationParticipant.create(otherParticipant, ParticipantRole.MEMBER));

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.INDIVIDUAL,
                null,
                null,
                Instant.now(),
                creator,
                participants,
                null,
                null
        );
    }

    public static Conversation createGroup(TenantId tenantId,
                                           String title,
                                           String description,
                                           UserId creatorId,
                                           Set<UserId> memberIds) {
        Set<ConversationParticipant> participants = new HashSet<>();
        participants.add(ConversationParticipant.create(creatorId, ParticipantRole.ADMIN));

        memberIds.forEach(memberId -> {
            if (!memberId.equals(creatorId)) {
                participants.add(ConversationParticipant.create(memberId, ParticipantRole.MEMBER));
            }
        });

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.GROUP,
                title,
                description,
                Instant.now(),
                creatorId,
                participants,
                null,
                null
        );
    }

    public Message addMessage(UserId senderId, String content) {
        boolean isParticipant = participants.stream().anyMatch(participant -> participant.getUserId().equals(senderId));
        if (!isParticipant) {
            throw new IllegalStateException("User is not a participant of this conversation");
        }

        Message newMessage = Message.create(this.id, senderId, content);
        this.messages.add(newMessage);
        this.lastMessagePreview = content;
        this.lastMessageAt = newMessage.getCreatedAt();

        this.participants.stream()
                .filter(participant -> !participant.getUserId().equals(senderId))
                .forEach(ConversationParticipant::incrementUnreadCount);

        return newMessage;
    }

    public void markParticipantAsRead(UserId userId) {
        this.participants.stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findFirst()
                .ifPresent(ConversationParticipant::markAsRead);
    }

    public void setTitle(String title) { this.title = title; }
    public List<Message> getMessages() { return messages; }
    public void setLastMessagePreview(String preview) { this.lastMessagePreview = preview; }
    public void setLastMessageAt(Instant at) { this.lastMessageAt = at; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public ConversationId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public UserId getCreatorId() { return creatorId; }
    public Instant getCreatedAt() { return createdAt; }
    public ConversationType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Set<ConversationParticipant> getParticipants() { return Collections.unmodifiableSet(participants); }
}
