package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.FileStorageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.*;
import br.com.dialogosistemas.chat_service.domain.model.message.Attachment;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetAttachmentDownloadUrlUseCaseTest {

    private final UUID conversationUuid = UUID.randomUUID();
    private final UUID userUuid = UUID.randomUUID();
    private final UUID otherUuid = UUID.randomUUID();
    private final UUID fileId = UUID.randomUUID();

    @Test
    void deve_retornar_signed_url_de_download() {
        var useCase = buildUseCase();
        var result = useCase.execute(UUID.randomUUID(), fileId, userUuid);

        assertNotNull(result);
        assertTrue(result.downloadUrl().contains("signed"));
    }

    @Test
    void deve_rejeitar_usuario_nao_participante() {
        UUID intruder = UUID.randomUUID();
        var useCase = buildUseCase();

        assertThrows(Exception.class, () -> useCase.execute(UUID.randomUUID(), fileId, intruder));
    }

    private GetAttachmentDownloadUrlUseCase buildUseCase() {
        Attachment att = new Attachment(fileId, "foto.jpg", "image/jpeg", 1024L, "path/foto.jpg", Instant.now());
        Message msg = Message.createWithAttachments(
                new ConversationId(conversationUuid), new UserId(userUuid), null, List.of(att)
        );

        var msgGateway = new StubMessageGateway(msg);
        var convGateway = new StubConversationGateway(conversation());
        var fileGateway = new StubFileStorageGateway();
        var properties = new FileUploadProperties(26_214_400L, 10, 15, 60, List.of("image/jpeg"), "bucket");

        return new GetAttachmentDownloadUrlUseCase(msgGateway, convGateway, fileGateway, properties);
    }

    private Conversation conversation() {
        return new Conversation(
                new ConversationId(conversationUuid), new TenantId(UUID.randomUUID()),
                ConversationType.INDIVIDUAL, null, null,
                Instant.parse("2026-03-31T10:00:00Z"), new UserId(userUuid),
                Set.of(
                        new ConversationParticipant(new UserId(userUuid), 0, null, ParticipantRole.MEMBER),
                        new ConversationParticipant(new UserId(otherUuid), 0, null, ParticipantRole.MEMBER)
                ), null, null
        );
    }

    private static class StubMessageGateway implements MessageGateway {
        private final Message msg;
        StubMessageGateway(Message msg) { this.msg = msg; }
        @Override public Message save(Message m, ConversationId cid) { return m; }
        @Override public Optional<Message> findById(MessageId id) { return Optional.of(msg); }
        @Override public List<Message> findHistoryBeforeCursor(ConversationId c, Instant d, UUID id, int l) { return List.of(); }
        @Override public List<Message> findUnreadByParticipant(ConversationId c, UserId u) { return List.of(); }
        @Override public void saveAll(List<Message> msgs, ConversationId cid) {}
    }

    private static class StubConversationGateway implements ConversationGateway {
        private final Conversation conv;
        StubConversationGateway(Conversation conv) { this.conv = conv; }
        @Override public Conversation save(Conversation c) { return c; }
        @Override public Optional<Conversation> findById(ConversationId id) { return Optional.ofNullable(conv); }
        @Override public List<Conversation> findAllByParticipant(UserId userId) { return List.of(); }
        @Override public void updateLastMessage(ConversationId id, String content, Instant sentAt) {}
    }

    private static class StubFileStorageGateway implements FileStorageGateway {
        @Override public URL generateUploadSignedUrl(String p, String ct, long ms, Duration t) { return null; }
        @Override public URL generateDownloadSignedUrl(String p, Duration t) {
            try { return new URL("https://storage.googleapis.com/bucket/" + p + "?signed=true"); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override public boolean objectExists(String path) { return true; }
        @Override public long getObjectSize(String path) { return 1024L; }
    }
}
