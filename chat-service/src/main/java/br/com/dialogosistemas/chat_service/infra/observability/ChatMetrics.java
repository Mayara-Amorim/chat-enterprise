package br.com.dialogosistemas.chat_service.infra.observability;

import br.com.dialogosistemas.chat_service.infra.messaging.ChatTopics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Metricas de negocio e de saude do chat-service (Fatia A). Encapsula o Micrometer para que
 * application/domain nao dependam dele.
 */
@Component
public class ChatMetrics implements SmartLifecycle {

    /**
     * Fase de shutdown do ChatMetrics.
     *
     * INVARIANTE (ver spec Fatia A secao 8 e spec Fatia C):
     * No SmartLifecycle, fase MAIOR para PRIMEIRO. Este componente precisa parar DEPOIS de tudo que
     * ainda drena conexao, para que o publish final do registry leve 0 no gauge de online:
     *   - WebServerStartStopLifecycle .......... Integer.MAX_VALUE - 1
     *   - Kafka AbstractMessageListenerContainer  Integer.MAX_VALUE - 100
     * Dai MAX_VALUE - 1000: folga suficiente abaixo de ambos.
     *
     * Alterar este valor invalida o teste de convergencia (spec secao 5). Confirmar os dois valores
     * acima na versao do Spring/Spring Kafka em uso antes de considerar a invariante validada.
     */
    public static final int SHUTDOWN_PHASE = Integer.MAX_VALUE - 1000;

    private static final String MESSAGES_SENT = "chat.messages.sent";
    private static final String USERS_ONLINE = "chat.users.online";
    private static final String PUBLISH_FAILURES = "chat.kafka.publish.failures";

    private final MeterRegistry registry;
    private final SimpUserRegistry userRegistry;
    private final Counter messagesSent;
    private final AtomicBoolean draining = new AtomicBoolean(false);
    private volatile boolean running;

    public ChatMetrics(MeterRegistry registry, SimpUserRegistry userRegistry) {
        this.registry = registry;
        this.userRegistry = userRegistry;

        this.messagesSent = Counter.builder(MESSAGES_SENT)
                .description("New chat messages successfully published to Kafka")
                .register(registry);

        Gauge.builder(USERS_ONLINE, this, ChatMetrics::readOnlineUsers)
                .description("Authenticated users connected via WebSocket to this instance")
                .register(registry);

        // Pre-registra os failure counters por topico conhecido: exportam 0 desde o boot,
        // entao o painel mostra 0 (nao "No data") e o alerta por ausencia-de-dados significa algo.
        for (String topic : ChatTopics.ALL) {
            Counter.builder(PUBLISH_FAILURES)
                    .tag("topic", topic)
                    .description("Kafka publish failures per topic")
                    .register(registry);
        }
    }

    /** Chamado no callback de sucesso do producer (mensagem nova confirmada no broker). */
    public void messageSent() {
        messagesSent.increment();
    }

    /** Chamado no callback de erro do producer, para qualquer topico. */
    public void publishFailed(String topic) {
        registry.counter(PUBLISH_FAILURES, "topic", topic).increment();
    }

    // Gauge deterministico: 0 quando drenando (shutdown), senao a contagem local do broker STOMP.
    private double readOnlineUsers() {
        return draining.get() ? 0d : userRegistry.getUserCount();
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        // Seta draining ANTES do registry fechar (ordenado por getPhase): quando o PushMeterRegistry
        // fizer o publish final no close, o gauge ja le 0. Premissa a verificar no smoke (spec 2.2).
        draining.set(true);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return SHUTDOWN_PHASE;
    }
}
