package com.dungeoneer.auth.application.port.out;

import com.dungeoneer.auth.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
