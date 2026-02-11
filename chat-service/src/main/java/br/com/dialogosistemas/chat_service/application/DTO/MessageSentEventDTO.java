package br.com.dialogosistemas.chat_service.application.DTO;

import java.time.Instant;
import java.util.UUID;

public record MessageSentEventDTO(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        Instant sentAt
) {}
