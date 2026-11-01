package com.dungeoneer.auth.adapter.out.security;

import com.dungeoneer.auth.application.port.out.TokenProviderPort;
import com.dungeoneer.auth.domain.model.Role;
import com.dungeoneer.auth.domain.model.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final String secret;
    private final long expirationSeconds;
    private JWSSigner signer;

    public JwtTokenProviderAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) long");
        }
        try {
            this.signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize HMAC signer", e);
        }
    }

    @Override
    public String generateToken(User user) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + (expirationSeconds * 1000));

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .claim("email", user.getEmail())
                    .claim("nickname", user.getNickname())
                    .claim("roles", user.getRoles().stream().map(Role::name).collect(Collectors.toList()))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    @Override
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    @Override
    public Optional<UUID> validateTokenAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(secret.getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                return Optional.empty();
            }
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                return Optional.empty();
            }
            String sub = signedJWT.getJWTClaimsSet().getSubject();
            if (sub == null || sub.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(sub));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
