package br.com.dialogosistemas.auth_service.infra.persistence;

import br.com.dialogosistemas.auth_service.domain.gateway.UserGateway;
import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.auth_service.infra.persistence.entity.UserEntity;
import br.com.dialogosistemas.auth_service.infra.persistence.mapper.UserMapper;
import br.com.dialogosistemas.auth_service.infra.persistence.repository.UserJpaRepository;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryGateway implements UserGateway {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    public UserRepositoryGateway(UserJpaRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        Optional<UserEntity> existing = jpaRepository.findById(user.getId().value());
        if (existing.isPresent()) {
            UserEntity entity = existing.get();
            entity.setName(user.getName());
            entity.setEmail(user.getEmail());
            entity.setRole(user.getRole());
            entity.setActive(user.isActive());
            var saved = jpaRepository.save(entity);
            return mapper.toDomain(saved);
        }
        var entity = mapper.toEntity(user);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByTenantAndExternalId(TenantId tenantId, String externalId) {
        return jpaRepository.findByTenantIdAndExternalId(tenantId.value(), externalId).map(mapper::toDomain);
    }
}
