package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenResponseDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.UserGateway;
import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.auth_service.infra.security.JwtTokenGenerator;
import br.com.dialogosistemas.shared_kernel.domain.exception.ForbiddenOperationException;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.springframework.stereotype.Service;

@Service
public class GenerateTokenUseCase {

    private final UserGateway userGateway;
    private final JwtTokenGenerator tokenGenerator;

    public GenerateTokenUseCase(UserGateway userGateway, JwtTokenGenerator tokenGenerator) {
        this.userGateway = userGateway;
        this.tokenGenerator = tokenGenerator;
    }

    public GenerateTokenResponseDTO execute(TenantId tenantId, GenerateTokenRequestDTO request) {
        User user = userGateway.findByTenantAndExternalId(tenantId, request.externalId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (!user.isActive()) {
            throw new ForbiddenOperationException("Usuario esta desativado");
        }

        String token = tokenGenerator.generateToken(user);
        return new GenerateTokenResponseDTO(token, tokenGenerator.getExpirationSeconds());
    }
}
