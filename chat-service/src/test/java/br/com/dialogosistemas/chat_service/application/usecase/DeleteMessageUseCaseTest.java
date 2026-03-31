package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageDeletedEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageEditedEventDTO;
import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteMessageUseCaseTest {

    @Test
    void adminDeletesMemberMessageAndPublishesEvent() {
        UUID conversationUuid = UUID.randomUUID();
        UUID adminUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, adminUuid, memberUuid);
        Message message = message(conversationUuid, messageUuid, memberUuid, "Mensagem do membro");

        StubConversationGateway conversationGateway = new StubConversationGateway(conversation);
        CapturingMessageGateway messageGateway = new CapturingMessageGateway(message);
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        DeleteMessageUseCase useCase = new DeleteMessageUseCase(messageGateway, conversationGateway, kafkaProducer);

        useCase.execute(conversationUuid, messageUuid, adminUuid);

        assertNotNull(messageGateway.savedMessage);
        assertNotNull(messageGateway.savedMessage.getDeletedAt());
        assertEquals(new UserId(adminUuid), messageGateway.savedMessage.getDeletedBy());
        assertNotNull(kafkaProducer.deletedEvent);
        assertEquals(messageUuid, kafkaProducer.deletedEvent.messageId());
        assertEquals(conversationUuid, kafkaProducer.deletedEvent.conversationId());
        assertEquals(adminUuid, kafkaProducer.deletedEvent.deletedBy());
    }

    @Test
    void memberCannotDeleteAdminMessage() {
        UUID conversationUuid = UUID.randomUUID();
        UUID adminUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, adminUuid, memberUuid);
        Message message = message(conversationUuid, messageUuid, adminUuid, "Mensagem do admin");

        StubConversationGateway conversationGateway = new StubConversationGateway(conversation);
        CapturingMessageGateway messageGateway = new CapturingMessageGateway(message);
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        DeleteMessageUseCase useCase = new DeleteMessageUseCase(messageGateway, conversationGateway, kafkaProducer);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(conversationUuid, messageUuid, memberUuid)
        );

        assertEquals("Acesso negado: Apenas o autor ou um administrador podem apagar esta mensagem.", exception.getMessage());
        assertNull(messageGateway.savedMessage);
        assertNull(kafkaProducer.deletedEvent);
    }

    @Test
    void deleteRejectsMessageFromAnotherConversation() {
        UUID adminUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        Conversation conversation = conversation(UUID.randomUUID(), adminUuid, memberUuid);
        Message message = message(UUID.randomUUID(), UUID.randomUUID(), memberUuid, "Mensagem");

        DeleteMessageUseCase useCase = new DeleteMessageUseCase(
                new CapturingMessageGateway(message),
                new StubConversationGateway(conversation),
                new CapturingChatKafkaProducer()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(conversation.getId().value(), message.getId().value(), adminUuid)
        );

        assertEquals("A mensagem não pertence a esta conversa.", exception.getMessage());
    }

    @Test
    void deleteRejectsMissingMessage() {
        UUID conversationUuid = UUID.randomUUID();
        UUID adminUuid = UUID.randomUUID();
        UUID memberUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, adminUuid, memberUuid);

        DeleteMessageUseCase useCase = new DeleteMessageUseCase(
                new CapturingMessageGateway(null),
                new StubConversationGateway(conversation),
                new CapturingChatKafkaProducer()
        );

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(conversationUuid, UUID.randomUUID(), adminUuid));
    }

    @Test
    void authorEditsOwnMessageAndPublishesEvent() {
        UUID conversationUuid = UUID.randomUUID();
        UUID authorUuid = UUID.randomUUID();
        UUID otherUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Message message = message(conversationUuid, messageUuid, authorUuid, "Mensagem original");

        CapturingMessageGateway messageGateway = new CapturingMessageGateway(message);
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        EditMessageUseCase useCase = new EditMessageUseCase(messageGateway, kafkaProducer);

        useCase.execute(conversationUuid, messageUuid, authorUuid, "Mensagem editada");

        assertNotNull(messageGateway.savedMessage);
        assertEquals("Mensagem editada", messageGateway.savedMessage.getContent());
        assertNotNull(messageGateway.savedMessage.getEditedAt());
        assertNotNull(kafkaProducer.editedEvent);
        assertEquals(messageUuid, kafkaProducer.editedEvent.messageId());
        assertEquals(conversationUuid, kafkaProducer.editedEvent.conversationId());
        assertEquals(authorUuid, kafkaProducer.editedEvent.editedBy());
        assertEquals("Mensagem editada", kafkaProducer.editedEvent.content());
    }

    @Test
    void editRejectsNonAuthor() {
        UUID conversationUuid = UUID.randomUUID();
        UUID authorUuid = UUID.randomUUID();
        UUID intruderUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Message message = message(conversationUuid, messageUuid, authorUuid, "Mensagem original");

        CapturingMessageGateway messageGateway = new CapturingMessageGateway(message);
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        EditMessageUseCase useCase = new EditMessageUseCase(messageGateway, kafkaProducer);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(conversationUuid, messageUuid, intruderUuid, "Ataque")
        );

        assertEquals("Acesso negado: Apenas o autor original pode editar esta mensagem.", exception.getMessage());
        assertNull(kafkaProducer.editedEvent);
    }

    @Test
    void editRejectsDeletedMessage() {
        UUID conversationUuid = UUID.randomUUID();
        UUID authorUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Message message = message(conversationUuid, messageUuid, authorUuid, "Mensagem original");
        message.deleteBy(new UserId(authorUuid), false);

        CapturingMessageGateway messageGateway = new CapturingMessageGateway(message);
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        EditMessageUseCase useCase = new EditMessageUseCase(messageGateway, kafkaProducer);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(conversationUuid, messageUuid, authorUuid, "Nao deveria editar")
        );

        assertEquals("A mensagem apagada não pode ser editada.", exception.getMessage());
        assertNull(kafkaProducer.editedEvent);
    }

    @Test
    void editRejectsMessageFromAnotherConversation() {
        UUID conversationUuid = UUID.randomUUID();
        UUID authorUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();
        Message message = message(UUID.randomUUID(), messageUuid, authorUuid, "Mensagem original");

        CapturingMessageGateway messageGateway = new CapturingMessageGateway(message);
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        EditMessageUseCase useCase = new EditMessageUseCase(messageGateway, kafkaProducer);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(conversationUuid, messageUuid, authorUuid, "Mensagem editada")
        );

        assertEquals("A mensagem não pertence a esta conversa.", exception.getMessage());
        assertNull(kafkaProducer.editedEvent);
    }

    private Conversation conversation(UUID conversationUuid, UUID adminUuid, UUID memberUuid) {
        return new Conversation(
                new ConversationId(conversationUuid),
                new TenantId(UUID.randomUUID()),
                ConversationType.GROUP,
                "Grupo",
                "Regras",
                Instant.parse("2026-03-31T10:00:00Z"),
                new UserId(adminUuid),
                Set.of(
                        new ConversationParticipant(new UserId(adminUuid), 0, null, ParticipantRole.ADMIN),
                        new ConversationParticipant(new UserId(memberUuid), 0, null, ParticipantRole.MEMBER)
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

    private static final class CapturingMessageGateway implements MessageGateway {
        private final Message messageToReturn;
        private Message savedMessage;

        private CapturingMessageGateway(Message messageToReturn) {
            this.messageToReturn = messageToReturn;
        }

        @Override
        public Message save(Message message, ConversationId conversationId) {
            this.savedMessage = message;
            return message;
        }

        @Override
        public Optional<Message> findById(MessageId messageId) {
            if (messageToReturn != null && messageToReturn.getId().equals(messageId)) {
                return Optional.of(messageToReturn);
            }
            return Optional.empty();
        }

        @Override
        public List<Message> findHistoryBeforeCursor(ConversationId conversationId, Instant cursorDate, UUID cursorId, int limit) {
            throw new UnsupportedOperationException();
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

    private static final class CapturingChatKafkaProducer extends ChatKafkaProducer {
        private MessageDeletedEventDTO deletedEvent;
        private MessageEditedEventDTO editedEvent;

        private CapturingChatKafkaProducer() {
            super(null);
        }

        @Override
        public void publishMessageDeleted(MessageDeletedEventDTO event) {
            this.deletedEvent = event;
        }

        @Override
        public void publishMessageEdited(MessageEditedEventDTO event) {
            this.editedEvent = event;
        }
    }
}
