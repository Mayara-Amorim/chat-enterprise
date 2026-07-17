package br.com.dialogosistemas.chat_service.domain.model.message;

import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.domain.valueObject.MessageId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageTest {

    @Test
    void deleteByAuthorTracksDeletionAndHidesPublicContent() {
        UserId author = new UserId(UUID.randomUUID());
        Message message = message(author);

        message.deleteBy(author, false);

        assertNotNull(message.getDeletedAt());
        assertEquals(author, message.getDeletedBy());
        assertNull(message.getContent());
        assertEquals("Mensagem original", message.getStoredContent());
    }

    @Test
    void deleteByRejectsNonAdminWhoIsNotAuthor() {
        Message message = message(new UserId(UUID.randomUUID()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> message.deleteBy(new UserId(UUID.randomUUID()), false)
        );

        assertEquals("Acesso negado: Apenas o autor ou um administrador podem apagar esta mensagem.", exception.getMessage());
    }

    @Test
    void editByAuthorUpdatesContentAndTracksEditedAt() {
        UserId author = new UserId(UUID.randomUUID());
        Message message = message(author);

        message.edit("Mensagem editada", author);

        assertEquals("Mensagem editada", message.getContent());
        assertEquals("Mensagem editada", message.getStoredContent());
        assertNotNull(message.getEditedAt());
    }

    @Test
    void editRejectsNonAuthor() {
        UserId author = new UserId(UUID.randomUUID());
        Message message = message(author);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> message.edit("Tentativa de invasao", new UserId(UUID.randomUUID()))
        );

        assertEquals("Acesso negado: Apenas o autor original pode editar esta mensagem.", exception.getMessage());
    }

    @Test
    void editRejectsDeletedMessage() {
        UserId author = new UserId(UUID.randomUUID());
        Message message = message(author);
        message.deleteBy(author, false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> message.edit("Nao deveria editar", author)
        );

        assertEquals("A mensagem apagada n\u00E3o pode ser editada.", exception.getMessage());
    }

    @Test
    void deve_criar_mensagem_com_attachments_sem_content() {
        Attachment att = new Attachment(
                UUID.randomUUID(), "file.pdf", "application/pdf",
                5000L, "tenants/t/conv/c/file.pdf", Instant.now()
        );
        Message msg = Message.createWithAttachments(
                new ConversationId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()),
                null,
                List.of(att)
        );

        assertNull(msg.getContent());
        assertEquals(1, msg.getAttachments().size());
    }

    @Test
    void deve_criar_mensagem_com_content_e_attachments() {
        Attachment att = new Attachment(
                UUID.randomUUID(), "img.png", "image/png",
                1024L, "path/img.png", Instant.now()
        );
        Message msg = Message.createWithAttachments(
                new ConversationId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()),
                "Olha essa foto",
                List.of(att)
        );

        assertEquals("Olha essa foto", msg.getContent());
        assertEquals(1, msg.getAttachments().size());
    }

    @Test
    void deve_rejeitar_mensagem_sem_content_e_sem_attachments() {
        assertThrows(IllegalArgumentException.class, () ->
                Message.createWithAttachments(
                        new ConversationId(UUID.randomUUID()),
                        new UserId(UUID.randomUUID()),
                        null,
                        List.of()
                )
        );
    }

    @Test
    void deve_rejeitar_mais_de_10_attachments() {
        List<Attachment> attachments = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> new Attachment(UUID.randomUUID(), "f" + i + ".jpg", "image/jpeg", 1024L, "p/" + i, Instant.now()))
                .toList();

        assertThrows(IllegalArgumentException.class, () ->
                Message.createWithAttachments(
                        new ConversationId(UUID.randomUUID()),
                        new UserId(UUID.randomUUID()),
                        "caption",
                        attachments
                )
        );
    }

    private Message message(UserId author) {
        return new Message(
                new MessageId(UUID.randomUUID()),
                new ConversationId(UUID.randomUUID()),
                author,
                "Mensagem original",
                Instant.parse("2026-03-31T12:00:00Z"),
                MessageStatus.SENT,
                Set.of()
        );
    }
}
