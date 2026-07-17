package br.com.dialogosistemas.auth_service.infra.persistence.entity;

import br.com.dialogosistemas.auth_service.domain.model.UserRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {}

    public UserEntity(UUID id, UUID tenantId, String externalId, String name, String email, UserRole role, boolean active, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.externalId = externalId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(UserRole role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
}
