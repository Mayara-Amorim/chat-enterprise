package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.domain.gateway.UserGateway;
import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.auth_service.domain.model.UserRole;
import br.com.dialogosistemas.shared_kernel.domain.exception.ForbiddenOperationException;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeactivateUserUseCaseTest {

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @Test
    void deve_desativar_usuario() {
        UserId userId = new UserId(UUID.randomUUID());
        User user = new User(userId, tenantId, "prof-1", "Joao", null, UserRole.MEDIADOR, true, Instant.now());
        CapturingGateway gateway = new CapturingGateway(user);
        DeactivateUserUseCase useCase = new DeactivateUserUseCase(gateway);

        useCase.execute(userId, tenantId);

        assertFalse(gateway.saved.isActive());
    }

    @Test
    void deve_rejeitar_usuario_nao_encontrado() {
        CapturingGateway gateway = new CapturingGateway(null);
        DeactivateUserUseCase useCase = new DeactivateUserUseCase(gateway);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new UserId(UUID.randomUUID()), tenantId));
    }

    @Test
    void deve_rejeitar_usuario_de_outro_tenant() {
        UserId userId = new UserId(UUID.randomUUID());
        TenantId otherTenant = new TenantId(UUID.randomUUID());
        User user = new User(userId, otherTenant, "prof-1", "Joao", null, UserRole.MEDIADOR, true, Instant.now());
        CapturingGateway gateway = new CapturingGateway(user);
        DeactivateUserUseCase useCase = new DeactivateUserUseCase(gateway);

        assertThrows(ForbiddenOperationException.class,
                () -> useCase.execute(userId, tenantId));
    }

    private static final class CapturingGateway implements UserGateway {
        private final User user;
        User saved;
        CapturingGateway(User user) { this.user = user; }
        @Override public User save(User user) { this.saved = user; return user; }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(user); }
        @Override public Optional<User> findByTenantAndExternalId(TenantId tenantId, String externalId) {
            return Optional.empty();
        }
    }
}
