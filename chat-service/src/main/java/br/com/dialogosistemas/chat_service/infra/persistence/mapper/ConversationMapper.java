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
        Set<ConversationParticipant> participants = entity.getParticipants().stream()
                .map(participant -> new ConversationParticipant(
                        new UserId(participant.getUserId()),
                        participant.getUnreadCount(),
                        participant.getLastReadAt(),
                        participant.getRole()
                ))
                .collect(Collectors.toSet());

        return new Conversation(
                new ConversationId(entity.getId()),
                new TenantId(entity.getTenantId()),
                entity.getType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCreatedAt(),
                new UserId(entity.getCreatorId()),
                participants,
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
        entity.setDescription(domain.getDescription());
        entity.setCreatorId(domain.getCreatorId().value());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setLastMessageContent(domain.getLastMessagePreview());
        entity.setLastMessageAt(domain.getLastMessageAt());

        domain.getParticipants().forEach(participant -> {
            ConversationParticipantEntity participantEntity = new ConversationParticipantEntity();
            participantEntity.setUserId(participant.getUserId().value());
            participantEntity.setUnreadCount(participant.getUnreadCount());
            participantEntity.setLastReadAt(participant.getLastReadAt());
            participantEntity.setRole(participant.getRole());
            entity.addParticipant(participantEntity);
        });

        return entity;
    }
}
