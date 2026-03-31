package br.com.dialogosistemas.chat_service.infra.persistence.repository;

import br.com.dialogosistemas.chat_service.ChatServiceApplication;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ParticipantRole;
import br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = ChatServiceApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:messagejparepository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.kafka.listener.auto-startup=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Transactional
class MessageJpaRepositoryTest {

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Autowired
    private ConversationJpaRepository conversationJpaRepository;

    @Test
    void findMessagesBeforeCursorReturnsOnlyOlderMessagesInDescendingOrder() {
        UUID conversationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        ConversationEntity conversation = persistConversation(conversationId, creatorId);

        MessageEntity newest = new MessageEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                conversation,
                creatorId,
                "mais nova",
                MessageStatus.SENT,
                Instant.parse("2026-03-31T12:00:00Z")
        );
        MessageEntity middle = new MessageEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                conversation,
                creatorId,
                "intermediaria",
                MessageStatus.SENT,
                Instant.parse("2026-03-31T11:00:00Z")
        );
        MessageEntity oldest = new MessageEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                conversation,
                creatorId,
                "mais antiga",
                MessageStatus.SENT,
                Instant.parse("2026-03-31T10:00:00Z")
        );
        messageJpaRepository.saveAllAndFlush(List.of(newest, middle, oldest));

        List<MessageEntity> history = messageJpaRepository.findMessagesBeforeCursor(
                conversationId,
                Instant.parse("2026-03-31T11:30:00Z"),
                UUID.fromString("00000000-0000-0000-0000-000000000099"),
                PageRequest.of(0, 2)
        );

        assertEquals(2, history.size());
        assertEquals("intermediaria", history.get(0).getContent());
        assertEquals("mais antiga", history.get(1).getContent());
    }

    @Test
    void findMessagesBeforeCursorUsesMessageIdAsTieBreakerWhenTimestampMatches() {
        UUID conversationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        ConversationEntity conversation = persistConversation(conversationId, creatorId);
        Instant sameTimestamp = Instant.parse("2026-03-31T11:00:00Z");

        MessageEntity higherId = new MessageEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                conversation,
                creatorId,
                "id-3",
                MessageStatus.SENT,
                sameTimestamp
        );
        MessageEntity middleId = new MessageEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                conversation,
                creatorId,
                "id-2",
                MessageStatus.SENT,
                sameTimestamp
        );
        MessageEntity lowerId = new MessageEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                conversation,
                creatorId,
                "id-1",
                MessageStatus.SENT,
                sameTimestamp
        );
        MessageEntity older = new MessageEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                conversation,
                creatorId,
                "mais antiga",
                MessageStatus.SENT,
                Instant.parse("2026-03-31T10:00:00Z")
        );
        messageJpaRepository.saveAllAndFlush(List.of(higherId, middleId, lowerId, older));

        List<MessageEntity> history = messageJpaRepository.findMessagesBeforeCursor(
                conversationId,
                sameTimestamp,
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                PageRequest.of(0, 3)
        );

        assertEquals(3, history.size());
        assertEquals("id-2", history.get(0).getContent());
        assertEquals("id-1", history.get(1).getContent());
        assertEquals("mais antiga", history.get(2).getContent());
    }

    private ConversationEntity persistConversation(UUID conversationId, UUID creatorId) {
        ConversationEntity conversation = new ConversationEntity(
                conversationId,
                UUID.randomUUID(),
                ConversationType.GROUP,
                "Grupo",
                creatorId,
                Instant.parse("2026-03-31T09:00:00Z"),
                null,
                null,
                "Regras"
        );
        conversation.addParticipant(new ConversationParticipantEntity(creatorId, 0, null, ParticipantRole.ADMIN));
        conversation.addParticipant(new ConversationParticipantEntity(UUID.randomUUID(), 0, null, ParticipantRole.MEMBER));
        return conversationJpaRepository.saveAndFlush(conversation);
    }
}
