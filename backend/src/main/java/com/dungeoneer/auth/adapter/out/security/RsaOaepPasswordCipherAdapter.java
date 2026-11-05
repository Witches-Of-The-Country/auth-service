package com.dungeoneer.auth.adapter.out.security;

import com.dungeoneer.auth.application.port.out.PasswordCipherPort;
import com.dungeoneer.auth.domain.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaOaepPasswordCipherAdapter implements PasswordCipherPort {

    private static final Logger log = LoggerFactory.getLogger(RsaOaepPasswordCipherAdapter.class);
    private static final String RSA_PREFIX = "rsa:";

    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final String publicKeyBase64;

    public RsaOaepPasswordCipherAdapter(
            @Value("${auth.rsa.public-key:}") String configuredPublicKey,
            @Value("${auth.rsa.private-key:}") String configuredPrivateKey
    ) {
        KeyPair keyPair = resolveKeyPair(configuredPublicKey, configuredPrivateKey);
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
        log.info("RsaOaepPasswordCipherAdapter initialized with RSA-2048 key pair.");
    }

    public RsaOaepPasswordCipherAdapter(KeyPair keyPair) {
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
    }

    @Override
    public String getPublicKeyBase64() {
        return this.publicKeyBase64;
    }

    @Override
    public String decryptPassword(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        if (input.startsWith(RSA_PREFIX)) {
            String base64Payload = input.substring(RSA_PREFIX.length()).trim();
            return decryptCiphertext(base64Payload);
        }

        // If not prefixed, return as fallback (e.g. for plaintext tests)
        return input;
    }

    private String decryptCiphertext(String base64Payload) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(base64Payload);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    new MGF1ParameterSpec("SHA-256"),
                    PSource.PSpecified.DEFAULT
            );
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey, oaepParams);
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Failed to decrypt RSA-OAEP password payload: {}", ex.getMessage());
            throw new InvalidCredentialsException("Invalid encrypted credentials");
        }
    }

    private static KeyPair resolveKeyPair(String pubKeyStr, String privKeyStr) {
        if (pubKeyStr != null && !pubKeyStr.isBlank() && privKeyStr != null && !privKeyStr.isBlank()) {
            try {
                KeyFactory kf = KeyFactory.getInstance("RSA");
                byte[] pubBytes = Base64.getDecoder().decode(cleanKey(pubKeyStr));
                byte[] privBytes = Base64.getDecoder().decode(cleanKey(privKeyStr));
                PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
                PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                return new KeyPair(pub, priv);
            } catch (Exception ex) {
                log.warn("Failed to parse configured RSA keys, falling back to dynamic generation: {}", ex.getMessage());
            }
        }

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not supported in this runtime environment", e);
        }
    }

    private static String cleanKey(String key) {
        return key.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s+", "");
    }
}
