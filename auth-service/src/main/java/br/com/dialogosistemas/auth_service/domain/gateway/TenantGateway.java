package br.com.dialogosistemas.auth_service.domain.gateway;

import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;

import java.util.Optional;

public interface TenantGateway {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(TenantId id);
    Optional<Tenant> findByApiKeyHash(String apiKeyHash);
}
