package br.com.dialogosistemas.chat_service.application.DTO;

import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import java.time.Instant;
import java.util.UUID;

public record MessageDTO(
        UUID id,
        String content,
        String senderId,
        Instant sentAt,
        String status
) {
    public static MessageDTO fromEntity(MessageEntity entity) {
        return new MessageDTO(
                entity.getId(),
                entity.getContent(),
                entity.getSenderId().toString(),
                entity.getCreatedAt(),
                entity.getStatus().name()
        );
    }
}