package br.com.dialogosistemas.chat_service.infra.messaging;

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

    // O SimpMessagingTemplate é a classe do Spring que permite "empurrar" dados para o WebSocket
    public ChatKafkaConsumer(SimpMessagingTemplate websocketTemplate) {
        this.websocketTemplate = websocketTemplate;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-group")
    public void listen(MessageSentEventDTO event) {
        logger.info("📩 Kafka recebeu evento: {}", event);

        // Define o destino dinâmico: /topic/chat.{conversationId}
        String destination = "/topic/chat." + event.conversationId();

        // Envia para todos os clientes inscritos nessa conversa específica
        websocketTemplate.convertAndSend(destination, event);

        logger.info("📡 Encaminhado para WebSocket: {}", destination);
    }

    @KafkaListener(topics = "chat-message-status-events", groupId = "chat-service-group")
    public void consumeStatusUpdate(MessageStatusUpdatedEventDTO event) {
        System.out.println("DEBUG: 📥 Kafka recebeu atualização de status: " + event);

        // Roteia para um sub-tópico específico de status da conversa
        String destination = "/topic/chat." + event.conversationId() + ".status";

        websocketTemplate.convertAndSend(destination, event);
        System.out.println("DEBUG: 📡 Status encaminhado para WebSocket no canal: " + destination);
    }
}