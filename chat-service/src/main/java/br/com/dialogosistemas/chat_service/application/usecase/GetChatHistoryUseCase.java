package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageDTO;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetChatHistoryUseCase {

    private final MessageJpaRepository messageRepository;

    public GetChatHistoryUseCase(MessageJpaRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> execute(UUID conversationId, int page, int size) {
        // Cria a paginação (Página 0 = As mensagens mais recentes)
        var pageable = PageRequest.of(page, size);

        return messageRepository.findByConversationId(conversationId, pageable)
                .stream()
                .map(MessageDTO::fromEntity)
                .toList();
    }
}