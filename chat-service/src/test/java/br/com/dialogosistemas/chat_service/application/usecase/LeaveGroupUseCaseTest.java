package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.GroupEventDTO;
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

class LeaveGroupUseCaseTest {

    @Test
    void deve_publicar_member_left_quando_membro_sai() {
        UUID ownerUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        UUID convUuid = UUID.randomUUID();

        Conversation group = createGroup(convUuid, ownerUuid, memberUuid);
        var gateway = new FakeConversationGateway(group);
        var kafka = new FakeKafkaProducer();
        var useCase = new LeaveGroupUseCase(gateway, kafka);

        useCase.execute(convUuid, memberUuid, false);

        assertEquals(1, kafka.groupEvents.size());
        assertEquals("MEMBER_LEFT", kafka.groupEvents.getFirst().type());
    }

    @Test
    void deve_publicar_group_dissolved_quando_owner_sai() {
        UUID ownerUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        UUID convUuid = UUID.randomUUID();

        Conversation group = createGroup(convUuid, ownerUuid, memberUuid);
        var gateway = new FakeConversationGateway(group);
        var kafka = new FakeKafkaProducer();
        var useCase = new LeaveGroupUseCase(gateway, kafka);

        useCase.execute(convUuid, ownerUuid, false);

        assertEquals(1, kafka.groupEvents.size());
        assertEquals("GROUP_DISSOLVED", kafka.groupEvents.getFirst().type());
        assertNotNull(gateway.savedConversation.getDissolvedAt());
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
        public Conversation save(Conversation conversation) { this.savedConversation = conversation; return conversation; }
        @Override
        public Optional<Conversation> findById(ConversationId id) { return Optional.ofNullable(conversation); }
        @Override
        public List<Conversation> findAllByParticipant(UserId userId) { throw new UnsupportedOperationException(); }
        @Override
        public void updateLastMessage(ConversationId id, String content, Instant sentAt) { throw new UnsupportedOperationException(); }
    }

    private static final class FakeKafkaProducer extends ChatKafkaProducer {
        List<GroupEventDTO> groupEvents = new ArrayList<>();
        FakeKafkaProducer() { super(null); }
        @Override
        public void publishGroupEvent(GroupEventDTO event) { groupEvents.add(event); }
    }
}
