package br.com.dialogosistemas.chat_service.integration;

import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:deletemessageapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=chat-delete-api-test",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
                "jwt.secret=MinhaChaveSecretaMuitoSeguraDeDesenvolvimento123"
        }
)
@EmbeddedKafka(partitions = 1, topics = {
        "chat-messages",
        "chat-message-status-events",
        "chat-message-deleted-events",
        "chat-message-edited-events"
})
class DeleteMessageApiIntegrationTest {

    private static final String JWT_SECRET = "MinhaChaveSecretaMuitoSeguraDeDesenvolvimento123";
    private static final String DELETED_TOPIC = "chat-message-deleted-events";
    private static final String EDITED_TOPIC = "chat-message-edited-events";

    @LocalServerPort
    private int port;

    @Autowired
    private ConversationJpaRepository conversationJpaRepository;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private Consumer<String, String> deletedEventConsumer;
    private Consumer<String, String> editedEventConsumer;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();

        Map<String, Object> consumerProps = org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps(
                "delete-message-api-" + UUID.randomUUID(),
                "false",
                embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        deletedEventConsumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();
        deletedEventConsumer.subscribe(List.of(DELETED_TOPIC));
        deletedEventConsumer.poll(Duration.ofMillis(200));

        editedEventConsumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();
        editedEventConsumer.subscribe(List.of(EDITED_TOPIC));
        editedEventConsumer.poll(Duration.ofMillis(200));
    }

    @AfterEach
    void tearDown() {
        if (deletedEventConsumer != null) {
            deletedEventConsumer.close();
        }
        if (editedEventConsumer != null) {
            editedEventConsumer.close();
        }
    }

    @Test
    void memberCannotDeleteAdminMessage() {
        SeededConversation seeded = seedConversationWithMessages();

        HttpResponse<String> response = sendDelete(
                endpoint(seeded.conversationId(), seeded.adminMessageId()),
                seeded.memberId(),
                seeded.tenantId()
        );

        assertTrue(response.statusCode() == HttpStatus.BAD_REQUEST.value() || response.statusCode() == HttpStatus.FORBIDDEN.value());
    }

    @Test
    void memberCannotEditAdminMessage() {
        SeededConversation seeded = seedConversationWithMessages();

        HttpResponse<String> response = sendPatch(
                endpoint(seeded.conversationId(), seeded.adminMessageId()),
                seeded.memberId(),
                seeded.tenantId(),
                "Tentativa de alteracao"
        );

        assertTrue(response.statusCode() == HttpStatus.BAD_REQUEST.value() || response.statusCode() == HttpStatus.FORBIDDEN.value());
    }

    @Test
    void adminDeletesMemberMessageAndHistoryHidesContentWhileKafkaPublishesEvent() throws Exception {
        SeededConversation seeded = seedConversationWithMessages();

        HttpResponse<String> deleteResponse = sendDelete(
                endpoint(seeded.conversationId(), seeded.memberMessageId()),
                seeded.adminId(),
                seeded.tenantId()
        );

        assertEquals(HttpStatus.NO_CONTENT.value(), deleteResponse.statusCode());

        MessageEntity deletedMessage = messageJpaRepository.findById(seeded.memberMessageId()).orElseThrow();
        assertEquals("Mensagem do membro", deletedMessage.getContent());

        Map<String, Object> deleteColumns = jdbcTemplate.queryForMap(
                "select deleted_at, deleted_by from messages where id = ?",
                seeded.memberMessageId()
        );
        assertNotNull(deleteColumns.get("deleted_at"));
        assertEquals(seeded.adminId(), deleteColumns.get("deleted_by"));

        HttpResponse<String> historyResponse = sendGet(
                "http://localhost:" + port + "/api/chat/conversations/" + seeded.conversationId() + "/messages?limit=20",
                seeded.adminId(),
                seeded.tenantId()
        );

        assertEquals(HttpStatus.OK.value(), historyResponse.statusCode());
        JsonNode historyJson = objectMapper.readTree(historyResponse.body());
        JsonNode deletedMessageJson = findMessage(historyJson.get("messages"), seeded.memberMessageId());
        assertNotNull(deletedMessageJson);
        assertTrue(deletedMessageJson.has("content"));
        assertTrue(deletedMessageJson.get("content").isNull());

        ConsumerRecord<String, String> eventRecord = awaitDeletedEvent(seeded.memberMessageId());
        JsonNode eventJson = objectMapper.readTree(eventRecord.value());
        assertEquals(seeded.memberMessageId().toString(), eventJson.get("messageId").asText());
        assertEquals(seeded.conversationId().toString(), eventJson.get("conversationId").asText());
        assertEquals(seeded.adminId().toString(), eventJson.get("deletedBy").asText());
        assertTrue(eventJson.hasNonNull("deletedAt"));
    }

