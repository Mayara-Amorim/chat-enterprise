package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.InboxItemDTO;
import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationParticipantEntity;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        return repository.findAllByParticipantId(userId).stream()
                .map(entity -> {
                    ConversationParticipantEntity myParticipant = entity.getParticipants().stream()
                            .filter(p -> p.getUserId().equals(userId))
                            .findFirst()
                            .orElse(null);


                    // Extrai os valores com segurança
                    Integer unreadCount = (myParticipant != null && myParticipant.getUnreadCount() != null)
                            ? myParticipant.getUnreadCount() : 0;
                    Instant lastReadAt = (myParticipant != null)
                            ? myParticipant.getLastReadAt() : null;
                    return new InboxItemDTO(
                            entity.getId(),
                            entity.getTitle(),
                            entity.getLastMessageContent(),
                            entity.getLastMessageAt(),
                            unreadCount,
                            lastReadAt
                    );
                })
                .sorted((a, b) -> {
                    if (b.lastMessageAt() == null) return -1;
                    if (a.lastMessageAt() == null) return 1;
                    return b.lastMessageAt().compareTo(a.lastMessageAt());
                })
                .toList();
    }
}