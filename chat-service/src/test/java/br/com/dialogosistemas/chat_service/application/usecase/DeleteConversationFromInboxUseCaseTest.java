package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.*;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeleteConversationFromInboxUseCaseTest {

    @Test
    void deve_apagar_conversa_do_inbox_apos_sair() {
        UUID ownerUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();

        Conversation group = Conversation.createGroup(new TenantId(UUID.randomUUID()), "G", "D",
                new UserId(ownerUuid), Set.of(new UserId(ownerUuid), new UserId(memberUuid)));
        group.leaveGroup(new UserId(memberUuid), false);

        var gateway = new FakeGateway(group);
        var useCase = new DeleteConversationFromInboxUseCase(gateway);

        useCase.execute(group.getId().value(), memberUuid);

        assertTrue(group.getParticipants().stream()
                .filter(p -> p.getUserId().equals(new UserId(memberUuid)))
                .findFirst().orElseThrow().isDeletedConversation());
    }

    @Test
    void deve_rejeitar_apagar_inbox_de_participante_ativo() {
        UUID ownerUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();

        Conversation group = Conversation.createGroup(new TenantId(UUID.randomUUID()), "G", "D",
                new UserId(ownerUuid), Set.of(new UserId(ownerUuid), new UserId(memberUuid)));

        var gateway = new FakeGateway(group);
        var useCase = new DeleteConversationFromInboxUseCase(gateway);

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(group.getId().value(), memberUuid));
    }

    private static final class FakeGateway implements ConversationGateway {
        private final Conversation conv;
        FakeGateway(Conversation conv) { this.conv = conv; }
        @Override public Conversation save(Conversation c) { return c; }
        @Override public Optional<Conversation> findById(ConversationId id) { return Optional.of(conv); }
        @Override public List<Conversation> findAllByParticipant(UserId u) { throw new UnsupportedOperationException(); }
        @Override public void updateLastMessage(ConversationId id, String c, Instant s) { throw new UnsupportedOperationException(); }
    }
}
