package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.List;
import java.util.UUID;

public record ConfirmUploadRequestDTO(
        UUID conversationId,
        List<UUID> uploadIds,
        String caption
) {}
