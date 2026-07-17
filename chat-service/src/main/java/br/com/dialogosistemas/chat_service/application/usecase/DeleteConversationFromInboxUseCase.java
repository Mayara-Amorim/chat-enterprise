package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteConversationFromInboxUseCase {

    private final ConversationGateway conversationGateway;

    public DeleteConversationFromInboxUseCase(ConversationGateway conversationGateway) {
        this.conversationGateway = conversationGateway;
    }

    @Transactional
    public void execute(UUID conversationUuid, UUID userUuid) {
        var convId = new ConversationId(conversationUuid);
        var conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa nao encontrada"));

        conversation.deleteFromInbox(new UserId(userUuid));
        conversationGateway.save(conversation);
    }
}
