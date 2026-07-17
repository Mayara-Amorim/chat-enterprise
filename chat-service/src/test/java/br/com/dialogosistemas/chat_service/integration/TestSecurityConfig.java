package br.com.dialogosistemas.chat_service.integration;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@TestConfiguration
public class TestSecurityConfig {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair for tests", e);
        }
    }

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
    }

    public static String generateJwt(UUID subject, UUID tenantId) {
        try {
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject.toString())
                    .claim("tenant_id", tenantId.toString())
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                    claims
            );
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test JWT", e);
        }
    }
}
