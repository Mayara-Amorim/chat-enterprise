package br.com.dialogosistemas.auth_service.integration;

import br.com.dialogosistemas.auth_service.application.dto.CreateTenantResponseDTO;
import br.com.dialogosistemas.auth_service.application.dto.TenantSummaryDTO;
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

    @Test
    void deve_desativar_tenant_e_invalidar_a_api_key() {
        CreateTenantResponseDTO criado = criarTenant("Tenant Para Desativar");

        // Antes: a API key funciona (provisiona usuario, endpoint protegido por X-API-Key)
        assertEquals(HttpStatus.CREATED, provisionarUsuario(criado.apiKey()).getStatusCode());

        ResponseEntity<Void> delete = restTemplate.exchange(
                "/api/auth/tenants/" + criado.tenantId(), HttpMethod.DELETE,
                new HttpEntity<>(masterKeyHeaders()), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, delete.getStatusCode());

        // Depois: a mesma API key para de valer. E este o efeito pratico da desativacao —
        // sem isso, "desativar" seria so uma flag no banco sem consequencia.
        assertEquals(HttpStatus.UNAUTHORIZED, provisionarUsuario(criado.apiKey()).getStatusCode());
    }

    @Test
    void deve_ser_idempotente_ao_desativar_duas_vezes() {
        CreateTenantResponseDTO criado = criarTenant("Tenant Idempotente");
        HttpEntity<Void> request = new HttpEntity<>(masterKeyHeaders());
        String url = "/api/auth/tenants/" + criado.tenantId();

        assertEquals(HttpStatus.NO_CONTENT,
                restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT,
                restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class).getStatusCode());
    }

    @Test
    void deve_retornar_404_ao_desativar_tenant_inexistente() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/tenants/" + java.util.UUID.randomUUID(), HttpMethod.DELETE,
                new HttpEntity<>(masterKeyHeaders()), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deve_rejeitar_desativacao_sem_master_key() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/tenants/" + java.util.UUID.randomUUID(), HttpMethod.DELETE,
                new HttpEntity<>(new HttpHeaders()), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deve_listar_tenants_com_master_key() {
        CreateTenantResponseDTO criado = criarTenant("Tenant Listavel");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/tenants", HttpMethod.GET, new HttpEntity<>(masterKeyHeaders()), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains(criado.tenantId().toString()),
                "o tenant recem-criado deveria aparecer na listagem");
    }

    @Test
    void a_listagem_nao_pode_conter_a_api_key_nem_o_hash() {
        // Checagem no JSON de verdade, nao no DTO: se alguem trocar o record por uma projecao
        // da entidade, o hash volta a sair e o teste de unidade sozinho nao pegaria.
        CreateTenantResponseDTO criado = criarTenant("Tenant Sem Vazamento");
        String hash = br.com.dialogosistemas.auth_service.domain.model.Tenant.hashApiKey(criado.apiKey());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/tenants", HttpMethod.GET, new HttpEntity<>(masterKeyHeaders()), String.class);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().contains(criado.apiKey()), "a API key em texto plano vazou na listagem");
        assertFalse(response.getBody().contains(hash), "o hash da API key vazou na listagem");
        assertFalse(response.getBody().toLowerCase().contains("apikey"), "campo apiKey presente no JSON");
    }

    @Test
    void deve_rejeitar_listagem_sem_master_key() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/tenants", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deve_rejeitar_size_acima_do_teto() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/tenants?size=201", HttpMethod.GET, new HttpEntity<>(masterKeyHeaders()), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deve_respeitar_o_tamanho_de_pagina() {
        criarTenant("Pagina A");
        criarTenant("Pagina B");
        criarTenant("Pagina C");

        ResponseEntity<TenantSummaryDTO[]> response = restTemplate.exchange(
                "/api/auth/tenants?page=0&size=2", HttpMethod.GET,
                new HttpEntity<>(masterKeyHeaders()), TenantSummaryDTO[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length, "size=2 deveria limitar a pagina a 2 itens");
    }

    private HttpHeaders masterKeyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Master-Key", "test-master-key");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private CreateTenantResponseDTO criarTenant(String nome) {
        ResponseEntity<CreateTenantResponseDTO> response = restTemplate.postForEntity(
                "/api/auth/tenants", new HttpEntity<>(Map.of("name", nome), masterKeyHeaders()),
                CreateTenantResponseDTO.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private ResponseEntity<String> provisionarUsuario(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/api/auth/users",
                new HttpEntity<>(Map.of("externalId", "u-" + java.util.UUID.randomUUID(),
                        "name", "Fulano", "email", "f@example.com", "role", "USUARIO_COMUM"), headers),
                String.class);
    }
}
