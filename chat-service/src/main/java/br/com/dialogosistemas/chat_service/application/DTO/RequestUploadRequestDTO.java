package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.UUID;

public record RequestUploadRequestDTO(
        UUID conversationId,
        String fileName,
        String contentType,
        long sizeInBytes
) {}
