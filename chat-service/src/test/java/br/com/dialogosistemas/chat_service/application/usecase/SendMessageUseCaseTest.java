package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.application.DTO.SendMessageRequestDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationParticipant;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
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

import static org.junit.jupiter.api.Assertions.*;

class SendMessageUseCaseTest {

    @Test
    void deve_enviar_mensagem_e_publicar_evento_kafka() {
        UUID conversationUuid = UUID.randomUUID();
        UUID senderUuid = UUID.randomUUID();
        UUID otherUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, senderUuid, otherUuid);

        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(conversation);
        CapturingMessageGateway messageGateway = new CapturingMessageGateway();
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        SendMessageUseCase useCase = new SendMessageUseCase(conversationGateway, kafkaProducer, messageGateway);

        SendMessageRequestDTO request = new SendMessageRequestDTO(conversationUuid, "Ola mundo");
        useCase.execute(request, senderUuid);

        assertNotNull(messageGateway.savedMessage);
        assertEquals("Ola mundo", messageGateway.savedMessage.getContent());
        assertEquals(new UserId(senderUuid), messageGateway.savedMessage.getSenderId());
        assertEquals(new ConversationId(conversationUuid), messageGateway.savedConversationId);

        assertNotNull(conversationGateway.lastMessageContent);
        assertEquals("Ola mundo", conversationGateway.lastMessageContent);
        assertEquals(conversationUuid, conversationGateway.lastMessageConversationId.value());

        assertNotNull(kafkaProducer.sentEvent);
        assertEquals(conversationUuid, kafkaProducer.sentEvent.conversationId());
        assertEquals(senderUuid, kafkaProducer.sentEvent.senderId());
        assertEquals("Ola mundo", kafkaProducer.sentEvent.content());
    }

    @Test
    void deve_lancar_excecao_quando_conversa_nao_existe() {
        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(null);
        CapturingMessageGateway messageGateway = new CapturingMessageGateway();
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        SendMessageUseCase useCase = new SendMessageUseCase(conversationGateway, kafkaProducer, messageGateway);

        SendMessageRequestDTO request = new SendMessageRequestDTO(UUID.randomUUID(), "Ola");

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request, UUID.randomUUID()));
        assertNull(messageGateway.savedMessage);
        assertNull(kafkaProducer.sentEvent);
    }

    @Test
    void deve_lancar_excecao_quando_usuario_nao_e_participante() {
        UUID conversationUuid = UUID.randomUUID();
        UUID participantA = UUID.randomUUID();
        UUID participantB = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, participantA, participantB);

        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(conversation);
        CapturingMessageGateway messageGateway = new CapturingMessageGateway();
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        SendMessageUseCase useCase = new SendMessageUseCase(conversationGateway, kafkaProducer, messageGateway);

        SendMessageRequestDTO request = new SendMessageRequestDTO(conversationUuid, "Intruso");

        assertThrows(IllegalStateException.class, () -> useCase.execute(request, intruder));
        assertNull(messageGateway.savedMessage);
        assertNull(kafkaProducer.sentEvent);
    }

    @Test
    void deve_incrementar_unread_count_dos_outros_participantes() {
        UUID conversationUuid = UUID.randomUUID();
        UUID senderUuid = UUID.randomUUID();
        UUID otherUuid = UUID.randomUUID();
        Conversation conversation = conversation(conversationUuid, senderUuid, otherUuid);

        CapturingConversationGateway conversationGateway = new CapturingConversationGateway(conversation);
        CapturingMessageGateway messageGateway = new CapturingMessageGateway();
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        SendMessageUseCase useCase = new SendMessageUseCase(conversationGateway, kafkaProducer, messageGateway);

        useCase.execute(new SendMessageRequestDTO(conversationUuid, "Msg 1"), senderUuid);

        ConversationParticipant otherParticipant = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(new UserId(otherUuid)))
                .findFirst().orElseThrow();
        assertEquals(1, otherParticipant.getUnreadCount());

        ConversationParticipant senderParticipant = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(new UserId(senderUuid)))
                .findFirst().orElseThrow();
        assertEquals(0, senderParticipant.getUnreadCount());
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

    private static final class CapturingConversationGateway implements ConversationGateway {
        private final Conversation conversation;
        ConversationId lastMessageConversationId;
        String lastMessageContent;

        private CapturingConversationGateway(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public Conversation save(Conversation conversation) { return conversation; }

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
            this.lastMessageConversationId = id;
            this.lastMessageContent = content;
        }
    }

    private static final class CapturingMessageGateway implements MessageGateway {
        Message savedMessage;
        ConversationId savedConversationId;

        @Override
        public Message save(Message message, ConversationId conversationId) {
            this.savedMessage = message;
            this.savedConversationId = conversationId;
            return message;
        }

        @Override
        public Optional<Message> findById(MessageId messageId) {
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
        MessageSentEventDTO sentEvent;

        private CapturingChatKafkaProducer() { super(null); }

        @Override
        public void send(MessageSentEventDTO event) {
            this.sentEvent = event;
        }
    }
}
