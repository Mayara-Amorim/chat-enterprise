package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeactivateTenantUseCaseTest {

    @Test
    void deve_desativar_tenant() {
        TenantId id = new TenantId(UUID.randomUUID());
        CapturingGateway gateway = new CapturingGateway(ativo(id));
        DeactivateTenantUseCase useCase = new DeactivateTenantUseCase(gateway);

        useCase.execute(id);

        assertNotNull(gateway.saved, "o tenant precisa ser persistido apos desativar");
        assertFalse(gateway.saved.isActive());
    }

    @Test
    void deve_lancar_excecao_quando_tenant_nao_existe() {
        CapturingGateway gateway = new CapturingGateway(null);
        DeactivateTenantUseCase useCase = new DeactivateTenantUseCase(gateway);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new TenantId(UUID.randomUUID())));
    }

    @Test
    void deve_ser_idempotente_quando_tenant_ja_esta_inativo() {
        // Desativar duas vezes nao pode virar erro: quem limpa ambiente costuma repetir o comando,
        // e 404/409 na segunda chamada obrigaria o chamador a tratar um caso sem consequencia.
        TenantId id = new TenantId(UUID.randomUUID());
        Tenant inativo = new Tenant(id, "ja-inativo", "hash", false, Instant.now());
        CapturingGateway gateway = new CapturingGateway(inativo);
        DeactivateTenantUseCase useCase = new DeactivateTenantUseCase(gateway);

        useCase.execute(id);

        assertNull(gateway.saved, "tenant ja inativo nao deve gerar escrita desnecessaria");
    }

    private static Tenant ativo(TenantId id) {
        return new Tenant(id, "tenant-de-teste", "hash", true, Instant.now());
    }

    private static final class CapturingGateway implements TenantGateway {
        private final Tenant tenant;
        Tenant saved;

        CapturingGateway(Tenant tenant) {
            this.tenant = tenant;
        }

        @Override public Tenant save(Tenant tenant) { this.saved = tenant; return tenant; }
        @Override public Optional<Tenant> findById(TenantId id) { return Optional.ofNullable(tenant); }
        @Override public Optional<Tenant> findByApiKeyHash(String apiKeyHash) { return Optional.empty(); }
        @Override public java.util.List<Tenant> findAll(int page, int size) { return java.util.List.of(); }
    }
}
