package br.com.dialogosistemas.chat_service.infra.messaging;

import br.com.dialogosistemas.chat_service.application.DTO.GroupEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageDeletedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageStatusUpdatedEventDTO;
import br.com.dialogosistemas.chat_service.infra.observability.ChatMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ChatKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(ChatKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ChatMetrics metrics;

    public ChatKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, ChatMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    public void send(MessageSentEventDTO event) {
        // Conta so no sucesso (thenRun nao roda em falha): "mensagens/s" = publicacoes confirmadas.
        publish(ChatTopics.MESSAGES, event.conversationId().toString(), event)
                .thenRun(metrics::messageSent);
    }

    public void publishStatusUpdate(MessageStatusUpdatedEventDTO event) {
        publish(ChatTopics.STATUS, event.conversationId().toString(), event);
    }

    public void publishMessageDeleted(MessageDeletedEventDTO event) {
        publish(ChatTopics.DELETED, event.conversationId().toString(), event);
    }

    public void publishMessageEdited(MessageEditedEventDTO event) {
        publish(ChatTopics.EDITED, event.conversationId().toString(), event);
    }

    public void publishGroupEvent(GroupEventDTO event) {
        publish(ChatTopics.GROUP, event.conversationId().toString(), event);
    }

    // O send do Kafka e assincrono; o CompletableFuture era descartado e a falha sumia. Aqui logamos
    // e contamos a falha (observabilidade). O cliente recupera o evento perdido via
    // GET /{conversationId}/messages?after=<cursor> ao reconectar — o banco e a fonte da verdade.
    private CompletableFuture<SendResult<String, Object>> publish(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Falha ao publicar evento Kafka no topico {} (key={}): {}", topic, key, ex.getMessage(), ex);
                metrics.publishFailed(topic);
            }
        });
        return future;
    }
}
