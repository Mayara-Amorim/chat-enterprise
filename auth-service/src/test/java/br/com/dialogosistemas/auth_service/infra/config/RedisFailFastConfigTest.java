package br.com.dialogosistemas.auth_service.infra.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protege o fail-fast do Redis em prod, no mesmo espirito do OtlpFailFastConfigTest do chat-service.
 *
 * O contador do rate limit de POST /api/auth/token vive no Redis. A base declara a URL COM default,
 * para dev subir sem configurar nada. O profile prod SOBRESCREVE com placeholder SEM default: sem a
 * env var, a resolucao falha e o boot morre.
 *
 * O modo de falha que este teste pega: alguem apaga a linha do prod. O app passa a herdar o default
 * da base, sobe VERDE apontando para localhost:6379, o Cloud Run recusa a conexao, e TODA emissao de
 * token passa sem ser contada — o limitador falha-aberto a cada chamada. A protecao some em silencio;
 * so um WARN por requisicao denuncia, e so se alguem estiver olhando.
 *
 * Esta declaracao anda em par com o --set-secrets do cloudbuild.yaml (SPRING_REDIS_URL=redis-url).
 * Mexer num sem o outro quebra o boot do deploy seguinte — que e ruidoso, e por isso preferivel.
 */
class RedisFailFastConfigTest {

    private static final String REDIS_URL = "spring.data.redis.url";

    @Test
    void deve_exigir_url_sem_default_em_prod() {
        Properties prod = load("application-prod.properties");

        assertEquals("${SPRING_REDIS_URL}", prod.getProperty(REDIS_URL),
                "prod precisa do placeholder SEM default; com default o app sobe apontando para "
                        + "localhost e o rate limit de /api/auth/token falha-aberto em silencio");
    }

    @Test
    void deve_manter_default_dev_safe_na_base() {
        Properties base = load("application.properties");

        String url = base.getProperty(REDIS_URL);
        assertNotNull(url, "a base precisa declarar " + REDIS_URL);
        // "${VAR}" nao tem default; "${VAR:algo}" tem.
        assertTrue(url.startsWith("${") && url.endsWith("}") && url.contains(":"),
                "a base precisa de default, senao ninguem sobe o auth-service sem configurar Redis: " + url);
    }

    /**
     * Le do DISCO, nao do classpath. O auth-service tem src/test/resources/application.properties,
     * e target/test-classes vem antes de target/classes — entao getResourceAsStream("/application.
     * properties") devolveria o arquivo de TESTE, e o teste passaria a validar a si mesmo em vez da
     * configuracao de producao. (O OtlpFailFastConfigTest do chat-service usa classpath sem
     * problema porque la nao existe properties de teste para sombrear.)
     */
    private static Properties load(String nomeArquivo) {
        Path caminho = Path.of("src", "main", "resources", nomeArquivo);
        assertTrue(Files.exists(caminho), "arquivo nao encontrado: " + caminho.toAbsolutePath());
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(caminho)) {
            properties.load(in);
        }
        catch (IOException e) {
            throw new IllegalStateException("falha lendo " + caminho, e);
        }
        return properties;
    }
}
