package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Tenant criado com sua API key")
public record CreateTenantResponseDTO(
        @Schema(description = "ID do tenant na plataforma", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID tenantId,
        @Schema(description = "API key em texto plano. Armazene com seguranca — nao sera exibida novamente", example = "dGhpcyBpcyBhIHNhbXBsZSBhcGkga2V5")
        String apiKey
) {}
