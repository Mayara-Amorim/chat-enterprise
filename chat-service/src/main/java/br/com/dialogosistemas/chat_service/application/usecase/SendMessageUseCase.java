package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.SendMessageRequestDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
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
    private final MessageGateway messageGateway; //

    public SendMessageUseCase(ConversationGateway conversationGateway, ChatKafkaProducer kafkaProducer, MessageGateway messageGateway) {
        this.conversationGateway = conversationGateway;
        this.kafkaProducer = kafkaProducer;
        this.messageGateway = messageGateway;
    }
    @Transactional
    public void execute(SendMessageRequestDTO request, UUID senderId) {
        UserId user = new UserId(senderId);
      ConversationId convId = new ConversationId(request.conversationId());
        Conversation conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Adicionar mensagem na memória (Valida se o usuário pode mandar mensagem)
        // Isso retorna o objeto Message criado e validado
        Message newMessage = conversation.addMessage(user, request.content());

        // Salvar APENAS a mensagem, usando o novo Gateway
        // Não salva mais a conversation inteira
        messageGateway.save(newMessage, convId);

        conversationGateway.updateLastMessage(
                convId,
                newMessage.getContent(),
                newMessage.getCreatedAt()
        );

        // 4. Publicar evento no Kafka
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