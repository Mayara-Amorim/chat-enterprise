package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para provisionar ou atualizar um usuario")
public record ProvisionUserRequestDTO(
        @Schema(description = "ID do usuario no sistema externo", example = "prof-123")
        @NotNull String externalId,
        @Schema(description = "Nome de exibicao do usuario", example = "Joao Silva")
        @NotNull String name,
        // email segue OPCIONAL — a descricao ja dizia isso e o dominio aceita nulo
        @Schema(description = "Email do usuario (opcional)", example = "joao@sesi.org.br")
        String email,
        @Schema(description = "Papel do usuario no sistema", example = "MEDIADOR",
                allowableValues = {"MEDIADOR", "USUARIO_COMUM"})
        String role
) {}
