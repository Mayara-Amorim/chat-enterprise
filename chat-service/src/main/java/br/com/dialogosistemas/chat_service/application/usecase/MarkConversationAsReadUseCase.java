package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import br.com.dialogosistemas.chat_service.infra.persistence.ConversationRepositoryGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MarkConversationAsReadUseCase {

    private final ConversationRepositoryGateway repository;

    public MarkConversationAsReadUseCase(ConversationRepositoryGateway repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(UUID conversationId, UUID userId) {
        var conversation = repository.findById(new ConversationId(conversationId))
                .orElseThrow(() -> new ResourceNotFoundException("Not found conversation"));
                        conversation.markParticipantAsRead(new UserId(userId));

        repository.save(conversation);
    }
}