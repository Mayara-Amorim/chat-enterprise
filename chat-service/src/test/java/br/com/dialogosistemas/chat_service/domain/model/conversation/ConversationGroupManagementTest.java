package br.com.dialogosistemas.chat_service.domain.model.conversation;

import br.com.dialogosistemas.shared_kernel.domain.exception.ForbiddenOperationException;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.TenantId;
import br.com.dialogosistemas.shared_kernel.domain.valueObject.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConversationGroupManagementTest {

    private UserId ownerId;
    private UserId adminId;
    private UserId memberId;
    private Conversation group;

    @BeforeEach
    void setUp() {
        ownerId = new UserId(UUID.randomUUID());
        adminId = new UserId(UUID.randomUUID());
        memberId = new UserId(UUID.randomUUID());
        TenantId tenantId = new TenantId(UUID.randomUUID());

        group = Conversation.createGroup(tenantId, "Test Group", "Desc",
                ownerId, Set.of(ownerId, adminId, memberId));

        group.promoteMember(ownerId, adminId);
    }

    private ConversationParticipant findParticipant(UserId userId) {
        return group.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void deve_adicionar_membro_quando_solicitado_por_admin() {
        UserId newMember = new UserId(UUID.randomUUID());
        group.addMember(adminId, newMember);
        assertTrue(group.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(newMember) && p.isActive()));
    }

    @Test
    void deve_rejeitar_adicao_quando_solicitado_por_member() {
        UserId newMember = new UserId(UUID.randomUUID());
        assertThrows(ForbiddenOperationException.class,
                () -> group.addMember(memberId, newMember));
    }

    @Test
    void deve_rejeitar_adicao_quando_usuario_ja_e_participante() {
        assertThrows(IllegalStateException.class,
                () -> group.addMember(ownerId, memberId));
    }

    @Test
    void deve_remover_membro_quando_solicitado_por_admin() {
        group.removeMember(adminId, memberId);
        assertFalse(findParticipant(memberId).isActive());
        assertNotNull(findParticipant(memberId).getLeftAt());
    }

    @Test
    void deve_rejeitar_remocao_do_owner() {
        assertThrows(ForbiddenOperationException.class,
                () -> group.removeMember(adminId, ownerId));
    }

    @Test
    void deve_rejeitar_remocao_quando_solicitado_por_member() {
        assertThrows(ForbiddenOperationException.class,
                () -> group.removeMember(memberId, adminId));
    }

    @Test
    void deve_promover_membro_quando_solicitado_por_owner() {
        UserId newMember = new UserId(UUID.randomUUID());
        group.addMember(ownerId, newMember);
        group.promoteMember(ownerId, newMember);
        assertEquals(ParticipantRole.ADMIN, findParticipant(newMember).getRole());
    }

    @Test
    void deve_rejeitar_promocao_quando_solicitado_por_admin() {
        UserId newMember = new UserId(UUID.randomUUID());
        group.addMember(ownerId, newMember);
        assertThrows(ForbiddenOperationException.class,
                () -> group.promoteMember(adminId, newMember));
    }

    @Test
    void deve_rebaixar_admin_quando_solicitado_por_owner() {
        group.demoteMember(ownerId, adminId);
        assertEquals(ParticipantRole.MEMBER, findParticipant(adminId).getRole());
    }

    @Test
    void deve_rejeitar_rebaixamento_quando_solicitado_por_admin() {
        UserId anotherAdmin = new UserId(UUID.randomUUID());
        group.addMember(ownerId, anotherAdmin);
        group.promoteMember(ownerId, anotherAdmin);
        assertThrows(ForbiddenOperationException.class,
                () -> group.demoteMember(adminId, anotherAdmin));
    }

    @Test
    void deve_registrar_saida_do_membro() {
        group.leaveGroup(memberId, false);
        assertFalse(findParticipant(memberId).isActive());
        assertNotNull(findParticipant(memberId).getLeftAt());
        assertFalse(findParticipant(memberId).isDeletedConversation());
    }

    @Test
    void deve_registrar_saida_e_apagar_conversa() {
        group.leaveGroup(memberId, true);
        assertFalse(findParticipant(memberId).isActive());
        assertTrue(findParticipant(memberId).isDeletedConversation());
    }

    @Test
    void deve_dissolver_grupo_quando_owner_sai() {
        group.leaveGroup(ownerId, false);
        assertNotNull(group.getDissolvedAt());
        group.getParticipants().forEach(p ->
                assertNotNull(p.getLeftAt(), "Participante " + p.getUserId() + " deveria ter leftAt"));
    }

    @Test
    void deve_rejeitar_mensagem_de_member_em_modo_admins_only() {
        group.updateMessagingPermission(ownerId, MessagingPermission.ADMINS_ONLY);
        assertThrows(ForbiddenOperationException.class,
                () -> group.addMessage(memberId, "Tentativa"));
    }

    @Test
    void deve_permitir_mensagem_de_admin_em_modo_admins_only() {
        group.updateMessagingPermission(ownerId, MessagingPermission.ADMINS_ONLY);
        var msg = group.addMessage(adminId, "Admin pode");
        assertEquals("Admin pode", msg.getContent());
    }

    @Test
    void deve_permitir_mensagem_de_owner_em_modo_admins_only() {
        group.updateMessagingPermission(ownerId, MessagingPermission.ADMINS_ONLY);
        var msg = group.addMessage(ownerId, "Owner pode");
        assertEquals("Owner pode", msg.getContent());
    }

    @Test
    void deve_rejeitar_mensagem_em_grupo_dissolvido() {
        group.leaveGroup(ownerId, false);
        assertThrows(ForbiddenOperationException.class,
                () -> group.addMessage(memberId, "Impossivel"));
    }

    @Test
    void deve_rejeitar_operacao_em_conversa_individual() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        UserId userA = new UserId(UUID.randomUUID());
        UserId userB = new UserId(UUID.randomUUID());
        Conversation individual = Conversation.createIndividual(tenantId, userA, userB);
        UserId newUser = new UserId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class,
                () -> individual.addMember(userA, newUser));
    }

    @Test
    void deve_apagar_conversa_do_inbox_apos_sair() {
        group.leaveGroup(memberId, false);
        group.deleteFromInbox(memberId);
        assertTrue(findParticipant(memberId).isDeletedConversation());
    }

    @Test
    void deve_apagar_conversa_do_inbox_apos_dissolucao() {
        group.leaveGroup(ownerId, false);
        group.deleteFromInbox(memberId);
        assertTrue(findParticipant(memberId).isDeletedConversation());
    }

    @Test
    void deve_rejeitar_apagar_inbox_de_participante_ativo() {
        assertThrows(IllegalStateException.class,
                () -> group.deleteFromInbox(memberId));
    }

    @Test
    void deve_alterar_messaging_permission() {
        group.updateMessagingPermission(adminId, MessagingPermission.ADMINS_ONLY);
        assertEquals(MessagingPermission.ADMINS_ONLY, group.getMessagingPermission());
    }

    @Test
    void deve_rejeitar_alteracao_por_member() {
        assertThrows(ForbiddenOperationException.class,
                () -> group.updateMessagingPermission(memberId, MessagingPermission.ADMINS_ONLY));
    }
}
