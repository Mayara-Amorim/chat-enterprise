package br.com.dialogosistemas.chat_service.api.controller;

import br.com.dialogosistemas.chat_service.application.DTO.ConversationResponseDTO;
import br.com.dialogosistemas.chat_service.application.DTO.CreateConversationRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.InboxItemDTO;
import br.com.dialogosistemas.chat_service.application.DTO.SendMessageRequestDTO;
import br.com.dialogosistemas.chat_service.application.usecase.CreateConversationUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.GetChatHistoryUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.GetInboxUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.SendMessageUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat/conversations")
public class ChatController {

    private final CreateConversationUseCase createConversationUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final GetInboxUseCase getInboxUseCase;

    public ChatController(CreateConversationUseCase createConversationUseCase,
                          SendMessageUseCase sendMessageUseCase,
                          GetChatHistoryUseCase getChatHistoryUseCase, GetInboxUseCase getInboxUseCase) {
        this.createConversationUseCase = createConversationUseCase;
        this.sendMessageUseCase = sendMessageUseCase;
        this.getChatHistoryUseCase = getChatHistoryUseCase;
        this.getInboxUseCase = getInboxUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponseDTO create(@RequestBody CreateConversationRequestDTO request) {
        // SIMULAÇÃO: Como ainda não temos o Token JWT configurado, vamos fixar IDs temporários
        // Em produção, isso virá do SecurityContextHolder
        UUID fakeTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID fakeUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        return createConversationUseCase.execute(request, fakeTenantId, fakeUserId);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendMessage(@RequestBody SendMessageRequestDTO request) {
        UUID fakeUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        sendMessageUseCase.execute(request, fakeUserId);
    }

    @GetMapping("/{conversationId}/messages")
    public java.util.List<br.com.dialogosistemas.chat_service.application.DTO.MessageDTO> getHistory(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return getChatHistoryUseCase.execute(conversationId, page, size);
    }

    @GetMapping("/inbox")
    public List<InboxItemDTO> getInbox() {
        // Mock de usuário
        UUID fakeUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        return getInboxUseCase.execute(fakeUserId);
    }
}