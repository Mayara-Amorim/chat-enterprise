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

class DemoteMemberUseCaseTest {

    @Test
    void deve_rebaixar_admin_e_publicar_evento() {
        UUID ownerUuid = UUID.randomUUID();
        UUID adminUuid = UUID.randomUUID();

        Conversation group = Conversation.createGroup(new TenantId(UUID.randomUUID()), "G", "D",
                new UserId(ownerUuid), Set.of(new UserId(ownerUuid), new UserId(adminUuid)));
        group.promoteMember(new UserId(ownerUuid), new UserId(adminUuid));

        var gateway = new FakeGateway(group);
        var kafka = new FakeKafka();
        var useCase = new DemoteMemberUseCase(gateway, kafka);

        useCase.execute(group.getId().value(), adminUuid, ownerUuid);

        assertEquals("MEMBER_DEMOTED", kafka.events.getFirst().type());
        assertEquals(ParticipantRole.MEMBER, group.getParticipants().stream()
                .filter(p -> p.getUserId().equals(new UserId(adminUuid))).findFirst().orElseThrow().getRole());
    }

    private static final class FakeGateway implements ConversationGateway {
        private final Conversation conv;
        FakeGateway(Conversation conv) { this.conv = conv; }
        @Override public Conversation save(Conversation c) { return c; }
        @Override public Optional<Conversation> findById(ConversationId id) { return Optional.of(conv); }
        @Override public List<Conversation> findAllByParticipant(UserId u) { throw new UnsupportedOperationException(); }
        @Override public void updateLastMessage(ConversationId id, String c, Instant s) { throw new UnsupportedOperationException(); }
    }
    private static final class FakeKafka extends ChatKafkaProducer {
        List<GroupEventDTO> events = new ArrayList<>();
        FakeKafka() { super(null, null); }
        @Override public void publishGroupEvent(GroupEventDTO e) { events.add(e); }
    }
}
