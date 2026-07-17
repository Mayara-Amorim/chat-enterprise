package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.RequestUploadRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.RequestUploadResponseDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.FileStorageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.PendingUploadGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.upload.PendingUpload;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class RequestUploadUseCase {

    private final ConversationGateway conversationGateway;
    private final FileStorageGateway fileStorageGateway;
    private final PendingUploadGateway pendingUploadGateway;
    private final FileUploadProperties properties;

    public RequestUploadUseCase(ConversationGateway conversationGateway,
                                FileStorageGateway fileStorageGateway,
                                PendingUploadGateway pendingUploadGateway,
                                FileUploadProperties properties) {
        this.conversationGateway = conversationGateway;
        this.fileStorageGateway = fileStorageGateway;
        this.pendingUploadGateway = pendingUploadGateway;
        this.properties = properties;
    }

    @Transactional
    public RequestUploadResponseDTO execute(RequestUploadRequestDTO request, UUID userId) {
        ConversationId convId = new ConversationId(request.conversationId());
        UserId user = new UserId(userId);

        Conversation conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(user) && p.isActive())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User is not a participant of this conversation"));

        if (!isContentTypeAllowed(request.contentType())) {
            throw new IllegalArgumentException("Content type not allowed: " + request.contentType());
        }

        if (request.sizeInBytes() > properties.maxSizeBytes()) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + properties.maxSizeBytes() + " bytes");
        }

        String tenantId = conversation.getTenantId().value().toString();
        String ext = extractExtension(request.fileName());
        String storagePath = "tenants/" + tenantId + "/conversations/" + request.conversationId() + "/" + UUID.randomUUID() + ext;

        PendingUpload pending = PendingUpload.create(
                convId, user, request.fileName(), request.contentType(),
                request.sizeInBytes(), storagePath, properties.signedUrlTtlMinutes()
        );
        pendingUploadGateway.save(pending);

        URL signedUrl = fileStorageGateway.generateUploadSignedUrl(
                storagePath,
                request.contentType(),
                properties.maxSizeBytes(),
                Duration.ofMinutes(properties.signedUrlTtlMinutes())
        );

        return new RequestUploadResponseDTO(pending.getId(), signedUrl.toString(), pending.getExpiresAt());
    }

    private boolean isContentTypeAllowed(String contentType) {
        return properties.allowedContentTypes().stream()
                .anyMatch(allowed -> {
                    if (allowed.endsWith("*")) {
                        return contentType.startsWith(allowed.substring(0, allowed.length() - 1));
                    }
                    return allowed.equals(contentType);
                });
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}
