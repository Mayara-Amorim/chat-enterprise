package br.com.dialogosistemas.chat_service.infra.messaging;
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
        // A chave (key) é o conversationId para garantir ordem de entrega na mesma partição
        kafkaTemplate.send("chat-messages", event.conversationId().toString(), event);
    }

    public void publishStatusUpdate(MessageStatusUpdatedEventDTO event) {
        // Publica no tópico 'chat-message-status-events' usando o ID da conversa como chave de partição
        kafkaTemplate.send("chat-message-status-events", event.conversationId().toString(), event);
        System.out.println("DEBUG: 📤 Evento de status publicado no Kafka para a mensagem: " + event.messageId() + " | Novo Status: " + event.status());
    }
}