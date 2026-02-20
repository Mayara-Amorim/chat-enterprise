package br.com.dialogosistemas.chat_service.domain.model.message;

import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Message {

    private final MessageId id;
    private final UserId senderId;
    private final String content;
    private final Instant createdAt;
    private MessageStatus status;
    private final Set<MessageReadReceipt> readReceipts;

    // Construtor completo para o Mapper
    public Message(MessageId id, UserId senderId, String content, Instant createdAt, MessageStatus status, Set<MessageReadReceipt> readReceipts) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.status = status;
        this.readReceipts = readReceipts != null ? new HashSet<>(readReceipts) : new HashSet<>();
    }

    // Factory de criação
    public static Message create(UserId senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        return new Message(
                new MessageId(UUID.randomUUID()),
                senderId,
                content,
                Instant.now(),
                MessageStatus.SENT,
                new HashSet<>()
        );
    }

    // Marcar como lida
    public void markAsReadBy(UserId readerId, int totalParticipantsInConversation) {
        if (this.senderId.equals(readerId)) return; // O remetente não gera recibo para si mesmo

        boolean isNewReceipt = this.readReceipts.add(new MessageReadReceipt(readerId, Instant.now()));

        if (isNewReceipt) {
            // Se todos os outros participantes leram, a mensagem está totalmente lida
            if (this.readReceipts.size() >= (totalParticipantsInConversation - 1)) {
                this.status = MessageStatus.READ;
            } else if (this.status == MessageStatus.SENT) {
                // Se pelo menos um leu num grupo, garantimos que foi entregue
                this.status = MessageStatus.DELIVERED;
            }
        }
    }

    public MessageId getId() { return id; }
    public UserId getSenderId() { return senderId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public MessageStatus getStatus() { return status; }
    public Set<MessageReadReceipt> getReadReceipts() { return Collections.unmodifiableSet(readReceipts); }
}