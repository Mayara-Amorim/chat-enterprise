package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.shared_kernel.domain.exception.ForbiddenOperationException;
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
    private MessagingPermission messagingPermission;
    private Instant dissolvedAt;

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
        this(id, tenantId, type, title, description, createdAt, creatorId, participants,
                lastMessagePreview, lastMessageAt, MessagingPermission.ALL, null);
    }

    public Conversation(ConversationId id,
                        TenantId tenantId,
                        ConversationType type,
                        String title,
                        String description,
                        Instant createdAt,
                        UserId creatorId,
                        Set<ConversationParticipant> participants,
                        String lastMessagePreview,
                        Instant lastMessageAt,
                        MessagingPermission messagingPermission,
                        Instant dissolvedAt) {
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
        this.messagingPermission = messagingPermission != null ? messagingPermission : MessagingPermission.ALL;
        this.dissolvedAt = dissolvedAt;
    }

    public static Conversation createIndividual(TenantId tenantId, UserId creator, UserId otherParticipant) {
        Set<ConversationParticipant> participants = new HashSet<>();
        participants.add(ConversationParticipant.create(creator, ParticipantRole.MEMBER));
        participants.add(ConversationParticipant.create(otherParticipant, ParticipantRole.MEMBER));

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.INDIVIDUAL,
                null, null,
                Instant.now(),
                creator,
                participants,
                null, null
        );
    }

    public static Conversation createGroup(TenantId tenantId,
                                           String title,
                                           String description,
                                           UserId creatorId,
                                           Set<UserId> memberIds) {
        Set<ConversationParticipant> participants = new HashSet<>();
        participants.add(ConversationParticipant.create(creatorId, ParticipantRole.OWNER));

        memberIds.forEach(memberId -> {
            if (!memberId.equals(creatorId)) {
                participants.add(ConversationParticipant.create(memberId, ParticipantRole.MEMBER));
            }
        });

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.GROUP,
                title, description,
                Instant.now(),
                creatorId,
                participants,
                null, null
        );
    }

    private void requireGroup() {
        if (this.type != ConversationType.GROUP) {
            throw new IllegalArgumentException("Operacao permitida apenas em grupos");
        }
    }

    private void requireNotDissolved() {
        if (this.dissolvedAt != null) {
            throw new IllegalStateException("Este grupo foi dissolvido");
        }
    }

    private ConversationParticipant findActiveParticipant(UserId userId) {
        return this.participants.stream()
                .filter(p -> p.getUserId().equals(userId) && p.isActive())
                .findFirst()
                .orElseThrow(() -> new ForbiddenOperationException("Usuario nao e participante desta conversa"));
    }

    public void addMember(UserId requesterId, UserId newMemberId) {
        requireGroup();
        requireNotDissolved();
        ConversationParticipant requester = findActiveParticipant(requesterId);

        if (!requester.hasAdminPrivileges()) {
            throw new ForbiddenOperationException("Apenas administradores podem realizar esta operacao");
        }

        boolean alreadyActive = this.participants.stream()
                .anyMatch(p -> p.getUserId().equals(newMemberId) && p.isActive());
        if (alreadyActive) {
            throw new IllegalStateException("Usuario ja e participante desta conversa");
        }

        this.participants.add(ConversationParticipant.create(newMemberId, ParticipantRole.MEMBER));
    }

    public void removeMember(UserId requesterId, UserId targetId) {
        requireGroup();
        requireNotDissolved();
        ConversationParticipant requester = findActiveParticipant(requesterId);
        ConversationParticipant target = findActiveParticipant(targetId);

        if (!requester.hasAdminPrivileges()) {
            throw new ForbiddenOperationException("Apenas administradores podem realizar esta operacao");
        }
        if (target.getRole() == ParticipantRole.OWNER) {
            throw new ForbiddenOperationException("O criador do grupo nao pode ser removido");
        }

        target.leave(false);
    }

    public void promoteMember(UserId requesterId, UserId targetId) {
        requireGroup();
        requireNotDissolved();
        ConversationParticipant requester = findActiveParticipant(requesterId);
        ConversationParticipant target = findActiveParticipant(targetId);

        if (requester.getRole() != ParticipantRole.OWNER) {
            throw new ForbiddenOperationException("Apenas o criador do grupo pode realizar esta operacao");
        }

        target.promoteToAdmin();
    }

    public void demoteMember(UserId requesterId, UserId targetId) {
        requireGroup();
        requireNotDissolved();
        ConversationParticipant requester = findActiveParticipant(requesterId);
        ConversationParticipant target = findActiveParticipant(targetId);

        if (requester.getRole() != ParticipantRole.OWNER) {
            throw new ForbiddenOperationException("Apenas o criador do grupo pode realizar esta operacao");
        }

        target.demoteToMember();
    }

    public void leaveGroup(UserId userId, boolean deleteConversation) {
        requireGroup();
        requireNotDissolved();
        ConversationParticipant participant = findActiveParticipant(userId);

        if (participant.getRole() == ParticipantRole.OWNER) {
            dissolve(userId);
            return;
        }

        participant.leave(deleteConversation);
    }

    private void dissolve(UserId ownerId) {
        this.dissolvedAt = Instant.now();
        this.participants.forEach(p -> {
            if (p.isActive()) {
                p.markLeftAt(this.dissolvedAt);
            }
        });
    }

    public void updateMessagingPermission(UserId requesterId, MessagingPermission permission) {
        requireGroup();
        requireNotDissolved();
        ConversationParticipant requester = findActiveParticipant(requesterId);

        if (!requester.hasAdminPrivileges()) {
            throw new ForbiddenOperationException("Apenas administradores podem realizar esta operacao");
        }

        this.messagingPermission = permission;
    }

    public void deleteFromInbox(UserId userId) {
        ConversationParticipant participant = this.participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenOperationException("Usuario nao e participante desta conversa"));

        if (participant.isActive() && this.dissolvedAt == null) {
            throw new IllegalStateException("Saia do grupo antes de apagar a conversa do inbox");
        }

        participant.deleteFromInbox();
    }

    public Message addMessage(UserId senderId, String content) {
        if (this.dissolvedAt != null) {
            throw new ForbiddenOperationException("Nao e possivel enviar mensagens em um grupo dissolvido");
        }

        ConversationParticipant sender = this.participants.stream()
                .filter(p -> p.getUserId().equals(senderId) && p.isActive())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User is not a participant of this conversation"));

        if (this.type == ConversationType.GROUP
                && this.messagingPermission == MessagingPermission.ADMINS_ONLY
                && !sender.hasAdminPrivileges()) {
            throw new ForbiddenOperationException("Apenas administradores podem enviar mensagens neste grupo");
        }

        Message newMessage = Message.create(this.id, senderId, content);
        this.messages.add(newMessage);
        this.lastMessagePreview = content;
        this.lastMessageAt = newMessage.getCreatedAt();

        this.participants.stream()
                .filter(p -> !p.getUserId().equals(senderId) && p.isActive())
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
    public MessagingPermission getMessagingPermission() { return messagingPermission; }
    public Instant getDissolvedAt() { return dissolvedAt; }
}
