package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para provisionar ou atualizar um usuario")
public record ProvisionUserRequestDTO(
        @Schema(description = "ID do usuario no sistema externo", example = "prof-123")
        String externalId,
        @Schema(description = "Nome de exibicao do usuario", example = "Joao Silva")
        String name,
        @Schema(description = "Email do usuario (opcional)", example = "joao@sesi.org.br")
        String email,
        @Schema(description = "Papel do usuario no sistema", example = "TEACHER")
        String role
) {}
