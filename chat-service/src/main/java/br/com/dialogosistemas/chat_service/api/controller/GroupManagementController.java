package br.com.dialogosistemas.chat_service.api.controller;

import br.com.dialogosistemas.chat_service.application.DTO.AddMembersRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.LeaveGroupRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.UpdateSettingsRequestDTO;
import br.com.dialogosistemas.chat_service.application.usecase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat/conversations/{conversationId}")
@Tag(name = "Gerenciamento de Grupo", description = "Operacoes de gerenciamento de membros e configuracoes de grupo")
public class GroupManagementController {

    private final AddMemberUseCase addMemberUseCase;
    private final RemoveMemberUseCase removeMemberUseCase;
    private final PromoteMemberUseCase promoteMemberUseCase;
    private final DemoteMemberUseCase demoteMemberUseCase;
    private final LeaveGroupUseCase leaveGroupUseCase;
    private final UpdateGroupSettingsUseCase updateGroupSettingsUseCase;
    private final DeleteConversationFromInboxUseCase deleteConversationFromInboxUseCase;

    public GroupManagementController(AddMemberUseCase addMemberUseCase,
                                     RemoveMemberUseCase removeMemberUseCase,
                                     PromoteMemberUseCase promoteMemberUseCase,
                                     DemoteMemberUseCase demoteMemberUseCase,
                                     LeaveGroupUseCase leaveGroupUseCase,
                                     UpdateGroupSettingsUseCase updateGroupSettingsUseCase,
                                     DeleteConversationFromInboxUseCase deleteConversationFromInboxUseCase) {
        this.addMemberUseCase = addMemberUseCase;
        this.removeMemberUseCase = removeMemberUseCase;
        this.promoteMemberUseCase = promoteMemberUseCase;
        this.demoteMemberUseCase = demoteMemberUseCase;
        this.leaveGroupUseCase = leaveGroupUseCase;
        this.updateGroupSettingsUseCase = updateGroupSettingsUseCase;
        this.deleteConversationFromInboxUseCase = deleteConversationFromInboxUseCase;
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Adicionar membros", description = "Adiciona um ou mais membros ao grupo. Requer role OWNER ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membros adicionados"),
            @ApiResponse(responseCode = "403", description = "Sem permissao"),
            @ApiResponse(responseCode = "409", description = "Usuario ja e participante")
    })
    public void addMembers(@PathVariable UUID conversationId,
                           @Valid @RequestBody AddMembersRequestDTO request,
                           @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        addMemberUseCase.execute(conversationId, request.userIds(), requesterId);
    }

    @DeleteMapping("/members/{userId}")
    @Operation(summary = "Remover membro", description = "Remove um membro do grupo. Requer role OWNER ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro removido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao ou tentativa de remover OWNER")
    })
    public ResponseEntity<Void> removeMember(@PathVariable UUID conversationId,
                                              @PathVariable UUID userId,
                                              @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        removeMemberUseCase.execute(conversationId, userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/members/{userId}/promote")
    @Operation(summary = "Promover a admin", description = "Promove um membro a ADMIN. Requer role OWNER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro promovido"),
            @ApiResponse(responseCode = "403", description = "Apenas OWNER pode promover")
    })
    public ResponseEntity<Void> promoteMember(@PathVariable UUID conversationId,
                                               @PathVariable UUID userId,
                                               @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        promoteMemberUseCase.execute(conversationId, userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/members/{userId}/demote")
    @Operation(summary = "Rebaixar a membro", description = "Rebaixa um ADMIN para MEMBER. Requer role OWNER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Admin rebaixado"),
            @ApiResponse(responseCode = "403", description = "Apenas OWNER pode rebaixar")
    })
    public ResponseEntity<Void> demoteMember(@PathVariable UUID conversationId,
                                              @PathVariable UUID userId,
                                              @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        demoteMemberUseCase.execute(conversationId, userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leave")
    @Operation(summary = "Sair do grupo", description = "Membro ou admin sai do grupo. Se OWNER sai, o grupo e dissolvido.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Saiu do grupo"),
            @ApiResponse(responseCode = "409", description = "Grupo ja dissolvido")
    })
    public ResponseEntity<Void> leaveGroup(@PathVariable UUID conversationId,
                                            @Valid @RequestBody LeaveGroupRequestDTO request,
                                            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        leaveGroupUseCase.execute(conversationId, userId, request.deleteConversation());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/settings")
    @Operation(summary = "Alterar configuracoes", description = "Altera a permissao de envio de mensagens (ALL ou ADMINS_ONLY). Requer OWNER ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Configuracao alterada"),
            @ApiResponse(responseCode = "403", description = "Sem permissao")
    })
    public ResponseEntity<Void> updateSettings(@PathVariable UUID conversationId,
                                                @Valid @RequestBody UpdateSettingsRequestDTO request,
                                                @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        updateGroupSettingsUseCase.execute(conversationId, requesterId, request.messagingPermission());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/inbox")
    @Operation(summary = "Apagar conversa do inbox", description = "Remove a conversa do inbox do usuario. So funciona apos sair do grupo ou grupo dissolvido.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conversa apagada do inbox"),
            @ApiResponse(responseCode = "409", description = "Usuario ainda e participante ativo")
    })
    public ResponseEntity<Void> deleteFromInbox(@PathVariable UUID conversationId,
                                                 @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        deleteConversationFromInboxUseCase.execute(conversationId, userId);
        return ResponseEntity.noContent().build();
    }
}
