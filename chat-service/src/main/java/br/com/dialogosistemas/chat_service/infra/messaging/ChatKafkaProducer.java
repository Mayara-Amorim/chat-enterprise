package br.com.dialogosistemas.chat_service.infra.messaging;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
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
}