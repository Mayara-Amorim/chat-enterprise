package br.com.dialogosistemas.shared_kernel.infra.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita a frequencia de chamadas a um endpoint. Ao estourar, a API responde
 * 429 com o header Retry-After (segundos ate liberar).
 *
 * Os valores sao constantes de compilacao: mudar um limite exige deploy. E aceito de proposito —
 * limite de taxa nao e um botao que se gira toda semana, e deixa-lo em propriedade externa
 * custaria uma fonte de configuracao a mais para um ganho que ninguem pediu.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Quantas chamadas sao permitidas dentro da janela. */
    int limit();

    /** Tamanho da janela, em segundos. */
    int windowSeconds() default 60;

    /** Como identificar quem esta chamando. */
    Scope scope() default Scope.USER;

    enum Scope {
        /** Subject do JWT. Para endpoints atras de autenticacao de usuario. */
        USER,
        /** Tenant resolvido pelo filtro de API key. Para endpoints onde ainda nao ha usuario. */
        API_KEY,
        /** IP de origem. Ultimo recurso, para endpoints publicos. */
        IP
    }
}
