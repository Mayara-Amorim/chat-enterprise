package br.com.dialogosistemas.chat_service.integration;


import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.conversation.MessagingPermission;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import br.com.dialogosistemas.chat_service.infra.util.CursorUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:chathistorysyncapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=chat-history-sync-api-test",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
                "jwt.jwks-uri=http://localhost:0"
        }
)
@EmbeddedKafka(partitions = 1, topics = {"chat-messages"})
@Import(TestSecurityConfig.class)
class ChatHistorySyncApiIntegrationTest {

    // Precisao de microssegundo de proposito: com o cursor truncando pra milissegundo, a mensagem
    // do boundary (a mais antiga) reapareceria no ?after=. Estes valores guardam contra essa regressao.
    private static final Instant OLDEST_AT = Instant.parse("2026-03-31T10:00:00.000123Z");
    private static final Instant MIDDLE_AT = Instant.parse("2026-03-31T11:00:00.000456Z");
    private static final Instant NEWEST_AT = Instant.parse("2026-03-31T12:00:00.000789Z");

    @LocalServerPort
    private int port;

    @Autowired
    private ConversationJpaRepository conversationJpaRepository;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }

    @Test
    void afterCursorReturnsOnlyNewerMessagesInAscendingOrder() throws Exception {
        Seeded seeded = seedConversationWithThreeMessages();
        String afterCursor = CursorUtils.encode(OLDEST_AT, seeded.oldestMessageId());

        HttpResponse<String> response = sendGet(
                messagesUrl(seeded.conversationId()) + "?after=" + afterCursor,
                seeded.memberId(),
                seeded.tenantId()
        );

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        JsonNode messages = json.get("messages");

        assertEquals(2, messages.size());
        // ordem crescente: intermediaria primeiro, mais nova depois
        assertEquals("intermediaria", messages.get(0).get("content").asText());
        assertEquals("mais nova", messages.get(1).get("content").asText());
        // a mensagem do cursor (mais antiga) NAO deve voltar — prova que roteou pro forward, nao pro historico
        assertNull(findMessage(messages, seeded.oldestMessageId()));
        // nextCursor aponta pra ultima (mais nova), pra continuar sincronizando pra frente
        CursorUtils.DecodedCursor nextCursor = CursorUtils.decode(json.get("nextCursor").asText());
        assertEquals(NEWEST_AT, nextCursor.createdAt());
        assertEquals(seeded.newestMessageId(), nextCursor.id());
    }

    @Test
    void afterCursorFromLatestMessageReturnsEmptyAndNullCursor() throws Exception {
        Seeded seeded = seedConversationWithThreeMessages();
        String afterCursor = CursorUtils.encode(NEWEST_AT, seeded.newestMessageId());

        HttpResponse<String> response = sendGet(
                messagesUrl(seeded.conversationId()) + "?after=" + afterCursor,
                seeded.memberId(),
                seeded.tenantId()
        );

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals(0, json.get("messages").size());
        assertTrue(json.get("nextCursor").isNull());
    }

    @Test
    void afterCursorRejectsRequesterOutsideConversation() throws Exception {
        Seeded seeded = seedConversationWithThreeMessages();
        String afterCursor = CursorUtils.encode(OLDEST_AT, seeded.oldestMessageId());

        HttpResponse<String> response = sendGet(
                messagesUrl(seeded.conversationId()) + "?after=" + afterCursor,
                UUID.randomUUID(),
                seeded.tenantId()
        );

        assertTrue(response.statusCode() == HttpStatus.BAD_REQUEST.value()
                || response.statusCode() == HttpStatus.FORBIDDEN.value());
    }

    private Seeded seedConversationWithThreeMessages() {
        UUID tenantId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID oldestMessageId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID middleMessageId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID newestMessageId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        ConversationEntity conversation = new ConversationEntity(
                conversationId,
                tenantId,
                ConversationType.GROUP,
                "Grupo de teste",
                adminId,
                Instant.parse("2026-03-31T09:00:00Z"),
                "mais nova",
                NEWEST_AT,
                "Regras",
                MessagingPermission.ALL,
                null
        );
        conversation.addParticipant(new ConversationParticipantEntity(adminId, 0, null, ParticipantRole.ADMIN));
        conversation.addParticipant(new ConversationParticipantEntity(memberId, 0, null, ParticipantRole.MEMBER));
        ConversationEntity saved = conversationJpaRepository.saveAndFlush(conversation);

        MessageEntity oldest = new MessageEntity(oldestMessageId, saved, memberId, "mais antiga", MessageStatus.SENT, OLDEST_AT);
        MessageEntity middle = new MessageEntity(middleMessageId, saved, memberId, "intermediaria", MessageStatus.SENT, MIDDLE_AT);
        MessageEntity newest = new MessageEntity(newestMessageId, saved, adminId, "mais nova", MessageStatus.SENT, NEWEST_AT);
        messageJpaRepository.saveAllAndFlush(List.of(oldest, middle, newest));

        return new Seeded(tenantId, conversationId, memberId, oldestMessageId, newestMessageId);
    }

    private String messagesUrl(UUID conversationId) {
        return "http://localhost:" + port + "/api/chat/conversations/" + conversationId + "/messages";
    }

    private HttpResponse<String> sendGet(String url, UUID subjectId, UUID tenantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + TestSecurityConfig.generateJwt(subjectId, tenantId))
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao executar GET de teste", exception);
        }
    }

    private JsonNode findMessage(JsonNode messages, UUID messageId) {
        for (JsonNode message : messages) {
            if (messageId.toString().equals(message.get("id").asText())) {
                return message;
            }
        }
        return null;
    }

    private record Seeded(
            UUID tenantId,
            UUID conversationId,
            UUID memberId,
            UUID oldestMessageId,
            UUID newestMessageId
    ) {
    }
}
