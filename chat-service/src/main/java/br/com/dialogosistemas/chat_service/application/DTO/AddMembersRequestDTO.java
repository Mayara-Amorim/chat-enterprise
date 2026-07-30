package br.com.dialogosistemas.chat_service.application.DTO;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record AddMembersRequestDTO(@NotNull Set<UUID> userIds) {}
