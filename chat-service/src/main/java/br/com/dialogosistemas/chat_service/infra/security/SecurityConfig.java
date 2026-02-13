package br.com.dialogosistemas.chat_service.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Chave secreta PROVISÓRIA para desenvolvimento (deve ter 32 caracteres ou mais para HS256)
    // No futuro, isso será substituído pela Chave Pública RSA do Auth Service
    @Value("${jwt.secret:MinhaChaveSecretaMuitoSeguraDeDesenvolvimento123}")
    private String jwtSecret;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            // Libera o handshake do WebSocket e o Swagger (se tiver)
                            .requestMatchers("/ws/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(Customizer.withDefaults())
                    );

            return http.build();
        }

        /**
         * Bean Decodificador de JWT.
         * Como não temos o Auth Service rodando ainda para fornecer a URL do JWK Set,
         * configuramos um decoder local que valida a assinatura usando uma chave simétrica (HMAC).
         */
        @Bean
        public JwtDecoder jwtDecoder() {
            // Algoritmo HS256 requer uma chave de pelo menos 256 bits (32 bytes)
            byte[] keyBytes = jwtSecret.getBytes();
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

            return NimbusJwtDecoder.withSecretKey(secretKey).build();
        }
}
