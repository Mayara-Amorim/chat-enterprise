package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.UUID;

public record SendMessageRequestDTO(
        UUID conversationId,
        String content
        // Futuro: String type (TEXT, IMAGE, AUDIO)
) {}