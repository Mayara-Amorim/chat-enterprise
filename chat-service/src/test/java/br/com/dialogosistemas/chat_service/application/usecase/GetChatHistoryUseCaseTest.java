package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationParticipant;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.util.CursorUtils;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetChatHistoryUseCaseTest {

    @Test
    void executeReturnsMessagesAndNextCursorUsingProvidedCursor() {
        UUID conversationUuid = UUID.randomUUID();
        UUID requesterUuid = UUID.randomUUID();
        Instant cursorCreatedAt = Instant.parse("2026-03-31T12:30:00Z");
        UUID cursorMessageId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        String cursor = CursorUtils.encode(cursorCreatedAt, cursorMessageId);
        Instant newestMessageAt = Instant.parse("2026-03-31T12:00:00Z");
        Instant oldestMessageAt = Instant.parse("2026-03-31T11:00:00Z");
        UUID oldestMessageId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        StubConversationGateway conversationGateway = new StubConversationGateway(
                conversationWithParticipant(conversationUuid, requesterUuid)
        );
        CapturingMessageGateway messageGateway = new CapturingMessageGateway(List.of(
                message("Mensagem mais nova", requesterUuid, newestMessageAt),
                message("Mensagem mais antiga", UUID.randomUUID(), oldestMessageAt, oldestMessageId)
        ));

        GetChatHistoryUseCase useCase = new GetChatHistoryUseCase(conversationGateway, messageGateway);

        var response = useCase.execute(conversationUuid, requesterUuid, cursor, 20);

        assertEquals(cursorCreatedAt, messageGateway.capturedCursorDate);
        assertEquals(cursorMessageId, messageGateway.capturedCursorId);
        assertEquals(20, messageGateway.capturedLimit);
        assertEquals(conversationUuid, messageGateway.capturedConversationId.value());
        assertEquals(2, response.messages().size());
        assertEquals("Mensagem mais nova", response.messages().getFirst().content());
        CursorUtils.DecodedCursor nextCursor = CursorUtils.decode(response.nextCursor());
        assertEquals(oldestMessageAt, nextCursor.createdAt());
        assertEquals(oldestMessageId, nextCursor.id());
    }

    @Test
    void executePassesNullCursorWhenCursorIsNotProvided() {
        UUID conversationUuid = UUID.randomUUID();
        UUID requesterUuid = UUID.randomUUID();

        StubConversationGateway conversationGateway = new StubConversationGateway(
                conversationWithParticipant(conversationUuid, requesterUuid)
        );
        CapturingMessageGateway messageGateway = new CapturingMessageGateway(List.of(
                message("Mensagem unica", requesterUuid, Instant.parse("2026-03-31T10:00:00Z"))
        ));

        GetChatHistoryUseCase useCase = new GetChatHistoryUseCase(conversationGateway, messageGateway);

        useCase.execute(conversationUuid, requesterUuid, null, 10);

        assertEquals(null, messageGateway.capturedCursorDate);
        assertEquals(null, messageGateway.capturedCursorId);
    }

    @Test
    void executeRejectsRequesterOutsideConversation() {
        UUID conversationUuid = UUID.randomUUID();
        UUID participantUuid = UUID.randomUUID();
        UUID outsiderUuid = UUID.randomUUID();

        StubConversationGateway conversationGateway = new StubConversationGateway(
                conversationWithParticipant(conversationUuid, participantUuid)
        );
        CapturingMessageGateway messageGateway = new CapturingMessageGateway(List.of());
        GetChatHistoryUseCase useCase = new GetChatHistoryUseCase(conversationGateway, messageGateway);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        conversationUuid,
                        outsiderUuid,
                        CursorUtils.encode(Instant.parse("2026-03-31T12:00:00Z"), UUID.randomUUID()),
                        20
                )
        );

        assertEquals("Acesso negado: o utilizador não é participante desta conversa.", exception.getMessage());
        assertEquals(null, messageGateway.capturedConversationId);
    }

    @Test
    void executeRejectsInvalidOpaqueCursor() {
        UUID conversationUuid = UUID.randomUUID();
        UUID requesterUuid = UUID.randomUUID();

        StubConversationGateway conversationGateway = new StubConversationGateway(
                conversationWithParticipant(conversationUuid, requesterUuid)
        );
        CapturingMessageGateway messageGateway = new CapturingMessageGateway(List.of());
        GetChatHistoryUseCase useCase = new GetChatHistoryUseCase(conversationGateway, messageGateway);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(conversationUuid, requesterUuid, "cursor-invalido", 20)
        );

        assertEquals("Cursor inválido", exception.getMessage());
    }

    private static Conversation conversationWithParticipant(UUID conversationUuid, UUID participantUuid) {
        return new Conversation(
                new ConversationId(conversationUuid),
                new TenantId(UUID.randomUUID()),
                ConversationType.GROUP,
                "Grupo",
                "Regras",
                Instant.parse("2026-03-31T09:00:00Z"),
                new UserId(participantUuid),
                Set.of(new ConversationParticipant(new UserId(participantUuid), 0, null, ParticipantRole.ADMIN)),
                null,
                null
        );
    }

    private static Message message(String content, UUID senderUuid, Instant createdAt) {
        return message(content, senderUuid, createdAt, UUID.randomUUID());
    }

    private static Message message(String content, UUID senderUuid, Instant createdAt, UUID messageId) {
        return new Message(
                new MessageId(messageId),
                new ConversationId(UUID.randomUUID()),
                new UserId(senderUuid),
                content,
                createdAt,
                MessageStatus.SENT,
                Set.of()
        );
    }

    private static final class StubConversationGateway implements ConversationGateway {
        private final Conversation conversation;

        private StubConversationGateway(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public Conversation save(Conversation conversation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Conversation> findById(ConversationId id) {
            return Optional.of(conversation);
        }

        @Override
        public List<Conversation> findAllByParticipant(UserId userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateLastMessage(ConversationId id, String content, Instant sentAt) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CapturingMessageGateway implements MessageGateway {
        private final List<Message> messagesToReturn;
        private ConversationId capturedConversationId;
        private Instant capturedCursorDate;
        private UUID capturedCursorId;
        private Integer capturedLimit;

        private CapturingMessageGateway(List<Message> messagesToReturn) {
            this.messagesToReturn = messagesToReturn;
        }

        @Override
        public Message save(Message message, ConversationId conversationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Message> findById(MessageId messageId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Message> findHistoryBeforeCursor(ConversationId conversationId, Instant cursorDate, UUID cursorId, int limit) {
            this.capturedConversationId = conversationId;
            this.capturedCursorDate = cursorDate;
            this.capturedCursorId = cursorId;
            this.capturedLimit = limit;
            return messagesToReturn;
        }

        @Override
        public List<Message> findUnreadByParticipant(ConversationId conversationId, UserId userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<Message> messages, ConversationId conversationId) {
            throw new UnsupportedOperationException();
        }
    }
}
