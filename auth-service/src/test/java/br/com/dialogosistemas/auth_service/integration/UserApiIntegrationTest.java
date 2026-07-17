package br.com.dialogosistemas.auth_service.integration;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.application.dto.ProvisionUserResponseDTO;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String createTenantAndGetApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Master-Key", "test-master-key");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("name", "Tenant User Test"), headers);
        ResponseEntity<CreateTenantResponseDTO> response = restTemplate.postForEntity("/api/auth/tenants", request, CreateTenantResponseDTO.class);
        return response.getBody().apiKey();
    }

    private String createSecondTenantAndGetApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Master-Key", "test-master-key");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("name", "Tenant 2"), headers);
        ResponseEntity<CreateTenantResponseDTO> response = restTemplate.postForEntity("/api/auth/tenants", request, CreateTenantResponseDTO.class);
        return response.getBody().apiKey();
    }

    @Test
    void deve_provisionar_user_com_api_key_valida() {
        String apiKey = createTenantAndGetApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "user-1", "name", "Maria", "email", "maria@test.com", "role", "MEDIADOR");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<ProvisionUserResponseDTO> response = restTemplate.postForEntity("/api/auth/users", request, ProvisionUserResponseDTO.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().userId());
        assertEquals("user-1", response.getBody().externalId());
    }

    @Test
    void deve_rejeitar_provisionar_com_role_invalido() {
        String apiKey = createTenantAndGetApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "user-2", "name", "Joao", "email", "joao@test.com", "role", "ADMIN");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/users", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deve_atualizar_user_existente_upsert() {
        String apiKey = createTenantAndGetApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body1 = Map.of("externalId", "user-upsert", "name", "Ana", "email", "ana@test.com", "role", "MEDIADOR");
        restTemplate.postForEntity("/api/auth/users", new HttpEntity<>(body1, headers), ProvisionUserResponseDTO.class);

        Map<String, String> body2 = Map.of("externalId", "user-upsert", "name", "Ana Silva", "email", "ana.silva@test.com", "role", "USUARIO_COMUM");
        ResponseEntity<ProvisionUserResponseDTO> response = restTemplate.postForEntity("/api/auth/users", new HttpEntity<>(body2, headers), ProvisionUserResponseDTO.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("user-upsert", response.getBody().externalId());
    }

    @Test
    void deve_rejeitar_provisionar_com_api_key_invalida() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "chave-invalida");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "user-3", "name", "Pedro", "role", "MEDIADOR");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/users", request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deve_desativar_user() {
        String apiKey = createTenantAndGetApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "user-deactivate", "name", "Carlos", "role", "MEDIADOR");
        ResponseEntity<ProvisionUserResponseDTO> created = restTemplate.postForEntity("/api/auth/users", new HttpEntity<>(body, headers), ProvisionUserResponseDTO.class);
        String userId = created.getBody().userId().toString();

        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange("/api/auth/users/" + userId, HttpMethod.DELETE, deleteRequest, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deve_rejeitar_desativar_user_de_outro_tenant() {
        String apiKey1 = createTenantAndGetApiKey();
        String apiKey2 = createSecondTenantAndGetApiKey();

        HttpHeaders headers1 = new HttpHeaders();
        headers1.set("X-API-Key", apiKey1);
        headers1.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("externalId", "user-cross-tenant", "name", "Cross", "role", "MEDIADOR");
        ResponseEntity<ProvisionUserResponseDTO> created = restTemplate.postForEntity("/api/auth/users", new HttpEntity<>(body, headers1), ProvisionUserResponseDTO.class);
        String userId = created.getBody().userId().toString();

        HttpHeaders headers2 = new HttpHeaders();
        headers2.set("X-API-Key", apiKey2);

        ResponseEntity<String> response = restTemplate.exchange("/api/auth/users/" + userId, HttpMethod.DELETE, new HttpEntity<>(headers2), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deve_rejeitar_desativar_user_inexistente() {
        String apiKey = createTenantAndGetApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        ResponseEntity<String> response = restTemplate.exchange("/api/auth/users/00000000-0000-0000-0000-000000000000", HttpMethod.DELETE, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
