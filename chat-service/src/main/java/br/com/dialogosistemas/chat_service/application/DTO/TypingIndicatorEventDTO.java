package br.com.dialogosistemas.chat_service.application.DTO;

import java.time.Instant;
import java.util.UUID;

public record TypingIndicatorEventDTO(
        UUID conversationId,
        UUID userId,
        Instant typingAt
) {}
