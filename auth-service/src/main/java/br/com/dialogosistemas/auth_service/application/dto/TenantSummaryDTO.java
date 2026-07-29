package br.com.dialogosistemas.auth_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Resumo de tenant para listagem administrativa.
 *
 * NAO expoe o apiKeyHash de proposito. A API key em texto plano so existe no momento da criacao;
 * o hash e material de credencial e nao tem por que sair daqui. Coberto por teste
 * (ListTenantsUseCaseTest.nao_deve_expor_o_hash_da_api_key_em_nenhum_campo).
 */
@Schema(description = "Resumo de um tenant")
public record TenantSummaryDTO(

        @Schema(description = "ID do tenant", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID tenantId,

        @Schema(description = "Nome do tenant", example = "SESI Nacional")
        String name,

        @Schema(description = "Se false, a API key do tenant recebe 401", example = "true")
        boolean active,

        @Schema(description = "Data de criacao (UTC)", example = "2026-07-29T12:00:00Z")
        Instant createdAt
) {}
