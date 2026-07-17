package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Usuario provisionado")
public record ProvisionUserResponseDTO(
        @Schema(description = "ID do usuario na plataforma de chat", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,
        @Schema(description = "ID do usuario no sistema externo", example = "prof-123")
        String externalId
) {}
