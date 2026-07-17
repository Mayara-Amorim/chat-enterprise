package br.com.dialogosistemas.auth_service.integration;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.application.dto.GenerateTokenResponseDTO;
import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserResponseDTO;
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
class TokenApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String createTenantAndGetApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Master-Key", "test-master-key");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("name", "Tenant Token Test"), headers);
        ResponseEntity<CreateTenantResponseDTO> response = restTemplate.postForEntity("/api/auth/tenants", request, CreateTenantResponseDTO.class);
        return response.getBody().apiKey();
    }

    private void provisionUser(String apiKey, String externalId, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", externalId, "name", name, "role", "MEDIADOR");
        restTemplate.postForEntity("/api/auth/users", new HttpEntity<>(body, headers), ProvisionUserResponseDTO.class);
    }

    private ProvisionUserResponseDTO provisionUserAndGet(String apiKey, String externalId, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", externalId, "name", name, "role", "MEDIADOR");
        return restTemplate.postForEntity("/api/auth/users", new HttpEntity<>(body, headers), ProvisionUserResponseDTO.class).getBody();
    }

    @Test
    void deve_gerar_token_para_user_valido() throws Exception {
        String apiKey = createTenantAndGetApiKey();
        provisionUser(apiKey, "token-user-1", "Maria");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "token-user-1");
        ResponseEntity<GenerateTokenResponseDTO> response = restTemplate.postForEntity("/api/auth/token", new HttpEntity<>(body, headers), GenerateTokenResponseDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());

        SignedJWT jwt = SignedJWT.parse(response.getBody().accessToken());
        assertEquals("Maria", jwt.getJWTClaimsSet().getStringClaim("name"));
        assertEquals("MEDIADOR", jwt.getJWTClaimsSet().getStringClaim("role"));
        assertNotNull(jwt.getJWTClaimsSet().getStringClaim("tenant_id"));
        assertNotNull(jwt.getJWTClaimsSet().getSubject());
    }

    @Test
    void deve_rejeitar_gerar_token_para_user_inexistente() {
        String apiKey = createTenantAndGetApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "nao-existe");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/token", new HttpEntity<>(body, headers), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deve_rejeitar_gerar_token_para_user_desativado() {
        String apiKey = createTenantAndGetApiKey();
        ProvisionUserResponseDTO user = provisionUserAndGet(apiKey, "token-deactivated", "Inativo");

        HttpHeaders deleteHeaders = new HttpHeaders();
        deleteHeaders.set("X-API-Key", apiKey);
        restTemplate.exchange("/api/auth/users/" + user.userId(), HttpMethod.DELETE, new HttpEntity<>(deleteHeaders), Void.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "token-deactivated");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/token", new HttpEntity<>(body, headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deve_rejeitar_gerar_token_sem_api_key() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "qualquer");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/token", new HttpEntity<>(body, headers), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
