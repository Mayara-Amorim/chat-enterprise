package br.com.dialogosistemas.chat_service.application.DTO;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ConfirmUploadRequestDTO(
        @NotNull UUID conversationId,
        @NotNull List<UUID> uploadIds,
        String caption
) {}
