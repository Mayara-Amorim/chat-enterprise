package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.FileStorageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.message.Attachment;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class GetAttachmentDownloadUrlUseCase {

    private final MessageGateway messageGateway;
    private final ConversationGateway conversationGateway;
    private final FileStorageGateway fileStorageGateway;
    private final FileUploadProperties properties;

    public GetAttachmentDownloadUrlUseCase(MessageGateway messageGateway,
                                           ConversationGateway conversationGateway,
                                           FileStorageGateway fileStorageGateway,
                                           FileUploadProperties properties) {
        this.messageGateway = messageGateway;
        this.conversationGateway = conversationGateway;
        this.fileStorageGateway = fileStorageGateway;
        this.properties = properties;
    }

    public record DownloadUrlResponse(String downloadUrl, Instant expiresAt) {}

    public DownloadUrlResponse execute(UUID messageId, UUID fileId, UUID userId) {
        UserId user = new UserId(userId);

        Message message = messageGateway.findById(new MessageId(messageId))
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        Conversation conversation = conversationGateway.findById(message.getConversationId())
                .orElseThrow(() -> new IllegalStateException("Conversation not found"));

        conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(user) && p.isActive())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User is not a participant of this conversation"));

        Attachment attachment = message.getAttachments().stream()
                .filter(a -> a.fileId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        Duration ttl = Duration.ofMinutes(properties.downloadUrlTtlMinutes());
        URL url = fileStorageGateway.generateDownloadSignedUrl(attachment.storagePath(), ttl);

        return new DownloadUrlResponse(url.toString(), Instant.now().plus(ttl));
    }
}
