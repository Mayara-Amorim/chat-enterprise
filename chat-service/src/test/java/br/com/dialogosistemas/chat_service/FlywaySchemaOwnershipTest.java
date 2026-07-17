package br.com.dialogosistemas.chat_service;

import br.com.dialogosistemas.chat_service.infra.persistence.repository.MessageJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = ChatServiceApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:flywayschema;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.kafka.listener.auto-startup=false"
        }
)
class FlywaySchemaOwnershipTest {

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Test
    void contextLoadsWhenFlywayOwnsTheSchema() {
        assertNotNull(messageJpaRepository);
    }
}
