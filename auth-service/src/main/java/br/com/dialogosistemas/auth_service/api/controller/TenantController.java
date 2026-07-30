package br.com.dialogosistemas.auth_service.api.controller;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.application.usecase.CreateTenantUseCase;
import br.com.dialogosistemas.auth_service.application.dto.TenantSummaryDTO;
import br.com.dialogosistemas.auth_service.application.usecase.DeactivateTenantUseCase;
import br.com.dialogosistemas.auth_service.application.usecase.ListTenantsUseCase;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/tenants")
@Tag(name = "Tenants", description = "Gerenciamento de tenants da plataforma")
public class TenantController {

    private final CreateTenantUseCase createTenantUseCase;
    private final DeactivateTenantUseCase deactivateTenantUseCase;
    private final ListTenantsUseCase listTenantsUseCase;

    public TenantController(CreateTenantUseCase createTenantUseCase,
                            DeactivateTenantUseCase deactivateTenantUseCase,
                            ListTenantsUseCase listTenantsUseCase) {
        this.createTenantUseCase = createTenantUseCase;
        this.deactivateTenantUseCase = deactivateTenantUseCase;
        this.listTenantsUseCase = listTenantsUseCase;
    }

    @GetMapping
    @Operation(
            summary = "Listar tenants",
            description = "Lista os tenants da plataforma, ativos e inativos, ordenados por data de criacao "
                    + "(mais recentes primeiro). Existe porque a criacao devolve o tenantId uma unica vez — "
                    + "sem listagem, um tenant cujo ID se perdeu ficaria inalcancavel para desativacao. "
                    + "NAO retorna a API key nem o hash dela. Paginado: size tem teto de 200."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de tenants"),
            @ApiResponse(responseCode = "400", description = "page negativo, size menor que 1 ou acima de 200"),
            @ApiResponse(responseCode = "401", description = "Master key invalida ou ausente")
    })
    @SecurityRequirement(name = "masterKey")
    public List<TenantSummaryDTO> listTenants(
            @Parameter(description = "Indice da pagina, base zero") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por pagina (maximo 200)") @RequestParam(defaultValue = "50") int size) {
        return listTenantsUseCase.execute(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar tenant",
            description = "Cria um novo tenant na plataforma. Retorna o tenantId e a API key em texto plano. "
                    + "A API key e retornada uma unica vez — armazene-a com seguranca."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tenant criado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Master key invalida ou ausente")
    })
    @SecurityRequirement(name = "masterKey")
    public CreateTenantResponseDTO createTenant(@Valid @RequestBody CreateTenantRequestDTO request) {
        return createTenantUseCase.execute(request);
    }

    @DeleteMapping("/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Desativar tenant",
            description = "Desativa um tenant. A API key dele para de funcionar imediatamente — "
                    + "requisicoes passam a receber 401. NAO remove o registro: o tenantId e referenciado "
                    + "por usuarios e por conversas no chat-service, entao exclusao fisica orfanaria esses "
                    + "dados. Operacao idempotente: desativar um tenant ja inativo retorna 204."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tenant desativado (ou ja estava inativo)"),
            @ApiResponse(responseCode = "401", description = "Master key invalida ou ausente"),
            @ApiResponse(responseCode = "404", description = "Tenant nao encontrado")
    })
    @SecurityRequirement(name = "masterKey")
    public void deactivateTenant(
            @Parameter(description = "ID do tenant", required = true) @PathVariable UUID tenantId) {
        deactivateTenantUseCase.execute(new TenantId(tenantId));
    }
}
