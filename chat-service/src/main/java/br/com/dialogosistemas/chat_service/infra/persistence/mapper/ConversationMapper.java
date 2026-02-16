package br.com.dialogosistemas.chat_service.infra.persistence.mapper;

import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationParticipant;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConversationMapper {

    public Conversation toDomain(ConversationEntity entity) {
        // Converte as entidades filhas (JPA) para objetos de domínio
        Set<ConversationParticipant> participants = entity.getParticipants().stream()
                .map(pEntity -> new ConversationParticipant(
                        new UserId(pEntity.getUserId()),
                        pEntity.getUnreadCount(),
                        pEntity.getLastReadAt()
                ))
                .collect(Collectors.toSet());

        UserId creator = new UserId(entity.getCreatorId());
        TenantId tenant = new TenantId(entity.getTenantId());
        ConversationId convId = new ConversationId(entity.getId());

        // Reconstrói o Aggregate Root passando a coleção de objetos ricos
        return new Conversation(
                convId,
                tenant,
                entity.getType(),
                entity.getTitle(),
                participants,
                creator,
                entity.getCreatedAt(),
                entity.getLastMessageContent(),
                entity.getLastMessageAt()
        );
    }

    public ConversationEntity toEntity(Conversation domain) {
        ConversationEntity entity = new ConversationEntity();
        entity.setId(domain.getId().value());
        entity.setTenantId(domain.getTenantId().value());
        entity.setType(domain.getType());
        entity.setTitle(domain.getTitle());
        entity.setCreatorId(domain.getCreatorId().value());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setLastMessageContent(domain.getLastMessagePreview());
        entity.setLastMessageAt(domain.getLastMessageAt());

        // Converte os objetos de domínio para entidades filhas
        // O método addParticipant da entidade garante a amarração bidirecional (foreign key)
        domain.getParticipants().forEach(pDomain -> {
            ConversationParticipantEntity pEntity = new ConversationParticipantEntity(
                    pDomain.getUserId().value(),
                    pDomain.getUnreadCount(),
                    pDomain.getLastReadAt()
            );
            entity.addParticipant(pEntity);
        });

        return entity;
    }
}