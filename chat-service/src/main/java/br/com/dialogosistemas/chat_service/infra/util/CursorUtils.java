package br.com.dialogosistemas.chat_service.infra.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class CursorUtils {

    private static final String SEPARATOR = "_";

    private CursorUtils() {
    }

    public static String encode(Instant createdAt, UUID id) {
        if (createdAt == null || id == null) {
            return null;
        }

        // ISO-8601 preserva a precisao de microssegundos do timestamptz; toEpochMilli() truncava
        // pra milissegundo e fazia a mensagem do cursor reaparecer no ?after= (duplicata no sync).
        String raw = createdAt.toString() + SEPARATOR + id;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static DecodedCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(SEPARATOR, 2);
            return new DecodedCursor(
                    Instant.parse(parts[0]),
                    UUID.fromString(parts[1])
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cursor inválido");
        }
    }

    public record DecodedCursor(Instant createdAt, UUID id) {
    }
}
