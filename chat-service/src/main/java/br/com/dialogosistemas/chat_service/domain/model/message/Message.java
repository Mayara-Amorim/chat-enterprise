package br.com.dialogosistemas.chat_service.domain.model.message;

import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import java.time.Instant;
import java.util.UUID;
public class Message {

    private final MessageId id;
    private final UserId senderId;
    private final String content; // Futuramente pode ser um ValueObject "MessageContent" para suportar Mídia
    private final Instant createdAt;
    private MessageStatus status;

    private Message(MessageId id, UserId senderId, String content, Instant createdAt, MessageStatus status) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static Message create(UserId senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        return new Message(
                new MessageId(UUID.randomUUID()),
                senderId,
                content,
                Instant.now(),
                MessageStatus.SENT
        );
    }

    public void markAsDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
        }
    }

    public void markAsRead() {
        // Se foi lido, implicitamente foi entregue
        this.status = MessageStatus.READ;
    }

    public MessageId getId() { return id; }
    public UserId getSenderId() { return senderId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public MessageStatus getStatus() { return status; }

}