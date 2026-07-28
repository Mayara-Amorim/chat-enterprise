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
    void encodeAndDecodePreservesSubMillisecondPrecision() {
        // Postgres timestamptz guarda microssegundos; o cursor precisa preservar isso,
        // senao a mensagem do cursor (boundary) reaparece na busca com ?after= (duplicata).
        Instant createdAt = Instant.parse("2026-07-28T14:27:14.262141Z");
        UUID messageId = UUID.fromString("227dfa01-298b-45b2-9b15-e03b5839f113");

        CursorUtils.DecodedCursor decoded = CursorUtils.decode(CursorUtils.encode(createdAt, messageId));

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
