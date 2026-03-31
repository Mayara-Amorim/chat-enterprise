package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageDeletedEventDTO;
import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteMessageUseCase {

    private final MessageGateway messageGateway;
    private final ConversationGateway conversationGateway;
    private final ChatKafkaProducer chatKafkaProducer;

    public DeleteMessageUseCase(MessageGateway messageGateway,
                                ConversationGateway conversationGateway,
                                ChatKafkaProducer chatKafkaProducer) {
        this.messageGateway = messageGateway;
        this.conversationGateway = conversationGateway;
        this.chatKafkaProducer = chatKafkaProducer;
    }

    @Transactional
    public void execute(UUID conversationUuid, UUID messageUuid, UUID requesterUuid) {
        ConversationId conversationId = new ConversationId(conversationUuid);
        MessageId messageId = new MessageId(messageUuid);
        UserId requesterId = new UserId(requesterUuid);

        var conversation = conversationGateway.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada"));

        boolean isRequesterAdmin = conversation.getParticipants().stream()
                .anyMatch(participant -> participant.getUserId().equals(requesterId) && participant.getRole() == ParticipantRole.ADMIN);

        var message = messageGateway.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem não encontrada"));

        if (!message.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("A mensagem não pertence a esta conversa.");
        }

        message.deleteBy(requesterId, isRequesterAdmin);
        messageGateway.save(message, conversationId);

        chatKafkaProducer.publishMessageDeleted(new MessageDeletedEventDTO(
                messageUuid,
                conversationUuid,
                requesterUuid,
                message.getDeletedAt()
        ));
    }
}
