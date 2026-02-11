package br.com.dialogosistemas.chat_service.infra.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita um broker de memória simples para enviar mensagens de volta aos clientes
        // Prefixos: /topic (para broadcast/grupos), /queue (para mensagens diretas)
        config.enableSimpleBroker("/topic", "/queue");

        // Prefixo para mensagens que saem do cliente para o servidor (se formos enviar via socket no futuro)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexão inicial (Handshake)
        // O Front vai conectar em: ws://localhost:8081/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // Permite conexões de qualquer lugar (CORS dev)
        //.withSockJS(); // Removido por enquanto para testar com clientes STOMP puros
    }
}