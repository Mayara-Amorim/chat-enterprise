package br.com.dialogosistemas.chat_service.infra.persistence.repository;

import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageAttachmentJpaRepository extends JpaRepository<MessageAttachmentEntity, UUID> {
    List<MessageAttachmentEntity> findByMessageId(UUID messageId);
}
