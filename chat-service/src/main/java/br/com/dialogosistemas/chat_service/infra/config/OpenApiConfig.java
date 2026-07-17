package br.com.dialogosistemas.chat_service.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat Service API")
                        .description("Servico de chat em tempo real multi-tenant. "
                                + "Suporta conversas individuais e em grupo, mensagens, historico e indicadores de digitacao via WebSocket.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Dialogo Sistemas")
                                .email("contato@dialogosistemas.com.br")))
                .addSecurityItem(new SecurityRequirement().addList("bearerJwt"))
                .components(new Components()
                        .addSecuritySchemes("bearerJwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT emitido pelo auth-service. Claims: sub (userId), tenant_id, name, role.")));
    }
}
