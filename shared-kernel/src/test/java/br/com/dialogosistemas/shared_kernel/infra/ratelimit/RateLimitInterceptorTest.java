package br.com.dialogosistemas.shared_kernel.infra.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.method.HandlerMethod;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valores;
    private RateLimitInterceptor interceptor;
    private HttpServletRequest requisicao;
    private HttpServletResponse resposta;

    /** Alvo das anotacoes: o interceptor le a @RateLimit do metodo via HandlerMethod. */
    static class ControllerFalso {
        @RateLimit(limit = 3, windowSeconds = 60)
        public void limitado() {}

        public void semLimite() {}
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valores = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valores);
        interceptor = new RateLimitInterceptor(redis);

        requisicao = mock(HttpServletRequest.class);
        resposta = mock(HttpServletResponse.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("usuario-1");
        when(requisicao.getUserPrincipal()).thenReturn(principal);
    }

    private HandlerMethod handler(String metodo) throws Exception {
        return new HandlerMethod(new ControllerFalso(), ControllerFalso.class.getMethod(metodo));
    }

    @Test
    void deve_liberar_quando_dentro_do_limite() throws Exception {
        when(valores.increment(anyString())).thenReturn(3L);

        assertTrue(interceptor.preHandle(requisicao, resposta, handler("limitado")));
        verify(resposta, never()).setStatus(429);
    }

    @Test
    void deve_bloquear_com_429_e_retry_after_quando_estoura_o_limite() throws Exception {
        when(valores.increment(anyString())).thenReturn(4L);
        when(redis.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(42L);

        assertFalse(interceptor.preHandle(requisicao, resposta, handler("limitado")));
        verify(resposta).setStatus(429);
        verify(resposta).setHeader("Retry-After", "42");
    }

    @Test
    void deve_usar_a_janela_cheia_no_retry_after_quando_o_ttl_nao_existe() throws Exception {
        // -2 = a chave expirou entre o INCR e a leitura do TTL. Retry-After 0 convidaria o
        // cliente a repetir imediatamente, entao a janela cheia e a resposta honesta.
        when(valores.increment(anyString())).thenReturn(9L);
        when(redis.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-2L);

        interceptor.preHandle(requisicao, resposta, handler("limitado"));

        verify(resposta).setHeader("Retry-After", "60");
    }

    @Test
    void deve_definir_a_expiracao_apenas_na_primeira_chamada_da_janela() throws Exception {
        when(valores.increment(anyString())).thenReturn(1L);
        interceptor.preHandle(requisicao, resposta, handler("limitado"));
        verify(redis).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));

        when(valores.increment(anyString())).thenReturn(2L);
        interceptor.preHandle(requisicao, resposta, handler("limitado"));
        // continua sendo uma unica chamada, a da primeira requisicao
        verify(redis).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void deve_liberar_quando_o_redis_falha() throws Exception {
        // FAIL-OPEN: limitador indisponivel nao pode derrubar a API que ele protege.
        when(valores.increment(anyString())).thenThrow(new RuntimeException("redis fora do ar"));

        assertTrue(interceptor.preHandle(requisicao, resposta, handler("limitado")));
        verify(resposta, never()).setStatus(429);
    }

    @Test
    void deve_ignorar_endpoint_sem_anotacao() throws Exception {
        assertTrue(interceptor.preHandle(requisicao, resposta, handler("semLimite")));
        verify(redis, never()).opsForValue();
    }

    @Test
    void deve_separar_a_cota_por_usuario() throws Exception {
        when(valores.increment(anyString())).thenReturn(1L);
        interceptor.preHandle(requisicao, resposta, handler("limitado"));

        org.mockito.ArgumentCaptor<String> chave = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valores).increment(chave.capture());
        assertTrue(chave.getValue().endsWith(":u:usuario-1"),
                "a chave deve conter o identificador do chamador, senao a cota seria global: " + chave.getValue());
    }

    @Test
    void deve_cair_para_ip_quando_nao_ha_usuario_autenticado() throws Exception {
        when(requisicao.getUserPrincipal()).thenReturn(null);
        when(requisicao.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7, 10.0.0.1");
        when(valores.increment(anyString())).thenReturn(1L);

        interceptor.preHandle(requisicao, resposta, handler("limitado"));

        org.mockito.ArgumentCaptor<String> chave = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valores).increment(chave.capture());
        // atras do Cloud Run o getRemoteAddr() e o proxy; o cliente real e o primeiro do XFF
        assertEquals(true, chave.getValue().endsWith(":ip:203.0.113.7"), chave.getValue());
    }
}
