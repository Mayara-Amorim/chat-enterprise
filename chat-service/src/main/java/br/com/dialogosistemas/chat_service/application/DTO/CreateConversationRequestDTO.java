package br.com.dialogosistemas.chat_service.application.DTO;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

// title e description sao OPCIONAIS de proposito: conversa INDIVIDUAL nao tem nenhum dos dois.
// Protegido por deve_aceitar_corpo_sem_os_campos_opcionais.
public record CreateConversationRequestDTO(
        @NotNull String type,
        String title,
        String description,
        @NotNull Set<UUID> participants
) {}
