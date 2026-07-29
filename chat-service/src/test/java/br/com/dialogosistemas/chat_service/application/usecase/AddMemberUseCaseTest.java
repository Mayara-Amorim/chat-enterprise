package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.GroupEventDTO;
import br.com.dialogosistemas.shared_kernel.domain.exception.ForbiddenOperationException;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.*;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AddMemberUseCaseTest {

    @Test
    void deve_adicionar_membro_e_publicar_evento() {
        UUID ownerUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        UUID newMemberUuid = UUID.randomUUID();
        UUID convUuid = UUID.randomUUID();

        Conversation group = createGroup(convUuid, ownerUuid, memberUuid);
        var gateway = new FakeConversationGateway(group);
        var kafka = new FakeKafkaProducer();
        var useCase = new AddMemberUseCase(gateway, kafka);

        useCase.execute(convUuid, Set.of(newMemberUuid), ownerUuid);

        assertNotNull(gateway.savedConversation);
        assertTrue(gateway.savedConversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(new UserId(newMemberUuid))));
        assertEquals(1, kafka.groupEvents.size());
        assertEquals("MEMBER_ADDED", kafka.groupEvents.getFirst().type());
    }

    @Test
    void deve_rejeitar_adicao_por_member() {
        UUID ownerUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        UUID newMemberUuid = UUID.randomUUID();
        UUID convUuid = UUID.randomUUID();

        Conversation group = createGroup(convUuid, ownerUuid, memberUuid);
        var gateway = new FakeConversationGateway(group);
        var kafka = new FakeKafkaProducer();
        var useCase = new AddMemberUseCase(gateway, kafka);

        assertThrows(ForbiddenOperationException.class,
                () -> useCase.execute(convUuid, Set.of(newMemberUuid), memberUuid));
        assertTrue(kafka.groupEvents.isEmpty());
    }

    private Conversation createGroup(UUID convUuid, UUID ownerUuid, UUID memberUuid) {
        return Conversation.createGroup(
                new TenantId(UUID.randomUUID()),
                "Test", "Desc",
                new UserId(ownerUuid),
                Set.of(new UserId(ownerUuid), new UserId(memberUuid)));
    }

    private static final class FakeConversationGateway implements ConversationGateway {
        private final Conversation conversation;
        Conversation savedConversation;

        FakeConversationGateway(Conversation conversation) { this.conversation = conversation; }

        @Override
        public Conversation save(Conversation conversation) {
            this.savedConversation = conversation;
            return conversation;
        }
        @Override
        public Optional<Conversation> findById(ConversationId id) { return Optional.ofNullable(conversation); }
        @Override
        public List<Conversation> findAllByParticipant(UserId userId) { throw new UnsupportedOperationException(); }
        @Override
        public void updateLastMessage(ConversationId id, String content, Instant sentAt) { throw new UnsupportedOperationException(); }
    }

    private static final class FakeKafkaProducer extends ChatKafkaProducer {
        List<GroupEventDTO> groupEvents = new ArrayList<>();
        FakeKafkaProducer() { super(null, null); }
        @Override
        public void publishGroupEvent(GroupEventDTO event) { groupEvents.add(event); }
    }
}
