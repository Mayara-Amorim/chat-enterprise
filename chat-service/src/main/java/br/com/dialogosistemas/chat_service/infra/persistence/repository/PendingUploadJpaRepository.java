package br.com.dialogosistemas.chat_service.infra.persistence.repository;

import br.com.dialogosistemas.chat_service.infra.persistence.entity.PendingUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface PendingUploadJpaRepository extends JpaRepository<PendingUploadEntity, UUID> {

    @Modifying
    @Query("DELETE FROM PendingUploadEntity p WHERE p.expiresAt < :now")
    int deleteAllExpired(Instant now);
}
