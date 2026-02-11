package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.SendMessageRequestDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SendMessageUseCase {

    private final ConversationGateway conversationGateway;
    private final ChatKafkaProducer kafkaProducer;

    public SendMessageUseCase(ConversationGateway conversationGateway, ChatKafkaProducer kafkaProducer) {
        this.conversationGateway = conversationGateway;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public void execute(SendMessageRequestDTO request, UUID senderId) {
        UserId user = new UserId(senderId);
       ConversationId convId = new ConversationId(request.conversationId());

        // 1. Buscar a conversa
        Conversation conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // 2. Adicionar mensagem (O Domínio valida se o sender participa)
        Message newMessage = conversation.addMessage(user, request.content());

        // 3. Salvar no banco (A mensagem é salva em cascata ou via gateway)
        conversationGateway.save(conversation);

        // 4. Publicar evento no Kafka (Assíncrono)
        MessageSentEventDTO event = new MessageSentEventDTO(
                newMessage.getId().value(),
                conversation.getId().value(),
                newMessage.getSenderId().value(),
                newMessage.getContent(),
                newMessage.getCreatedAt()
        );
        kafkaProducer.send(event);
    }
}