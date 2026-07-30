package br.com.dialogosistemas.auth_service.api.controller;

import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserResponseDTO;
import br.com.dialogosistemas.auth_service.application.usecase.DeactivateUserUseCase;
import br.com.dialogosistemas.auth_service.application.usecase.ProvisionUserUseCase;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/users")
@Tag(name = "Usuarios", description = "Provisionamento e desativacao de usuarios")
@SecurityRequirement(name = "tenantApiKey")
public class UserController {

    private final ProvisionUserUseCase provisionUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;

    public UserController(ProvisionUserUseCase provisionUserUseCase, DeactivateUserUseCase deactivateUserUseCase) {
        this.provisionUserUseCase = provisionUserUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Provisionar usuario",
            description = "Cria ou atualiza um usuario no tenant. Se o externalId ja existir para o tenant, "
                    + "atualiza name, email e role (upsert)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado ou atualizado"),
            @ApiResponse(responseCode = "401", description = "API key invalida ou ausente")
    })
    public ProvisionUserResponseDTO provisionUser(
            @Valid @RequestBody ProvisionUserRequestDTO request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
        TenantId tenantId = (TenantId) httpRequest.getAttribute("tenantId");
        if (tenantId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Tenant not authenticated");
        }
        return provisionUserUseCase.execute(tenantId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Desativar usuario",
            description = "Desativa o usuario (soft-delete). Usuarios desativados nao podem receber tokens."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario desativado"),
            @ApiResponse(responseCode = "401", description = "API key invalida ou ausente"),
            @ApiResponse(responseCode = "400", description = "Usuario nao encontrado ou nao pertence ao tenant")
    })
    public void deactivateUser(
            @Parameter(description = "ID do usuario na plataforma de chat") @PathVariable UUID userId,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
        TenantId tenantId = (TenantId) httpRequest.getAttribute("tenantId");
        if (tenantId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Tenant not authenticated");
        }
        deactivateUserUseCase.execute(new UserId(userId), tenantId);
    }
}
