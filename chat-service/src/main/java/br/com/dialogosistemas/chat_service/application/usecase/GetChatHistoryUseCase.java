package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.MessageDTO;
import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.ConversationJpaRepository;
import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetChatHistoryUseCase {

    private final MessageJpaRepository messageRepository;
    private final ConversationJpaRepository conversationRepository;

    public GetChatHistoryUseCase(MessageJpaRepository messageRepository, ConversationJpaRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> execute(UUID conversationId, UUID requesterId, int page, int size) {
        // Passo 1: Validação de segurança (O utilizador pertence à conversa?)
        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada."));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(requesterId));

        if (!isParticipant) {
            throw new IllegalArgumentException("Acesso negado: o utilizador não é participante desta conversa.");
        }

        // Passo 2: Paginação baseada em Offset (LIMIT e OFFSET)
        var pageable = PageRequest.of(page, size);

        // Passo 3: Busca e mapeamento para DTO
        return messageRepository.findByConversationId(conversationId, pageable)
                .stream()
                .map(entity -> new MessageDTO(
                        entity.getId(),
                        entity.getSenderId().toString(), // Confirme se a sua MessageEntity tem este método
                        entity.getContent(),
                        entity.getCreatedAt(),
                        entity.getStatus().name()
                ))
                .toList();
    }
}