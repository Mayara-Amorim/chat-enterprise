package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.List;

public record ChatHistoryResponseDTO(
        List<MessageDTO> messages,
        String nextCursor
) {
}
