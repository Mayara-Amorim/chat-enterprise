package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageStatusUpdatedEventDTO;
import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.chat_service.infra.persistence.ConversationRepositoryGateway;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MarkConversationAsReadUseCase {

    private final ConversationRepositoryGateway conversationRepository;
    private final MessageGateway messageGateway;
    private final ChatKafkaProducer chatKafkaProducer; // Novo: Injeção do Produtor

    public MarkConversationAsReadUseCase(ConversationRepositoryGateway conversationRepository,
                                         MessageGateway messageGateway,
                                         ChatKafkaProducer chatKafkaProducer) {
        this.conversationRepository = conversationRepository;
        this.messageGateway = messageGateway;
        this.chatKafkaProducer = chatKafkaProducer;
    }

    @Transactional
    public void execute(UUID conversationId, UUID userId) {
        var convId = new ConversationId(conversationId);
        var user = new UserId(userId);

        var conversation = conversationRepository.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found conversation"));

        conversation.markParticipantAsRead(user);
        conversationRepository.save(conversation);

        List<Message> unreadMessages = messageGateway.findUnreadByParticipant(convId, user);
        int totalParticipants = conversation.getParticipants().size();

        for (Message msg : unreadMessages) {
            msg.markAsReadBy(user, totalParticipants);
        }

        if (!unreadMessages.isEmpty()) {
            messageGateway.saveAll(unreadMessages, convId);

            // Disparo de eventos após salvar no banco
            for (Message msg : unreadMessages) {
                MessageStatusUpdatedEventDTO event = new MessageStatusUpdatedEventDTO(
                        msg.getId().value(),
                        conversationId,
                        msg.getStatus(),
                        userId,
                        Instant.now()
                );
                chatKafkaProducer.publishStatusUpdate(event);
            }
        }
    }
}