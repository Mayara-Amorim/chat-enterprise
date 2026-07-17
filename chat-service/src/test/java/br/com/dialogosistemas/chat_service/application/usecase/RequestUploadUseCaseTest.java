package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.RequestUploadRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.RequestUploadResponseDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.FileStorageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.PendingUploadGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.*;
import br.com.dialogosistemas.chat_service.domain.model.upload.PendingUpload;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RequestUploadUseCaseTest {

    private final UUID conversationUuid = UUID.randomUUID();
    private final UUID userUuid = UUID.randomUUID();
    private final UUID otherUuid = UUID.randomUUID();

    @Test
    void deve_gerar_signed_url_para_upload_valido() {
        var useCase = buildUseCase(conversation(conversationUuid, userUuid, otherUuid));

        var request = new RequestUploadRequestDTO(conversationUuid, "foto.jpg", "image/jpeg", 1024L);
        RequestUploadResponseDTO response = useCase.execute(request, userUuid);

        assertNotNull(response.uploadId());
        assertNotNull(response.signedUrl());
        assertNotNull(response.expiresAt());
    }

    @Test
    void deve_rejeitar_content_type_nao_permitido() {
        var useCase = buildUseCase(conversation(conversationUuid, userUuid, otherUuid));

        var request = new RequestUploadRequestDTO(conversationUuid, "virus.exe", "application/x-executable", 1024L);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request, userUuid));
    }

    @Test
    void deve_rejeitar_tamanho_acima_do_limite() {
        var useCase = buildUseCase(conversation(conversationUuid, userUuid, otherUuid));

        var request = new RequestUploadRequestDTO(conversationUuid, "big.mp4", "video/mp4", 30_000_000L);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request, userUuid));
    }

    @Test
    void deve_rejeitar_usuario_nao_participante() {
        UUID intruder = UUID.randomUUID();
        var useCase = buildUseCase(conversation(conversationUuid, userUuid, otherUuid));

        var request = new RequestUploadRequestDTO(conversationUuid, "f.jpg", "image/jpeg", 1024L);

        assertThrows(Exception.class, () -> useCase.execute(request, intruder));
    }

    private RequestUploadUseCase buildUseCase(Conversation conv) {
        var conversationGateway = new StubConversationGateway(conv);
        var fileStorageGateway = new StubFileStorageGateway();
        var pendingUploadGateway = new StubPendingUploadGateway();
        var properties = new FileUploadProperties(
                26_214_400L, 10, 15, 60,
                List.of("image/jpeg", "image/png", "video/mp4", "application/pdf"),
                "test-bucket"
        );
        return new RequestUploadUseCase(conversationGateway, fileStorageGateway, pendingUploadGateway, properties);
    }

    private Conversation conversation(UUID convId, UUID userA, UUID userB) {
        return new Conversation(
                new ConversationId(convId),
                new TenantId(UUID.randomUUID()),
                ConversationType.INDIVIDUAL,
                null, null,
                Instant.parse("2026-03-31T10:00:00Z"),
                new UserId(userA),
                Set.of(
                        new ConversationParticipant(new UserId(userA), 0, null, ParticipantRole.MEMBER),
                        new ConversationParticipant(new UserId(userB), 0, null, ParticipantRole.MEMBER)
                ),
                null, null
        );
    }

    private static class StubConversationGateway implements ConversationGateway {
        private final Conversation conversation;
        StubConversationGateway(Conversation conversation) { this.conversation = conversation; }
        @Override public Conversation save(Conversation c) { return c; }
        @Override public Optional<Conversation> findById(ConversationId id) { return Optional.ofNullable(conversation); }
        @Override public List<Conversation> findAllByParticipant(UserId userId) { return List.of(); }
        @Override public void updateLastMessage(ConversationId id, String content, Instant sentAt) {}
    }

    private static class StubFileStorageGateway implements FileStorageGateway {
        @Override
        public URL generateUploadSignedUrl(String path, String contentType, long maxSize, Duration ttl) {
            try { return new URL("https://storage.googleapis.com/test-bucket/" + path + "?signed=true"); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public URL generateDownloadSignedUrl(String path, Duration ttl) {
            try { return new URL("https://storage.googleapis.com/test-bucket/" + path); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override public boolean objectExists(String path) { return true; }
        @Override public long getObjectSize(String path) { return 1024L; }
    }

    private static class StubPendingUploadGateway implements PendingUploadGateway {
        @Override public PendingUpload save(PendingUpload pu) { return pu; }
        @Override public Optional<PendingUpload> findById(UUID id) { return Optional.empty(); }
        @Override public List<PendingUpload> findAllByIds(List<UUID> ids) { return List.of(); }
        @Override public void deleteById(UUID id) {}
        @Override public void deleteExpired() {}
    }
}
