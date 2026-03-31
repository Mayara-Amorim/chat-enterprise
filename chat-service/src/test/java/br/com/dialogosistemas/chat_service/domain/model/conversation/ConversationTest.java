package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationTest {

    @Test
    void createGroupAssignsDescriptionAndParticipantRoles() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        UserId creatorId = new UserId(UUID.randomUUID());
        UserId memberId = new UserId(UUID.randomUUID());

        Conversation conversation = Conversation.createGroup(
                tenantId,
                "Arquitetura",
                "Grupo para alinhar regras de negocio",
                creatorId,
                Set.of(creatorId, memberId)
        );

        assertEquals("Grupo para alinhar regras de negocio", conversation.getDescription());
        assertEquals(ParticipantRole.ADMIN, findParticipant(conversation, creatorId).getRole());
        assertEquals(ParticipantRole.MEMBER, findParticipant(conversation, memberId).getRole());
    }

    private ConversationParticipant findParticipant(Conversation conversation, UserId userId) {
        return conversation.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }
}
