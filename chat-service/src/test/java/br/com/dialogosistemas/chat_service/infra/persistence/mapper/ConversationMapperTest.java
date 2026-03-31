package br.com.dialogosistemas.chat_service.infra.persistence.mapper;

import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationParticipant;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationMapperTest {

    private final ConversationMapper mapper = new ConversationMapper();

    @Test
    void toDomainMapsDescriptionAndParticipantRoles() {
        UUID creatorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        ConversationEntity entity = new ConversationEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(UUID.randomUUID());
        entity.setType(ConversationType.GROUP);
        entity.setTitle("Governanca");
        entity.setDescription("Regras oficiais do grupo");
        entity.setCreatorId(creatorId);
        entity.setCreatedAt(Instant.now());
        entity.addParticipant(new ConversationParticipantEntity(creatorId, 0, null, ParticipantRole.ADMIN));
        entity.addParticipant(new ConversationParticipantEntity(memberId, 0, null, ParticipantRole.MEMBER));

        Conversation conversation = mapper.toDomain(entity);

        assertEquals("Regras oficiais do grupo", conversation.getDescription());
        assertEquals(ParticipantRole.ADMIN, findParticipant(conversation, creatorId).getRole());
        assertEquals(ParticipantRole.MEMBER, findParticipant(conversation, memberId).getRole());
    }

    @Test
    void toEntityMapsDescriptionAndParticipantRoles() {
        UUID creatorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        Conversation conversation = new Conversation(
                new ConversationId(UUID.randomUUID()),
                new TenantId(UUID.randomUUID()),
                ConversationType.GROUP,
                "Governanca",
                "Regras oficiais do grupo",
                Instant.now(),
                new UserId(creatorId),
                Set.of(
                        new ConversationParticipant(new UserId(creatorId), 0, null, ParticipantRole.ADMIN),
                        new ConversationParticipant(new UserId(memberId), 0, null, ParticipantRole.MEMBER)
                ),
                null,
                null
        );

        ConversationEntity entity = mapper.toEntity(conversation);

        assertEquals("Regras oficiais do grupo", entity.getDescription());
        assertEquals(ParticipantRole.ADMIN, findParticipant(entity, creatorId).getRole());
        assertEquals(ParticipantRole.MEMBER, findParticipant(entity, memberId).getRole());
    }

    private ConversationParticipant findParticipant(Conversation conversation, UUID userId) {
        return conversation.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(new UserId(userId)))
                .findFirst()
                .orElseThrow();
    }

    private ConversationParticipantEntity findParticipant(ConversationEntity entity, UUID userId) {
        return entity.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }
}
