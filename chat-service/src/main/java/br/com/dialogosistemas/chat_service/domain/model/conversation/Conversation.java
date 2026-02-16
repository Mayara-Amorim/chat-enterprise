package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Conversation {

    private final ConversationId id;
    private final TenantId tenantId;
    private final ConversationType type;
    private String title;
    // Mudança: Agora guarda os objetos ricos de participantes
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
                        Set<ConversationParticipant> participants,
                        UserId creatorId,
                        Instant createdAt,
                        String lastMessagePreview,
                        Instant lastMessageAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.title = title;
        this.participants = participants;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.messages = new ArrayList<>();
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageAt = lastMessageAt;
    }

    public static Conversation createIndividual(TenantId tenantId, UserId creator, UserId otherParticipant) {
        Set<ConversationParticipant> parts = new HashSet<>();
        parts.add(new ConversationParticipant(creator));
        parts.add(new ConversationParticipant(otherParticipant));

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.INDIVIDUAL,
                null,
                parts,
                creator,
                Instant.now(),
                null,
                null
        );
    }

    public static Conversation createGroup(TenantId tenantId, UserId creator, String groupTitle, Set<UserId> initialParticipants) {
        if (groupTitle == null || groupTitle.isBlank()) {
            throw new IllegalArgumentException("Group title is required");
        }

        Set<ConversationParticipant> parts = initialParticipants.stream()
                .map(ConversationParticipant::new)
                .collect(Collectors.toSet());
        parts.add(new ConversationParticipant(creator));

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.GROUP,
                groupTitle,
                parts,
                creator,
                Instant.now(),
                null,
                null
        );
    }

    // Regra de Negócio: Adicionar mensagem E incrementar contadores
    public Message addMessage(UserId senderId, String content) {
        boolean isParticipant = participants.stream().anyMatch(p -> p.getUserId().equals(senderId));
        if (!isParticipant) {
            throw new IllegalStateException("User is not a participant of this conversation");
        }

        Message newMessage = Message.create(senderId, content);
        this.messages.add(newMessage);
        this.lastMessagePreview = content;
        this.lastMessageAt = newMessage.getCreatedAt();

        // Incrementa o unread_count de todos os participantes, exceto o remetente
        this.participants.stream()
                .filter(p -> !p.getUserId().equals(senderId))
                .forEach(ConversationParticipant::incrementUnreadCount);

        return newMessage;
    }

    // Regra de Negócio: Marcar como lido para um usuário
    public void markParticipantAsRead(UserId userId) {
        this.participants.stream()
                .filter(p -> p.getUserId().equals(userId))
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
    public Set<ConversationParticipant> getParticipants() { return Collections.unmodifiableSet(participants); }
}