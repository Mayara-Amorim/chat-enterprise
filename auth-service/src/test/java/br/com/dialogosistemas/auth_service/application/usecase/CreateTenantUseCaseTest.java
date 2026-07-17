package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CreateTenantUseCaseTest {

    @Test
    void deve_criar_tenant_e_retornar_api_key() {
        CapturingTenantGateway gateway = new CapturingTenantGateway();
        CreateTenantUseCase useCase = new CreateTenantUseCase(gateway);

        CreateTenantResponseDTO response = useCase.execute(new CreateTenantRequestDTO("SESI"));

        assertNotNull(response.tenantId());
        assertNotNull(response.apiKey());
        assertFalse(response.apiKey().isBlank());
        assertNotNull(gateway.saved);
        assertEquals("SESI", gateway.saved.getName());
        assertTrue(gateway.saved.isActive());
    }

    @Test
    void deve_lancar_excecao_quando_nome_vazio() {
        CreateTenantUseCase useCase = new CreateTenantUseCase(new CapturingTenantGateway());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new CreateTenantRequestDTO("")));
    }

    private static final class CapturingTenantGateway implements TenantGateway {
        Tenant saved;
        @Override public Tenant save(Tenant tenant) { this.saved = tenant; return tenant; }
        @Override public Optional<Tenant> findById(TenantId id) { return Optional.empty(); }
        @Override public Optional<Tenant> findByApiKeyHash(String hash) { return Optional.empty(); }
    }
}
