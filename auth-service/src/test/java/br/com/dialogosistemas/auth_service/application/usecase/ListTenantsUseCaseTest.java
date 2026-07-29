package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.TenantSummaryDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.shared_kernel.domain.exception.InvalidInputException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListTenantsUseCaseTest {

    @Test
    void deve_listar_tenants_ativos_e_inativos() {
        Tenant ativo = tenant("Ativo", "hash-a", true);
        Tenant inativo = tenant("Inativo", "hash-b", false);
        ListTenantsUseCase useCase = new ListTenantsUseCase(new SpyGateway(List.of(ativo, inativo)));

        List<TenantSummaryDTO> resultado = useCase.execute(0, 50);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(TenantSummaryDTO::active));
        assertFalse(resultado.stream().allMatch(TenantSummaryDTO::active));
    }

    @Test
    void deve_expor_id_nome_situacao_e_criacao() {
        TenantId id = new TenantId(UUID.randomUUID());
        Instant criadoEm = Instant.parse("2026-07-29T12:00:00Z");
        ListTenantsUseCase useCase = new ListTenantsUseCase(
                new SpyGateway(List.of(new Tenant(id, "SESI Nacional", "hash", true, criadoEm))));

        TenantSummaryDTO dto = useCase.execute(0, 50).getFirst();

        assertEquals(id.value(), dto.tenantId());
        assertEquals("SESI Nacional", dto.name());
        assertTrue(dto.active());
        assertEquals(criadoEm, dto.createdAt());
    }

    @Test
    void nao_deve_expor_o_hash_da_api_key_em_nenhum_campo() {
        // Listagem e exatamente onde vazamento de credencial passa despercebido: o hash existe no
        // dominio e um record com "todos os campos" o carregaria junto sem ninguem notar.
        String hashSecreto = "hash-que-nao-pode-vazar";
        ListTenantsUseCase useCase = new ListTenantsUseCase(
                new SpyGateway(List.of(tenant("T", hashSecreto, true))));

        String serializado = useCase.execute(0, 50).getFirst().toString();

        assertFalse(serializado.contains(hashSecreto), "o hash da API key vazou na resposta da listagem");
    }

    @Test
    void deve_repassar_pagina_e_tamanho_ao_gateway() {
        SpyGateway gateway = new SpyGateway(List.of());
        new ListTenantsUseCase(gateway).execute(3, 25);

        assertEquals(3, gateway.pageRecebida);
        assertEquals(25, gateway.sizeRecebido);
    }

    @Test
    void deve_rejeitar_size_acima_do_teto() {
        // Sem teto, o cliente anula a protecao pedindo size=999999 — paginacao que so vale
        // quando o chamador coopera nao e protecao.
        ListTenantsUseCase useCase = new ListTenantsUseCase(new SpyGateway(List.of()));

        assertThrows(InvalidInputException.class,
                () -> useCase.execute(0, ListTenantsUseCase.MAX_SIZE + 1));
    }

    @Test
    void deve_rejeitar_pagina_negativa_e_size_invalido() {
        ListTenantsUseCase useCase = new ListTenantsUseCase(new SpyGateway(List.of()));

        assertThrows(InvalidInputException.class, () -> useCase.execute(-1, 50));
        assertThrows(InvalidInputException.class, () -> useCase.execute(0, 0));
    }

    @Test
    void deve_retornar_lista_vazia_quando_nao_ha_tenants() {
        assertTrue(new ListTenantsUseCase(new SpyGateway(List.of())).execute(0, 50).isEmpty());
    }

    private static Tenant tenant(String nome, String hash, boolean ativo) {
        return new Tenant(new TenantId(UUID.randomUUID()), nome, hash, ativo, Instant.now());
    }

    private static final class SpyGateway implements TenantGateway {
        private final List<Tenant> tenants;
        int pageRecebida = -1;
        int sizeRecebido = -1;

        SpyGateway(List<Tenant> tenants) {
            this.tenants = tenants;
        }

        @Override public Tenant save(Tenant tenant) { return tenant; }
        @Override public Optional<Tenant> findById(TenantId id) { return Optional.empty(); }
        @Override public Optional<Tenant> findByApiKeyHash(String apiKeyHash) { return Optional.empty(); }

        @Override
        public List<Tenant> findAll(int page, int size) {
            this.pageRecebida = page;
            this.sizeRecebido = size;
            return tenants;
        }
    }
}
