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
                convId,
                tenant,
                entity.getType(),
                entity.getTitle(),
                participants,
                creator,
                entity.getCreatedAt()
        );


        return conversation;
    }

    public ConversationEntity toEntity(Conversation domain) {
        ConversationEntity entity = new ConversationEntity(
                domain.getId().value(),
                domain.getTenantId().value(),
                domain.getTenantId().toString().isEmpty() ? null : null, // (ajuste técnico)
                null, // title ajustado abaixo
                domain.getCreatorId().value(),
                domain.getCreatedAt() // Ajuste se não tiver getCreatedAt no domain, adicione lá
        );

        // Ajustes finos
        entity.setTenantId(domain.getTenantId().value());
        entity.setType(domain.getType()); // Assumindo getter no domain
        entity.setTitle(domain.getTitle()); // Assumindo getter no domain

        // Mapeia participantes
        Set<UUID> ids = domain.getParticipants().stream() // Assumindo getter público para participants
                .map(UserId::value)
                .collect(Collectors.toSet());
        entity.setParticipantIds(ids);

        // Mapeia mensagens (novas)
        // salvamos mensagens separadas, mas se for cascata:
        if (domain.getUnmodifiableMessages() != null) {
            var messageEntities = domain.getUnmodifiableMessages().stream()
                    .map(msg -> new MessageEntity(
                            msg.getId().value(),
                            entity, // Associa a entidade pai
                            msg.getSenderId().value(),
                            msg.getContent(),
                            msg.getStatus(),
                            msg.getCreatedAt()
                    )).collect(Collectors.toList());
            entity.setMessages(messageEntities);
        }

        return entity;
    }
}