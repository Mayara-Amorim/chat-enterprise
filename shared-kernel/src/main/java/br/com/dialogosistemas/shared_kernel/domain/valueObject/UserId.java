package br.com.dialogosistemas.shared_kernel.domain.valueObject;

import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        if (value == null) throw new IllegalArgumentException("UserId cannot be null");
    }
    public static UserId fromString(String uuid) {
        return new UserId(UUID.fromString(uuid));
    }
    @Override
    public String toString() {
        return value.toString();
    }
}