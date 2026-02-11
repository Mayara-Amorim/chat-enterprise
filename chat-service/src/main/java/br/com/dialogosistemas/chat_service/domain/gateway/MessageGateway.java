package br.com.dialogosistemas.chat_service.domain.gateway;

import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;

public interface MessageGateway {
    // Salvamos a mensagem vinculando-a explicitamente a um ID de conversa
    Message save(Message message, ConversationId conversationId);
}