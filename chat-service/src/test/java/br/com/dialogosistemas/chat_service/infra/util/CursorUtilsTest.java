package br.com.dialogosistemas.chat_service.infra.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CursorUtilsTest {

    @Test
    void encodeAndDecodeRoundTrip() {
        Instant createdAt = Instant.parse("2026-03-31T11:00:00Z");
        UUID messageId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        String encoded = CursorUtils.encode(createdAt, messageId);
        CursorUtils.DecodedCursor decoded = CursorUtils.decode(encoded);

        assertEquals(createdAt, decoded.createdAt());
        assertEquals(messageId, decoded.id());
    }

    @Test
    void encodeReturnsNullWhenInputIsIncomplete() {
        assertNull(CursorUtils.encode(null, UUID.randomUUID()));
        assertNull(CursorUtils.encode(Instant.now(), null));
    }

    @Test
    void decodeReturnsNullWhenCursorIsBlank() {
        assertNull(CursorUtils.decode(null));
        assertNull(CursorUtils.decode(" "));
    }

    @Test
    void decodeRejectsInvalidCursor() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> CursorUtils.decode("cursor-invalido")
        );

        assertEquals("Cursor inválido", exception.getMessage());
    }
}
