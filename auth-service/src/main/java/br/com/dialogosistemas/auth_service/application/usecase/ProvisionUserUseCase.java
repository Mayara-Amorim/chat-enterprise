package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserResponseDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.UserGateway;
import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.auth_service.domain.model.UserRole;
import br.com.dialogosistemas.shared_kernel.domain.exception.InvalidInputException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class ProvisionUserUseCase {

    private final UserGateway userGateway;

    public ProvisionUserUseCase(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public ProvisionUserResponseDTO execute(TenantId tenantId, ProvisionUserRequestDTO request) {
        UserRole role = parseRole(request.role());

        var existing = userGateway.findByTenantAndExternalId(tenantId, request.externalId());

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            user.update(request.name(), request.email(), role);
        } else {
            user = User.create(tenantId, request.externalId(), request.name(), request.email(), role);
        }

        userGateway.save(user);
        return new ProvisionUserResponseDTO(user.getId().value(), user.getExternalId());
    }

    private UserRole parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            throw new InvalidInputException("Role invalido. Valores aceitos: " + Arrays.toString(UserRole.values()));
        }
        try {
            return UserRole.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Role invalido. Valores aceitos: " + Arrays.toString(UserRole.values()));
        }
    }
}
