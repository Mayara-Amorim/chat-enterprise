package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.ChatHistoryResponseDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageDTO;
import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.chat_service.infra.util.CursorUtils;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GetChatHistoryUseCase {

    private final ConversationGateway conversationGateway;
    private final MessageGateway messageGateway;

    public GetChatHistoryUseCase(ConversationGateway conversationGateway, MessageGateway messageGateway) {
        this.conversationGateway = conversationGateway;
        this.messageGateway = messageGateway;
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponseDTO execute(UUID conversationId, UUID requesterId, String cursor, int limit) {
        ConversationId convId = new ConversationId(conversationId);
        UserId requester = new UserId(requesterId);

        var conversation = conversationGateway.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada."));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(participant -> participant.getUserId().equals(requester));

        if (!isParticipant) {
            throw new IllegalArgumentException("Acesso negado: o utilizador não é participante desta conversa.");
        }

        CursorUtils.DecodedCursor decodedCursor = CursorUtils.decode(cursor);
        Instant cursorDate = decodedCursor != null ? decodedCursor.createdAt() : null;
        UUID cursorId = decodedCursor != null ? decodedCursor.id() : null;

        List<MessageDTO> messages = messageGateway.findHistoryBeforeCursor(convId, cursorDate, cursorId, limit).stream()
                .map(message -> new MessageDTO(
                        message.getId().value(),
                        message.getContent(),
                        message.getSenderId().value().toString(),
                        message.getCreatedAt(),
                        message.getStatus().name()
                ))
                .toList();

        String nextCursor = messages.isEmpty()
                ? null
                : CursorUtils.encode(messages.getLast().sentAt(), messages.getLast().id());
        return new ChatHistoryResponseDTO(messages, nextCursor);
    }
}
