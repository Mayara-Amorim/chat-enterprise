package br.com.dialogosistemas.chat_service.application.DTO;

import jakarta.validation.constraints.NotNull;

public record EditMessageRequestDTO(
        @NotNull String content
) {
}
