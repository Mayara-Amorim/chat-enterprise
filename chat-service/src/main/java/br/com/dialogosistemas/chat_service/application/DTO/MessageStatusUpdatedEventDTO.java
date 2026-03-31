package br.com.dialogosistemas.chat_service.application.DTO;

import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;

import java.time.Instant;
import java.util.UUID;

public record MessageStatusUpdatedEventDTO(
        UUID messageId,
        UUID conversationId,
        MessageStatus status,
        UUID readerId,
        Instant readAt
) {
}