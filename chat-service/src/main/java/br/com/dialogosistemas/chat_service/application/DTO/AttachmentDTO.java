package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.UUID;

public record AttachmentDTO(
        UUID fileId,
        String originalFileName,
        String contentType,
        long sizeInBytes
) {}
