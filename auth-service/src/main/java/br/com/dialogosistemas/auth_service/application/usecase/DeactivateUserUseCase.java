package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.domain.gateway.UserGateway;
import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.shared_kernel.domain.exception.ForbiddenOperationException;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;

@Service
public class DeactivateUserUseCase {

    private final UserGateway userGateway;

    public DeactivateUserUseCase(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public void execute(UserId userId, TenantId tenantId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (!user.getTenantId().equals(tenantId)) {
            throw new ForbiddenOperationException("Usuario nao pertence a este tenant");
        }

        user.deactivate();
        userGateway.save(user);
    }
}
