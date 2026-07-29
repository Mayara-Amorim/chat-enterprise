package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.springframework.stereotype.Service;

/**
 * Desativa um tenant. NAO remove a linha: {@code TenantId} e referenciado por usuarios aqui e por
 * conversas no chat-service, entao exclusao fisica orfanaria esses dados.
 *
 * Desativar ja e suficiente para o efeito pratico — o ApiKeyAuthenticationFilter recusa
 * (401) qualquer requisicao de tenant inativo, entao a API key para de funcionar na hora.
 */
@Service
public class DeactivateTenantUseCase {

    private final TenantGateway tenantGateway;

    public DeactivateTenantUseCase(TenantGateway tenantGateway) {
        this.tenantGateway = tenantGateway;
    }

    public void execute(TenantId tenantId) {
        Tenant tenant = tenantGateway.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nao encontrado"));

        // Idempotente: repetir a chamada nao e erro nem gera escrita.
        if (!tenant.isActive()) {
            return;
        }

        tenant.deactivate();
        tenantGateway.save(tenant);
    }
}
