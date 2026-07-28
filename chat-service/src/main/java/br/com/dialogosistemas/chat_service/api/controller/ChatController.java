package br.com.dialogosistemas.chat_service.api.controller;

import br.com.dialogosistemas.chat_service.application.DTO.ChatHistoryResponseDTO;
import br.com.dialogosistemas.chat_service.application.DTO.CreateConversationRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.EditMessageRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.InboxItemDTO;
import br.com.dialogosistemas.chat_service.application.DTO.SendMessageRequestDTO;
import br.com.dialogosistemas.chat_service.application.usecase.CreateConversationUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.DeleteMessageUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.EditMessageUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.GetChatHistoryUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.GetInboxUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.MarkConversationAsReadUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.SendMessageUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat/conversations")
@Tag(name = "Conversas", description = "Gerenciamento de conversas, mensagens e inbox")
public class ChatController {

    private final CreateConversationUseCase createConversationUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final GetInboxUseCase getInboxUseCase;
    private final MarkConversationAsReadUseCase markConversationAsReadUseCase;
    private final DeleteMessageUseCase deleteMessageUseCase;
    private final EditMessageUseCase editMessageUseCase;

    public ChatController(CreateConversationUseCase createConversationUseCase,
                          SendMessageUseCase sendMessageUseCase,
                          GetChatHistoryUseCase getChatHistoryUseCase,
                          GetInboxUseCase getInboxUseCase,
                          MarkConversationAsReadUseCase markConversationAsReadUseCase,
                          DeleteMessageUseCase deleteMessageUseCase,
                          EditMessageUseCase editMessageUseCase) {
        this.createConversationUseCase = createConversationUseCase;
        this.sendMessageUseCase = sendMessageUseCase;
        this.getChatHistoryUseCase = getChatHistoryUseCase;
        this.getInboxUseCase = getInboxUseCase;
        this.markConversationAsReadUseCase = markConversationAsReadUseCase;
        this.deleteMessageUseCase = deleteMessageUseCase;
        this.editMessageUseCase = editMessageUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar conversa",
            description = "Cria uma nova conversa individual ou em grupo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conversa criada"),
            @ApiResponse(responseCode = "401", description = "Token JWT invalido")
    })
    public void createConversation(@RequestBody CreateConversationRequestDTO request,
                                   @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
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
    @Operation(
            summary = "Enviar mensagem",
            description = "Envia uma mensagem em uma conversa existente. O remetente deve ser participante da conversa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensagem enviada"),
            @ApiResponse(responseCode = "400", description = "Conversa nao encontrada"),
            @ApiResponse(responseCode = "403", description = "Usuario nao e participante da conversa")
    })
    public void sendMessage(@RequestBody SendMessageRequestDTO request,
                            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID senderId = UUID.fromString(jwt.getSubject());
        sendMessageUseCase.execute(request, senderId);
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(
            summary = "Obter historico ou sincronizar mensagens novas",
            description = "Por padrao retorna o historico (mensagens mais antigas primeiro) paginado por cursor via `cursor`. " +
                    "Para sincronizar apos reconectar, envie `after` com o cursor da ultima mensagem ja recebida: " +
                    "retorna apenas as mensagens que chegaram DEPOIS dele (ordem crescente). " +
                    "Use `cursor` OU `after`, nunca os dois."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagens retornadas")
    })
    public ResponseEntity<ChatHistoryResponseDTO> getChatHistory(
            @PathVariable UUID conversationId,
            @Parameter(description = "Cursor para paginacao do historico, para tras (formato: timestamp_uuid)") @RequestParam(required = false) String cursor,
            @Parameter(description = "Cursor da ultima mensagem recebida; retorna as mais novas que ele (sync ao reconectar)") @RequestParam(required = false) String after,
            @Parameter(description = "Numero maximo de mensagens") @RequestParam(defaultValue = "20") int limit,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID loggedUserId = UUID.fromString(jwt.getSubject());
        ChatHistoryResponseDTO history = (after != null)
                ? getChatHistoryUseCase.executeSince(conversationId, loggedUserId, after, limit)
                : getChatHistoryUseCase.execute(conversationId, loggedUserId, cursor, limit);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    @Operation(
            summary = "Excluir mensagem",
            description = "Exclui uma mensagem (soft-delete). O autor pode excluir suas proprias mensagens; admins podem excluir qualquer mensagem do grupo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mensagem excluida"),
            @ApiResponse(responseCode = "400", description = "Mensagem nao encontrada"),
            @ApiResponse(responseCode = "403", description = "Sem permissao para excluir")
    })
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID requesterId = UUID.fromString(jwt.getSubject());
        deleteMessageUseCase.execute(conversationId, messageId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{conversationId}/messages/{messageId}")
    @Operation(
            summary = "Editar mensagem",
            description = "Edita o conteudo de uma mensagem. Apenas o autor pode editar. Mensagens excluidas nao podem ser editadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mensagem editada"),
            @ApiResponse(responseCode = "400", description = "Mensagem nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Mensagem ja foi excluida")
    })
    public ResponseEntity<Void> editMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestBody EditMessageRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID requesterId = UUID.fromString(jwt.getSubject());
        editMessageUseCase.execute(conversationId, messageId, requesterId, request.content());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inbox")
    @Operation(
            summary = "Obter inbox",
            description = "Retorna a lista de conversas do usuario com preview da ultima mensagem e contagem de nao lidas."
    )
    @ApiResponse(responseCode = "200", description = "Inbox retornado")
    public List<InboxItemDTO> getInbox(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return getInboxUseCase.execute(userId);
    }

    @PatchMapping("/{conversationId}/read")
    @Operation(
            summary = "Marcar como lida",
            description = "Marca todas as mensagens de uma conversa como lidas pelo usuario."
    )
    @ApiResponse(responseCode = "204", description = "Conversa marcada como lida")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID loggedUserId = UUID.fromString(jwt.getSubject());
        markConversationAsReadUseCase.execute(conversationId, loggedUserId);
        return ResponseEntity.noContent().build();
    }
}
