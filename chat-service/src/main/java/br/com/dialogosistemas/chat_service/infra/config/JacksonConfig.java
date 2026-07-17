package br.com.dialogosistemas.chat_service.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary // Garante que este seja o ObjectMapper principal do sistema
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                // 1. Adiciona suporte ao 'Instant', 'LocalDate', etc.
                .addModule(new JavaTimeModule())

                // 2. Desativa datas como números (timestamps), forçando ISO-8601 (Strings)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

                .build();
    }
}