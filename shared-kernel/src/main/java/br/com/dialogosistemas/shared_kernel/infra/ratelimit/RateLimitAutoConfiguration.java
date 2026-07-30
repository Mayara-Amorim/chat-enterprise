package br.com.dialogosistemas.shared_kernel.infra.ratelimit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuracao em vez de @Component: o @SpringBootApplication de cada servico varre o
 * proprio pacote (br.com.dialogosistemas.chat_service / auth_service), nunca o do shared-kernel.
 * Sem isto seria preciso lembrar de um @ComponentScan extra em cada servico — e esquecer disso
 * desligaria o limitador em silencio.
 *
 * A propriedade ratelimit.enabled existe como chave de desligamento: se o limitador comecar a
 * barrar trafego legitimo em producao, da para desliga-lo sem reverter codigo. Os testes de
 * integracao a usam para nao depender de um Redis de verdade.
 */
// Sem 'after = RedisAutoConfiguration.class': no Spring Boot 4 essa classe mudou de modulo, e a
// ordenacao nao era necessaria — nao ha @ConditionalOnBean aqui, e a injecao do StringRedisTemplate
// e resolvida por dependencia de bean, nao por ordem de auto-configuracao.
@AutoConfiguration
@ConditionalOnClass({StringRedisTemplate.class, WebMvcConfigurer.class})
@ConditionalOnProperty(name = "ratelimit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor(StringRedisTemplate redis) {
        return new RateLimitInterceptor(redis);
    }

    @Bean
    public WebMvcConfigurer rateLimitWebMvcConfigurer(RateLimitInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
