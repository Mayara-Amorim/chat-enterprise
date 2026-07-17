package br.com.dialogosistemas.auth_service.api.controller;

import br.com.dialogosistemas.auth_service.infra.security.RsaKeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "JWKS", description = "Chave publica para validacao de tokens JWT")
public class JwksController {

    private final RsaKeyProvider rsaKeyProvider;

    public JwksController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(
            summary = "Obter chave publica JWKS",
            description = "Retorna o JSON Web Key Set com a chave publica RSA usada para assinar os tokens JWT. "
                    + "Endpoint publico — usado pelo chat-service para validar tokens."
    )
    @ApiResponse(responseCode = "200", description = "JWK Set retornado com sucesso")
    public Map<String, Object> jwks() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyProvider.getPublicKey())
                .keyID(rsaKeyProvider.getKeyId())
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}
