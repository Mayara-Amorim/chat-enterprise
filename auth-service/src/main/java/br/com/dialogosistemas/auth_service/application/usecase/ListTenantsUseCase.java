package br.com.dialogosistemas.auth_service.application.usecase;

import br.com.dialogosistemas.auth_service.application.dto.TenantSummaryDTO;
import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.shared_kernel.domain.exception.InvalidInputException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lista os tenants da plataforma para operacao administrativa (protegida por master key).
 *
 * Existe porque criar tenant devolve o ID uma unica vez: sem listagem, um tenant cujo ID se perdeu
 * fica inalcancavel para desativacao.
 *
 * Retorna ativos E inativos — quem administra precisa enxergar o que ja foi desativado, senao
 * "sumiu da lista" e "nao existe" viram a mesma coisa.
 *
 * Paginado. Tenant e cliente: a tabela cresce com o negocio, e listagem sem limite e o tipo de
 * consulta que so machuca quando ja e tarde. O teto de {@link #MAX_SIZE} impede que o parametro
 * vindo do cliente anule a protecao.
 */
@Service
public class ListTenantsUseCase {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    private final TenantGateway tenantGateway;

    public ListTenantsUseCase(TenantGateway tenantGateway) {
        this.tenantGateway = tenantGateway;
    }

    public List<TenantSummaryDTO> execute(int page, int size) {
        if (page < 0) {
            throw new InvalidInputException("page nao pode ser negativo");
        }
        if (size < 1) {
            throw new InvalidInputException("size precisa ser no minimo 1");
        }
        if (size > MAX_SIZE) {
            throw new InvalidInputException("size nao pode passar de " + MAX_SIZE);
        }

        return tenantGateway.findAll(page, size).stream()
                .map(ListTenantsUseCase::toSummary)
                .toList();
    }

    private static TenantSummaryDTO toSummary(Tenant tenant) {
        return new TenantSummaryDTO(
                tenant.getId().value(),
                tenant.getName(),
                tenant.isActive(),
                tenant.getCreatedAt()
        );
    }
}
