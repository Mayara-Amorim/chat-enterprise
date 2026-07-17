package br.com.dialogosistemas.auth_service.integration;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenResponseDTO;
import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserResponseDTO;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class JwksApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deve_retornar_jwks_sem_autenticacao() {
        ResponseEntity<String> response = restTemplate.getForEntity("/.well-known/jwks.json", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("keys"));
    }

    @Test
    void deve_validar_jwt_com_chave_do_jwks() throws Exception {
        // 1. Criar tenant e user
        HttpHeaders masterHeaders = new HttpHeaders();
        masterHeaders.set("X-Master-Key", "test-master-key");
        masterHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<CreateTenantResponseDTO> tenantResp = restTemplate.postForEntity(
                "/api/auth/tenants", new HttpEntity<>(Map.of("name", "JWKS Test"), masterHeaders), CreateTenantResponseDTO.class);
        String apiKey = tenantResp.getBody().apiKey();

        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.set("X-API-Key", apiKey);
        apiHeaders.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity("/api/auth/users",
                new HttpEntity<>(Map.of("externalId", "jwks-user", "name", "JWKS User", "role", "USUARIO_COMUM"), apiHeaders),
                ProvisionUserResponseDTO.class);

        // 2. Gerar token
        ResponseEntity<GenerateTokenResponseDTO> tokenResp = restTemplate.postForEntity(
                "/api/auth/token", new HttpEntity<>(Map.of("externalId", "jwks-user"), apiHeaders), GenerateTokenResponseDTO.class);
        String jwt = tokenResp.getBody().accessToken();

        // 3. Buscar JWKS
        ResponseEntity<String> jwksResp = restTemplate.getForEntity("/.well-known/jwks.json", String.class);
        JWKSet jwkSet = JWKSet.parse(jwksResp.getBody());
        RSAKey rsaKey = (RSAKey) jwkSet.getKeys().getFirst();

        // 4. Validar assinatura do JWT com a chave publica do JWKS
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

        assertTrue(signedJWT.verify(verifier), "JWT deve ser verificavel com a chave publica do JWKS");
        assertEquals("USUARIO_COMUM", signedJWT.getJWTClaimsSet().getStringClaim("role"));
    }
}
