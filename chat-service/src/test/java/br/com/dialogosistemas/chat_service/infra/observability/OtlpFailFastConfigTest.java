package br.com.dialogosistemas.chat_service.infra.observability;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protege o fail-fast de OTLP em prod (spec Fatia A secao 3.3, criterio de aceite 7).
 *
 * A base (application.properties) declara url/Authorization COM default, para que dev consiga
 * apontar para o Grafana Cloud ao validar sem quebrar o boot de quem nao configurou nada.
 * O profile prod SOBRESCREVE as duas com placeholder SEM default: faltando a env var, a resolucao
 * do placeholder falha e o boot morre — que e o comportamento desejado.
 *
 * O modo de falha que este teste pega: alguem apaga (ou poe default n) as linhas do prod. O app
 * passa a herdar o default da base, sobe VERDE apontando para localhost:4318 e nao exporta metrica
 * nenhuma. Sem este teste, isso so aparece quando alguem estranhar o dashboard vazio.
 *
 * Nao subimos contexto com profile prod de proposito: isso exigiria Postgres/Kafka reais e falharia
 * por motivos alheios ao que esta sendo verificado. A resolucao de placeholder em si e garantia do
 * Spring; o que precisa de protecao e a NOSSA declaracao.
 */
class OtlpFailFastConfigTest {

    private static final String URL_KEY = "management.otlp.metrics.export.url";
    private static final String AUTH_KEY = "management.otlp.metrics.export.headers.Authorization";

    @Test
    void prodOverridesUrlAndCredentialWithoutDefaults() {
        Properties prod = load("/application-prod.properties");

        assertEquals("${OTLP_METRICS_URL}", prod.getProperty(URL_KEY),
                "prod precisa sobrescrever a url com placeholder SEM default, senao nao ha fail-fast");
        assertEquals("${OTLP_AUTH_HEADER}", prod.getProperty(AUTH_KEY),
                "prod precisa sobrescrever a credencial com placeholder SEM default (token rotacionado "
                        + "e a falha silenciosa mais provavel — spec 3.3)");
        assertEquals("true", prod.getProperty("management.otlp.metrics.export.enabled"),
                "prod exporta por padrao; default false em prod e app verde sem metrica nenhuma");
    }

    @Test
    void baseKeepsDevSafeDefaults() {
        Properties base = load("/application.properties");

        assertTrue(hasDefault(base.getProperty(URL_KEY)),
                "a base precisa de default na url, senao dev nao sobe sem configurar OTLP");
        assertTrue(hasDefault(base.getProperty(AUTH_KEY)),
                "a base precisa de default na credencial, senao dev nao sobe sem configurar OTLP");
        assertEquals("${OTLP_ENABLED:false}", base.getProperty("management.otlp.metrics.export.enabled"),
                "export desligado por padrao fora de prod (spec 3.3)");
    }

    @Test
    void stepAndTemporalityArePinned() {
        Properties base = load("/application.properties");

        // step=30s serve ao rate([5m]) e a janela de 90s do gauge (spec 2.2). step=1m default
        // deixaria o painel de mensagens/s vazio.
        assertEquals("30s", base.getProperty("management.otlp.metrics.export.step"));
        // rate() exige cumulative; OTEL_..._TEMPORALITY_PREFERENCE pode virar delta e quebrar calado.
        assertEquals("cumulative", base.getProperty("management.otlp.metrics.export.aggregation-temporality"));
    }

    // "${VAR}" nao tem default; "${VAR:algo}" e "${VAR:}" tem.
    private static boolean hasDefault(String placeholder) {
        assertNotNull(placeholder, "propriedade ausente na base");
        return placeholder.startsWith("${") && placeholder.endsWith("}") && placeholder.contains(":");
    }

    private static Properties load(String classpathResource) {
        Properties properties = new Properties();
        try (InputStream in = OtlpFailFastConfigTest.class.getResourceAsStream(classpathResource)) {
            assertNotNull(in, "recurso nao encontrado no classpath: " + classpathResource);
            properties.load(in);
        }
        catch (IOException e) {
            throw new IllegalStateException("falha lendo " + classpathResource, e);
        }
        return properties;
    }
}
