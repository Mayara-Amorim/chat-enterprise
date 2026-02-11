package br.com.dialogosistemas.chat_service.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Necessário para POST via Postman/APIDog funcionar sem token
                .authorizeHttpRequests(authorize -> authorize
                        // PERMITE TUDO (Somente para fase de desenvolvimento inicial)
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
