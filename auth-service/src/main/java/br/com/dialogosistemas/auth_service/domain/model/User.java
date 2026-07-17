package br.com.dialogosistemas.auth_service.domain.model;

import br.com.dialogosistemas.shared_kernel.domain.exception.InvalidInputException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UserId id;
    private final TenantId tenantId;
    private final String externalId;
    private String name;
    private String email;
    private UserRole role;
    private boolean active;
    private final Instant createdAt;

    public User(UserId id, TenantId tenantId, String externalId, String name, String email, UserRole role, boolean active, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.externalId = externalId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static User create(TenantId tenantId, String externalId, String name, String email, UserRole role) {
        if (externalId == null || externalId.isBlank()) {
            throw new InvalidInputException("External ID cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("User name cannot be empty");
        }
        if (role == null) {
            throw new InvalidInputException("User role cannot be empty");
        }
        return new User(new UserId(UUID.randomUUID()), tenantId, externalId, name, email, role, true, Instant.now());
    }

    public void update(String name, String email, UserRole role) {
        if (name != null && !name.isBlank()) this.name = name;
        if (email != null) this.email = email;
        if (role != null) this.role = role;
    }

    public void deactivate() { this.active = false; }

    public UserId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
