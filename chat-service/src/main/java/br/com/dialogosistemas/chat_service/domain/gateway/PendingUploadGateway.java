package br.com.dialogosistemas.chat_service.domain.gateway;

import br.com.dialogosistemas.chat_service.domain.model.upload.PendingUpload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PendingUploadGateway {
    PendingUpload save(PendingUpload pendingUpload);
    Optional<PendingUpload> findById(UUID id);
    List<PendingUpload> findAllByIds(List<UUID> ids);
    void deleteById(UUID id);
    void deleteExpired();
}
