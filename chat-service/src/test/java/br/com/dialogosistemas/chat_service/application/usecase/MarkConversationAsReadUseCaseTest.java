package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageStatusUpdatedEventDTO;
import br.com.dialogosistemas.shared_kernel.domain.exception.ResourceNotFoundException;
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
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MarkConversationAsReadUseCaseTest {

    @Test
    void deve_marcar_mensagens_como_lidas_e_publicar_eventos() {
        UUID conversationUuid = UUID.randomUUID();
        UUID readerUuid = UUID.randomUUID();
        UUID senderUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, senderUuid, readerUuid);

        Message msg1 = message(conversationUuid, UUID.randomUUID(), senderUuid, "Msg 1");
        Message msg2 = message(conversationUuid, UUID.randomUUID(), senderUuid, "Msg 2");

        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(conversation);
        StubMessageGateway messageGateway = new StubMessageGateway(List.of(msg1, msg2));
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        MarkConversationAsReadUseCase useCase = new MarkConversationAsReadUseCase(conversationGateway, messageGateway, kafkaProducer);

        useCase.execute(conversationUuid, readerUuid);

        assertNotNull(conversationGateway.savedConversation);

        ConversationParticipant reader = conversationGateway.savedConversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(new UserId(readerUuid)))
                .findFirst().orElseThrow();
        assertEquals(0, reader.getUnreadCount());
        assertNotNull(reader.getLastReadAt());

        assertNotNull(messageGateway.savedMessages);
        assertEquals(2, messageGateway.savedMessages.size());

        assertEquals(2, kafkaProducer.statusEvents.size());
        assertEquals(conversationUuid, kafkaProducer.statusEvents.get(0).conversationId());
        assertEquals(readerUuid, kafkaProducer.statusEvents.get(0).readerId());
    }

    @Test
    void deve_marcar_status_READ_quando_todos_participantes_leram() {
        UUID conversationUuid = UUID.randomUUID();
        UUID readerUuid = UUID.randomUUID();
        UUID senderUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, senderUuid, readerUuid);

        Message msg = message(conversationUuid, UUID.randomUUID(), senderUuid, "Msg");

        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(conversation);
        StubMessageGateway messageGateway = new StubMessageGateway(List.of(msg));
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        MarkConversationAsReadUseCase useCase = new MarkConversationAsReadUseCase(conversationGateway, messageGateway, kafkaProducer);

        useCase.execute(conversationUuid, readerUuid);

        assertEquals(MessageStatus.READ, msg.getStatus());
    }

    @Test
    void nao_deve_publicar_eventos_quando_nao_ha_mensagens_nao_lidas() {
        UUID conversationUuid = UUID.randomUUID();
        UUID readerUuid = UUID.randomUUID();
        UUID senderUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, senderUuid, readerUuid);

        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(conversation);
        StubMessageGateway messageGateway = new StubMessageGateway(List.of());
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        MarkConversationAsReadUseCase useCase = new MarkConversationAsReadUseCase(conversationGateway, messageGateway, kafkaProducer);

        useCase.execute(conversationUuid, readerUuid);

        assertTrue(kafkaProducer.statusEvents.isEmpty());
        assertNull(messageGateway.savedMessages);
    }

    @Test
    void deve_lancar_excecao_quando_conversa_nao_existe() {
        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(null);
        StubMessageGateway messageGateway = new StubMessageGateway(List.of());
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        MarkConversationAsReadUseCase useCase = new MarkConversationAsReadUseCase(conversationGateway, messageGateway, kafkaProducer);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), UUID.randomUUID()));
        assertTrue(kafkaProducer.statusEvents.isEmpty());
    }

    @Test
    void nao_deve_criar_receipt_quando_leitor_e_o_proprio_autor() {
        UUID conversationUuid = UUID.randomUUID();
        UUID senderUuid = UUID.randomUUID();
        UUID otherUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, senderUuid, otherUuid);

        Message msg = message(conversationUuid, UUID.randomUUID(), senderUuid, "Propria msg");

        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(conversation);
        StubMessageGateway messageGateway = new StubMessageGateway(List.of(msg));
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        MarkConversationAsReadUseCase useCase = new MarkConversationAsReadUseCase(conversationGateway, messageGateway, kafkaProducer);

        useCase.execute(conversationUuid, senderUuid);

        assertTrue(msg.getReadReceipts().isEmpty());
        assertEquals(MessageStatus.SENT, msg.getStatus());
    }

    private Conversation conversation(UUID conversationUuid, UUID userA, UUID userB) {
        return new Conversation(
                new ConversationId(conversationUuid),
                new TenantId(UUID.randomUUID()),
                ConversationType.INDIVIDUAL,
                null,
                null,
                Instant.parse("2026-03-31T10:00:00Z"),
                new UserId(userA),
                Set.of(
                        new ConversationParticipant(new UserId(userA), 0, null, ParticipantRole.MEMBER),
                        new ConversationParticipant(new UserId(userB), 0, null, ParticipantRole.MEMBER)
                ),
                null,
                null
        );
    }

    private Message message(UUID conversationUuid, UUID messageUuid, UUID senderUuid, String content) {
        return new Message(
                new MessageId(messageUuid),
                new ConversationId(conversationUuid),
                new UserId(senderUuid),
                content,
                Instant.parse("2026-03-31T11:00:00Z"),
                MessageStatus.SENT,
                Set.of()
        );
    }

    private static final class CapturingConversationGateway implements ConversationGateway {
        private final Conversation conversation;
        Conversation savedConversation;

        private CapturingConversationGateway(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public Conversation save(Conversation conversation) {
            this.savedConversation = conversation;
            return conversation;
        }

        @Override
        public Optional<Conversation> findById(ConversationId id) {
            return Optional.ofNullable(conversation);
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

    private static final class StubMessageGateway implements MessageGateway {
        private final List<Message> unreadMessages;
        List<Message> savedMessages;

        private StubMessageGateway(List<Message> unreadMessages) {
            this.unreadMessages = unreadMessages;
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
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Message> findUnreadByParticipant(ConversationId conversationId, UserId userId) {
            return unreadMessages;
        }

        @Override
        public void saveAll(List<Message> messages, ConversationId conversationId) {
            this.savedMessages = messages;
        }
    }

    private static final class CapturingChatKafkaProducer extends ChatKafkaProducer {
        List<MessageStatusUpdatedEventDTO> statusEvents = new ArrayList<>();

        private CapturingChatKafkaProducer() { super(null, null); }

        @Override
        public void publishStatusUpdate(MessageStatusUpdatedEventDTO event) {
            this.statusEvents.add(event);
        }
    }
}
