package com.dungeoneer.auth.adapter.in.web.dto;

public record PublicKeyResponse(
        String publicKey,
        String algorithm,
        String hash
) {
    public static PublicKeyResponse rsaOaep(String publicKeyBase64) {
        return new PublicKeyResponse(publicKeyBase64, "RSA-OAEP", "SHA-256");
    }
}
