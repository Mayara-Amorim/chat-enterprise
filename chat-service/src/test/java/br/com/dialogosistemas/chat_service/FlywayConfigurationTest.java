package br.com.dialogosistemas.chat_service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlywayConfigurationTest {

    @Test
    void applicationPropertiesEnableFlywayWithBaselineForExistingSchema() throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(inputStream);
            properties.load(inputStream);
        }

        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", properties.getProperty("spring.flyway.enabled"));
        assertEquals("true", properties.getProperty("spring.flyway.baseline-on-migrate"));
        assertEquals("classpath:db/migration/{vendor}", properties.getProperty("spring.flyway.locations"));
    }
}
