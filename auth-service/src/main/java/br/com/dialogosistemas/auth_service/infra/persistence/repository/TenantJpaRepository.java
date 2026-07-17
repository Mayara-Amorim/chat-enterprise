package br.com.dialogosistemas.auth_service.infra.persistence.repository;

import br.com.dialogosistemas.auth_service.infra.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantJpaRepository extends JpaRepository<TenantEntity, UUID> {
    Optional<TenantEntity> findByApiKeyHash(String apiKeyHash);
}
