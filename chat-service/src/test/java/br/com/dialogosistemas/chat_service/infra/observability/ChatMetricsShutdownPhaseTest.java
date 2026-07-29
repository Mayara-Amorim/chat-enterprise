package br.com.dialogosistemas.chat_service.infra.observability;

import br.com.dialogosistemas.chat_service.ChatServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mede as fases reais de shutdown nas versoes de Spring/Spring Kafka em uso (spec Fatia A secao 2.2
 * premissa 2, secao 8, criterio de aceite 11).
 *
 * A spec anotava os valores esperados com a ressalva "confirmar na versao em uso". Confirmado — e a
 * anotacao estava errada: o web server do Boot 4.0.2 nao esta em MAX_VALUE-1, e sim em MAX-1024 e
 * MAX-2048, em dois beans. Alem disso, os beans que seguram as sessoes WebSocket estao na fase 0,
 * o que torna "parar depois de tudo que drena conexao" impossivel com fase positiva. Ver o javadoc
 * de {@link ChatMetrics#SHUTDOWN_PHASE} para a analise completa.
 *
 * Por isso este teste NAO afirma a invariante como a spec a escreveu — ela era falsa. Afirma o que
 * de fato sustenta o zero final:
 *
 *   1. ChatMetrics e um SmartLifecycle. Isso, e so isso, garante que stop() roda antes de
 *      destroyBeans() e portanto antes do publish final em PushMeterRegistry.close().
 *   2. Os listeners Kafka param ANTES do ChatMetrics — a instancia para de contar depois de parar
 *      de consumir, nao antes.
 *
 * Por que nao um smoke manual: no Windows nao ha SIGTERM, entao matar o processo nunca dispara o
 * shutdown hook e a ordem nao seria observavel localmente.
 */
@SpringBootTest(
        classes = ChatServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:shutdownphase;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.kafka.listener.auto-startup=false",
                "management.otlp.metrics.export.enabled=false"
        })
class ChatMetricsShutdownPhaseTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("deve_participar_do_lifecycle_para_que_stop_preceda_o_publish_final")
    void chatMetricsIsASmartLifecycleBean() {
        // Esta e a garantia dura: o Spring roda todo SmartLifecycle.stop() em
        // lifecycleProcessor.onClose(), que precede destroyBeans() — e o publish final do
        // PushMeterRegistry acontece no destroy. Independe do valor da fase.
        assertInstanceOf(SmartLifecycle.class, context.getBean(ChatMetrics.class));
    }

    @Test
    @DisplayName("deve_parar_depois_dos_listeners_kafka_quando_o_contexto_fecha")
    void kafkaListenersStopBeforeChatMetrics() {
        SmartLifecycle kafkaRegistry = findKafkaListenerRegistry()
                .orElseThrow(() -> new AssertionError(
                        "KafkaListenerEndpointRegistry nao encontrado. Sem ele este teste passaria "
                                + "por ausencia de sujeito — revalidar antes de seguir."));

        // Fase MAIOR para primeiro: os listeners precisam estar ACIMA do ChatMetrics.
        assertTrue(kafkaRegistry.getPhase() > ChatMetrics.SHUTDOWN_PHASE,
                "listeners Kafka (fase %d) deveriam parar antes do ChatMetrics (fase %d)"
                        .formatted(kafkaRegistry.getPhase(), ChatMetrics.SHUTDOWN_PHASE));
    }

    @Test
    @DisplayName("deve_registrar_a_realidade_medida_das_fases_do_web_server")
    void webServerPhasesAreBelowChatMetrics() {
        // Trava a realidade MEDIDA, nao a que a spec supunha. Se um upgrade do Spring mudar isso,
        // este teste quebra e alguem reavalia — em vez de o javadoc envelhecer em silencio.
        Map<String, SmartLifecycle> lifecycles = context.getBeansOfType(SmartLifecycle.class);

        boolean foundWebServer = false;
        for (Map.Entry<String, SmartLifecycle> entry : lifecycles.entrySet()) {
            if (!entry.getValue().getClass().getName().contains("WebServer")) {
                continue;
            }
            foundWebServer = true;
            assertTrue(entry.getValue().getPhase() < ChatMetrics.SHUTDOWN_PHASE,
                    ("%s mudou de fase (%d) e agora para ANTES do ChatMetrics (%d). Isso nao quebra "
                            + "o zero final (garantido pela flag draining), mas contraria o javadoc "
                            + "de SHUTDOWN_PHASE — atualizar a analise.")
                            .formatted(entry.getKey(), entry.getValue().getPhase(), ChatMetrics.SHUTDOWN_PHASE));
        }

        assertTrue(foundWebServer, "nenhum bean de web server no contexto — teste sem sujeito");
    }

    private Optional<SmartLifecycle> findKafkaListenerRegistry() {
        return context.getBeansOfType(SmartLifecycle.class).values().stream()
                .filter(bean -> bean.getClass().getName().startsWith("org.springframework.kafka")
                        && bean.getClass().getName().contains("EndpointRegistry"))
                .findFirst();
    }
}
