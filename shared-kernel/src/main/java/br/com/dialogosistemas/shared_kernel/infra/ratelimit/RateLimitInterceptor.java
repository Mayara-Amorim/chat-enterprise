package br.com.dialogosistemas.shared_kernel.infra.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

/**
 * Janela fixa por contador no Redis: INCR na primeira chamada define a chave, EXPIRE fixa a
 * janela, e o TTL restante vira o Retry-After. Nao e token bucket — janela fixa deixa passar
 * ate 2x o limite na virada de duas janelas adjacentes. Aceito: o objetivo aqui e conter abuso
 * e custo, nao dosar trafego com precisao.
 *
 * FAIL-OPEN: se o Redis estiver inacessivel, a requisicao PASSA e fica um WARN no log. Limitador
 * e protecao de custo, nao de correcao — derrubar a emissao de token porque o Redis piscou seria
 * pior que o abuso que ele evita. O WARN e a alca para alerta por log.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final String PREFIXO = "ratelimit:";

    private final StringRedisTemplate redis;

    public RateLimitInterceptor(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        RateLimit regra = metodo.getMethodAnnotation(RateLimit.class);
        if (regra == null) {
            return true;
        }

        String chave = PREFIXO + metodo.getBeanType().getSimpleName() + "." + metodo.getMethod().getName()
                + ":" + identificar(request, regra.scope());

        long chamadas;
        try {
            Long resultado = redis.opsForValue().increment(chave);
            chamadas = resultado == null ? 0L : resultado;
            if (chamadas == 1L) {
                redis.expire(chave, regra.windowSeconds(), TimeUnit.SECONDS);
            }
        } catch (RuntimeException e) {
            log.warn("rate limit indisponivel, liberando a requisicao (fail-open): chave={}", chave, e);
            return true;
        }

        if (chamadas <= regra.limit()) {
            return true;
        }

        long restante = restanteEmSegundos(chave, regra.windowSeconds());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(restante));
        log.info("rate limit estourado: chave={} chamadas={} limite={}", chave, chamadas, regra.limit());
        return false;
    }

    /**
     * Sem depender do spring-security: o resource server preenche o Principal do servlet com o
     * subject do JWT, e o filtro de API key do auth-service poe o tenant no atributo da requisicao.
     */
    private String identificar(HttpServletRequest request, RateLimit.Scope escopo) {
        return switch (escopo) {
            case USER -> {
                Principal principal = request.getUserPrincipal();
                // Sem principal, cai para IP em vez de jogar todo mundo no mesmo balde "null" —
                // senao um anonimo consumiria a cota de todos os outros anonimos.
                yield principal != null ? "u:" + principal.getName() : ip(request);
            }
            case API_KEY -> {
                Object tenant = request.getAttribute("tenantId");
                yield tenant != null ? "t:" + tenant : ip(request);
            }
            case IP -> ip(request);
        };
    }

    /** Atras do Cloud Run o getRemoteAddr() e o proxy; o cliente real e o primeiro do XFF. */
    private String ip(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return "ip:" + encaminhado.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private long restanteEmSegundos(String chave, int janela) {
        try {
            Long ttl = redis.getExpire(chave, TimeUnit.SECONDS);
            // -1 = sem TTL, -2 = chave sumiu entre o INCR e agora. Nos dois casos a janela cheia
            // e a resposta honesta, e nunca 0 (que convidaria o cliente a repetir na hora).
            if (ttl != null && ttl > 0) {
                return ttl;
            }
        } catch (RuntimeException e) {
            log.warn("falha ao ler TTL para o Retry-After, usando a janela cheia: chave={}", chave, e);
        }
        return janela;
    }
}
