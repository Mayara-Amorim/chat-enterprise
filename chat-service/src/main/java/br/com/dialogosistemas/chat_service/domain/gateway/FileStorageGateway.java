package br.com.dialogosistemas.chat_service.domain.gateway;

import java.net.URL;
import java.time.Duration;

public interface FileStorageGateway {
    URL generateUploadSignedUrl(String storagePath, String contentType, long maxSizeBytes, Duration ttl);
    URL generateDownloadSignedUrl(String storagePath, Duration ttl);
    boolean objectExists(String storagePath);
    long getObjectSize(String storagePath);
}
