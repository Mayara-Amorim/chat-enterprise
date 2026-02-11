package br.com.dialogosistemas.chat_service.domain.model.message;

public enum MessageStatus {
    SENT,       // Enviado ao servidor
    DELIVERED,  // Entregue ao destinatário
    READ,       // Lido pelo destinatário
    FAILED      // Erro no envio
}