package br.com.dialogosistemas.auth_service.domain.model;

import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

public class Tenant {

    private final TenantId id;
    private final String name;
    private final String apiKeyHash;
    private boolean active;
    private final Instant createdAt;

    public Tenant(TenantId id, String name, String apiKeyHash, boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.apiKeyHash = apiKeyHash;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static CreateResult create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be empty");
        }
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String rawApiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String hash = hashApiKey(rawApiKey);
        Tenant tenant = new Tenant(TenantId.generate(), name, hash, true, Instant.now());
        return new CreateResult(tenant, rawApiKey);
    }

    public static String hashApiKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawApiKey.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public void deactivate() { this.active = false; }

    public TenantId getId() { return id; }
    public String getName() { return name; }
    public String getApiKeyHash() { return apiKeyHash; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public record CreateResult(Tenant tenant, String rawApiKey) {}
}
