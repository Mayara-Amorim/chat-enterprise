package br.com.dialogosistemas.chat_service.domain.model.message;

import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Message {

    private final MessageId id;
    private final ConversationId conversationId;
    private final UserId senderId;
    private String content;
    private final Instant createdAt;
    private MessageStatus status;
    private final Set<MessageReadReceipt> readReceipts;
    private Instant editedAt;
    private Instant deletedAt;
    private UserId deletedBy;

    public Message(MessageId id,
                   ConversationId conversationId,
                   UserId senderId,
                   String content,
                   Instant createdAt,
                   MessageStatus status,
                   Set<MessageReadReceipt> readReceipts) {
        this(id, conversationId, senderId, content, createdAt, status, readReceipts, null, null, null);
    }

    public Message(MessageId id,
                   ConversationId conversationId,
                   UserId senderId,
                   String content,
                   Instant createdAt,
                   MessageStatus status,
                   Set<MessageReadReceipt> readReceipts,
                   Instant deletedAt,
                   UserId deletedBy) {
        this(id, conversationId, senderId, content, createdAt, status, readReceipts, null, deletedAt, deletedBy);
    }

    public Message(MessageId id,
                   ConversationId conversationId,
                   UserId senderId,
                   String content,
                   Instant createdAt,
                   MessageStatus status,
                   Set<MessageReadReceipt> readReceipts,
                   Instant editedAt,
                   Instant deletedAt,
                   UserId deletedBy) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.status = status;
        this.readReceipts = readReceipts != null ? new HashSet<>(readReceipts) : new HashSet<>();
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }

    public static Message create(ConversationId conversationId, UserId senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        return new Message(
                new MessageId(UUID.randomUUID()),
                conversationId,
                senderId,
                content,
                Instant.now(),
                MessageStatus.SENT,
                new HashSet<>()
        );
    }

    public void markAsReadBy(UserId readerId, int totalParticipantsInConversation) {
        if (this.senderId.equals(readerId)) {
            return;
        }

        boolean isNewReceipt = this.readReceipts.add(new MessageReadReceipt(readerId, Instant.now()));

        if (isNewReceipt) {
            if (this.readReceipts.size() >= (totalParticipantsInConversation - 1)) {
                this.status = MessageStatus.READ;
            } else if (this.status == MessageStatus.SENT) {
                this.status = MessageStatus.DELIVERED;
            }
        }
    }

    public void edit(String newContent, UserId requesterId) {
        if (this.deletedAt != null) {
            throw new IllegalStateException("A mensagem apagada não pode ser editada.");
        }
        if (!this.senderId.equals(requesterId)) {
            throw new IllegalArgumentException("Acesso negado: Apenas o autor original pode editar esta mensagem.");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        this.content = newContent;
        this.editedAt = Instant.now();
    }

    public void deleteBy(UserId requesterId, boolean isRequesterAdmin) {
        if (this.deletedAt != null) {
            throw new IllegalStateException("A mensagem já se encontra apagada.");
        }

        boolean isAuthor = this.senderId.equals(requesterId);
        if (!isAuthor && !isRequesterAdmin) {
            throw new IllegalArgumentException("Acesso negado: Apenas o autor ou um administrador podem apagar esta mensagem.");
        }

        this.deletedAt = Instant.now();
        this.deletedBy = requesterId;
    }

    public MessageId getId() { return id; }
    public ConversationId getConversationId() { return conversationId; }
    public UserId getSenderId() { return senderId; }
    public String getContent() { return deletedAt != null ? null : content; }
    public String getStoredContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public MessageStatus getStatus() { return status; }
    public Set<MessageReadReceipt> getReadReceipts() { return Collections.unmodifiableSet(readReceipts); }
    public Instant getEditedAt() { return editedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public UserId getDeletedBy() { return deletedBy; }
}
