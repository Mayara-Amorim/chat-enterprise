package br.com.dialogosistemas.auth_service.api.controller;

import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenRequestDTO;
import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenResponseDTO;
import br.com.dialogosistemas.auth_service.application.usecase.GenerateTokenUseCase;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/token")
@Tag(name = "Token", description = "Emissao de tokens JWT")
@SecurityRequirement(name = "tenantApiKey")
public class TokenController {

    private final GenerateTokenUseCase generateTokenUseCase;

    public TokenController(GenerateTokenUseCase generateTokenUseCase) {
        this.generateTokenUseCase = generateTokenUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Gerar token JWT",
            description = "Gera um token JWT (RS256) para o usuario identificado pelo externalId. "
                    + "O usuario deve estar ativo. Token expira em 24h (configuravel)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token gerado com sucesso"),
            @ApiResponse(responseCode = "401", description = "API key invalida ou ausente"),
            @ApiResponse(responseCode = "400", description = "Usuario nao encontrado ou desativado")
    })
    public GenerateTokenResponseDTO generateToken(
            @RequestBody GenerateTokenRequestDTO request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
        TenantId tenantId = (TenantId) httpRequest.getAttribute("tenantId");
        if (tenantId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Tenant not authenticated");
        }
        return generateTokenUseCase.execute(tenantId, request);
    }
}
