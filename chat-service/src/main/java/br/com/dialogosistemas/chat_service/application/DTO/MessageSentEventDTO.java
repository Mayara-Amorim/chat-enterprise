package br.com.dialogosistemas.chat_service.application.DTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageSentEventDTO(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        Instant sentAt,
        List<AttachmentDTO> attachments
) {
    public MessageSentEventDTO(UUID messageId, UUID conversationId, UUID senderId, String content, Instant sentAt) {
        this(messageId, conversationId, senderId, content, sentAt, List.of());
    }
}
