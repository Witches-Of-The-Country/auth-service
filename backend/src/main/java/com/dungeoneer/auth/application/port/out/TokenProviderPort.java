package com.dungeoneer.auth.application.port.out;

import com.dungeoneer.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface TokenProviderPort {
    String generateToken(User user);
    long getExpirationSeconds();
    Optional<UUID> validateTokenAndGetUserId(String token);
}

