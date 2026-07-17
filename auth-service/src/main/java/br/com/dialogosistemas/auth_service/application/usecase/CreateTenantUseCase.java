package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import org.springframework.stereotype.Service;

@Service
public class CreateTenantUseCase {

    private final TenantGateway tenantGateway;

    public CreateTenantUseCase(TenantGateway tenantGateway) {
        this.tenantGateway = tenantGateway;
    }

    public CreateTenantResponseDTO execute(CreateTenantRequestDTO request) {
        Tenant.CreateResult result = Tenant.create(request.name());
        tenantGateway.save(result.tenant());
        return new CreateTenantResponseDTO(result.tenant().getId().value(), result.rawApiKey());
    }
}
