package br.com.dialogosistemas.chat_service.domain.gateway;

import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageGateway {
    // Salvamos a mensagem vinculando-a explicitamente a um ID de conversa
    Message save(Message message, ConversationId conversationId);
    Optional<Message> findById(MessageId messageId);
    List<Message> findHistoryBeforeCursor(ConversationId conversationId, Instant cursorDate, UUID cursorId, int limit);
    List<Message> findUnreadByParticipant(ConversationId conversationId, UserId userId);
    void saveAll(List<Message> messages, ConversationId conversationId);
}
