package br.com.dialogosistemas.auth_service.integration;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
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
class TenantApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deve_criar_tenant_com_master_key_valida() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Master-Key", "test-master-key");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("name", "Tenant Teste"), headers);
        ResponseEntity<CreateTenantResponseDTO> response = restTemplate.postForEntity("/api/auth/tenants", request, CreateTenantResponseDTO.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().tenantId());
        assertNotNull(response.getBody().apiKey());
    }

    @Test
    void deve_rejeitar_criacao_com_master_key_invalida() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Master-Key", "chave-errada");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("name", "Tenant"), headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/tenants", request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deve_rejeitar_criacao_sem_header_master_key() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("name", "Tenant"), headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/tenants", request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
