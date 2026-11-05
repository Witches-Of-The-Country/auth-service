package com.dungeoneer.auth.adapter.out.security;

import com.dungeoneer.auth.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaOaepPasswordCipherAdapterTest {

    private RsaOaepPasswordCipherAdapter cipherAdapter;

    @BeforeEach
    void setUp() {
        cipherAdapter = new RsaOaepPasswordCipherAdapter("", "");
    }

    @Test
    @DisplayName("Should export valid non-empty RSA public key in Base64 SPKI format")
    void shouldExportPublicKey() {
        String pubKeyBase64 = cipherAdapter.getPublicKeyBase64();
        assertNotNull(pubKeyBase64);
        assertFalse(pubKeyBase64.isBlank());

        assertDoesNotThrow(() -> {
            byte[] bytes = Base64.getDecoder().decode(pubKeyBase64);
            KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        });
    }

    @Test
    @DisplayName("Should successfully decrypt RSA-OAEP encrypted password prefixed with rsa:")
    void shouldDecryptRsaOaepEncryptedPassword() throws Exception {
        String originalPassword = "SuperSecretPassword123!";

        // Encrypt with the adapter's public key using standard RSA-OAEP SHA-256
        byte[] pubKeyBytes = Base64.getDecoder().decode(cipherAdapter.getPublicKeyBase64());
        PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubKeyBytes));

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                new MGF1ParameterSpec("SHA-256"),
                PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.ENCRYPT_MODE, pubKey, oaepParams);
        byte[] encryptedBytes = cipher.doFinal(originalPassword.getBytes(StandardCharsets.UTF_8));
        String encryptedPayload = "rsa:" + Base64.getEncoder().encodeToString(encryptedBytes);

        String decrypted = cipherAdapter.decryptPassword(encryptedPayload);
        assertEquals(originalPassword, decrypted);
    }

    @Test
    @DisplayName("Should return raw password if not prefixed with rsa: (fallback mode)")
    void shouldReturnRawPasswordOnFallback() {
        String raw = "plainPassword123";
        assertEquals(raw, cipherAdapter.decryptPassword(raw));
        assertNull(cipherAdapter.decryptPassword(null));
        assertEquals("", cipherAdapter.decryptPassword(""));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when rsa: ciphertext is corrupt or invalid")
    void shouldThrowExceptionOnCorruptCiphertext() {
        assertThrows(InvalidCredentialsException.class, () ->
                cipherAdapter.decryptPassword("rsa:invalid-base-64-corrupt-data!!!")
        );
    }
}
