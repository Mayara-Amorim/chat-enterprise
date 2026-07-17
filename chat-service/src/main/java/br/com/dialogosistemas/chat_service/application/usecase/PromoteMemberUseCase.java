package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.GroupEventDTO;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PromoteMemberUseCase {

    private final ConversationGateway conversationGateway;
    private final ChatKafkaProducer kafkaProducer;

    public PromoteMemberUseCase(ConversationGateway conversationGateway, ChatKafkaProducer kafkaProducer) {
        this.conversationGateway = conversationGateway;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public void execute(UUID conversationUuid, UUID targetUuid, UUID requesterUuid) {
        var convId = new ConversationId(conversationUuid);
        var conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa nao encontrada"));

        conversation.promoteMember(new UserId(requesterUuid), new UserId(targetUuid));
        conversationGateway.save(conversation);

        kafkaProducer.publishGroupEvent(GroupEventDTO.memberEvent(
                "MEMBER_PROMOTED", conversationUuid, targetUuid, requesterUuid));
    }
}
