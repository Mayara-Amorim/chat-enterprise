package br.com.dialogosistemas.chat_service.infra.persistence;

import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageReadReceipt;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageReadReceiptEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    public Optional<Message> findById(MessageId messageId) {
        return messageRepository.findById(messageId.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findHistoryBeforeCursor(ConversationId conversationId, Instant cursorDate, UUID cursorId, int limit) {
        return messageRepository.findMessagesBeforeCursor(
                        conversationId.value(),
                        cursorDate,
                        cursorId,
                        PageRequest.of(0, limit)
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findUnreadByParticipant(ConversationId conversationId, UserId userId) {
        return messageRepository.findUnreadByParticipant(
                conversationId.value(),
                userId.value(),
                MessageStatus.READ
        ).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveAll(List<Message> messages, ConversationId conversationId) {
        if (messages.isEmpty()) {
            return;
        }

        List<UUID> messageIds = messages.stream().map(message -> message.getId().value()).toList();
        List<MessageEntity> managedEntities = messageRepository.findAllById(messageIds);

        for (MessageEntity entity : managedEntities) {
            Message domainMessage = messages.stream()
                    .filter(message -> message.getId().value().equals(entity.getId()))
                    .findFirst()
                    .orElseThrow();

            entity.setContent(domainMessage.getStoredContent());
            entity.setStatus(domainMessage.getStatus());
            entity.setEditedAt(domainMessage.getEditedAt());
            entity.setDeletedAt(domainMessage.getDeletedAt());
            entity.setDeletedBy(domainMessage.getDeletedBy() != null ? domainMessage.getDeletedBy().value() : null);

            domainMessage.getReadReceipts().forEach(domainReceipt -> {
                boolean alreadyExists = entity.getReadReceipts().stream()
                        .anyMatch(receipt -> receipt.getUserId().equals(domainReceipt.getUserId().value()));

                if (!alreadyExists) {
                    entity.addReadReceipt(new MessageReadReceiptEntity(
                            domainReceipt.getUserId().value(),
                            domainReceipt.getReadAt()
                    ));
                }
            });
        }
    }

    private Message toDomain(MessageEntity entity) {
        Set<MessageReadReceipt> receipts = entity.getReadReceipts().stream()
                .map(receipt -> new MessageReadReceipt(new UserId(receipt.getUserId()), receipt.getReadAt()))
                .collect(Collectors.toSet());

        return new Message(
                new MessageId(entity.getId()),
                new ConversationId(entity.getConversation().getId()),
                new UserId(entity.getSenderId()),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getStatus(),
                receipts,
                entity.getEditedAt(),
                entity.getDeletedAt(),
                entity.getDeletedBy() != null ? new UserId(entity.getDeletedBy()) : null
        );
    }

    private MessageEntity toEntity(Message domain, ConversationEntity conversationRef) {
        MessageEntity entity = new MessageEntity(
                domain.getId().value(),
                conversationRef,
                domain.getSenderId().value(),
                domain.getStoredContent(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
        entity.setEditedAt(domain.getEditedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setDeletedBy(domain.getDeletedBy() != null ? domain.getDeletedBy().value() : null);
        domain.getReadReceipts().forEach(receipt ->
                entity.addReadReceipt(new MessageReadReceiptEntity(receipt.getUserId().value(), receipt.getReadAt()))
        );
        return entity;
    }
}
