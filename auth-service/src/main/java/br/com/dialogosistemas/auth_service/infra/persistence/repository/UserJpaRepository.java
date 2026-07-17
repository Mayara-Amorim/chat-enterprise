package br.com.dialogosistemas.auth_service.infra.persistence.repository;

import br.com.dialogosistemas.auth_service.infra.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByTenantIdAndExternalId(UUID tenantId, String externalId);
}
