package br.com.dialogosistemas.chat_service.application.DTO;

import java.time.Instant;
import java.util.UUID;

public record InboxItemDTO(
        UUID conversationId,
        String title,
        String lastMessagePreview,
        Instant lastMessageAt
) {}