package br.com.dialogosistemas.chat_service.application.DTO;

import java.time.Instant;
import java.util.UUID;

public record RequestUploadResponseDTO(
        UUID uploadId,
        String signedUrl,
        Instant expiresAt
) {}
