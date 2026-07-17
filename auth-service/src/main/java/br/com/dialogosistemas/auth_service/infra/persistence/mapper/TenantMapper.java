package br.com.dialogosistemas.auth_service.infra.persistence.mapper;

import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.auth_service.infra.persistence.entity.TenantEntity;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper {

    public Tenant toDomain(TenantEntity entity) {
        return new Tenant(
                new TenantId(entity.getId()),
                entity.getName(),
                entity.getApiKeyHash(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    public TenantEntity toEntity(Tenant tenant) {
        return new TenantEntity(
                tenant.getId().value(),
                tenant.getName(),
                tenant.getApiKeyHash(),
                tenant.isActive(),
                tenant.getCreatedAt()
        );
    }
}
