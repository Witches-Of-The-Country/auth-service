package com.dungeoneer.auth.application.port.out;

import com.dungeoneer.auth.domain.model.User;

public interface TokenProviderPort {
    String generateToken(User user);
    long getExpirationSeconds();
}
