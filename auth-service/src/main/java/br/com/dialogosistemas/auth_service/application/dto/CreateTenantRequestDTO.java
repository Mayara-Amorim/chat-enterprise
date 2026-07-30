package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criacao de um novo tenant")
public record CreateTenantRequestDTO(
        @Schema(description = "Nome do tenant", example = "SESI Nacional")
        @NotNull String name
) {}
