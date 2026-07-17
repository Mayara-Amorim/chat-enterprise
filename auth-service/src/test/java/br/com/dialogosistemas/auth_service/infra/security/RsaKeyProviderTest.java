package br.com.dialogosistemas.auth_service.infra.security;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyProviderTest {

    @Test
    void deve_gerar_par_de_chaves_em_memoria() {
        RsaKeyProvider provider = new RsaKeyProvider(null, null, null);
        assertNotNull(provider.getPublicKey());
        assertNotNull(provider.getPrivateKey());
        assertEquals("RSA", provider.getPublicKey().getAlgorithm());
        assertEquals("RSA", provider.getPrivateKey().getAlgorithm());
    }

    @Test
    void deve_gerar_kid_consistente() {
        RsaKeyProvider provider = new RsaKeyProvider(null, null, null);
        assertNotNull(provider.getKeyId());
        assertFalse(provider.getKeyId().isBlank());
    }

    @Test
    void deve_carregar_chave_privada_de_base64() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        RsaKeyProvider provider = new RsaKeyProvider(null, null, privateKeyBase64);

        assertNotNull(provider.getPrivateKey());
        assertNotNull(provider.getPublicKey());
        assertEquals("RSA", provider.getPrivateKey().getAlgorithm());
        assertEquals("RSA", provider.getPublicKey().getAlgorithm());
    }
}
