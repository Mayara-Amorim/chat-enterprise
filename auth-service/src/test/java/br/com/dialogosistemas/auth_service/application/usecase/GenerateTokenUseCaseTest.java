package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenResponseDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.UserGateway;
import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.auth_service.domain.model.UserRole;
import br.com.dialogosistemas.auth_service.infra.security.JwtTokenGenerator;
import br.com.dialogosistemas.auth_service.infra.security.RsaKeyProvider;
import br.com.dialogosistemas.shared_kernel.domain.exception.ForbiddenOperationException;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTokenUseCaseTest {

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final RsaKeyProvider rsaKeyProvider = new RsaKeyProvider(null, null, null);
    private final JwtTokenGenerator tokenGenerator = new JwtTokenGenerator(rsaKeyProvider, 3600);

    @Test
    void deve_gerar_token_para_usuario_ativo() {
        User user = new User(new UserId(UUID.randomUUID()), tenantId, "prof-1", "Joao", null, UserRole.MEDIADOR, true, Instant.now());
        StubUserGateway gateway = new StubUserGateway(user);
        GenerateTokenUseCase useCase = new GenerateTokenUseCase(gateway, tokenGenerator);

        GenerateTokenResponseDTO response = useCase.execute(tenantId, new GenerateTokenRequestDTO("prof-1"));

        assertNotNull(response.accessToken());
        assertFalse(response.accessToken().isBlank());
        assertEquals(3600, response.expiresIn());
    }

    @Test
    void deve_rejeitar_usuario_inativo() {
        User user = new User(new UserId(UUID.randomUUID()), tenantId, "prof-1", "Joao", null, UserRole.MEDIADOR, false, Instant.now());
        StubUserGateway gateway = new StubUserGateway(user);
        GenerateTokenUseCase useCase = new GenerateTokenUseCase(gateway, tokenGenerator);

        assertThrows(ForbiddenOperationException.class,
                () -> useCase.execute(tenantId, new GenerateTokenRequestDTO("prof-1")));
    }

    @Test
    void deve_rejeitar_usuario_nao_encontrado() {
        StubUserGateway gateway = new StubUserGateway(null);
        GenerateTokenUseCase useCase = new GenerateTokenUseCase(gateway, tokenGenerator);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(tenantId, new GenerateTokenRequestDTO("nao-existe")));
    }

    private static final class StubUserGateway implements UserGateway {
        private final User user;
        StubUserGateway(User user) { this.user = user; }
        @Override public User save(User user) { throw new UnsupportedOperationException(); }
        @Override public Optional<User> findById(UserId id) { return Optional.empty(); }
        @Override public Optional<User> findByTenantAndExternalId(TenantId tenantId, String externalId) {
            return Optional.ofNullable(user);
        }
    }
}
