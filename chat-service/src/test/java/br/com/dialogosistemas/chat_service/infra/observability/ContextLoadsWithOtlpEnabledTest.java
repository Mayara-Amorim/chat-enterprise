package br.com.dialogosistemas.chat_service.infra.observability;

import br.com.dialogosistemas.chat_service.ChatServiceApplication;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Protege a receita OTLP (spec Fatia A secao 3.1): micrometer-registry-otlp +
 * spring-boot-opentelemetry + pin protobuf-java 4.32.0. Sobe o contexto com o export OTLP
 * habilitado e afirma que o bean do OtlpMeterRegistry foi CONSTRUIDO.
 *
 * Se alguem remover o pin do protobuf (ou o modulo opentelemetry), o skew volta e o registry
 * estoura na construcao (era o cenario do spike) -> este teste quebra no CI, nao em prod.
 *
 * URL inalcancavel + step=1h evitam qualquer tentativa de publish durante o teste; falha de
 * publish e logada como WARN, nao lancada. O que importa aqui e a construcao do registry.
 */
@SpringBootTest(classes = ChatServiceApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:otlpcontext;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.listener.auto-startup=false",
        "management.otlp.metrics.export.enabled=true",
        "management.otlp.metrics.export.url=http://localhost:1/v1/metrics",
        "management.otlp.metrics.export.step=1h"
})
class ContextLoadsWithOtlpEnabledTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void otlpRegistryIsAvailable() {
        assertFalse(
                context.getBeansOfType(OtlpMeterRegistry.class).isEmpty(),
                "OtlpMeterRegistry ausente: a receita OTLP quebrou (pin do protobuf ou modulo opentelemetry?)"
        );
    }
}
