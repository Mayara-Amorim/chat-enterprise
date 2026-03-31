package br.com.dialogosistemas.chat_service.application.DTO;

import java.time.Instant;
import java.util.UUID;

public record MessageDeletedEventDTO(
        UUID messageId,
        UUID conversationId,
        UUID deletedBy,
        Instant deletedAt
) {
}
