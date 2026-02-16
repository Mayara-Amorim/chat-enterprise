package br.com.dialogosistemas.chat_service.api.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Verifica apenas quando o cliente tenta CONECTAR
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // O cliente deve enviar o token num header customizado, ex: 'X-Authorization'
            // ou 'Authorization' (se o cliente STOMP suportar)
            List<String> authorization = accessor.getNativeHeader("Authorization");

            if (authorization == null || authorization.isEmpty()) {
                throw new IllegalArgumentException("Acesso negado: Token JWT não fornecido no WebSocket.");
            }

            String token = authorization.get(0).replace("Bearer ", "");

            try {
                // Valida o token (se for inválido ou expirado, lança exceção e desconecta)
                Jwt jwt = jwtDecoder.decode(token);

                // Opcional: Validar tenant_id aqui também se quiser ser muito restritiva
                System.out.println("✅ WebSocket Conectado: " + jwt.getSubject());

                // Injeta o usuário na sessão do WebSocket para uso futuro
                accessor.setUser(() -> jwt.getSubject());

            } catch (Exception e) {
                throw new IllegalArgumentException("Acesso negado: Token JWT inválido ou expirado.");
            }
        }
        return message;
    }
}