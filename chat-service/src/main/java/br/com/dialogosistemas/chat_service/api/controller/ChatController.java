package br.com.dialogosistemas.chat_service.api.controller;

import br.com.dialogosistemas.chat_service.application.DTO.ConversationResponseDTO;
import br.com.dialogosistemas.chat_service.application.DTO.CreateConversationRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.SendMessageRequestDTO;
import br.com.dialogosistemas.chat_service.application.usecase.CreateConversationUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.SendMessageUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat/conversations")
public class ChatController {

    private final CreateConversationUseCase createConversationUseCase;
    private final SendMessageUseCase sendMessageUseCase;

    public ChatController(CreateConversationUseCase createConversationUseCase, SendMessageUseCase sendMessageUseCase) {
        this.createConversationUseCase = createConversationUseCase;
        this.sendMessageUseCase = sendMessageUseCase;
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
}