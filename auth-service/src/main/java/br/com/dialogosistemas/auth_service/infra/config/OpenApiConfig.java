package br.com.dialogosistemas.auth_service.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
                        .title("Auth Service API")
                        .description("Servico de autenticacao para a plataforma de chat multi-tenant. "
                                + "Gerencia tenants, usuarios e emissao de tokens JWT (RS256).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Dialogo Sistemas")
                                .email("contato@dialogosistemas.com.br")))
                .components(new Components()
                        .addSecuritySchemes("masterKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Master-Key")
                                .description("Chave master para criacao de tenants. Definida via variavel de ambiente AUTH_MASTER_KEY."))
                        .addSecuritySchemes("tenantApiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API key do tenant. Gerada ao criar o tenant e retornada uma unica vez.")));
    }
}
