package br.com.dialogosistemas.auth_service.api.controller;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.application.usecase.CreateTenantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/tenants")
@Tag(name = "Tenants", description = "Gerenciamento de tenants da plataforma")
public class TenantController {

    private final CreateTenantUseCase createTenantUseCase;

    public TenantController(CreateTenantUseCase createTenantUseCase) {
        this.createTenantUseCase = createTenantUseCase;
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
    public CreateTenantResponseDTO createTenant(@RequestBody CreateTenantRequestDTO request) {
        return createTenantUseCase.execute(request);
    }
}
