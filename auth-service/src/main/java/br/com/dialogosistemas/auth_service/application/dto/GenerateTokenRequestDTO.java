package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para gerar um token JWT")
public record GenerateTokenRequestDTO(
        @Schema(description = "ID do usuario no sistema externo", example = "prof-123")
        @NotNull String externalId
) {}
