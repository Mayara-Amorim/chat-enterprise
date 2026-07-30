package br.com.dialogosistemas.chat_service.application.DTO;

import jakarta.validation.constraints.NotNull;

public record UpdateSettingsRequestDTO(@NotNull String messagingPermission) {}
