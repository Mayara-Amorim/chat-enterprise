package br.com.dialogosistemas.chat_service.application.DTO;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequestUploadRequestDTO(
        @NotNull UUID conversationId,
        @NotNull String fileName,
        @NotNull String contentType,
        long sizeInBytes
) {}
