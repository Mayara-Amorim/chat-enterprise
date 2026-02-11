package br.com.dialogosistemas.chat_service.infra.persistence.repository;

import br.com.dialogosistemas.chat_service.infra.persistence.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {

    // JPQL para buscar conversas onde o usuário X está na lista de participantes
    @Query("SELECT c FROM ConversationEntity c JOIN c.participantIds p WHERE p = :userId")
    List<ConversationEntity> findAllByParticipantId(@Param("userId") UUID userId);

}