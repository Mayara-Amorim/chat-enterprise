package br.com.dialogosistemas.chat_service.infra.messaging;

import br.com.dialogosistemas.chat_service.application.DTO.MessageDeletedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageStatusUpdatedEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ChatKafkaConsumer.class);
    private final SimpMessagingTemplate websocketTemplate;

    public ChatKafkaConsumer(SimpMessagingTemplate websocketTemplate) {
        this.websocketTemplate = websocketTemplate;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-group")
    public void listen(MessageSentEventDTO event) {
        String destination = "/topic/chat." + event.conversationId();
        logger.info("Kafka recebeu evento de mensagem: {}", event);
        websocketTemplate.convertAndSend(destination, event);
        logger.info("Encaminhado para WebSocket: {}", destination);
    }

    @KafkaListener(topics = "chat-message-status-events", groupId = "chat-service-group")
    public void consumeStatusUpdate(MessageStatusUpdatedEventDTO event) {
        String destination = "/topic/chat." + event.conversationId() + ".status";
        logger.info("Kafka recebeu atualização de status: {}", event);
        websocketTemplate.convertAndSend(destination, event);
        logger.info("Status encaminhado para WebSocket: {}", destination);
    }

    @KafkaListener(topics = "chat-message-deleted-events", groupId = "chat-service-group")
    public void consumeDeletedMessage(MessageDeletedEventDTO event) {
        String destination = "/topic/chat." + event.conversationId() + ".deleted";
        logger.info("Kafka recebeu evento de exclusão: {}", event);
        websocketTemplate.convertAndSend(destination, event);
        logger.info("Exclusão encaminhada para WebSocket: {}", destination);
    }

    @KafkaListener(topics = "chat-message-edited-events", groupId = "chat-service-group")
    public void consumeEditedMessage(MessageEditedEventDTO event) {
        String destination = "/topic/chat." + event.conversationId() + ".edited";
        logger.info("Kafka recebeu evento de edição: {}", event);
        websocketTemplate.convertAndSend(destination, event);
        logger.info("Edição encaminhada para WebSocket: {}", destination);
    }
}
