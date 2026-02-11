package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.Set;
import java.util.UUID;

public record CreateConversationRequestDTO(String type,            // INDIVIDUAL ou GROUP
                                           String title,           // Obrigatório se for GROUP
                                           Set<UUID> participants// IDs dos outros participantes (não precisa mandar o seu próprio)
) {}