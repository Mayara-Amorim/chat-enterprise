package br.com.dialogosistemas.chat_service.application.DTO;

import java.time.Instant;
import java.util.UUID;

public record MessageEditedEventDTO(
        UUID messageId,
        UUID conversationId,
        UUID editedBy,
        String content,
        Instant editedAt
) {
}
