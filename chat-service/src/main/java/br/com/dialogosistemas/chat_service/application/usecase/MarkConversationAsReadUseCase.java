package br.com.dialogosistemas.chat_service.application.usecase;

import br.com.dialogosistemas.chat_service.domain.exception.ResourceNotFoundException;
import br.com.dialogosistemas.chat_service.domain.gateway.MessageGateway;
import br.com.dialogosistemas.chat_service.domain.model.message.Message;
import br.com.dialogosistemas.chat_service.domain.valueObject.ConversationId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import br.com.dialogosistemas.chat_service.infra.persistence.ConversationRepositoryGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MarkConversationAsReadUseCase {

    private final ConversationRepositoryGateway conversationRepository;
    private final MessageGateway messageGateway;

    public MarkConversationAsReadUseCase(ConversationRepositoryGateway conversationRepository, MessageGateway messageGateway) {
        this.conversationRepository = conversationRepository;
        this.messageGateway = messageGateway;
    }

    @Transactional
    public void execute(UUID conversationId, UUID userId) {
        var convId = new ConversationId(conversationId);
        var user = new UserId(userId);

        // 1. Zera o contador na Entidade Conversation
        var conversation = conversationRepository.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found conversation"));

        conversation.markParticipantAsRead(user);
        conversationRepository.save(conversation);

        // 2. Busca as mensagens não lidas e descobre o tamanho do grupo
        List<Message> unreadMessages = messageGateway.findUnreadByParticipant(convId, user);
        int totalParticipants = conversation.getParticipants().size();

        // 3. Aplica a regra de Domínio: A Mensagem decide se muda para DELIVERED ou READ
        for (Message msg : unreadMessages) {
            msg.markAsReadBy(user, totalParticipants);
        }

        // 4. Salva os recibos e os novos status em lote
        if (!unreadMessages.isEmpty()) {
            messageGateway.saveAll(unreadMessages, convId);
        }
    }
}