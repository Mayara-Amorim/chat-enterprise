package br.com.dialogosistemas.chat_service.infra.persistence;

import br.com.dialogosistemas.chat_service.domain.gateway.PendingUploadGateway;
import br.com.dialogosistemas.chat_service.domain.model.upload.PendingUpload;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.PendingUploadEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.PendingUploadJpaRepository;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PendingUploadRepositoryGateway implements PendingUploadGateway {

    private final PendingUploadJpaRepository repository;

    public PendingUploadRepositoryGateway(PendingUploadJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PendingUpload save(PendingUpload pu) {
        PendingUploadEntity entity = new PendingUploadEntity(
                pu.getId(), pu.getConversationId().value(), pu.getUserId().value(),
                pu.getOriginalFileName(), pu.getContentType(), pu.getSizeInBytes(),
                pu.getStoragePath(), pu.getCreatedAt(), pu.getExpiresAt()
        );
        repository.save(entity);
        return pu;
    }

    @Override
    public Optional<PendingUpload> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PendingUpload> findAllByIds(List<UUID> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteExpired() {
        repository.deleteAllExpired(Instant.now());
    }

    private PendingUpload toDomain(PendingUploadEntity e) {
        return new PendingUpload(
                e.getId(),
                new ConversationId(e.getConversationId()),
                new UserId(e.getUserId()),
                e.getOriginalFileName(),
                e.getContentType(),
                e.getSizeInBytes(),
                e.getStoragePath(),
                e.getCreatedAt(),
                e.getExpiresAt()
        );
    }
}
