package br.com.dialogosistemas.chat_service.infra.observability;

import br.com.dialogosistemas.chat_service.infra.messaging.ChatTopics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ChatMetrics.class);

    /**
     * Fase de shutdown do ChatMetrics. No SmartLifecycle, fase MAIOR para PRIMEIRO.
     *
     * FASES REAIS MEDIDAS (Boot 4.0.2 / Spring Kafka em uso — ver ChatMetricsShutdownPhaseTest):
     *   MAX-100    KafkaListenerEndpointRegistry     para 1o
     *   MAX-1000   ChatMetrics (este)                para 2o
     *   MAX-1024   WebServerGracefulShutdownLifecycle
     *   MAX-2048   WebServerStartStopLifecycle
     *   0          subProtocolWebSocketHandler / simpleBrokerMessageHandler / userDestination...
     *
     * CORRECAO DA SPEC: a Fatia A secao 2.2 documentava o web server em MAX_VALUE-1 e pedia que
     * este componente parasse DEPOIS de tudo que drena conexao. Os dois pontos estao errados:
     *   1. No Boot 4.0.2 o web server sao DOIS beans, em MAX-1024 e MAX-2048, nao MAX-1.
     *   2. "Parar por ultimo entre os que drenam conexao" e INALCANCAVEL com fase positiva — os
     *      beans que seguram as sessoes STOMP/WebSocket (que alimentam o SimpUserRegistry) estao
     *      na fase 0.
     *
     * E nao precisa ser alcancavel. O zero final nao depende de ordem de fase, e sim da flag
     * 'draining' (secao 8: "com a flag, o valor 0 nao depende mais da ordem do drain") somada a
     * garantia do Spring de que lifecycleProcessor.onClose() roda antes de destroyBeans() — e o
     * publish final acontece no destroy, dentro de PushMeterRegistry.close(). Isso vale para
     * QUALQUER fase, entao o valor aqui e conservador, nao critico.
     *
     * O que o valor ainda compra: parar depois dos listeners Kafka (MAX-100) e antes do drain do
     * web server, ou seja, a instancia deixa de se declarar "com usuarios online" cedo no shutdown
     * em vez de tarde — util em rolling deploy.
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
        // fizer o publish final no close(), o gauge ja le 0. Verificado no fonte do Micrometer:
        // PushMeterRegistry.close() chama publishSafelyOrSkipIfInProgress(), e o Spring roda todo
        // SmartLifecycle.stop() antes de destroyBeans() — entao a ordem esta garantida.
        int stillConnected = userRegistry.getUserCount();
        draining.set(true);
        running = false;
        // Log deliberado: e a unica evidencia observavel de que o zero final saiu. Sem ele, o smoke
        // de shutdown (spec secao 5) nao tem como distinguir "publicou 0" de "nao publicou nada".
        logger.info("ChatMetrics drenando na fase {}: {} usuario(s) ainda conectado(s), "
                + "gauge chat.users.online forcado a 0 para o publish final", SHUTDOWN_PHASE, stillConnected);
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
