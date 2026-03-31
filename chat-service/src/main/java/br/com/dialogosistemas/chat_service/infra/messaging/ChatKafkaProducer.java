package br.com.dialogosistemas.chat_service.infra.messaging;

import br.com.dialogosistemas.chat_service.application.DTO.MessageDeletedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageStatusUpdatedEventDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ChatKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(MessageSentEventDTO event) {
        kafkaTemplate.send("chat-messages", event.conversationId().toString(), event);
    }

    public void publishStatusUpdate(MessageStatusUpdatedEventDTO event) {
        kafkaTemplate.send("chat-message-status-events", event.conversationId().toString(), event);
    }

    public void publishMessageDeleted(MessageDeletedEventDTO event) {
        kafkaTemplate.send("chat-message-deleted-events", event.conversationId().toString(), event);
    }

    public void publishMessageEdited(MessageEditedEventDTO event) {
        kafkaTemplate.send("chat-message-edited-events", event.conversationId().toString(), event);
    }
}
