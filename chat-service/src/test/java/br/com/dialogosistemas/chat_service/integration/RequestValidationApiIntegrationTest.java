package br.com.dialogosistemas.chat_service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Corpo de requisicao com campo obrigatorio ausente deve virar 400, nao 500.
 *
 * Descoberto em 2026-07-30 por um gerador de carga que mandou nomes de campo errados: o campo
 * chegava nulo no use case, estourava NullPointerException, e o ApiExceptionHandler nao cobre NPE
 * — entao erro de cliente saia como erro de servidor, poluindo qualquer painel de 5xx.
 *
 * O conserto NAO pode ser mapear NPE para 400: isso transformaria bug real de dominio em "culpa do
 * cliente". A validacao acontece na fronteira, com @Valid + @NotNull, antes do use case rodar.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:requestvalidationapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=chat-request-validation-test",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
                "jwt.jwks-uri=http://localhost:0",
                // POST /messages e /upload/request tem @RateLimit; sem Redis, cada chamada
                // pagaria o timeout de conexao. O limitador e testado em RateLimitInterceptorTest.
                "ratelimit.enabled=false"
        }
)
@EmbeddedKafka(partitions = 1, topics = {
        "chat-messages",
        "chat-message-status-events",
        "chat-message-deleted-events",
        "chat-message-edited-events",
        "chat-group-events"
})
@Import(TestSecurityConfig.class)
class RequestValidationApiIntegrationTest {

    @LocalServerPort
    private int port;

    private HttpClient httpClient;
    private UUID usuario;
    private UUID tenant;
    private UUID conversa;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
        usuario = UUID.randomUUID();
        tenant = UUID.randomUUID();
        conversa = UUID.randomUUID();
    }

    // Os dois casos que a carga de 2026-07-30 pegou em producao, com 500 de verdade.

    @Test
    void deve_retornar_400_quando_messagingPermission_ausente() throws Exception {
        HttpResponse<String> resposta = enviar(
                "PATCH", "/api/chat/conversations/" + conversa + "/settings", "{}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), resposta.statusCode(),
                "campo obrigatorio ausente deve ser 400, nao 500");
    }

    @Test
    void deve_retornar_400_quando_userIds_ausente() throws Exception {
        HttpResponse<String> resposta = enviar(
                "POST", "/api/chat/conversations/" + conversa + "/members", "{}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), resposta.statusCode(),
                "campo obrigatorio ausente deve ser 400, nao 500");
    }

    // Mesma classe de defeito nos demais corpos obrigatorios.

    @Test
    void deve_retornar_400_quando_conversationId_ausente_no_envio_de_mensagem() throws Exception {
        HttpResponse<String> resposta = enviar(
                "POST", "/api/chat/conversations/messages", "{\"content\":\"oi\"}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), resposta.statusCode());
    }

    @Test
    void deve_retornar_400_quando_type_ausente_na_criacao_de_conversa() throws Exception {
        HttpResponse<String> resposta = enviar(
                "POST", "/api/chat/conversations",
                "{\"title\":\"sem type\",\"participants\":[\"" + UUID.randomUUID() + "\"]}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), resposta.statusCode());
    }

    @Test
    void deve_retornar_400_quando_content_ausente_na_edicao_de_mensagem() throws Exception {
        HttpResponse<String> resposta = enviar(
                "PATCH", "/api/chat/conversations/" + conversa + "/messages/" + UUID.randomUUID(), "{}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), resposta.statusCode());
    }

    @Test
    void deve_retornar_400_quando_fileName_ausente_no_pedido_de_upload() throws Exception {
        HttpResponse<String> resposta = enviar(
                "POST", "/api/conversations/" + conversa + "/upload/request",
                "{\"conversationId\":\"" + conversa + "\",\"contentType\":\"image/png\",\"sizeInBytes\":10}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), resposta.statusCode());
    }

    // Guarda contra excesso de zelo: campo OPCIONAL ausente nao pode virar 400.
    // Sem isto, anotar tudo com @NotNull passaria nos testes acima e quebraria clientes legitimos.
    @Test
    void deve_aceitar_corpo_sem_os_campos_opcionais() throws Exception {
        HttpResponse<String> resposta = enviar(
                "POST", "/api/chat/conversations",
                "{\"type\":\"INDIVIDUAL\",\"participants\":[\"" + UUID.randomUUID() + "\"]}");

        assertEquals(HttpStatus.CREATED.value(), resposta.statusCode(),
                "title e description sao opcionais — ausencia deles nao e erro de validacao");
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + port + caminho))
                .header("Authorization", "Bearer " + TestSecurityConfig.generateJwt(usuario, tenant))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