    @Test
    void authorEditsOwnMessageAndHistoryShowsUpdatedContentWhileKafkaPublishesEvent() throws Exception {
        SeededConversation seeded = seedConversationWithMessages();
        String updatedContent = "Mensagem do membro editada";

        HttpResponse<String> patchResponse = sendPatch(
                endpoint(seeded.conversationId(), seeded.memberMessageId()),
                seeded.memberId(),
                seeded.tenantId(),
                updatedContent
        );

        assertEquals(HttpStatus.NO_CONTENT.value(), patchResponse.statusCode());

        Map<String, Object> editColumns = jdbcTemplate.queryForMap(
                "select content, edited_at, deleted_at from messages where id = ?",
                seeded.memberMessageId()
        );
        assertEquals(updatedContent, editColumns.get("content"));
        assertNotNull(editColumns.get("edited_at"));
        assertNull(editColumns.get("deleted_at"));

        HttpResponse<String> historyResponse = sendGet(
                "http://localhost:" + port + "/api/chat/conversations/" + seeded.conversationId() + "/messages?limit=20",
                seeded.memberId(),
                seeded.tenantId()
        );

        assertEquals(HttpStatus.OK.value(), historyResponse.statusCode());
        JsonNode historyJson = objectMapper.readTree(historyResponse.body());
        JsonNode editedMessageJson = findMessage(historyJson.get("messages"), seeded.memberMessageId());
        assertNotNull(editedMessageJson);
        assertEquals(updatedContent, editedMessageJson.get("content").asText());

    }

    @Test
    void deletedMessageCannotBeEdited() {
        SeededConversation seeded = seedConversationWithMessages();

        HttpResponse<String> deleteResponse = sendDelete(
                endpoint(seeded.conversationId(), seeded.memberMessageId()),
                seeded.adminId(),
                seeded.tenantId()
        );
        assertEquals(HttpStatus.NO_CONTENT.value(), deleteResponse.statusCode());

        HttpResponse<String> patchResponse = sendPatch(
                endpoint(seeded.conversationId(), seeded.memberMessageId()),
                seeded.memberId(),
                seeded.tenantId(),
                "Nao deveria editar"
        );

        assertEquals(HttpStatus.CONFLICT.value(), patchResponse.statusCode());
    }

    private SeededConversation seedConversationWithMessages() {
        UUID tenantId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID adminMessageId = UUID.randomUUID();
        UUID memberMessageId = UUID.randomUUID();

        ConversationEntity conversation = new ConversationEntity(
                conversationId,
                tenantId,
                ConversationType.GROUP,
                "Grupo de teste",
                adminId,
                Instant.parse("2026-03-31T12:00:00Z"),
                "Mensagem do membro",
                Instant.parse("2026-03-31T12:02:00Z"),
                "Regras"
        );
        conversation.addParticipant(new ConversationParticipantEntity(adminId, 0, null, ParticipantRole.ADMIN));
        conversation.addParticipant(new ConversationParticipantEntity(memberId, 0, null, ParticipantRole.MEMBER));
        ConversationEntity savedConversation = conversationJpaRepository.saveAndFlush(conversation);

        MessageEntity adminMessage = new MessageEntity(
                adminMessageId,
                savedConversation,
                adminId,
                "Mensagem do admin",
                MessageStatus.SENT,
                Instant.parse("2026-03-31T12:01:00Z")
        );
        MessageEntity memberMessage = new MessageEntity(
                memberMessageId,
                savedConversation,
                memberId,
                "Mensagem do membro",
                MessageStatus.SENT,
                Instant.parse("2026-03-31T12:02:00Z")
        );
        messageJpaRepository.saveAllAndFlush(List.of(adminMessage, memberMessage));

        return new SeededConversation(tenantId, conversationId, adminId, memberId, adminMessageId, memberMessageId);
    }

    private String endpoint(UUID conversationId, UUID messageId) {
        return "http://localhost:" + port + "/api/chat/conversations/" + conversationId + "/messages/" + messageId;
    }

    private HttpResponse<String> sendDelete(String url, UUID subjectId, UUID tenantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + generateJwt(subjectId, tenantId))
                    .DELETE()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao executar DELETE de teste", exception);
        }
    }

    private HttpResponse<String> sendGet(String url, UUID subjectId, UUID tenantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + generateJwt(subjectId, tenantId))
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao executar GET de teste", exception);
        }
    }

    private HttpResponse<String> sendPatch(String url, UUID subjectId, UUID tenantId, String content) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + generateJwt(subjectId, tenantId))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"content\":\"" + content + "\"}"))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao executar PATCH de teste", exception);
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

    private ConsumerRecord<String, String> awaitDeletedEvent(UUID messageId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = deletedEventConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (record.value().contains(messageId.toString())) {
                    return record;
                }
            }
        }
        throw new AssertionError("Nenhum evento de exclusão encontrado para a mensagem " + messageId);
    }

    private ConsumerRecord<String, String> awaitEditedEvent(UUID messageId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = editedEventConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (record.value().contains(messageId.toString())) {
                    return record;
                }
            }
        }
        throw new AssertionError("Nenhum evento de edição encontrado para a mensagem " + messageId);
    }

    private String generateJwt(UUID subject, UUID tenantId) {
        try {
            String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String payload = base64Url("{\"sub\":\"" + subject + "\",\"tenant_id\":\"" + tenantId + "\"}");
            String unsignedToken = header + "." + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));

            return unsignedToken + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao gerar JWT de teste", exception);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record SeededConversation(
            UUID tenantId,
            UUID conversationId,
            UUID adminId,
            UUID memberId,
            UUID adminMessageId,
            UUID memberMessageId
    ) {
    }
}
