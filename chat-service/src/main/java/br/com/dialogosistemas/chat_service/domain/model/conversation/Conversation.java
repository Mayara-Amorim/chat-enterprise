package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import com.fasterxml.jackson.annotation.JacksonInject;


import java.time.Instant;
import java.util.*;

public class Conversation {

    private final ConversationId id;
    private final TenantId tenantId;
    private final ConversationType type;
    private String title; // Opcional para INDIVIDUAL
    private final Set<UserId> participants;
    private final List<Message> messages; // Em memória, carrega apenas as recentes (lazy loading na infra)
    private final UserId creatorId;
    private final Instant createdAt;

    // Construtor completo para Reconstituição (via Banco de Dados)
    public Conversation(ConversationId id, TenantId tenantId, ConversationType type, String title, Set<UserId> participants, UserId creatorId, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.title = title;
        this.participants = participants;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.messages = new ArrayList<>();
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
                Instant.now()
        );
    }

    public static Conversation createGroup(TenantId tenantId, UserId creator, String groupTitle, Set<UserId> initialParticipants) {
        if (groupTitle == null || groupTitle.isBlank()) {
            throw new IllegalArgumentException("Group title is required");
        }

        Set<UserId> allParticipants = new HashSet<>(initialParticipants);
        allParticipants.add(creator); // Garante que o criador está no grupo

        return new Conversation(
                new ConversationId(UUID.randomUUID()),
                tenantId,
                ConversationType.GROUP,
                groupTitle,
                allParticipants,
                creator,
                Instant.now()
        );
    }

    public Message addMessage(UserId senderId, String content) {
        if (!participants.contains(senderId)) {
            throw new IllegalStateException("User is not a participant of this conversation");
        }

        Message newMessage = Message.create(senderId, content);
        this.messages.add(newMessage);
        return newMessage;
    }

    // Adicionar Participante (Regra: só em grupos)
    public void addParticipant(UserId requesterId, UserId newParticipant) {
        if (this.type != ConversationType.GROUP) {
            throw new IllegalStateException("Cannot add participants to individual conversation");
        }
        // Futuro: Validar se requester é ADMIN
        this.participants.add(newParticipant);
    }

    public ConversationId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public List<Message> getUnmodifiableMessages() { return Collections.unmodifiableList(messages); }

    public UserId getCreatorId() {
        return creatorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ConversationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Retorna uma visualização imutável dos participantes.
     * Isso impede que classes externas (como o Mapper) adicionem usuários diretamente na lista,
     * furando a validação do método addParticipant().
     */
    public Set<UserId> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }
}