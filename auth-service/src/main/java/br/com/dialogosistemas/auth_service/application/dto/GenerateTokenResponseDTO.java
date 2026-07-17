package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT gerado")
public record GenerateTokenResponseDTO(
        @Schema(description = "Token JWT assinado com RS256", example = "eyJhbGciOiJSUzI1NiIs...")
        String accessToken,
        @Schema(description = "Tempo de expiracao em segundos", example = "86400")
        long expiresIn
) {}
