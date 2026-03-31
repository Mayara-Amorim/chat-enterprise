package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EditMessageUseCase {

    private final MessageGateway messageGateway;
    private final ChatKafkaProducer chatKafkaProducer;

    public EditMessageUseCase(MessageGateway messageGateway, ChatKafkaProducer chatKafkaProducer) {
        this.messageGateway = messageGateway;
        this.chatKafkaProducer = chatKafkaProducer;
    }

    @Transactional
    public void execute(UUID conversationUuid, UUID messageUuid, UUID requesterUuid, String newContent) {
        ConversationId conversationId = new ConversationId(conversationUuid);
        MessageId messageId = new MessageId(messageUuid);
        UserId requesterId = new UserId(requesterUuid);

        var message = messageGateway.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem não encontrada"));

        if (!message.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("A mensagem não pertence a esta conversa.");
        }

        message.edit(newContent, requesterId);
        messageGateway.save(message, conversationId);

        chatKafkaProducer.publishMessageEdited(new MessageEditedEventDTO(
                messageUuid,
                conversationUuid,
                requesterUuid,
                message.getContent(),
                message.getEditedAt()
        ));
    }
}
