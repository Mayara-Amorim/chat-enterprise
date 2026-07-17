package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserResponseDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.UserGateway;
import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.auth_service.domain.model.UserRole;
import br.com.dialogosistemas.shared_kernel.domain.exception.InvalidInputException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProvisionUserUseCaseTest {

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @Test
    void deve_criar_novo_usuario() {
        CapturingUserGateway gateway = new CapturingUserGateway(null);
        ProvisionUserUseCase useCase = new ProvisionUserUseCase(gateway);

        ProvisionUserResponseDTO response = useCase.execute(tenantId,
                new ProvisionUserRequestDTO("prof-1", "Joao", "joao@sesi.org", "MEDIADOR"));

        assertNotNull(response.userId());
        assertEquals("prof-1", response.externalId());
        assertNotNull(gateway.saved);
        assertEquals("Joao", gateway.saved.getName());
        assertEquals(UserRole.MEDIADOR, gateway.saved.getRole());
    }

    @Test
    void deve_atualizar_usuario_existente() {
        User existing = new User(new UserId(UUID.randomUUID()), tenantId, "prof-1", "Joao", null, UserRole.MEDIADOR, true, Instant.now());
        CapturingUserGateway gateway = new CapturingUserGateway(existing);
        ProvisionUserUseCase useCase = new ProvisionUserUseCase(gateway);

        ProvisionUserResponseDTO response = useCase.execute(tenantId,
                new ProvisionUserRequestDTO("prof-1", "Joao Silva", "joao@sesi.org", "USUARIO_COMUM"));

        assertEquals(existing.getId().value(), response.userId());
        assertEquals("Joao Silva", gateway.saved.getName());
        assertEquals(UserRole.USUARIO_COMUM, gateway.saved.getRole());
    }

    @Test
    void deve_rejeitar_role_invalido() {
        CapturingUserGateway gateway = new CapturingUserGateway(null);
        ProvisionUserUseCase useCase = new ProvisionUserUseCase(gateway);

        assertThrows(InvalidInputException.class,
                () -> useCase.execute(tenantId,
                        new ProvisionUserRequestDTO("prof-1", "Joao", null, "ADMIN")));
    }

    private static final class CapturingUserGateway implements UserGateway {
        private final User existingUser;
        User saved;
        CapturingUserGateway(User existingUser) { this.existingUser = existingUser; }
        @Override public User save(User user) { this.saved = user; return user; }
        @Override public Optional<User> findById(UserId id) { return Optional.empty(); }
        @Override public Optional<User> findByTenantAndExternalId(TenantId tenantId, String externalId) {
            return Optional.ofNullable(existingUser);
        }
    }
}
