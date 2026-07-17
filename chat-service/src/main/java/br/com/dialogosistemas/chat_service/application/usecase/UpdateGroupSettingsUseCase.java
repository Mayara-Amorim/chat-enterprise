package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.GroupEventDTO;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.MessagingPermission;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class UpdateGroupSettingsUseCase {

    private final ConversationGateway conversationGateway;
    private final ChatKafkaProducer kafkaProducer;

    public UpdateGroupSettingsUseCase(ConversationGateway conversationGateway, ChatKafkaProducer kafkaProducer) {
        this.conversationGateway = conversationGateway;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public void execute(UUID conversationUuid, UUID requesterUuid, String messagingPermissionStr) {
        var convId = new ConversationId(conversationUuid);
        var conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa nao encontrada"));

        MessagingPermission permission = MessagingPermission.valueOf(
                messagingPermissionStr.trim().toUpperCase(Locale.ROOT));

        conversation.updateMessagingPermission(new UserId(requesterUuid), permission);
        conversationGateway.save(conversation);

        kafkaProducer.publishGroupEvent(GroupEventDTO.settingsEvent(
                conversationUuid, requesterUuid, permission.name()));
    }
}
