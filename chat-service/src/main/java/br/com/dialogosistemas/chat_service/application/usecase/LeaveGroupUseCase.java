package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.GroupEventDTO;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LeaveGroupUseCase {

    private final ConversationGateway conversationGateway;
    private final ChatKafkaProducer kafkaProducer;

    public LeaveGroupUseCase(ConversationGateway conversationGateway, ChatKafkaProducer kafkaProducer) {
        this.conversationGateway = conversationGateway;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public void execute(UUID conversationUuid, UUID userUuid, boolean deleteConversation) {
        var convId = new ConversationId(conversationUuid);
        var userId = new UserId(userUuid);

        Conversation conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa nao encontrada"));

        boolean wasDissolvedBefore = conversation.getDissolvedAt() != null;
        conversation.leaveGroup(userId, deleteConversation);
        conversationGateway.save(conversation);

        if (!wasDissolvedBefore && conversation.getDissolvedAt() != null) {
            kafkaProducer.publishGroupEvent(GroupEventDTO.dissolvedEvent(conversationUuid, userUuid));
        } else {
            kafkaProducer.publishGroupEvent(GroupEventDTO.memberEvent(
                    "MEMBER_LEFT", conversationUuid, userUuid, userUuid));
        }
    }
}
