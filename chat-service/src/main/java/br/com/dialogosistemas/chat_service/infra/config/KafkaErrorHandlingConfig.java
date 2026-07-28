package br.com.dialogosistemas.chat_service.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Antes: quando o consumer falhava ao encaminhar um evento pro WebSocket, o Spring tentava algumas
 * vezes e depois DESCARTAVA o evento em silencio (offset confirmado, sem rastro).
 *
 * Agora: apos esgotar as tentativas, o evento vai pro topico {@code <topico>.DLT}, ficando visivel
 * para monitoramento e investigacao.
 *
 * ponytail: o DLT aqui e OBSERVABILIDADE, nao re-entrega. Entrega em tempo real ao WebSocket e
 * best-effort (se o usuario nao esta conectado, nem da erro). Quem garante a mensagem e o banco +
 * GET /{conversationId}/messages?after=<cursor> no reconnect.
 *
 * O Boot injeta automaticamente este DefaultErrorHandler no container factory dos @KafkaListener.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        // 3 tentativas com 1s de intervalo antes de mandar pro .DLT
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}
