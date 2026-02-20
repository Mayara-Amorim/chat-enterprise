package br.com.dialogosistemas.chat_service.infra.persistence.repository;

import br.com.dialogosistemas.chat_service.infra.persistence.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {

    // Busca mensagens da conversa, ordenadas por data descrescente (mais novas primeiro)
    // O Pageable do Spring já resolve o LIMIT e OFFSET automaticamente
    @Query("SELECT m FROM MessageEntity m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<MessageEntity> findByConversationId(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Query("SELECT m FROM MessageEntity m WHERE m.conversation.id = :conversationId AND m.createdAt < :lastMessageDate ORDER BY m.createdAt DESC")
    List<MessageEntity> findHistoryBefore(@Param("conversationId") UUID conversationId, @Param("lastMessageDate") Instant lastMessageDate, Pageable pageable);

    //Traz mensagens que o ususario ainda nao leu com NOT EXISTS
    @Query("SELECT m FROM MessageEntity m " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.senderId != :userId " +
            "AND m.status != :readStatus " + // Tipagem Forte
            "AND NOT EXISTS (SELECT r FROM m.readReceipts r WHERE r.userId = :userId)")
    List<MessageEntity> findUnreadByParticipant(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("readStatus") br.com.dialogosistemas.chat_service.domain.model.message.MessageStatus readStatus
    );
}