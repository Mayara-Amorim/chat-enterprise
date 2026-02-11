package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.*;

public class Conversation {

    private final ConversationId id;
    private final TenantId tenantId;
    private final ConversationType type;
    private String title;
    private final Set<UserId> participants;
    private final List<Message> messages;
    private final UserId creatorId;
    private final Instant createdAt;

    // --- NOVOS CAMPOS (Read Model) ---
    private String lastMessagePreview;
    private Instant lastMessageAt;

    // Construtor completo para Reconstituição (via Mapper/Banco de Dados)
    public Conversation(ConversationId id,
                        TenantId tenantId,
                        ConversationType type,
                        String title,
                        Set<UserId> participants,
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

        // Inicialização dos novos campos
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageAt = lastMessageAt;
    }

    // Factory: Criar Conversa Individual
    public static Conversation createIndividual(TenantId tenantId, UserId creator, UserId otherParticipant) {
        Set<UserId> participants = new HashSet<>();
        participants.add(creator);
        participants.add(otherParticipant);

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.INDIVIDUAL,
                null,
                participants,
                creator,
                Instant.now(),
                null, // Sem mensagem inicial
                null  // Sem data inicial
        );
    }

    // Factory: Criar Grupo
    public static Conversation createGroup(TenantId tenantId, UserId creator, String groupTitle, Set<UserId> initialParticipants) {
        if (groupTitle == null || groupTitle.isBlank()) {
            throw new IllegalArgumentException("Group title is required");
        }

        Set<UserId> allParticipants = new HashSet<>(initialParticipants);
        allParticipants.add(creator);

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.GROUP,
                groupTitle,
                allParticipants,
                creator,
                Instant.now(),
                null, // Sem mensagem inicial
                null  // Sem data inicial
        );
    }

    public Message addMessage(UserId senderId, String content) {
        if (!participants.contains(senderId)) {
            throw new IllegalStateException("User is not a participant of this conversation");
        }

        Message newMessage = Message.create(senderId, content);
        this.messages.add(newMessage);
        this.lastMessagePreview = content;
        this.lastMessageAt = newMessage.getCreatedAt();

        return newMessage;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public ConversationId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public UserId getCreatorId() { return creatorId; }
    public Instant getCreatedAt() { return createdAt; }
    public ConversationType getType() { return type; }
    public String getTitle() { return title; }
    public Set<UserId> getParticipants() { return Collections.unmodifiableSet(participants); }
}