package br.com.dialogosistemas.chat_service.application.DTO;

import java.util.UUID;

public record GroupEventDTO(
        String type,
        UUID conversationId,
        UUID userId,
        UUID triggeredBy,
        String messagingPermission
) {
    public static GroupEventDTO memberEvent(String type, UUID conversationId, UUID userId, UUID triggeredBy) {
        return new GroupEventDTO(type, conversationId, userId, triggeredBy, null);
    }

    public static GroupEventDTO settingsEvent(UUID conversationId, UUID triggeredBy, String messagingPermission) {
        return new GroupEventDTO("SETTINGS_CHANGED", conversationId, null, triggeredBy, messagingPermission);
    }

    public static GroupEventDTO dissolvedEvent(UUID conversationId, UUID dissolvedBy) {
        return new GroupEventDTO("GROUP_DISSOLVED", conversationId, null, dissolvedBy, null);
    }
}
