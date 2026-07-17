package br.com.dialogosistemas.shared_kernel.domain.valueObject;

import java.util.UUID;

/**
 * Value Object representando a identidade do Cliente (Tenant).
 * Record do Java: Imutável, com equals/hashCode prontos.
 */
public record TenantId(UUID value) {
    public TenantId {
        if (value == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }

    public static TenantId fromString(String uuid) {
        return new TenantId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}