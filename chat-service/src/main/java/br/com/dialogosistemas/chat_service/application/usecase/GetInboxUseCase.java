package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.InboxItemDTO;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetInboxUseCase {

    private final ConversationJpaRepository repository;

    public GetInboxUseCase(ConversationJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InboxItemDTO> execute(UUID userId) {
        // Busca direta na View/Tabela otimizada
        return repository.findAllByParticipantId(userId).stream()
                .map(entity -> new InboxItemDTO(
                        entity.getId(),
                        entity.getTitle(), // Futuro: Se for null (chat privado), buscar nome do outro user
                        entity.getLastMessageContent(),
                        entity.getLastMessageAt()
                ))
                // Ordena: Conversas com mensagens mais recentes no topo
                .sorted((a, b) -> {
                    if (b.lastMessageAt() == null) return -1;
                    if (a.lastMessageAt() == null) return 1;
                    return b.lastMessageAt().compareTo(a.lastMessageAt());
                })
                .toList();
    }
}