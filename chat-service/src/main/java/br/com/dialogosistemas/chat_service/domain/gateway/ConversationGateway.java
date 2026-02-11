package br.com.dialogosistemas.chat_service.domain.gateway;

import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConversationGateway {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(ConversationId id);
    List<Conversation> findAllByParticipant(UserId userId);
    void updateLastMessage(ConversationId id, String content, Instant sentAt);

}