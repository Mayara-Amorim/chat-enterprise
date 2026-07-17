package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.application.DTO.ConversationResponseDTO;
import br.com.dialogosistemas.chat_service.application.DTO.CreateConversationRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.MessageSentEventDTO;
import br.com.dialogosistemas.chat_service.domain.gateway.ConversationGateway;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.conversation.Conversation;
import br.com.dialogosistemas.chat_service.domain.model.conversation.ConversationType;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.infra.messaging.ChatKafkaProducer;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreateConversationUseCase {

    private static final String GROUP_RULES_PREFIX = "Regras do Grupo:\n";

    private final ConversationGateway conversationGateway;
    private final MessageGateway messageGateway;
    private final ChatKafkaProducer chatKafkaProducer;

    public CreateConversationUseCase(ConversationGateway conversationGateway,
                                     MessageGateway messageGateway,
                                     ChatKafkaProducer chatKafkaProducer) {
        this.conversationGateway = conversationGateway;
        this.messageGateway = messageGateway;
        this.chatKafkaProducer = chatKafkaProducer;
    }

    @Transactional
    public ConversationResponseDTO execute(CreateConversationRequestDTO request, UUID tenantId, UUID creatorId) {
        TenantId tenant = new TenantId(tenantId);
        UserId creator = new UserId(creatorId);
        ConversationType conversationType = ConversationType.valueOf(request.type().trim().toUpperCase(Locale.ROOT));
        Set<UserId> participantIds = request.participants().stream()
                .map(UserId::new)
                .collect(Collectors.toSet());

        Conversation conversation = switch (conversationType) {
            case GROUP -> Conversation.createGroup(
                    tenant,
                    request.title(),
                    request.description(),
                    creator,
                    participantIds
            );
            case INDIVIDUAL -> Conversation.createIndividual(tenant, creator, participantIds.iterator().next());
            default -> throw new IllegalArgumentException("Conversation type not supported: " + request.type());
        };

        Conversation savedConversation = conversationGateway.save(conversation);

        if (conversationType == ConversationType.GROUP && hasGroupRules(request.description())) {
            Message rulesMessage = Message.create(savedConversation.getId(), creator, GROUP_RULES_PREFIX + request.description());
            messageGateway.save(rulesMessage, savedConversation.getId());
            conversationGateway.updateLastMessage(
                    savedConversation.getId(),
                    rulesMessage.getContent(),
                    rulesMessage.getCreatedAt()
            );

            chatKafkaProducer.send(new MessageSentEventDTO(
                    rulesMessage.getId().value(),
                    savedConversation.getId().value(),
                    creator.value(),
                    rulesMessage.getContent(),
                    rulesMessage.getCreatedAt()
            ));
        }

        return ConversationResponseDTO.fromDomain(savedConversation);
    }

    private boolean hasGroupRules(String description) {
        return description != null && !description.isBlank();
    }
}
