package br.com.dialogosistemas.auth_service.domain.gateway;

import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.util.Optional;

public interface UserGateway {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByTenantAndExternalId(TenantId tenantId, String externalId);
}
