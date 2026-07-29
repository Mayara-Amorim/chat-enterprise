package br.com.dialogosistemas.auth_service.infra.persistence;

import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import br.com.dialogosistemas.auth_service.infra.persistence.mapper.TenantMapper;
import br.com.dialogosistemas.auth_service.infra.persistence.repository.TenantJpaRepository;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TenantRepositoryGateway implements TenantGateway {

    private final TenantJpaRepository jpaRepository;
    private final TenantMapper mapper;

    public TenantRepositoryGateway(TenantJpaRepository jpaRepository, TenantMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Tenant save(Tenant tenant) {
        var entity = mapper.toEntity(tenant);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Tenant> findById(TenantId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findByApiKeyHash(String apiKeyHash) {
        return jpaRepository.findByApiKeyHash(apiKeyHash).map(mapper::toDomain);
    }

    @Override
    public List<Tenant> findAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findAll(pageable).map(mapper::toDomain).getContent();
    }
}
