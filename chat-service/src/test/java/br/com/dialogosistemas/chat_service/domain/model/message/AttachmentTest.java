package br.com.dialogosistemas.chat_service.domain.model.message;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentTest {

    @Test
    void deve_criar_attachment_valido() {
        UUID fileId = UUID.randomUUID();
        Attachment attachment = new Attachment(
                fileId,
                "foto.jpg",
                "image/jpeg",
                2048000L,
                "tenants/t1/conversations/c1/file.jpg",
                Instant.now()
        );

        assertEquals(fileId, attachment.fileId());
        assertEquals("foto.jpg", attachment.originalFileName());
        assertEquals("image/jpeg", attachment.contentType());
        assertEquals(2048000L, attachment.sizeInBytes());
    }

    @Test
    void deve_rejeitar_fileName_nulo() {
        assertThrows(IllegalArgumentException.class, () ->
                new Attachment(UUID.randomUUID(), null, "image/jpeg", 1024L, "path", Instant.now())
        );
    }

    @Test
    void deve_rejeitar_size_zero_ou_negativo() {
        assertThrows(IllegalArgumentException.class, () ->
                new Attachment(UUID.randomUUID(), "file.jpg", "image/jpeg", 0L, "path", Instant.now())
        );
    }
}
