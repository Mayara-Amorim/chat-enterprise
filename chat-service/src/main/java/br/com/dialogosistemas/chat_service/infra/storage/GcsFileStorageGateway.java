package br.com.dialogosistemas.chat_service.infra.storage;

import br.com.dialogosistemas.chat_service.domain.gateway.FileStorageGateway;
import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class GcsFileStorageGateway implements FileStorageGateway {

    private final Storage storage;
    private final FileUploadProperties properties;

    public GcsFileStorageGateway(Storage storage, FileUploadProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    @Override
    public URL generateUploadSignedUrl(String storagePath, String contentType, long maxSizeBytes, Duration ttl) {
        BlobInfo blobInfo = BlobInfo.newBuilder(properties.gcsBucket(), storagePath)
                .setContentType(contentType)
                .build();

        return storage.signUrl(
                blobInfo,
                ttl.toMinutes(), TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withContentType()
        );
    }

    @Override
    public URL generateDownloadSignedUrl(String storagePath, Duration ttl) {
        BlobInfo blobInfo = BlobInfo.newBuilder(properties.gcsBucket(), storagePath).build();

        return storage.signUrl(
                blobInfo,
                ttl.toMinutes(), TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)
        );
    }

    @Override
    public boolean objectExists(String storagePath) {
        Blob blob = storage.get(properties.gcsBucket(), storagePath);
        return blob != null && blob.exists();
    }

    @Override
    public long getObjectSize(String storagePath) {
        Blob blob = storage.get(properties.gcsBucket(), storagePath);
        if (blob == null || !blob.exists()) {
            throw new IllegalStateException("Object not found in storage: " + storagePath);
        }
        return blob.getSize();
    }
}
