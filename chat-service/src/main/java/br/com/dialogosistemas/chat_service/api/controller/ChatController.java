package br.com.dialogosistemas.chat_service.api.controller;

import br.com.dialogosistemas.chat_service.application.DTO.*;
import br.com.dialogosistemas.chat_service.application.usecase.CreateConversationUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.GetChatHistoryUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.GetInboxUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.SendMessageUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import br.com.dialogosistemas.chat_service.application.usecase.MarkConversationAsReadUseCase;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat/conversations")
public class ChatController {

    private final CreateConversationUseCase createConversationUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final GetInboxUseCase getInboxUseCase;
    private final MarkConversationAsReadUseCase markConversationAsReadUseCase;


    public ChatController(CreateConversationUseCase createConversationUseCase,
                          SendMessageUseCase sendMessageUseCase,
                          GetChatHistoryUseCase getChatHistoryUseCase, GetInboxUseCase getInboxUseCase, MarkConversationAsReadUseCase markConversationAsReadUseCase) {
        this.createConversationUseCase = createConversationUseCase;
        this.sendMessageUseCase = sendMessageUseCase;
        this.getChatHistoryUseCase = getChatHistoryUseCase;
        this.getInboxUseCase = getInboxUseCase;
        this.markConversationAsReadUseCase = markConversationAsReadUseCase;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createConversation(@RequestBody CreateConversationRequestDTO request,
                                   @AuthenticationPrincipal Jwt jwt) {
        UUID creatorId = UUID.fromString(jwt.getSubject());
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new IllegalArgumentException("Token JWT inválido: Claim 'tenant_id' é obrigatória.");
        }
        UUID tenantId = UUID.fromString(tenantClaim);
        createConversationUseCase.execute(request, tenantId, creatorId);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendMessage(@RequestBody SendMessageRequestDTO request,
                            @AuthenticationPrincipal Jwt jwt) {

        UUID senderId = UUID.fromString(jwt.getSubject());
        sendMessageUseCase.execute(request, senderId);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getChatHistory(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        UUID loggedUserId = UUID.fromString(jwt.getSubject());

        List<MessageDTO> history = getChatHistoryUseCase.execute(conversationId, loggedUserId, page, size);

        return ResponseEntity.ok(history);
    }

    @GetMapping("/inbox")
    public List<InboxItemDTO> getInbox(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return getInboxUseCase.execute(userId);
    }

    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID loggedUserId = UUID.fromString(jwt.getSubject());
        markConversationAsReadUseCase.execute(conversationId, loggedUserId);
        return ResponseEntity.noContent().build();
    }
}