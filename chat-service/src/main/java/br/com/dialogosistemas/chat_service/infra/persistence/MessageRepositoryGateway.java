package br.com.dialogosistemas.chat_service.infra.persistence;

import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MessageRepositoryGateway implements MessageGateway {

    private final MessageJpaRepository messageRepository;
    private final ConversationJpaRepository conversationRepository;

    public MessageRepositoryGateway(MessageJpaRepository messageRepository, ConversationJpaRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Override
    @Transactional
    public Message save(Message message, ConversationId conversationId) {
        // 1. Precisamos de uma referência da Conversa para o FK (Foreign Key)
        // O getReferenceById é mais leve que o findById pois não faz select, apenas cria um proxy com o ID
        ConversationEntity conversationRef = conversationRepository.getReferenceById(conversationId.value());

        // mapeamento manual rápido (Domain -> Entity)
        MessageEntity entity = new MessageEntity(
                message.getId().value(),
                conversationRef, // Vincula o pai
                message.getSenderId().value(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt()
        );
        messageRepository.save(entity);

        return message;
    }
}