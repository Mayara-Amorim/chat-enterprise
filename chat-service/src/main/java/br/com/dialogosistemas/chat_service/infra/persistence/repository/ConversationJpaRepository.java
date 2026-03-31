package br.com.dialogosistemas.chat_service.infra.persistence.repository;

import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {

//    // JPQL para buscar conversas onde o usuário X está na lista de participantes
//    @Query("SELECT c FROM ConversationEntity c JOIN c.participantIds p WHERE p = :userId")
//    List<ConversationEntity> findAllByParticipantId(@Param("userId") UUID userId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE conversations 
        SET last_message_content = :content, 
            last_message_at = :timestamp 
        WHERE id = :id
        """, nativeQuery = true)
    void updateLastMessage(@Param("id") UUID id,
                           @Param("content") String content,
                           @Param("timestamp") Instant timestamp);

    @Query(value = """
        SELECT c.* FROM conversations c
        JOIN conversation_participants cp ON c.id = cp.conversation_id
        WHERE cp.user_id = :userId
        """, nativeQuery = true)
    List<ConversationEntity> findAllByParticipantId(@Param("userId") UUID userId);
}