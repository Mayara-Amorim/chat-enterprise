package br.com.dialogosistemas.auth_service.infra.security;

import br.com.dialogosistemas.auth_service.domain.model.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenGenerator {

    private final RsaKeyProvider rsaKeyProvider;
    private final long expirationSeconds;

    public JwtTokenGenerator(RsaKeyProvider rsaKeyProvider,
                             @Value("${jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().value().toString())
                .claim("tenant_id", user.getTenantId().value().toString())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKeyProvider.getKeyId())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);

        try {
            signedJWT.sign(new RSASSASigner(rsaKeyProvider.getPrivateKey()));
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }

        return signedJWT.serialize();
    }

    public long getExpirationSeconds() { return expirationSeconds; }
}
