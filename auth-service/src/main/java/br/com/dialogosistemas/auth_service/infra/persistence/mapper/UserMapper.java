package br.com.dialogosistemas.auth_service.infra.persistence.mapper;

import br.com.dialogosistemas.auth_service.domain.model.User;
import br.com.dialogosistemas.auth_service.infra.persistence.entity.UserEntity;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return new User(
                new UserId(entity.getId()),
                new TenantId(entity.getTenantId()),
                entity.getExternalId(),
                entity.getName(),
                entity.getEmail(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    public UserEntity toEntity(User user) {
        return new UserEntity(
                user.getId().value(),
                user.getTenantId().value(),
                user.getExternalId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
