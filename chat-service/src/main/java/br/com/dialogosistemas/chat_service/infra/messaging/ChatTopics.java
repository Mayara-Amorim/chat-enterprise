package br.com.dialogosistemas.chat_service.infra.messaging;

import java.util.List;

/**
 * Nomes dos topicos Kafka do chat, num lugar so — usados pelo producer e pelo pre-registro de
 * metricas de falha (ChatMetrics), evitando literais duplicados.
 */
public final class ChatTopics {

    public static final String MESSAGES = "chat-messages";
    public static final String STATUS = "chat-message-status-events";
    public static final String DELETED = "chat-message-deleted-events";
    public static final String EDITED = "chat-message-edited-events";
    public static final String GROUP = "chat-group-events";

    public static final List<String> ALL = List.of(MESSAGES, STATUS, DELETED, EDITED, GROUP);

    private ChatTopics() {
    }
}
