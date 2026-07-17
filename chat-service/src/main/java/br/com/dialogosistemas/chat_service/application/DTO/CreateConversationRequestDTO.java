package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.Set;
import java.util.UUID;

public record CreateConversationRequestDTO(
        String type,
        String title,
        String description,
        Set<UUID> participants
) {}
