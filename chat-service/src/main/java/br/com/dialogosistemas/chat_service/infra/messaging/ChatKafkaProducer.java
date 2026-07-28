package br.com.dialogosistemas.chat_service.infra.messaging;

import br.com.dialogosistemas.chat_service.application.DTO.GroupEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageDeletedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageStatusUpdatedEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(ChatKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ChatKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(MessageSentEventDTO event) {
        publish("chat-messages", event.conversationId().toString(), event);
    }

    public void publishStatusUpdate(MessageStatusUpdatedEventDTO event) {
        publish("chat-message-status-events", event.conversationId().toString(), event);
    }

    public void publishMessageDeleted(MessageDeletedEventDTO event) {
        publish("chat-message-deleted-events", event.conversationId().toString(), event);
    }

    public void publishMessageEdited(MessageEditedEventDTO event) {
        publish("chat-message-edited-events", event.conversationId().toString(), event);
    }

    public void publishGroupEvent(GroupEventDTO event) {
        publish("chat-group-events", event.conversationId().toString(), event);
    }

    // O send do Kafka e assincrono; antes o CompletableFuture era descartado e a falha sumia em silencio.
    // Aqui logamos a falha. ponytail: so observabilidade — o cliente recupera o evento perdido
    // via GET /{conversationId}/messages?after=<cursor> ao reconectar, o banco continua sendo a fonte da verdade.
    private void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Falha ao publicar evento Kafka no topico {} (key={}): {}", topic, key, ex.getMessage(), ex);
            }
        });
    }
}
