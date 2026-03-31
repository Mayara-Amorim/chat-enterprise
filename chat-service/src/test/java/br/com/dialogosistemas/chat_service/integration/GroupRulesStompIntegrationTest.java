package br.com.dialogosistemas.chat_service.integration;

import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageDeletedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:chatstomp;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=chat-group-test",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
                "jwt.secret=MinhaChaveSecretaMuitoSeguraDeDesenvolvimento123"
        }
)
@EmbeddedKafka(partitions = 1, topics = {"chat-messages", "chat-message-status-events", "chat-message-deleted-events", "chat-message-edited-events"})
class GroupRulesStompIntegrationTest {

    private static final String JWT_SECRET = "MinhaChaveSecretaMuitoSeguraDeDesenvolvimento123";

    @LocalServerPort
    private int port;

    @Autowired
    private ChatKafkaProducer chatKafkaProducer;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private ConversationJpaRepository conversationJpaRepository;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    private WebSocketStompClient stompClient;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        kafkaListenerEndpointRegistry.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(container, 1));

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        converter.setObjectMapper(objectMapper);

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(converter);
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void subscribedClientReceivesRulesMessageBroadcastFromKafka() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MessageSentEventDTO expectedEvent = new MessageSentEventDTO(
                UUID.randomUUID(),
                conversationId,
                senderId,
                "Regras do Grupo:\nProibido falar de politica",
                Instant.now()
        );
        BlockingQueue<MessageSentEventDTO> receivedEvents = new LinkedBlockingQueue<>();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + generateJwt(senderId, UUID.randomUUID()));

        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                                        byte[] payload, Throwable exception) {
                                throw new AssertionError("STOMP handshake failed", exception);
                            }

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                throw new AssertionError("STOMP transport failed", exception);
                            }
                        }
                )
                .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/chat." + conversationId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageSentEventDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedEvents.add((MessageSentEventDTO) payload);
            }
        });

        Thread.sleep(300);
        chatKafkaProducer.send(expectedEvent);

        MessageSentEventDTO receivedEvent = receivedEvents.poll(10, TimeUnit.SECONDS);

        assertNotNull(receivedEvent);
        assertEquals(expectedEvent.messageId(), receivedEvent.messageId());
        assertEquals(expectedEvent.conversationId(), receivedEvent.conversationId());
        assertEquals(expectedEvent.senderId(), receivedEvent.senderId());
        assertEquals(expectedEvent.content(), receivedEvent.content());

        session.disconnect();
    }

    @Test
    void subscribedClientReceivesDeletedEventWhenDeleteEndpointReturnsNoContent() throws Exception {
        SeededConversation seeded = seedConversationWithMessages();
        BlockingQueue<MessageDeletedEventDTO> deletedEvents = new LinkedBlockingQueue<>();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + generateJwt(seeded.adminId(), seeded.tenantId()));

        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                                        byte[] payload, Throwable exception) {
                                throw new AssertionError("STOMP handshake failed", exception);
                            }

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                throw new AssertionError("STOMP transport failed", exception);
                            }
                        }
                )
                .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/chat." + seeded.conversationId() + ".deleted", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageDeletedEventDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                deletedEvents.add((MessageDeletedEventDTO) payload);
            }
        });

        Thread.sleep(300);

        HttpResponse<String> deleteResponse = sendDelete(
                "http://localhost:" + port + "/api/chat/conversations/" + seeded.conversationId() + "/messages/" + seeded.memberMessageId(),
                seeded.adminId(),
                seeded.tenantId()
        );

        assertEquals(HttpStatus.NO_CONTENT.value(), deleteResponse.statusCode());

        MessageDeletedEventDTO receivedEvent = deletedEvents.poll(10, TimeUnit.SECONDS);
        assertNotNull(receivedEvent);
        assertEquals(seeded.memberMessageId(), receivedEvent.messageId());
        assertEquals(seeded.conversationId(), receivedEvent.conversationId());
        assertEquals(seeded.adminId(), receivedEvent.deletedBy());
        assertNotNull(receivedEvent.deletedAt());

        session.disconnect();
    }

    @Test
    void subscribedClientReceivesEditedEventWhenPatchEndpointReturnsNoContent() throws Exception {
        SeededConversation seeded = seedConversationWithMessages();
        BlockingQueue<MessageEditedEventDTO> editedEvents = new LinkedBlockingQueue<>();
        String updatedContent = "Mensagem do membro editada";

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + generateJwt(seeded.memberId(), seeded.tenantId()));

        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                                        byte[] payload, Throwable exception) {
                                throw new AssertionError("STOMP handshake failed", exception);
                            }

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                throw new AssertionError("STOMP transport failed", exception);
                            }
                        }
                )
                .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/chat." + seeded.conversationId() + ".edited", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageEditedEventDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                editedEvents.add((MessageEditedEventDTO) payload);
            }
        });

        Thread.sleep(300);

        HttpResponse<String> patchResponse = sendPatch(
                "http://localhost:" + port + "/api/chat/conversations/" + seeded.conversationId() + "/messages/" + seeded.memberMessageId(),
                seeded.memberId(),
                seeded.tenantId(),
                updatedContent
        );

        assertEquals(HttpStatus.NO_CONTENT.value(), patchResponse.statusCode());

        MessageEditedEventDTO receivedEvent = editedEvents.poll(10, TimeUnit.SECONDS);
        assertNotNull(receivedEvent);
        assertEquals(seeded.memberMessageId(), receivedEvent.messageId());
        assertEquals(seeded.conversationId(), receivedEvent.conversationId());
        assertEquals(seeded.memberId(), receivedEvent.editedBy());
        assertEquals(updatedContent, receivedEvent.content());
        assertNotNull(receivedEvent.editedAt());

        session.disconnect();
    }

    private SeededConversation seedConversationWithMessages() {
        UUID tenantId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID memberMessageId = UUID.randomUUID();

        ConversationEntity conversation = new ConversationEntity(
                conversationId,
                tenantId,
                ConversationType.GROUP,
                "Grupo de exclusao",
                adminId,
                Instant.parse("2026-03-31T12:00:00Z"),
                "Mensagem do membro",
                Instant.parse("2026-03-31T12:02:00Z"),
                "Regras"
        );
        conversation.addParticipant(new ConversationParticipantEntity(adminId, 0, null, ParticipantRole.ADMIN));
        conversation.addParticipant(new ConversationParticipantEntity(memberId, 0, null, ParticipantRole.MEMBER));
        ConversationEntity savedConversation = conversationJpaRepository.saveAndFlush(conversation);

        MessageEntity memberMessage = new MessageEntity(
                memberMessageId,
                savedConversation,
                memberId,
                "Mensagem do membro",
                MessageStatus.SENT,
                Instant.parse("2026-03-31T12:02:00Z")
        );
        messageJpaRepository.saveAllAndFlush(List.of(memberMessage));

        return new SeededConversation(tenantId, conversationId, adminId, memberId, memberMessageId);
    }

    private HttpResponse<String> sendDelete(String url, UUID subjectId, UUID tenantId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + generateJwt(subjectId, tenantId))
                .DELETE()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPatch(String url, UUID subjectId, UUID tenantId, String content) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + generateJwt(subjectId, tenantId))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"content\":\"" + content + "\"}"))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String generateJwt(UUID subject, UUID tenantId) throws Exception {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"" + subject + "\",\"tenant_id\":\"" + tenantId + "\"}");
        String unsignedToken = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));

        return unsignedToken + "." + signature;
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
            UUID memberMessageId
    ) {
    }
}
