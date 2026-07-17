package br.com.dialogosistemas.chat_service.application.DTO;

import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponseDTO(
        UUID id,
        String type,
        String title,
        Instant createdAt
) {
    // Mapper estático simples para transformar Domínio em JSON
    public static ConversationResponseDTO fromDomain(Conversation conversation) {
        return new ConversationResponseDTO(
                conversation.getId().value(),
                conversation.getType().name(),
                conversation.getTitle(),
                conversation.getCreatedAt()
        );
    }
}