package br.com.dialogosistemas.chat_service.infra.messaging;

import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.infra.observability.ChatMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatKafkaProducerMetricsTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private SimpleMeterRegistry registry;
    private ChatMetrics metrics;
    private ChatKafkaProducer producer;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ChatMetrics(registry, new ZeroUserRegistry());
        producer = new ChatKafkaProducer(kafkaTemplate, metrics);
    }

    @Test
    void successIncrementsMessagesSentOnly() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.<SendResult<String, Object>>completedFuture(null));

        producer.send(event());

        assertEquals(1.0, registry.get("chat.messages.sent").counter().count());
        assertEquals(0.0, registry.get("chat.kafka.publish.failures")
                .tag("topic", "chat-messages").counter().count());
    }

    @Test
    void failureIncrementsPublishFailuresNotMessagesSent() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        producer.send(event());

        assertEquals(0.0, registry.get("chat.messages.sent").counter().count());
        assertEquals(1.0, registry.get("chat.kafka.publish.failures")
                .tag("topic", "chat-messages").counter().count());
    }

    private static MessageSentEventDTO event() {
        return new MessageSentEventDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "ola", Instant.parse("2026-07-28T12:00:00Z"));
    }

    private static final class ZeroUserRegistry implements SimpUserRegistry {
        @Override public int getUserCount() { return 0; }
        @Override public SimpUser getUser(String userName) { return null; }
        @Override public Set<SimpUser> getUsers() { return Set.of(); }
        @Override public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) { return Set.of(); }
    }
}
