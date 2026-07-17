package br.com.dialogosistemas.chat_service.domain.model.message;

import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;

import java.time.Instant;
import java.util.Objects;

public class MessageReadReceipt {

    private final UserId userId;
    private final Instant readAt;

    public MessageReadReceipt(UserId userId, Instant readAt) {
        if (userId == null || readAt == null) throw new IllegalArgumentException("UserId e readAt são obrigatórios");
        this.userId = userId;
        this.readAt = readAt;
    }

    public UserId getUserId() { return userId; }
    public Instant getReadAt() { return readAt; }

    // O equals é baseado exclusivamente no utilizador, pois um utilizador só lê a mesma mensagem uma vez.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageReadReceipt that = (MessageReadReceipt) o;
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}