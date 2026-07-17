package br.com.dialogosistemas.chat_service.infra.persistence;

import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.mapper.ConversationMapper;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ConversationRepositoryGateway implements ConversationGateway {

    private static final Logger log = LoggerFactory.getLogger(ConversationRepositoryGateway.class);

    private final ConversationJpaRepository jpaRepository;
    private final ConversationMapper mapper;

    public ConversationRepositoryGateway(ConversationJpaRepository jpaRepository, ConversationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Conversation save(Conversation conversation) {
        Optional<ConversationEntity> existingOpt = jpaRepository.findById(conversation.getId().value());

        if (existingOpt.isPresent()) {
            // ATUALIZAÇÃO SEGURA (Dirty Checking)
            // Não recriamos as coleções, apenas atualizamos os campos
            ConversationEntity existing = existingOpt.get();
            existing.setTitle(conversation.getTitle());
            existing.setDescription(conversation.getDescription());
            existing.setLastMessageContent(conversation.getLastMessagePreview());
            existing.setLastMessageAt(conversation.getLastMessageAt());
            existing.setMessagingPermission(conversation.getMessagingPermission());
            existing.setDissolvedAt(conversation.getDissolvedAt());

            conversation.getParticipants().forEach(domainParticipant -> {
                existing.getParticipants().stream()
                        .filter(p -> p.getUserId().equals(domainParticipant.getUserId().value()))
                        .findFirst()
                        .ifPresentOrElse(p -> {
                            p.setUnreadCount(domainParticipant.getUnreadCount());
                            p.setLastReadAt(domainParticipant.getLastReadAt());
                            p.setRole(domainParticipant.getRole());
                            p.setLeftAt(domainParticipant.getLeftAt());
                            p.setDeletedConversation(domainParticipant.isDeletedConversation());
                        }, () -> {
                            ConversationParticipantEntity newParticipant = new ConversationParticipantEntity();
                            newParticipant.setUserId(domainParticipant.getUserId().value());
                            newParticipant.setUnreadCount(domainParticipant.getUnreadCount());
                            newParticipant.setLastReadAt(domainParticipant.getLastReadAt());
                            newParticipant.setRole(domainParticipant.getRole());
                            newParticipant.setLeftAt(domainParticipant.getLeftAt());
                            newParticipant.setDeletedConversation(domainParticipant.isDeletedConversation());
                            existing.addParticipant(newParticipant);
                        });
            });

            // O Hibernate salva automaticamente no fim da transação
            return mapper.toDomain(existing);
        } else {
            // CRIAÇÃO NOVA (Apenas para novos grupos/conversas)
            var entity = mapper.toEntity(conversation);
            var savedEntity = jpaRepository.save(entity);
            return mapper.toDomain(savedEntity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(ConversationId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findAllByParticipant(UserId userId) {
        return jpaRepository.findAllByParticipantId(userId.value())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateLastMessage(ConversationId id, String content, Instant sentAt) {
        jpaRepository.updateLastMessage(id.value(), content, sentAt);
        log.debug("Atualizando conversa {} com: {}", id.value(), content);
    }
}
