package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.Set;
import java.util.UUID;

public record AddMembersRequestDTO(Set<UUID> userIds) {}
