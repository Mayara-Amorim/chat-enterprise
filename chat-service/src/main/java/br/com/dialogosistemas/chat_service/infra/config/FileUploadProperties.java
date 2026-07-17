package br.com.dialogosistemas.chat_service.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "file-upload")
public record FileUploadProperties(
        long maxSizeBytes,
        int maxAttachmentsPerMessage,
        int signedUrlTtlMinutes,
        int downloadUrlTtlMinutes,
        List<String> allowedContentTypes,
        String gcsBucket
) {
    public FileUploadProperties {
        if (maxSizeBytes <= 0) maxSizeBytes = 26_214_400L;
        if (maxAttachmentsPerMessage <= 0) maxAttachmentsPerMessage = 10;
        if (signedUrlTtlMinutes <= 0) signedUrlTtlMinutes = 15;
        if (downloadUrlTtlMinutes <= 0) downloadUrlTtlMinutes = 60;
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = List.of(
                    "image/jpeg", "image/png", "image/gif", "image/webp",
                    "application/pdf", "audio/mpeg", "audio/ogg",
                    "video/mp4", "video/webm", "application/zip"
            );
        }
        if (gcsBucket == null || gcsBucket.isBlank()) gcsBucket = "chat-dev-attachments";
    }
}
