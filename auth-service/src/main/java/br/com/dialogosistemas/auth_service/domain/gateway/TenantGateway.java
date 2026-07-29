package br.com.dialogosistemas.auth_service.domain.gateway;

import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;

import java.util.List;
import java.util.Optional;

public interface TenantGateway {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(TenantId id);
    Optional<Tenant> findByApiKeyHash(String apiKeyHash);

    /**
     * Pagina de tenants, ordenada por data de criacao (mais recente primeiro).
     * Recebe primitivos de proposito: Pageable e tipo do Spring Data e nao entra no dominio
     * (mesma convencao de MessageGateway.findHistoryBeforeCursor).
     *
     * @param page indice da pagina, base zero
     * @param size itens por pagina
     */
    List<Tenant> findAll(int page, int size);
}
