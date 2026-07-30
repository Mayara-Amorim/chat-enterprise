package br.com.dialogosistemas.chat_service.application.DTO;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendMessageRequestDTO(
        @NotNull UUID conversationId,
        @NotNull String content
) {}
