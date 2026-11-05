package com.dungeoneer.auth.application.port.out;

public interface PasswordCipherPort {

    /**
     * Returns the RSA-2048 public key in SPKI Base64 format.
     */
    String getPublicKeyBase64();

    /**
     * Decrypts an incoming encrypted password ciphertext (e.g. prefixed with "rsa:<base64>").
     * If the input is not encrypted, returns the raw password as a fallback.
     */
    String decryptPassword(String encryptedPassword);
}
