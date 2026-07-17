package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.AttachmentDTO;
import br.com.dialogosistemas.chat_service.application.DTO.ConfirmUploadRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.FileStorageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.PendingUploadGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.message.Attachment;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.model.upload.PendingUpload;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConfirmUploadUseCase {

    private final ConversationGateway conversationGateway;
    private final PendingUploadGateway pendingUploadGateway;
    private final FileStorageGateway fileStorageGateway;
    private final MessageGateway messageGateway;
    private final ChatKafkaProducer kafkaProducer;
    private final FileUploadProperties properties;

    public ConfirmUploadUseCase(ConversationGateway conversationGateway,
                                PendingUploadGateway pendingUploadGateway,
                                FileStorageGateway fileStorageGateway,
                                MessageGateway messageGateway,
                                ChatKafkaProducer kafkaProducer,
                                FileUploadProperties properties) {
        this.conversationGateway = conversationGateway;
        this.pendingUploadGateway = pendingUploadGateway;
        this.fileStorageGateway = fileStorageGateway;
        this.messageGateway = messageGateway;
        this.kafkaProducer = kafkaProducer;
        this.properties = properties;
    }

    @Transactional
    public void execute(ConfirmUploadRequestDTO request, UUID userId) {
        ConversationId convId = new ConversationId(request.conversationId());
        UserId user = new UserId(userId);

        Conversation conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(user) && p.isActive())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User is not a participant of this conversation"));

        List<PendingUpload> pendingUploads = pendingUploadGateway.findAllByIds(request.uploadIds());

        if (pendingUploads.size() != request.uploadIds().size()) {
            throw new IllegalArgumentException("One or more upload IDs not found");
        }

        List<Attachment> attachments = pendingUploads.stream().map(pu -> {
            if (!pu.getUserId().equals(user)) {
                throw new IllegalArgumentException("Upload does not belong to this user: " + pu.getId());
            }
            if (pu.isExpired()) {
                throw new IllegalStateException("Upload expired: " + pu.getId());
            }
            if (!fileStorageGateway.objectExists(pu.getStoragePath())) {
                throw new IllegalStateException("File not found in storage: " + pu.getId());
            }

            return new Attachment(
                    pu.getId(),
                    pu.getOriginalFileName(),
                    pu.getContentType(),
                    pu.getSizeInBytes(),
                    pu.getStoragePath(),
                    Instant.now()
            );
        }).toList();

        Message message = Message.createWithAttachments(convId, user, request.caption(), attachments);
        messageGateway.save(message, convId);

        String preview = request.caption() != null ? request.caption() : attachments.getFirst().originalFileName();
        conversationGateway.updateLastMessage(convId, preview, message.getCreatedAt());

        List<AttachmentDTO> attachmentDTOs = attachments.stream()
                .map(a -> new AttachmentDTO(a.fileId(), a.originalFileName(), a.contentType(), a.sizeInBytes()))
                .toList();

        MessageSentEventDTO event = new MessageSentEventDTO(
                message.getId().value(),
                conversation.getId().value(),
                message.getSenderId().value(),
                message.getContent(),
                message.getCreatedAt(),
                attachmentDTOs
        );
        kafkaProducer.send(event);

        pendingUploads.forEach(pu -> pendingUploadGateway.deleteById(pu.getId()));
    }
}
