package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.ConfirmUploadRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.FileStorageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.PendingUploadGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.*;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.model.upload.PendingUpload;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmUploadUseCaseTest {

    private final UUID conversationUuid = UUID.randomUUID();
    private final UUID userUuid = UUID.randomUUID();
    private final UUID otherUuid = UUID.randomUUID();

    @Test
    void deve_confirmar_upload_e_criar_mensagem_com_attachment() {
        UUID uploadId = UUID.randomUUID();
        PendingUpload pending = new PendingUpload(
                uploadId, new ConversationId(conversationUuid), new UserId(userUuid),
                "foto.jpg", "image/jpeg", 2048L, "tenants/t/conv/c/file.jpg",
                Instant.now(), Instant.now().plusSeconds(900)
        );

        var msgGateway = new CapturingMessageGateway();
        var kafkaProducer = new CapturingKafkaProducer();
        var useCase = buildUseCase(pending, msgGateway, kafkaProducer);

        var request = new ConfirmUploadRequestDTO(conversationUuid, List.of(uploadId), "Olha!");
        useCase.execute(request, userUuid);

        assertNotNull(msgGateway.savedMessage);
        assertEquals("Olha!", msgGateway.savedMessage.getContent());
        assertEquals(1, msgGateway.savedMessage.getAttachments().size());
        assertEquals("foto.jpg", msgGateway.savedMessage.getAttachments().getFirst().originalFileName());
        assertNotNull(kafkaProducer.sentEvent);
    }

    @Test
    void deve_rejeitar_upload_expirado() {
        UUID uploadId = UUID.randomUUID();
        PendingUpload expired = new PendingUpload(
                uploadId, new ConversationId(conversationUuid), new UserId(userUuid),
                "foto.jpg", "image/jpeg", 2048L, "path",
                Instant.now().minusSeconds(1800), Instant.now().minusSeconds(900)
        );

        var msgGateway = new CapturingMessageGateway();
        var kafkaProducer = new CapturingKafkaProducer();
        var useCase = buildUseCase(expired, msgGateway, kafkaProducer);

        var request = new ConfirmUploadRequestDTO(conversationUuid, List.of(uploadId), null);

        assertThrows(IllegalStateException.class, () -> useCase.execute(request, userUuid));
        assertNull(msgGateway.savedMessage);
    }

    @Test
    void deve_rejeitar_upload_de_outro_usuario() {
        UUID uploadId = UUID.randomUUID();
        UUID anotherUser = UUID.randomUUID();
        PendingUpload pending = new PendingUpload(
                uploadId, new ConversationId(conversationUuid), new UserId(anotherUser),
                "foto.jpg", "image/jpeg", 2048L, "path",
                Instant.now(), Instant.now().plusSeconds(900)
        );

        var msgGateway = new CapturingMessageGateway();
        var kafkaProducer = new CapturingKafkaProducer();
        var useCase = buildUseCase(pending, msgGateway, kafkaProducer);

        var request = new ConfirmUploadRequestDTO(conversationUuid, List.of(uploadId), null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request, userUuid));
    }

    private ConfirmUploadUseCase buildUseCase(PendingUpload pending,
                                              CapturingMessageGateway msgGateway,
                                              CapturingKafkaProducer kafkaProducer) {
        var convGateway = new StubConversationGateway(conversation(conversationUuid, userUuid, otherUuid));
        var pendingGateway = new StubPendingUploadGateway(pending);
        var fileGateway = new StubFileStorageGateway();
        var properties = new FileUploadProperties(26_214_400L, 10, 15, 60, List.of("image/jpeg"), "bucket");
        return new ConfirmUploadUseCase(convGateway, pendingGateway, fileGateway, msgGateway, kafkaProducer, properties);
    }

    private Conversation conversation(UUID convId, UUID userA, UUID userB) {
        return new Conversation(
                new ConversationId(convId), new TenantId(UUID.randomUUID()),
                ConversationType.INDIVIDUAL, null, null,
                Instant.parse("2026-03-31T10:00:00Z"), new UserId(userA),
                Set.of(
                        new ConversationParticipant(new UserId(userA), 0, null, ParticipantRole.MEMBER),
                        new ConversationParticipant(new UserId(userB), 0, null, ParticipantRole.MEMBER)
                ), null, null
        );
    }

    private static class StubConversationGateway implements ConversationGateway {
        private final Conversation conv;
        StubConversationGateway(Conversation conv) { this.conv = conv; }
        @Override public Conversation save(Conversation c) { return c; }
        @Override public Optional<Conversation> findById(ConversationId id) { return Optional.ofNullable(conv); }
        @Override public List<Conversation> findAllByParticipant(UserId userId) { return List.of(); }
        @Override public void updateLastMessage(ConversationId id, String content, Instant sentAt) {}
    }

    private static class StubPendingUploadGateway implements PendingUploadGateway {
        private final PendingUpload pending;
        StubPendingUploadGateway(PendingUpload pending) { this.pending = pending; }
        @Override public PendingUpload save(PendingUpload pu) { return pu; }
        @Override public Optional<PendingUpload> findById(UUID id) { return Optional.ofNullable(pending); }
        @Override public List<PendingUpload> findAllByIds(List<UUID> ids) {
            return pending != null ? List.of(pending) : List.of();
        }
        @Override public void deleteById(UUID id) {}
        @Override public void deleteExpired() {}
    }

    private static class StubFileStorageGateway implements FileStorageGateway {
        @Override public URL generateUploadSignedUrl(String p, String ct, long ms, Duration t) { return null; }
        @Override public URL generateDownloadSignedUrl(String p, Duration t) { return null; }
        @Override public boolean objectExists(String path) { return true; }
        @Override public long getObjectSize(String path) { return 2048L; }
    }

    private static class CapturingMessageGateway implements MessageGateway {
        Message savedMessage;
        @Override public Message save(Message msg, ConversationId cid) { this.savedMessage = msg; return msg; }
        @Override public Optional<Message> findById(MessageId id) { return Optional.empty(); }
        @Override public List<Message> findHistoryBeforeCursor(ConversationId cid, Instant d, UUID id, int l) { return List.of(); }
        @Override public List<Message> findUnreadByParticipant(ConversationId cid, UserId uid) { return List.of(); }
        @Override public void saveAll(List<Message> msgs, ConversationId cid) {}
    }

    private static class CapturingKafkaProducer extends ChatKafkaProducer {
        MessageSentEventDTO sentEvent;
        CapturingKafkaProducer() { super(null, null); }
        @Override public void send(MessageSentEventDTO event) { this.sentEvent = event; }
    }
}
