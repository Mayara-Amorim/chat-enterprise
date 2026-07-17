package br.com.dialogosistemas.auth_service.infra.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public RsaKeyProvider(
            @Value("${jwt.public-key-path:#{null}}") String publicKeyPath,
            @Value("${jwt.private-key-path:#{null}}") String privateKeyPath,
            @Value("${RSA_PRIVATE_KEY_BASE64:#{null}}") String privateKeyBase64) {

        if (privateKeyBase64 != null && !privateKeyBase64.isBlank()) {
            log.info("Carregando chave RSA de variavel de ambiente (Base64)");
            this.privateKey = decodePrivateKeyFromBase64(privateKeyBase64);
            this.publicKey = derivePublicKey(this.privateKey);
        } else if (publicKeyPath != null && privateKeyPath != null) {
            log.info("Carregando chaves RSA de arquivos: {}, {}", publicKeyPath, privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
            this.privateKey = loadPrivateKey(privateKeyPath);
        } else {
            log.info("Gerando par de chaves RSA em memoria (modo dev)");
            KeyPair keyPair = generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        }
        this.keyId = "auth-service-key-1";
    }

    public RSAPublicKey getPublicKey() { return publicKey; }
    public RSAPrivateKey getPrivateKey() { return privateKey; }
    public String getKeyId() { return keyId; }

    private RSAPrivateKey decodePrivateKeyFromBase64(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode RSA private key from Base64", e);
        }
    }

    private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) privateKey;
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
            return (RSAPublicKey) factory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive public key from private key", e);
        }
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA not available", e);
        }
    }

    private RSAPublicKey loadPublicKey(String path) {
        try {
            String pem = Files.readString(Path.of(path));
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key from " + path, e);
        }
    }

    private RSAPrivateKey loadPrivateKey(String path) {
        try {
            String pem = Files.readString(Path.of(path));
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key from " + path, e);
        }
    }
}
