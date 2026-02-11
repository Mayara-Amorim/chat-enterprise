package br.com.dialogosistemas.chat_service.infra.persistence.mapper;

import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;

import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ConversationMapper {

    public Conversation toDomain(ConversationEntity entity) {
        // Converte IDs brutos para Value Objects
        Set<UserId> participants = entity.getParticipantIds().stream()
                .map(uuid -> new UserId(uuid))
                .collect(Collectors.toSet());

        UserId creator = new UserId(entity.getCreatorId());
        TenantId tenant = new TenantId(entity.getTenantId());
       ConversationId convId = new ConversationId(entity.getId());

        // Reconstrói o Aggregate Root
        Conversation conversation = new Conversation(
                convId, tenant, entity.getType(), entity.getTitle(), participants, creator, entity.getCreatedAt(),entity.getLastMessageContent(),
                entity.getLastMessageAt());


        return conversation;
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

        // Converter Set<UserId> para Set<UUID>
        Set<UUID> participantIds = domain.getParticipants().stream()
                .map(UserId::value)
                .collect(Collectors.toSet());

        entity.setParticipantIds(participantIds);

        return entity;
    }
}