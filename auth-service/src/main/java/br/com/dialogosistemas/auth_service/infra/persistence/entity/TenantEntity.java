package br.com.dialogosistemas.auth_service.infra.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TenantEntity() {}

    public TenantEntity(UUID id, String name, String apiKeyHash, boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.apiKeyHash = apiKeyHash;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getApiKeyHash() { return apiKeyHash; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setActive(boolean active) { this.active = active; }
}
