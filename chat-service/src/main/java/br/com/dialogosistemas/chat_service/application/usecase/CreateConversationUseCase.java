package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.ConversationResponseDTO;
import br.com.dialogosistemas.chat_service.application.DTO.CreateConversationRequestDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreateConversationUseCase {

    private final ConversationGateway conversationGateway;

    public CreateConversationUseCase(ConversationGateway conversationGateway) {
        this.conversationGateway = conversationGateway;
    }

    @Transactional
    public ConversationResponseDTO execute(CreateConversationRequestDTO request, UUID tenantId, UUID creatorId) {
        // 1. Converter IDs brutos para Value Objects
        TenantId tenant = new TenantId(tenantId);
        UserId creator = new UserId(creatorId);

        // 2. Criar a Entidade de Domínio (aqui as regras de negócio são validadas)
        Conversation conversation;

        if ("GROUP".equalsIgnoreCase(request.type())) {
            var participants = request.participants().stream()
                    .map(UserId::new)
                    .collect(Collectors.toSet());

            conversation = Conversation.createGroup(tenant, creator, request.title(), participants);
        } else {
            // Assumindo que para individual vem apenas 1 ID na lista
            UUID otherUserId = request.participants().iterator().next();
            conversation = Conversation.createIndividual(tenant, creator, new UserId(otherUserId));
        }

        // 3. Persistir via Gateway
        Conversation saved = conversationGateway.save(conversation);

        // 4. Retornar DTO
        return ConversationResponseDTO.fromDomain(saved);
    }
}