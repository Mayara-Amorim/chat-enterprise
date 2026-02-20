package br.com.dialogosistemas.chat_service.infra.persistence;

import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageReadReceipt;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageReadReceiptEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        ConversationEntity conversationRef = conversationRepository.getReferenceById(conversationId.value());
        MessageEntity entity = toEntity(message, conversationRef);
        messageRepository.save(entity);
        return message;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findUnreadByParticipant(ConversationId conversationId, UserId userId) {
        // Passamos o Enum explicitamente agora
        return messageRepository.findUnreadByParticipant(
                conversationId.value(),
                userId.value(),
                br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus.READ
        ).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveAll(List<Message> messages, ConversationId conversationId) {
        if (messages.isEmpty()) {
            System.out.println("DEBUG: Nenhuma mensagem para atualizar status.");
            return;
        }

        System.out.println("DEBUG: Atualizando " + messages.size() + " mensagens para o status LIDO.");


        List<UUID> messageIds = messages.stream().map(m -> m.getId().value()).toList();

        // 2. Buscamos as Entidades Gerenciadas pelo Hibernate (Attached)
        List<MessageEntity> managedEntities = messageRepository.findAllById(messageIds);

        // 3. Atualizamos o estado da Entidade com base no Domínio
        for (MessageEntity entity : managedEntities) {
            Message domainMessage = messages.stream()
                    .filter(m -> m.getId().value().equals(entity.getId()))
                    .findFirst()
                    .orElseThrow();

            // Atualiza o Status (ex: de SENT para DELIVERED ou READ)
            entity.setStatus(domainMessage.getStatus());

            // Varre os recibos do domínio e adiciona à Entidade caso não existam
            domainMessage.getReadReceipts().forEach(domainReceipt -> {
                boolean alreadyExists = entity.getReadReceipts().stream()
                        .anyMatch(r -> r.getUserId().equals(domainReceipt.getUserId().value()));

                if (!alreadyExists) {
                    entity.addReadReceipt(new MessageReadReceiptEntity(
                            domainReceipt.getUserId().value(),
                            domainReceipt.getReadAt()
                    ));
                }
            });
        }

    }

    // --- Mappers Privados ---
    private Message toDomain(MessageEntity entity) {
        Set<MessageReadReceipt> receipts = entity.getReadReceipts().stream()
                .map(r -> new MessageReadReceipt(new UserId(r.getUserId()), r.getReadAt()))
                .collect(Collectors.toSet());

        return new Message(
                new MessageId(entity.getId()),
                new UserId(entity.getSenderId()),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getStatus(),
                receipts
        );
    }

    private MessageEntity toEntity(Message domain, ConversationEntity conversationRef) {
        MessageEntity entity = new MessageEntity(
                domain.getId().value(),
                conversationRef,
                domain.getSenderId().value(),
                domain.getContent(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
        domain.getReadReceipts().forEach(r ->
                entity.addReadReceipt(new MessageReadReceiptEntity(r.getUserId().value(), r.getReadAt()))
        );
        return entity;
    }
}