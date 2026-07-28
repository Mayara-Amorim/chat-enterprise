package br.com.dialogosistemas.chat_service.infra.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMetricsTest {

    private SimpleMeterRegistry registry;
    private StubUserRegistry userRegistry;
    private ChatMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        userRegistry = new StubUserRegistry(3);
        metrics = new ChatMetrics(registry, userRegistry);
    }

    @Test
    void messageSentIncrementsCounter() {
        metrics.messageSent();
        metrics.messageSent();

        assertEquals(2.0, registry.get("chat.messages.sent").counter().count());
    }

    @Test
    void publishFailuresArePreRegisteredWithZeroPerTopic() {
        // Antes de qualquer falha, o counter ja existe com 0 (painel mostra 0, nao "No data").
        assertEquals(0.0, registry.get("chat.kafka.publish.failures")
                .tag("topic", "chat-messages").counter().count());
    }

    @Test
    void publishFailedIncrementsTaggedCounter() {
        metrics.publishFailed("chat-messages");

        assertEquals(1.0, registry.get("chat.kafka.publish.failures")
                .tag("topic", "chat-messages").counter().count());
    }

    @Test
    void usersOnlineReflectsRegistryCount() {
        assertEquals(3.0, registry.get("chat.users.online").gauge().value());
    }

    @Test
    void usersOnlineGoesToZeroWhenDraining() {
        metrics.stop();

        assertEquals(0.0, registry.get("chat.users.online").gauge().value());
    }

    @Test
    void shutdownPhaseIsBelowWebServerAndKafka() {
        assertEquals(Integer.MAX_VALUE - 1000, metrics.getPhase());
    }

    private static final class StubUserRegistry implements SimpUserRegistry {
        private final int count;

        private StubUserRegistry(int count) {
            this.count = count;
        }

        @Override
        public int getUserCount() {
            return count;
        }

        @Override
        public SimpUser getUser(String userName) {
            return null;
        }

        @Override
        public Set<SimpUser> getUsers() {
            return Set.of();
        }

        @Override
        public Set<SimpSubscription> findSubscriptions(org.springframework.messaging.simp.user.SimpSubscriptionMatcher matcher) {
            return Set.of();
        }
    }
}
