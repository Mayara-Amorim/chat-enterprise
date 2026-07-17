package br.com.dialogosistemas.chat_service.api.controller;

import br.com.dialogosistemas.chat_service.application.DTO.TypingIndicatorEventDTO;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@Controller
public class TypingWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public TypingWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.typing.{conversationId}")
    public void handleTyping(@DestinationVariable UUID conversationId, Principal principal) {
        var event = new TypingIndicatorEventDTO(
                conversationId,
                UUID.fromString(principal.getName()),
                Instant.now()
        );
        messagingTemplate.convertAndSend("/topic/chat." + conversationId + ".typing", event);
    }
}
