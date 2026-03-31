package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.CreateConversationRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationParticipant;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreateConversationUseCaseTest {

    @Test
    void executeCreatesRulesMessageForGroupWithDescription() {
        CapturingConversationGateway conversationGateway = new CapturingConversationGateway();
        CapturingMessageGateway messageGateway = new CapturingMessageGateway();
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        CreateConversationUseCase useCase = new CreateConversationUseCase(conversationGateway, messageGateway, kafkaProducer);
        UUID tenantId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        CreateConversationRequestDTO request = new CreateConversationRequestDTO(
                "GROUP",
                "Time de Produto",
                "Canal para discussoes do time",
                Set.of(memberId)
        );

        var response = useCase.execute(request, tenantId, creatorId);

        assertNotNull(response);
        assertEquals("Canal para discussoes do time", conversationGateway.savedConversation.getDescription());
        assertEquals(ParticipantRole.ADMIN, findParticipant(conversationGateway.savedConversation, creatorId).getRole());
        assertEquals(ParticipantRole.MEMBER, findParticipant(conversationGateway.savedConversation, memberId).getRole());

        assertEquals(1, messageGateway.savedMessages.size());
        SavedMessage savedMessage = messageGateway.savedMessages.getFirst();
        assertEquals(conversationGateway.savedConversation.getId(), savedMessage.conversationId());
        assertEquals("Regras do Grupo:\nCanal para discussoes do time", savedMessage.message().getContent());
        assertEquals(new UserId(creatorId), savedMessage.message().getSenderId());

        assertEquals(conversationGateway.savedConversation.getId(), conversationGateway.updatedConversationId);
        assertEquals(savedMessage.message().getContent(), conversationGateway.updatedContent);
        assertEquals(savedMessage.message().getCreatedAt(), conversationGateway.updatedSentAt);

        assertEquals(1, kafkaProducer.sentEvents.size());
        MessageSentEventDTO event = kafkaProducer.sentEvents.getFirst();
        assertEquals(savedMessage.message().getId().value(), event.messageId());
        assertEquals(conversationGateway.savedConversation.getId().value(), event.conversationId());
        assertEquals(creatorId, event.senderId());
        assertEquals(savedMessage.message().getContent(), event.content());
    }

    @Test
    void executeDoesNotCreateRulesMessageWhenDescriptionIsBlank() {
        CapturingConversationGateway conversationGateway = new CapturingConversationGateway();
        CapturingMessageGateway messageGateway = new CapturingMessageGateway();
        CapturingChatKafkaProducer kafkaProducer = new CapturingChatKafkaProducer();
        CreateConversationUseCase useCase = new CreateConversationUseCase(conversationGateway, messageGateway, kafkaProducer);

        CreateConversationRequestDTO request = new CreateConversationRequestDTO(
                "GROUP",
                "Time de Produto",
                "   ",
                Set.of(UUID.randomUUID())
        );

        useCase.execute(request, UUID.randomUUID(), UUID.randomUUID());

        assertEquals(0, messageGateway.savedMessages.size());
        assertEquals(0, kafkaProducer.sentEvents.size());
        assertNull(conversationGateway.updatedConversationId);
    }

    private ConversationParticipant findParticipant(Conversation conversation, UUID userId) {
        return conversation.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(new UserId(userId)))
                .findFirst()
                .orElseThrow();
    }

    private record SavedMessage(Message message, ConversationId conversationId) {
    }

    private static final class CapturingConversationGateway implements ConversationGateway {
        private Conversation savedConversation;
        private ConversationId updatedConversationId;
        private String updatedContent;
        private Instant updatedSentAt;

        @Override
        public Conversation save(Conversation conversation) {
            this.savedConversation = conversation;
            return conversation;
        }

        @Override
        public Optional<Conversation> findById(ConversationId id) {
            return Optional.empty();
        }

        @Override
        public List<Conversation> findAllByParticipant(UserId userId) {
            return List.of();
        }

        @Override
        public void updateLastMessage(ConversationId id, String content, Instant sentAt) {
            this.updatedConversationId = id;
            this.updatedContent = content;
            this.updatedSentAt = sentAt;
        }
    }

    private static final class CapturingMessageGateway implements MessageGateway {
        private final List<SavedMessage> savedMessages = new ArrayList<>();

        @Override
        public Message save(Message message, ConversationId conversationId) {
            savedMessages.add(new SavedMessage(message, conversationId));
            return message;
        }

        @Override
        public Optional<Message> findById(br.com.dialogosistemas.chat_service.domain.valueObject.MessageId messageId) {
            return Optional.empty();
        }

        @Override
        public List<Message> findHistoryBeforeCursor(ConversationId conversationId, Instant cursorDate, UUID cursorId, int limit) {
            return List.of();
        }

        @Override
        public List<Message> findUnreadByParticipant(ConversationId conversationId, UserId userId) {
            return List.of();
        }

        @Override
        public void saveAll(List<Message> messages, ConversationId conversationId) {
        }
    }

    private static final class CapturingChatKafkaProducer extends ChatKafkaProducer {
        private final List<MessageSentEventDTO> sentEvents = new ArrayList<>();

        private CapturingChatKafkaProducer() {
            super(null);
        }

        @Override
        public void send(MessageSentEventDTO event) {
            sentEvents.add(event);
        }
    }
}
